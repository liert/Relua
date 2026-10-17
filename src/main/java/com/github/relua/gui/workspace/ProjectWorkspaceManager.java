package com.github.relua.gui.workspace;

import com.github.relua.gui.views.EditorTab;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 项目与工作区持久化业务管理器，统一调度单文件伴生数据库与文件夹集中工程数据库的生命周期
 */
public class ProjectWorkspaceManager {
    private static final Logger LOGGER = Logger.getLogger(ProjectWorkspaceManager.class.getName());

    public static final String WORKSPACE_DIR_NAME = ".relua";
    public static final String WORKSPACE_FILE_NAME = "project.relua";
    public static final String COMPANION_EXT = ".relua";

    private static final ProjectWorkspaceManager INSTANCE = new ProjectWorkspaceManager();

    private File currentRootFolder;
    private WorkspaceData currentWorkspace = new WorkspaceData();
    private final Map<String, FileMetadata> memoryFileCache = new ConcurrentHashMap<>();

    private ProjectWorkspaceManager() {
    }

    public static ProjectWorkspaceManager getInstance() {
        return INSTANCE;
    }

    /**
     * 当用户打开文件夹时切换并加载工作区
     * @param folder 项目根目录
     */
    public synchronized void openFolderWorkspace(File folder) {
        this.currentRootFolder = normalizeFolder(folder);
        this.memoryFileCache.clear();
        this.currentWorkspace = new WorkspaceData();

        if (folder == null || !folder.isDirectory()) {
            return;
        }

        File dbFile = getWorkspaceDatabaseFile(this.currentRootFolder);
        if (dbFile.exists() && dbFile.length() > 0) {
            try {
                this.currentWorkspace = BinaryPersistenceService.load(dbFile);
                if (this.currentWorkspace != null) {
                    this.memoryFileCache.putAll(this.currentWorkspace.getModifiedFiles());
                    LOGGER.info("成功加载工程数据库: " + dbFile.getAbsolutePath() + "，包含分析记录 " + memoryFileCache.size() + " 项");
                } else {
                    this.currentWorkspace = new WorkspaceData();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "读取工程数据库失败: " + e.getMessage(), e);
                this.currentWorkspace = new WorkspaceData();
            }
        }
    }

    private File normalizeFolder(File folder) {
        if (folder == null) return null;
        File abs = folder.getAbsoluteFile();
        String path = abs.getPath();
        while ((path.endsWith(File.separator) || path.endsWith("/")) && path.length() > 3) {
            path = path.substring(0, path.length() - 1);
        }
        return new File(path);
    }

    /**
     * 获取指定文件的持久化修改元数据（支持从文件夹工程库或单文件伴生库中自适应提取）
     * @param file 目标文件
     * @return 元数据对象，若无修改记录则返回 null
     */
    public FileMetadata getFileMetadata(File file) {
        if (file == null) {
            return null;
        }

        // 1. 如果当前处于文件夹模式，优先从工程内存缓存中按相对路径检索
        String relKey = toRelativePath(file);
        FileMetadata cached = memoryFileCache.get(relKey);
        if (cached != null) {
            return cached;
        }

        // 2. 规范化回退匹配（必须能够唯一确定目标记录，不能跨目录仅凭文件名冲突匹配）
        // a. 相对路径大小写不敏感唯一匹配（支持 Windows 等大小写不敏感文件系统）
        FileMetadata matchedByRelKey = null;
        int matchCount = 0;
        for (Map.Entry<String, FileMetadata> entry : memoryFileCache.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(relKey)) {
                matchedByRelKey = entry.getValue();
                matchCount++;
            }
        }
        if (matchCount == 1) {
            return matchedByRelKey;
        }

        // b. 规范化绝对路径唯一匹配（针对以绝对路径或规范化路径记录的场景）
        String absPath = file.getAbsolutePath().replace('\\', '/');
        cached = memoryFileCache.get(absPath);
        if (cached != null) {
            return cached;
        }
        try {
            String canonicalRel = file.getCanonicalPath().replace('\\', '/');
            cached = memoryFileCache.get(canonicalRel);
            if (cached != null) {
                return cached;
            }
        } catch (IOException ignored) {
        }

