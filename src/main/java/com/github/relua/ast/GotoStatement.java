package com.github.relua.ast;

public class GotoStatement extends Statement {
    public final String label;
    
    public GotoStatement(String label, SourcePos pos) {
        super(NodeType.GOTO, pos);
        this.label = label;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}