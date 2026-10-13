package com.github.relua.gui.workspace;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.github.relua.model.LineRange;

/**
 * 单个文件的持久化元数据模型，记录源码快照、函数范围、分析修改与文件指纹
 */
public class FileMetadata {
    private String relativePath;
    private String fileSha256;
    private final Map<String, String> renamedVariables = new LinkedHashMap<>();
    private final Map<Integer, String> lineComments = new LinkedHashMap<>();
    private String editedText;
    private long lastModifiedTime;
    private boolean sourceSnapshot;
    private final Map<String, LineRange> chunkLineRanges = new LinkedHashMap<>();
    private int caretPosition;

    public boolean hasSourceSnapshot() { return sourceSnapshot && editedText != null; }
    public void setSourceSnapshot(boolean sourceSnapshot) { this.sourceSnapshot = sourceSnapshot; }
    public Map<String, LineRange> getChunkLineRanges() { return Collections.unmodifiableMap(chunkLineRanges); }
    public void setChunkLineRanges(Map<String, LineRange> ranges) {
        Map<String, LineRange> copy = ranges == null ? Collections.emptyMap() : new LinkedHashMap<>(ranges);
        chunkLineRanges.clear();
        chunkLineRanges.putAll(copy);
    }
    public int getCaretPosition() { return caretPosition; }
    public void setCaretPosition(int caretPosition) { this.caretPosition = Math.max(0, caretPosition); }

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
     * 判断当前文件是否存在需要保存的分析记录或源码快照。
     * @return 是否有持久化内容
     */
    public boolean hasModifications() {
        return sourceSnapshot || !renamedVariables.isEmpty() || !lineComments.isEmpty() || (editedText != null && !editedText.isEmpty());
    }
}
