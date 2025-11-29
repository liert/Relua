package com.github.relua.ast;

public abstract class Statement extends AstNode {
    public Statement(NodeType type, SourcePos pos) {
        super(type, pos);
    }
}