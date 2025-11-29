package com.github.relua.ast;

/**
 * 未完成的IF对象，由TEST+JMP产生，等待后续填充then/else内容
 */
public class PendingIf {
    public final Expression condition;
    public final int thenStart;
    public final int thenEnd;
    public final Integer elseStart; // 可 null
    public final Integer elseEnd;   // 可 null
    public final SourcePos pos;

    public PendingIf(Expression cond, int thenStart, int thenEnd,
                    Integer elseStart, Integer elseEnd, SourcePos pos) {
        this.condition = cond;
        this.thenStart = thenStart;
        this.thenEnd = thenEnd;
        this.elseStart = elseStart;
        this.elseEnd = elseEnd;
        this.pos = pos;
    }
}