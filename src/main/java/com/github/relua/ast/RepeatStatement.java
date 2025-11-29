package com.github.relua.ast;

public class RepeatStatement extends Statement {
    public final Block body;
    public final Expression condition;
    
    public RepeatStatement(Block body, Expression condition, SourcePos pos) {
        super(NodeType.REPEAT, pos);
        this.body = body;
        this.condition = condition;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}