package com.github.relua.ast;

public class ExpressionStatement extends Statement {
    public final Expression expression;
    
    public ExpressionStatement(Expression expression, SourcePos pos) {
        super(NodeType.EXPR_STMT, pos);
        this.expression = expression;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}