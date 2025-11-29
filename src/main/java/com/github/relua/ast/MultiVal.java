package com.github.relua.ast;

import java.util.List;

public class MultiVal extends Expression {
    public final List<Expression> values;
    
    public MultiVal(List<Expression> values, SourcePos pos) {
        super(NodeType.MULTI, pos);
        this.values = values;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}