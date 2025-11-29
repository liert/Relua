package com.github.relua.ast;

public class ForNumeric extends Statement {
    public final String name;
    public final Expression start;
    public final Expression end;
    public final Expression step;
    public final Block body;
    
    public ForNumeric(String name, Expression start, Expression end, Expression step, Block body, SourcePos pos) {
        super(NodeType.FORNUM, pos);
        this.name = name;
        this.start = start;
        this.end = end;
        this.step = step;
        this.body = body;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}