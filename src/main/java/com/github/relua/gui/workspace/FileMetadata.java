package com.github.relua.gui.workspace;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单个文件的持久化元数据模型，记录变量重命名映射、用户覆盖代码与文件指纹
 */
public class FileMetadata {
    private String relativePath;
    private String fileSha256;
    private final Map<String, String> renamedVariables = new LinkedHashMap<>();
    private String editedText;
    private long lastModifiedTime;

    public FileMetadata() {
        this.lastModifiedTime = System.currentTimeMillis();
    }

    public FileMetadata(String relativePath, String fileSha256, Map<String, String> renamedVariables, String editedText) {
        this.relativePath = relativePath;
        this.fileSha256 = fileSha256;
        if (renamedVariables != null) {
            this.renamedVariables.putAll(renamedVariables);
        }
        this.editedText = editedText;
        this.lastModifiedTime = System.currentTimeMillis();
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getFileSha256() {
        return fileSha256;
    }

    public void setFileSha256(String fileSha256) {
        this.fileSha256 = fileSha256;
    }

    public Map<String, String> getRenamedVariables() {
        return Collections.unmodifiableMap(renamedVariables);
    }

    public void setRenamedVariables(Map<String, String> vars) {
        this.renamedVariables.clear();
        if (vars != null) {
            this.renamedVariables.putAll(vars);
        }
    }

    public void addRenamedVariable(String oldName, String newName) {
        if (oldName != null && newName != null) {
            this.renamedVariables.put(oldName, newName);
        }
    }

    public String getEditedText() {
        return editedText;
    }

    public void setEditedText(String editedText) {
        this.editedText = editedText;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    /**
     * 判断当前文件是否存在实际的修改记录（有重命名变量或有编辑代码文本）
     * @return 是否有实际修改
     */
    public boolean hasModifications() {
        return !renamedVariables.isEmpty() || (editedText != null && !editedText.isEmpty());
    }
}
