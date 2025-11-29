package com.github.relua.ast;

public class NumberConst extends Expression {
    public final double value;
    
    public NumberConst(double v, SourcePos pos) {
        super(NodeType.NUMBER, pos);
        this.value = v;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}