        // c. 如果当前是单文件模式（currentRootFolder == null）或者文件处于工程根目录（relKey 不包含 '/'），
        // 且 memoryFileCache 中不存在路径分隔符的同名 key 唯一匹配：
        if (!relKey.contains("/")) {
            FileMetadata singleMatch = null;
            int singleCount = 0;
            for (Map.Entry<String, FileMetadata> entry : memoryFileCache.entrySet()) {
                if (!entry.getKey().contains("/") && entry.getKey().equalsIgnoreCase(relKey)) {
                    singleMatch = entry.getValue();
                    singleCount++;
                }
            }
            if (singleCount == 1) {
                return singleMatch;
            }
        }

        // 2. 检查单文件同目录下是否存在独立的伴生库 (<name>.relua)
        File companionFile = getCompanionFile(file);
        if (companionFile.exists() && companionFile.length() > 0) {
            try {
                WorkspaceData singleData = BinaryPersistenceService.load(companionFile);
                if (singleData != null) {
                    // 单文件伴生库中提取唯一的修改记录
                    for (FileMetadata meta : singleData.getModifiedFiles().values()) {
                        memoryFileCache.put(relKey, meta);
                        return meta;
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("读取伴生文件失败: " + companionFile.getAbsolutePath() + ", " + e.getMessage());
            }
        }

        return null;
    }

    /**
     * 更新并缓存指定文件的修改数据
     * @param file 文件对象
     * @param renamedVars 重命名变量映射表
     * @param editedText 用户编辑覆盖文本
     */
    public void recordFileModification(File file, Map<String, String> renamedVars, String editedText) {
        recordFileModification(file, renamedVars, editedText, null);
    }

    /**
     * 更新并缓存指定文件的修改数据（含行注释）
     * @param file 文件对象
     * @param renamedVars 重命名变量映射表
     * @param editedText 用户编辑覆盖文本
     * @param lineComments 行注释表
     */
    public void recordFileModification(File file, Map<String, String> renamedVars, String editedText, Map<Integer, String> lineComments) {
        if (file == null) {
            return;
        }

        String relPath = toRelativePath(file);
        FileMetadata meta = memoryFileCache.computeIfAbsent(relPath, k -> new FileMetadata());
        meta.setRelativePath(relPath);
        meta.setRenamedVariables(renamedVars);
        meta.setEditedText(editedText);
        if (lineComments != null) {
            meta.setLineComments(lineComments);
        }
        meta.setLastModifiedTime(System.currentTimeMillis());

        if (meta.getFileSha256() == null) {
            meta.setFileSha256(computeSha256(file));
        }
    }

    /**
     * 执行工作区保存（自动判定单文件模式或文件夹工程模式）
     * @param openedTabs 当前打开的所有 EditorTab 列表
     * @param activeTab 当前聚焦的 EditorTab
     */
    public synchronized void save(List<EditorTab> openedTabs, EditorTab activeTab) {
        try {
            saveChecked(openedTabs, activeTab);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "保存分析数据失败: " + e.getMessage(), e);
        }
    }

