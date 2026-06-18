package com.github.relua.decompiler;

public class ClosureSkipResult {
    public final Object astNode;
    public final int skipCount;

    public ClosureSkipResult(Object astNode, int skipCount) {
        this.astNode = astNode;
        this.skipCount = skipCount;
    }
}
