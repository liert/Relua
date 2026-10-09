package com.github.relua.gui.workspace;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 二进制紧凑压缩持久化引擎，负责将工作区或单文件元数据序列化为 GZIP 二进制数据库（.relua）并提供极速反序列化
 */
public final class BinaryPersistenceService {

    /**
     * Relua 二进制格式魔数：0x52454C55 (ASCII 'RELU')
     */
    public static final int MAGIC_NUMBER = 0x52454C55;

    /**
     * 当前格式主版本号
     */
    public static final short FORMAT_VERSION = 1;

    private BinaryPersistenceService() {
    }

    /**
     * 将工作区数据持久化写入指定的二进制数据库文件（采用 GZIP 流式高压缩）
     * @param targetFile 目标 .relua 文件
     * @param data 工作区数据
     * @throws IOException IO 异常
     */
    public static void save(File targetFile, WorkspaceData data) throws IOException {
        if (targetFile == null || data == null) {
            return;
        }

        // 确保父级目录存在（如 .relua 目录）
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 使用临时文件写入再原子重命名，防止写入中途断电导致文件损坏
        File tempFile = new File(targetFile.getAbsolutePath() + ".tmp");

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(tempFile))))) {

            // 1. 魔数与版本区 (Header)
            out.writeInt(MAGIC_NUMBER);
            out.writeShort(FORMAT_VERSION);
            out.writeLong(System.currentTimeMillis());

            // 2. 会话状态区 (Session)
            out.writeUTF(data.getActiveFilePath() != null ? data.getActiveFilePath() : "");

            List<String> openedTabs = data.getOpenedTabPaths();
            out.writeInt(openedTabs.size());
            for (String tabPath : openedTabs) {
                out.writeUTF(tabPath != null ? tabPath : "");
            }

            // 3. 修改文件增量区 (Modified Files Delta)
            // 稀疏过滤：只写入实际有修改的文件
            Map<String, FileMetadata> modifiedMap = data.getModifiedFiles();
            List<FileMetadata> validList = new ArrayList<>();
            for (FileMetadata meta : modifiedMap.values()) {
                if (meta != null && meta.hasModifications()) {
                    validList.add(meta);
                }
            }

            out.writeInt(validList.size());
            for (FileMetadata meta : validList) {
                // 文件相对路径
                out.writeUTF(meta.getRelativePath() != null ? meta.getRelativePath() : "");
                // SHA-256 指纹
                out.writeUTF(meta.getFileSha256() != null ? meta.getFileSha256() : "");
                // 最后修改时间
                out.writeLong(meta.getLastModifiedTime());

                // 重命名变量字典
                Map<String, String> renamedVars = meta.getRenamedVariables();
                out.writeInt(renamedVars.size());
                for (Map.Entry<String, String> entry : renamedVars.entrySet()) {
                    out.writeUTF(entry.getKey());
                    out.writeUTF(entry.getValue());
                }

                // 用户编辑覆盖代码文本（采用 4 字节 int 长度支持超长代码，防 64KB writeUTF 溢出）
                String editedText = meta.getEditedText();
                if (editedText != null && !editedText.isEmpty()) {
                    out.writeBoolean(true);
                    writeLongString(out, editedText);
                } else {
                    out.writeBoolean(false);
                }
            }
            out.flush();
        }

        // 原子替换正式文件
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (!tempFile.renameTo(targetFile)) {
            // 在某些 Windows 文件锁情况下重命名失败时，尝试普通覆盖
            try (FileInputStream in = new FileInputStream(tempFile);
                 FileOutputStream fos = new FileOutputStream(targetFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                }
            }
            tempFile.delete();
        }
    }

    /**
     * 从指定的二进制数据库文件中反序列化加载工作区数据
     * @param sourceFile 源 .relua 文件
     * @return 解析后的工作区数据，若文件不存在或为空则返回 null
     * @throws IOException IO 异常或格式损坏
     */
    public static WorkspaceData load(File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.exists() || sourceFile.length() == 0) {
            return null;
        }

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new GZIPInputStream(new FileInputStream(sourceFile))))) {

            // 1. 校验魔数
            int magic = in.readInt();
            if (magic != MAGIC_NUMBER) {
                throw new IOException("非法 Relua 二进制数据库文件，魔数不匹配 (expected 0x52454C55, got " + Integer.toHexString(magic) + ")");
            }

            short version = in.readShort();
            if (version > FORMAT_VERSION) {
                throw new IOException("不支持的 Relua 数据库版本 (version=" + version + ")，请升级 Relua 客户端");
            }

            long saveTimestamp = in.readLong();

            WorkspaceData data = new WorkspaceData();
            data.setVersion(version);
            data.setSaveTime(saveTimestamp);

            // 2. 读取会话状态区
            String activeFilePath = in.readUTF();
            data.setActiveFilePath(activeFilePath.isEmpty() ? null : activeFilePath);

            int tabCount = in.readInt();
            List<String> openedTabs = new ArrayList<>(tabCount);
            for (int i = 0; i < tabCount; i++) {
                String tab = in.readUTF();
                if (!tab.isEmpty()) {
                    openedTabs.add(tab);
                }
            }
            data.setOpenedTabPaths(openedTabs);

            // 3. 读取修改文件增量区
            int modifiedCount = in.readInt();
            Map<String, FileMetadata> modifiedMap = new LinkedHashMap<>(modifiedCount);

            for (int i = 0; i < modifiedCount; i++) {
                String relPath = in.readUTF();
                String sha256 = in.readUTF();
                long lastModifiedTime = in.readLong();

                // 重命名变量表
                int varCount = in.readInt();
                Map<String, String> renamedVars = new LinkedHashMap<>(varCount);
                for (int v = 0; v < varCount; v++) {
                    String oldName = in.readUTF();
                    String newName = in.readUTF();
                    renamedVars.put(oldName, newName);
                }

                // 编辑代码
                boolean hasEdited = in.readBoolean();
                String editedText = hasEdited ? readLongString(in) : null;

                FileMetadata meta = new FileMetadata(relPath, sha256.isEmpty() ? null : sha256, renamedVars, editedText);
                meta.setLastModifiedTime(lastModifiedTime);

                modifiedMap.put(relPath, meta);
            }
            data.setModifiedFiles(modifiedMap);

            return data;
        }
    }

    /**
     * 写入可突破 64KB 限制的 UTF-8 长字符串
     */
    private static void writeLongString(DataOutputStream out, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    /**
     * 读取突破 64KB 限制的 UTF-8 长字符串
     */
    private static String readLongString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0) {
            return null;
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
