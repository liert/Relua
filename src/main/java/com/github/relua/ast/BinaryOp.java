package com.github.relua.ast;

public class BinaryOp extends Expression {
    public final String op;
    public final Expression left;
    public final Expression right;
    
    public BinaryOp(String op, Expression left, Expression right, SourcePos pos) {
        super(NodeType.BINARY_OP, pos);
        this.op = op;
        this.left = left;
        this.right = right;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}