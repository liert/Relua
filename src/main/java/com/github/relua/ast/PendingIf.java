package com.github.relua.ast;

import java.util.List;
import java.util.ArrayList;

/**
 * 未完成的IF对象，由TEST+JMP产生，等待后续填充then/else内容
 */
public class PendingIf {
    public final Expression condition;
    public final boolean flag;  // true不加not，false加not
    public final int thenStart;
    public final int thenEnd;
    public final Integer elseStart; // 可 null
    public final Integer elseEnd;   // 可 null
    public final SourcePos pos;
    public final PendingTest.TestType type;
    public final List<AstNode> astNodes; // 附加的AST节点

    public PendingIf(Expression cond, boolean flag, int thenStart, int thenEnd,
                    Integer elseStart, Integer elseEnd, PendingTest.TestType type, SourcePos pos) {
        this.condition = cond;
        this.flag = flag;
        this.thenStart = thenStart;
        this.thenEnd = thenEnd;
        this.elseStart = elseStart;
        this.elseEnd = elseEnd;
        this.type = type;
        this.pos = pos;
        this.astNodes = new ArrayList<>();
    }
    
    public PendingIf(Expression cond, boolean flag, int thenStart, int thenEnd,
                    Integer elseStart, Integer elseEnd, PendingTest.TestType type, SourcePos pos, List<AstNode> astNodes) {
        this.condition = cond;
        this.flag = flag;
        this.thenStart = thenStart;
        this.thenEnd = thenEnd;
        this.elseStart = elseStart;
        this.elseEnd = elseEnd;
        this.type = type;
        this.pos = pos;
        this.astNodes = astNodes;
    }
}