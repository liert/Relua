package com.github.relua.ast;

import java.util.List;

public class FunctionLiteral extends Expression {
    public final List<String> params;
    public final boolean vararg;
    public final Block body;
    
    public FunctionLiteral(List<String> params, boolean vararg, Block body, SourcePos pos) {
        super(NodeType.FUNC_LIT, pos);
        this.params = params;
        this.vararg = vararg;
        this.body = body;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}