    /** 保存并返回实际写入的数据库路径，写入失败时向调用方报告。 */
    public synchronized List<File> saveChecked(List<EditorTab> openedTabs, EditorTab activeTab) throws IOException {
        // 同步所有打开的 Tab 中最新的变量重命名、行注释与文本状态
        if (openedTabs != null) {
            for (EditorTab tab : openedTabs) {
                if (tab.getFile() != null && tab.isFileLoaded()) {
                    Map<String, String> renamed = tab.getRenamedVariables();
                    Map<Integer, String> comments = tab.getLineComments();
                    String relPath = toRelativePath(tab.getFile());
                    FileMetadata meta = memoryFileCache.computeIfAbsent(relPath, k -> new FileMetadata());
                    meta.setRelativePath(relPath);
                    meta.setRenamedVariables(renamed);
                    meta.setLineComments(comments);
                    // 所有已加载文件保存源码快照，包括没有重命名或注释的文件。
                    meta.setEditedText(tab.getLuaEditorView().getText());
                    meta.setLastModifiedTime(System.currentTimeMillis());
                    meta.setFileSha256(tab.getSourceFingerprint() != null
                            ? tab.getSourceFingerprint() : computeSha256(tab.getFile()));
                    meta.setSourceSnapshot(true);
                    meta.setChunkLineRanges(tab.getChunkLineRanges());
                    meta.setCaretPosition(tab.getLuaEditorView().getCodeArea().getCaretPosition());
                }
            }
        }

        List<File> savedFiles = new ArrayList<>();
        if (currentRootFolder != null && currentRootFolder.isDirectory()) {
            // 【文件夹工程模式】：写回根目录下的 .relua/project.relua
            savedFiles.add(saveFolderWorkspace(openedTabs, activeTab));
        } else if (openedTabs != null) {
            // 独立打开多个文件时，每个文件的分析结果都必须保存。
            IOException failure = null;
            for (EditorTab tab : openedTabs) {
                if (tab.isFileLoaded() && tab.getFile() != null) {
                    try {
                        File savedFile = saveSingleFileCompanion(tab);
                        if (savedFile != null) savedFiles.add(savedFile);
                    } catch (IOException e) {
                        if (failure == null) failure = e;
                        else failure.addSuppressed(e);
                    }
                }
            }
            if (failure != null) throw failure;
        }
        return savedFiles;
    }

    private File saveFolderWorkspace(List<EditorTab> openedTabs, EditorTab activeTab) throws IOException {
        WorkspaceData data = new WorkspaceData();
        data.setSaveTime(System.currentTimeMillis());

        if (activeTab != null && activeTab.getFile() != null) {
            data.setActiveFilePath(toRelativePath(activeTab.getFile()));
        }

        if (openedTabs != null) {
            for (EditorTab tab : openedTabs) {
                if (tab.getFile() != null) {
                    data.addOpenedTabPath(toRelativePath(tab.getFile()));
                }
            }
        }

        data.setModifiedFiles(memoryFileCache);

        File targetFile = getWorkspaceDatabaseFile(currentRootFolder);
        BinaryPersistenceService.save(targetFile, data);
        currentWorkspace = data;
        LOGGER.info("已保存工程数据库: " + targetFile.getAbsolutePath());
        return targetFile;
    }

    private File saveSingleFileCompanion(EditorTab tab) throws IOException {
        File file = tab.getFile();
        if (file == null) {
            return null;
        }

        FileMetadata meta = getFileMetadata(file);
        Map<String, String> renamed = tab.getRenamedVariables();
        Map<Integer, String> comments = tab.getLineComments();
        if ((meta == null || !meta.hasModifications()) && renamed.isEmpty() && comments.isEmpty()) {
            return null; // 无修改时不生成冗余伴生文件
        }

        WorkspaceData data = new WorkspaceData();
        data.setSaveTime(System.currentTimeMillis());
        data.setActiveFilePath(file.getName());
        data.addOpenedTabPath(file.getName());

        if (meta == null) {
            meta = new FileMetadata(file.getName(), computeSha256(file), renamed, null);
            meta.setLineComments(comments);
        } else {
            meta.setRenamedVariables(renamed);
            meta.setLineComments(comments);
        }
        meta.setEditedText(tab.getLuaEditorView().getText());
        data.putFileMetadata(file.getName(), meta);

        File companionFile = getCompanionFile(file);
        BinaryPersistenceService.save(companionFile, data);
        LOGGER.info("已保存单文件伴生数据库: " + companionFile.getAbsolutePath());
        return companionFile;
    }

    private String cacheKey(File file) {
        return toRelativePath(file);
    }

