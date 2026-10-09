package com.github.relua.gui.workspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作区持久化容器模型，包含会话状态（激活与打开的标签页）及所有被修改文件的增量元数据
 */
public class WorkspaceData {
    public static final short CURRENT_VERSION = 1;

    private short version = CURRENT_VERSION;
    private long saveTime;
    private String activeFilePath;
    private final List<String> openedTabPaths = new ArrayList<>();
    private final Map<String, FileMetadata> modifiedFiles = new LinkedHashMap<>();

    public WorkspaceData() {
        this.saveTime = System.currentTimeMillis();
    }

    public short getVersion() {
        return version;
    }

    public void setVersion(short version) {
        this.version = version;
    }

    public long getSaveTime() {
        return saveTime;
    }

    public void setSaveTime(long saveTime) {
        this.saveTime = saveTime;
    }

    public String getActiveFilePath() {
        return activeFilePath;
    }

    public void setActiveFilePath(String activeFilePath) {
        this.activeFilePath = activeFilePath;
    }

    public List<String> getOpenedTabPaths() {
        return Collections.unmodifiableList(openedTabPaths);
    }

    public void setOpenedTabPaths(List<String> paths) {
        this.openedTabPaths.clear();
        if (paths != null) {
            this.openedTabPaths.addAll(paths);
        }
    }

    public void addOpenedTabPath(String path) {
        if (path != null && !this.openedTabPaths.contains(path)) {
            this.openedTabPaths.add(path);
        }
    }

    public Map<String, FileMetadata> getModifiedFiles() {
        return Collections.unmodifiableMap(modifiedFiles);
    }

    public void setModifiedFiles(Map<String, FileMetadata> files) {
        this.modifiedFiles.clear();
        if (files != null) {
            this.modifiedFiles.putAll(files);
        }
    }

    public void putFileMetadata(String relativePath, FileMetadata metadata) {
        if (relativePath != null && metadata != null) {
            this.modifiedFiles.put(relativePath, metadata);
        }
    }

    public FileMetadata getFileMetadata(String relativePath) {
        if (relativePath == null) {
            return null;
        }
        return this.modifiedFiles.get(relativePath);
    }

    public void removeFileMetadata(String relativePath) {
        if (relativePath != null) {
            this.modifiedFiles.remove(relativePath);
        }
    }

    /**
     * 判断工作区中是否有实际被修改的文件记录
     * @return 是否有被修改的文件
     */
    public boolean hasAnyModifications() {
        return !modifiedFiles.isEmpty();
    }
}
