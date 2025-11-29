package com.github.relua.ast;

import java.util.List;

public class LocalAssign extends Statement {
    public final List<String> names;
    public final List<Expression> right;
    
    public LocalAssign(List<String> names, List<Expression> right, SourcePos pos) {
        super(NodeType.LOCAL_ASSIGN, pos);
        this.names = names;
        this.right = right;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}