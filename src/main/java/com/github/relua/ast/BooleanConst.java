package com.github.relua.ast;

public class BooleanConst extends Expression {
    public final boolean value;
    
    public BooleanConst(boolean v, SourcePos pos) {
        super(NodeType.BOOL, pos);
        this.value = v;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}