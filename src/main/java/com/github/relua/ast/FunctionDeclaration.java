package com.github.relua.ast;

public class FunctionDeclaration extends Statement {
    public final String name;
    public final FunctionLiteral func;
    public final boolean isLocal;
    
    public FunctionDeclaration(String name, FunctionLiteral func, boolean isLocal, SourcePos pos) {
        super(NodeType.FUNC_DECL, pos);
        this.name = name;
        this.func = func;
        this.isLocal = isLocal;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}