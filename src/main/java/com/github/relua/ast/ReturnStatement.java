package com.github.relua.ast;

import java.util.List;

public class ReturnStatement extends Statement {
    public final List<Expression> values;
    
    public ReturnStatement(List<Expression> values, SourcePos pos) {
        super(NodeType.RETURN, pos);
        this.values = values;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}