    /**
     * 将文件路径转换为跨平台的统一相对路径（使用正斜杠 '/'）
     */
    public String toRelativePath(File file) {
        if (file == null) {
            return "";
        }
        if (currentRootFolder == null) {
            return file.getName();
        }

        try {
            java.nio.file.Path filePath = file.getCanonicalFile().toPath();
            java.nio.file.Path rootPath = currentRootFolder.getCanonicalFile().toPath();
            if (filePath.startsWith(rootPath)) {
                return rootPath.relativize(filePath).toString().replace('\\', '/');
            }
        } catch (Exception ignored) {
        }

        try {
            java.nio.file.Path filePath = file.toPath().toAbsolutePath().normalize();
            java.nio.file.Path rootPath = currentRootFolder.toPath().toAbsolutePath().normalize();
            if (filePath.startsWith(rootPath)) {
                return rootPath.relativize(filePath).toString().replace('\\', '/');
            }
        } catch (Exception ignored) {
        }

        try {
            String rootStr = currentRootFolder.getAbsolutePath().replace('\\', '/');
            String fileStr = file.getAbsolutePath().replace('\\', '/');
            if (fileStr.toLowerCase().startsWith(rootStr.toLowerCase())) {
                String rel = fileStr.substring(rootStr.length());
                if (rel.startsWith("/")) {
                    rel = rel.substring(1);
                }
                return rel;
            }
        } catch (Exception ignored) {
        }

        return file.getAbsolutePath().replace('\\', '/');
    }

    /**
     * 根据工作区记录获取应还原的历史打开文件列表
     */
    public List<File> getHistoricalOpenedFiles() {
        List<File> list = new ArrayList<>();
        if (currentRootFolder == null || currentWorkspace == null) {
            return list;
        }

        for (String relPath : currentWorkspace.getOpenedTabPaths()) {
            File f = new File(currentRootFolder, relPath.replace('/', File.separatorChar));
            if (f.exists() && f.isFile()) {
                list.add(f);
            }
        }
        return list;
    }

    public File getCurrentRootFolder() {
        return currentRootFolder;
    }

    /**
     * 判断指定文件是否属于当前已打开工程文件夹的内部文件
     * @param file 目标文件
     * @return 如果当前处于文件夹工程模式且该文件位于工程根目录下，则返回 true；否则返回 false
     */
    public boolean isInternalWorkspaceFile(File file) {
        if (file == null || currentRootFolder == null || !currentRootFolder.isDirectory()) {
            return false;
        }
        try {
            java.nio.file.Path filePath = file.getCanonicalFile().toPath();
            java.nio.file.Path rootPath = currentRootFolder.getCanonicalFile().toPath();
            return !filePath.equals(rootPath) && filePath.startsWith(rootPath);
        } catch (Exception e) {
            try {
                java.nio.file.Path filePath = file.toPath().toAbsolutePath().normalize();
                java.nio.file.Path rootPath = currentRootFolder.toPath().toAbsolutePath().normalize();
                return !filePath.equals(rootPath) && filePath.startsWith(rootPath);
            } catch (Exception ex) {
                String filePath = file.getAbsolutePath();
                String rootPath = currentRootFolder.getAbsolutePath();
                if (!rootPath.endsWith(File.separator)) {
                    rootPath += File.separator;
                }
                return filePath.startsWith(rootPath);
            }
        }
    }

    /**
     * 判断指定路径字符串是否属于当前已打开工程文件夹的内部文件
     * @param path 目标路径
     * @return 如果当前处于文件夹工程模式且该路径位于工程根目录下，则返回 true；否则返回 false
     */
    public boolean isInternalWorkspaceFile(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        return isInternalWorkspaceFile(new File(path.trim()));
    }

    public WorkspaceData getCurrentWorkspace() {
        return currentWorkspace;
    }

    public File getWorkspaceDatabaseFile(File rootFolder) {
        return new File(new File(rootFolder, WORKSPACE_DIR_NAME), WORKSPACE_FILE_NAME);
    }

    public File getCompanionFile(File singleFile) {
        return new File(singleFile.getParentFile(), singleFile.getName() + COMPANION_EXT);
    }

    /** 只复用与当前原文件 SHA-256 一致的完整源码快照。调用方应在后台线程执行。 */
    public FileMetadata getValidSourceSnapshot(File file) {
        FileMetadata metadata = getValidFileMetadata(file);
        return metadata != null && metadata.hasSourceSnapshot() ? metadata : null;
    }

    public FileMetadata getValidFileMetadata(File file) {
        FileMetadata metadata = getFileMetadata(file);
        return metadata != null && metadata.getFileSha256() != null
                && metadata.getFileSha256().equals(computeSha256(file)) ? metadata : null;
    }

    public String getFileFingerprint(File file) { return computeSha256(file); }

    private String computeSha256(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }
        try (java.io.InputStream in = new java.io.FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) != -1) {
                digest.update(buf, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
