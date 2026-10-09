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
    private final Map<Integer, String> lineComments = new LinkedHashMap<>();
    private String editedText;
    private long lastModifiedTime;

    public FileMetadata() {
        this.lastModifiedTime = System.currentTimeMillis();
    }

    public FileMetadata(String relativePath, String fileSha256, Map<String, String> renamedVariables, String editedText) {
        this(relativePath, fileSha256, renamedVariables, editedText, null);
    }

    public FileMetadata(String relativePath, String fileSha256, Map<String, String> renamedVariables, String editedText, Map<Integer, String> lineComments) {
        this.relativePath = relativePath;
        this.fileSha256 = fileSha256;
        if (renamedVariables != null) {
            this.renamedVariables.putAll(renamedVariables);
        }
        this.editedText = editedText;
        if (lineComments != null) {
            this.lineComments.putAll(lineComments);
        }
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

    public Map<Integer, String> getLineComments() {
        return Collections.unmodifiableMap(lineComments);
    }

    public void setLineComments(Map<Integer, String> comments) {
        this.lineComments.clear();
        if (comments != null) {
            this.lineComments.putAll(comments);
        }
    }

    public void addLineComment(int line, String comment) {
        if (comment != null && !comment.trim().isEmpty()) {
            this.lineComments.put(line, comment.trim());
        } else {
            this.lineComments.remove(line);
        }
    }

    public String getLineComment(int line) {
        return this.lineComments.get(line);
    }

    /**
     * 判断当前文件是否存在实际的修改记录（有重命名变量、有行注释或有编辑代码文本）
     * @return 是否有实际修改
     */
    public boolean hasModifications() {
        return !renamedVariables.isEmpty() || !lineComments.isEmpty() || (editedText != null && !editedText.isEmpty());
    }
}
