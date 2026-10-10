package com.github.relua.gui.navigation;

import java.io.File;
import java.util.Objects;

/**
 * 代码行书签模型 (支持 1~9 数字助记符与代码预览)
 */
public class Bookmark {
    private final File file;
    private final int line; // 1-based
    private final Integer mnemonic; // 1~9，或者 null (无助记符普通书签)
    private final String previewText; // 代码行摘要

    public Bookmark(File file, int line, Integer mnemonic, String previewText) {
        this.file = file;
        this.line = Math.max(1, line);
        this.mnemonic = (mnemonic != null && mnemonic >= 1 && mnemonic <= 9) ? mnemonic : null;
        this.previewText = (previewText != null) ? previewText.trim() : "";
    }

    public File getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    public Integer getMnemonic() {
        return mnemonic;
    }

    public String getPreviewText() {
        return previewText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bookmark bookmark = (Bookmark) o;
        return line == bookmark.line && Objects.equals(file, bookmark.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, line);
    }

    @Override
    public String toString() {
        return (file != null ? file.getName() : "null") + ":" + line + (mnemonic != null ? " [" + mnemonic + "]" : "");
    }
}
