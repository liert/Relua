package com.github.relua.ast;

public abstract class Expression extends AstNode {
    public Expression(NodeType type, SourcePos pos) {
        super(type, pos);
    }
}