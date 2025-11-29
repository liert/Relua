package com.github.relua.ast;

public class NilConst extends Expression {
    public NilConst(SourcePos pos) {
        super(NodeType.NIL, pos);
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}