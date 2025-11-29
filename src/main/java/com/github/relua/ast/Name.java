package com.github.relua.ast;

public class Name extends Expression {
    public String name;
    public boolean isLocal = false;
    public int localIndex = -1;
    
    public Name(String name, SourcePos pos) {
        super(NodeType.NAME, pos);
        this.name = name;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }

}