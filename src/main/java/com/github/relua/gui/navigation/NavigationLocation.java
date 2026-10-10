package com.github.relua.gui.navigation;

import java.io.File;
import java.util.Objects;

/**
 * 光标与代码导航位置模型
 */
public class NavigationLocation {
    private final File file;
    private final int line; // 1-based
    private final int column; // 0-based

    public NavigationLocation(File file, int line, int column) {
        this.file = file;
        this.line = Math.max(1, line);
        this.column = Math.max(0, column);
    }

    public File getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NavigationLocation that = (NavigationLocation) o;
        return line == that.line &&
                column == that.column &&
                Objects.equals(file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, line, column);
    }

    @Override
    public String toString() {
        return (file != null ? file.getName() : "null") + ":" + line + ":" + column;
    }
}
