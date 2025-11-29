package com.github.relua.ast;

public class StringConst extends Expression {
    public final String value;
    
    public StringConst(String v, SourcePos pos) {
        super(NodeType.STRING, pos);
        this.value = v;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}