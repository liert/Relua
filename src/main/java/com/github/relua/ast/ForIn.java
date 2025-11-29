package com.github.relua.ast;

import java.util.List;

public class ForIn extends Statement {
    public final List<String> names;
    public final List<Expression> iterators;
    public final Block body;
    
    public ForIn(List<String> names, List<Expression> iterators, Block body, SourcePos pos) {
        super(NodeType.FORIN, pos);
        this.names = names;
        this.iterators = iterators;
        this.body = body;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}