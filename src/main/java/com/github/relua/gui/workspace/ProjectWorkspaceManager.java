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
        this.currentRootFolder = folder;
        this.memoryFileCache.clear();
        this.currentWorkspace = new WorkspaceData();

        if (folder == null || !folder.isDirectory()) {
            return;
        }

        File dbFile = getWorkspaceDatabaseFile(folder);
        if (dbFile.exists() && dbFile.length() > 0) {
            try {
                this.currentWorkspace = BinaryPersistenceService.load(dbFile);
                if (this.currentWorkspace != null) {
                    this.memoryFileCache.putAll(this.currentWorkspace.getModifiedFiles());
                    LOGGER.info("成功加载工程数据库: " + dbFile.getAbsolutePath() + "，包含修改记录 " + memoryFileCache.size() + " 项");
                } else {
                    this.currentWorkspace = new WorkspaceData();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "读取工程数据库失败: " + e.getMessage(), e);
                this.currentWorkspace = new WorkspaceData();
            }
        }
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
        if (currentRootFolder != null) {
            String relPath = toRelativePath(file);
            FileMetadata cached = memoryFileCache.get(relPath);
            if (cached != null) {
                return cached;
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
        if (file == null) {
            return;
        }

        String relPath = toRelativePath(file);
        FileMetadata meta = memoryFileCache.computeIfAbsent(relPath, k -> new FileMetadata());
        meta.setRelativePath(relPath);
        meta.setRenamedVariables(renamedVars);
        meta.setEditedText(editedText);
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
        // 同步所有打开的 Tab 中最新的变量重命名与文本状态
        if (openedTabs != null) {
            for (EditorTab tab : openedTabs) {
                if (tab.getFile() != null) {
                    Map<String, String> renamed = tab.getRenamedVariables();
                    String text = tab.getTextEditorView().getText();
                    if (!renamed.isEmpty()) {
                        recordFileModification(tab.getFile(), renamed, null);
                    }
                }
            }
        }

        if (currentRootFolder != null && currentRootFolder.isDirectory()) {
            // 【文件夹工程模式】：写回根目录下的 .relua/project.relua
            saveFolderWorkspace(openedTabs, activeTab);
        } else if (activeTab != null && activeTab.getFile() != null) {
            // 【单文件模式】：写回当前文件的伴生库 <file>.relua
            saveSingleFileCompanion(activeTab);
        }
    }

    private void saveFolderWorkspace(List<EditorTab> openedTabs, EditorTab activeTab) {
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
        try {
            BinaryPersistenceService.save(targetFile, data);
            LOGGER.info("已保存工程数据库: " + targetFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "保存工程数据库失败: " + e.getMessage(), e);
        }
    }

    private void saveSingleFileCompanion(EditorTab tab) {
        File file = tab.getFile();
        if (file == null) {
            return;
        }

        FileMetadata meta = getFileMetadata(file);
        Map<String, String> renamed = tab.getRenamedVariables();
        if ((meta == null || !meta.hasModifications()) && renamed.isEmpty()) {
            return; // 无修改时不生成冗余伴生文件
        }

        WorkspaceData data = new WorkspaceData();
        data.setSaveTime(System.currentTimeMillis());
        data.setActiveFilePath(file.getName());
        data.addOpenedTabPath(file.getName());

        if (meta == null) {
            meta = new FileMetadata(file.getName(), computeSha256(file), renamed, null);
        } else {
            meta.setRenamedVariables(renamed);
        }
        data.putFileMetadata(file.getName(), meta);

        File companionFile = getCompanionFile(file);
        try {
            BinaryPersistenceService.save(companionFile, data);
            LOGGER.info("已保存单文件伴生数据库: " + companionFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "保存单文件伴生数据库失败: " + e.getMessage(), e);
        }
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
            String rootPath = currentRootFolder.getCanonicalPath();
            String filePath = file.getCanonicalPath();

            if (filePath.startsWith(rootPath)) {
                String rel = filePath.substring(rootPath.length());
                if (rel.startsWith(File.separator) || rel.startsWith("/")) {
                    rel = rel.substring(1);
                }
                return rel.replace('\\', '/');
            }
        } catch (IOException e) {
            // 回退到普通文件名
        }
        return file.getName();
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

    public WorkspaceData getCurrentWorkspace() {
        return currentWorkspace;
    }

    public File getWorkspaceDatabaseFile(File rootFolder) {
        return new File(new File(rootFolder, WORKSPACE_DIR_NAME), WORKSPACE_FILE_NAME);
    }

    public File getCompanionFile(File singleFile) {
        return new File(singleFile.getParentFile(), singleFile.getName() + COMPANION_EXT);
    }

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
