package com.github.relua.ast;

import java.util.List;
import java.util.ArrayList;

/**
 * 待处理的TEST指令，用于记录TEST指令的信息，供后续JMP指令使用
 */
public class PendingTest {
    // 指令类型枚举
    public enum TestType {
        TEST,
        TESTSET
    }
    
    public final Expression condition;
    public final boolean flag;  // true不加not，false加not
    public final int pc;
    public final SourcePos pos;
    public final TestType type;
    public final List<AstNode> astNodes;

    public PendingTest(Expression condition, boolean flag, int pc, TestType type, SourcePos pos) {
        this.condition = condition;
        this.flag = flag;
        this.pc = pc;
        this.pos = pos;
        this.type = type;
        this.astNodes = new ArrayList<>();
    }
    
    // 添加AST节点
    public void addAstNode(AstNode node) {
        this.astNodes.add(node);
    }
}
