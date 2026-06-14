package com.github.relua.model;

public class LineRange {
    public final int startLine;
    public final int endLine;

    public LineRange(int startLine, int endLine) {
        this.startLine = startLine;
        this.endLine = endLine;
    }

    @Override
    public String toString() {
        return "[" + startLine + ", " + endLine + "]";
    }
}
