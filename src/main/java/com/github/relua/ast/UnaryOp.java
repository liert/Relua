package com.github.relua.ast;

public class UnaryOp extends Expression {
    public final String op;
    public final Expression expr;
    
    public UnaryOp(String op, Expression expr, SourcePos pos) {
        super(NodeType.UNARY_OP, pos);
        this.op = op;
        this.expr = expr;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}