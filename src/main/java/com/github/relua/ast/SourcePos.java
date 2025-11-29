package com.github.relua.ast;

public final class SourcePos {
    public final int pc;
    public final int line;
    
    public SourcePos(Integer pc, Integer line) {
        this.pc = pc == null ? -1 : pc;
        this.line = line == null ? -1 : line;
    }
}