package com.github.relua.ast;

/**
 * 待处理的TEST指令，用于记录TEST指令的信息，供后续JMP指令使用
 */
public class PendingTest {
    public final Expression condition;
    public final int pc;
    public final SourcePos pos;

    public PendingTest(Expression condition, int pc, SourcePos pos) {
        this.condition = condition;
        this.pc = pc;
        this.pos = pos;
    }
}
