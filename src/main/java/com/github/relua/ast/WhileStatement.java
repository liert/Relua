package com.github.relua.ast;

public class WhileStatement extends Statement {
    public final Expression condition;
    public final Block body;
    
    public WhileStatement(Expression condition, Block body, SourcePos pos) {
        super(NodeType.WHILE, pos);
        this.condition = condition;
        this.body = body;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}