package com.github.relua.ast;

import java.util.ArrayList;
import java.util.List;

public class Block extends Statement {
    public final List<Statement> statements = new ArrayList<>();
    
    public Block(SourcePos pos) {
        super(NodeType.BLOCK, pos);
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}