package com.github.relua.ast;

public class Vararg extends Expression {
    public Vararg(SourcePos pos) {
        super(NodeType.VARARG, pos);
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}