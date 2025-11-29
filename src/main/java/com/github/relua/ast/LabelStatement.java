package com.github.relua.ast;

/**
 * 标签语句，用于表示 Lua 中的标签，如 ::L42::
 */
public class LabelStatement extends Statement {
    public final String label;
    
    public LabelStatement(String label, SourcePos pos) {
        super(NodeType.LABEL, pos);
        this.label = label;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
