package com.github.relua.decompiler;

/**
 * IfElse模式类，用于表示if-else结构的模式
 */
public class IfElsePattern {
    public BasicBlock testBlock;
    public BasicBlock thenBlock;
    public BasicBlock elseBlock;
    public BasicBlock endBlock;
    
    public IfElsePattern(BasicBlock testBlock, BasicBlock thenBlock, BasicBlock elseBlock, BasicBlock endBlock) {
        this.testBlock = testBlock;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
        this.endBlock = endBlock;
    }
}