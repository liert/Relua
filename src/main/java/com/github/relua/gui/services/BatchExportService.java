package com.github.relua.gui.services;

import com.github.relua.decompiler.Decompiler;
import com.github.relua.gui.utils.LuaVariableUtils;
import com.github.relua.gui.workspace.FileMetadata;
import com.github.relua.gui.workspace.ProjectWorkspaceManager;
import com.github.relua.model.LuacFile;
import com.github.relua.parser.LuacParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 批量反编译与工程镜像导出服务，支持多线程加速、目录结构保留与持久化分析成果同步输出
 */
public class BatchExportService {
    private static final Logger LOGGER = Logger.getLogger(BatchExportService.class.getName());

    // 默认需要忽略的目录
    private static final Set<String> IGNORED_DIR_NAMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            ".git", ".svn", ".relua", ".idea", ".vscode", "target", "build"
    )));

    /**
     * 进度更新回调接口
     */
    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(int completed, int total, String currentFile);
    }

    /**
     * 导出结果汇报模型
     */
    public static class ExportResult {
        private final int totalFiles;
        private final int successCount;
        private final int failedCount;
        private final long durationMs;
        private final List<String> errorMessages;
        private final boolean cancelled;

        public ExportResult(int totalFiles, int successCount, int failedCount, long durationMs, List<String> errorMessages, boolean cancelled) {
            this.totalFiles = totalFiles;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.durationMs = durationMs;
            this.errorMessages = errorMessages != null ? errorMessages : Collections.emptyList();
            this.cancelled = cancelled;
        }

        public int getTotalFiles() {
            return totalFiles;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public List<String> getErrorMessages() {
            return Collections.unmodifiableList(errorMessages);
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    /**
     * 扫描源目录下所有候选 Lua 脚本或字节码文件
     * @param rootDir 源根目录
     * @return 扫描到的文件列表
     */
    public List<File> scanCandidateFiles(File rootDir) {
        List<File> files = new ArrayList<>();
        if (rootDir == null || !rootDir.exists()) {
            return files;
        }
        if (rootDir.isFile()) {
            if (isCandidateFile(rootDir)) {
                files.add(rootDir);
            }
            return files;
        }

        scanRecursive(rootDir, files);
        return files;
    }

    private void scanRecursive(File dir, List<File> result) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                if (!IGNORED_DIR_NAMES.contains(child.getName().toLowerCase())) {
                    scanRecursive(child, result);
                }
            } else if (child.isFile()) {
                if (isCandidateFile(child)) {
                    result.add(child);
                }
            }
        }
    }

    /**
     * 判断文件是否为候选的 Lua 源码或二进制字节码
     */
    public boolean isCandidateFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        String name = file.getName().toLowerCase();
        if (name.endsWith(".luac") || name.endsWith(".lua") || name.endsWith(".out") || name.endsWith(".bin")) {
            return true;
        }
        // 探测文件头前4字节是否为 Lua 字节码魔数 0x1B 0x4C 0x75 0x61
        return isLuaBytecodeHeader(file);
    }

    /**
     * 探测文件是否包含 Lua 字节码魔数
     */
    public static boolean isLuaBytecodeHeader(File file) {
        if (file == null || file.length() < 4) {
            return false;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[4];
            int read = fis.read(header);
            return read == 4 && header[0] == 0x1B && header[1] == 'L' && header[2] == 'u' && header[3] == 'a';
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 执行批量反编译导出
     * @param sourceRoot 源根目录（用于计算相对路径镜像）
     * @param targetRoot 目标导出根目录
     * @param files 待处理文件列表
     * @param cancelled 取消信号
     * @param listener 进度回调
     * @return 导出结果统计
     */
    public ExportResult exportBatch(File sourceRoot, File targetRoot, List<File> files, AtomicBoolean cancelled, ProgressListener listener) {
        long startTime = System.currentTimeMillis();
        if (files == null || files.isEmpty()) {
            return new ExportResult(0, 0, 0, 0, Collections.emptyList(), false);
        }

        int total = files.size();
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        int threadCount = Math.max(2, Math.min(Runtime.getRuntime().availableProcessors(), 8));
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Future<Void>> futures = new ArrayList<>();

        for (File sourceFile : files) {
            futures.add(executor.submit(new Callable<Void>() {
                @Override
                public Void call() {
                    if (cancelled != null && cancelled.get()) {
                        return null;
                    }

                    String relPath = computeRelativePath(sourceRoot, sourceFile);
                    if (listener != null) {
                        listener.onProgress(completed.get(), total, relPath);
                    }

                    try {
                        exportSingleFile(sourceFile, targetRoot, relPath);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "反编译导出失败: " + sourceFile.getAbsolutePath(), e);
                        failedCount.incrementAndGet();
                        errors.add(relPath + ": " + e.getMessage());
                    } finally {
                        int done = completed.incrementAndGet();
                        if (listener != null) {
                            listener.onProgress(done, total, relPath);
                        }
                    }
                    return null;
                }
            }));
        }

        // 等待所有线程执行完毕或取消
        try {
            for (Future<Void> future : futures) {
                if (cancelled != null && cancelled.get()) {
                    break;
                }
                try {
                    future.get();
                } catch (Exception ignored) {
                }
            }
        } finally {
            executor.shutdownNow();
        }

        long duration = System.currentTimeMillis() - startTime;
        boolean wasCancelled = cancelled != null && cancelled.get();
        return new ExportResult(total, successCount.get(), failedCount.get(), duration, errors, wasCancelled);
    }

    /**
     * 单个文件的反编译及导出处理
     */
    private void exportSingleFile(File sourceFile, File targetRoot, String relativePath) throws Exception {
        // 目标文件扩展名规范化：.luac / .bin / .out -> .lua
        String targetRelPath = relativePath;
        String lower = targetRelPath.toLowerCase();
        if (lower.endsWith(".luac")) {
            targetRelPath = targetRelPath.substring(0, targetRelPath.length() - 5) + ".lua";
        } else if (lower.endsWith(".bin") || lower.endsWith(".out")) {
            targetRelPath = targetRelPath.substring(0, targetRelPath.length() - 4) + ".lua";
        } else if (!lower.endsWith(".lua")) {
            targetRelPath = targetRelPath + ".lua";
        }

        File targetFile = new File(targetRoot, targetRelPath);
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        String finalContent = null;

        // 1. 检查是否存在持久化修改元数据
        FileMetadata meta = ProjectWorkspaceManager.getInstance().getFileMetadata(sourceFile);
        if (meta != null && meta.getEditedText() != null && !meta.getEditedText().trim().isEmpty()) {
            // 用户在 IDE 中编辑保存的代码优先输出
            finalContent = meta.getEditedText();
        } else {
            boolean isBytecode = isLuaBytecodeHeader(sourceFile);
            String code;
            if (isBytecode) {
                LuacParser parser = new LuacParser();
                LuacFile luacFile = parser.parse(sourceFile.getAbsolutePath());
                Decompiler decompiler = new Decompiler();
                code = decompiler.decompile(luacFile, false);
            } else {
                // 普通文本源码，直接读取
                code = new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
            }

            // 如果存在持久化变量重命名，同步替换应用
            if (meta != null && !meta.getRenamedVariables().isEmpty()) {
                for (Map.Entry<String, String> entry : meta.getRenamedVariables().entrySet()) {
                    String oldName = entry.getKey();
                    String newName = entry.getValue();
                    List<LuaVariableUtils.SymbolMatch> matches = LuaVariableUtils.findVariableOccurrences(code, oldName, -1);
                    if (!matches.isEmpty()) {
                        StringBuilder sb = new StringBuilder(code);
                        for (int i = matches.size() - 1; i >= 0; i--) {
                            LuaVariableUtils.SymbolMatch m = matches.get(i);
                            sb.replace(m.start, m.end, newName);
                        }
                        code = sb.toString();
                    }
                }
            }

            // 如果存在持久化行注释，插入注释
            if (meta != null && !meta.getLineComments().isEmpty()) {
                code = applyCommentsToCode(code, meta.getLineComments());
            }

            finalContent = code;
        }

        // 3. 将最终内容安全写入目标文件
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8)) {
            writer.write(finalContent != null ? finalContent : "");
        }
    }

    /**
     * 将行注释应用合并到反编译源码文本中
     */
    private String applyCommentsToCode(String code, Map<Integer, String> comments) {
        if (code == null || comments == null || comments.isEmpty()) {
            return code;
        }
        String[] lines = code.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            int lineNum = i + 1;
            String line = lines[i];
            String comment = comments.get(lineNum);
            if (comment != null && !comment.trim().isEmpty()) {
                if (line.endsWith("\r")) {
                    line = line.substring(0, line.length() - 1);
                }
                line = line + "  -- [Note] " + comment.trim();
            }
            sb.append(line);
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 计算相对于源根目录的相对路径
     */
    private String computeRelativePath(File sourceRoot, File file) {
        if (sourceRoot == null || file == null) {
            return file != null ? file.getName() : "";
        }
        if (sourceRoot.isFile()) {
            return file.getName();
        }
        String rootPath = sourceRoot.getAbsolutePath().replace('\\', '/');
        String filePath = file.getAbsolutePath().replace('\\', '/');
        if (filePath.startsWith(rootPath)) {
            String rel = filePath.substring(rootPath.length());
            if (rel.startsWith("/")) {
                rel = rel.substring(1);
            }
            return rel;
        }
        return file.getName();
    }
}
