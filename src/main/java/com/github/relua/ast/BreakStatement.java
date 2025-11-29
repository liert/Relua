package com.github.relua.ast;

public class BreakStatement extends Statement {
    public BreakStatement(SourcePos pos) {
        super(NodeType.BREAK, pos);
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}