package com.github.relua.ast;

public class IndexExpr extends Expression {
    public final Expression table;
    public final Expression index;
    
    public IndexExpr(Expression table, Expression index, SourcePos pos) {
        super(NodeType.INDEX, pos);
        this.table = table;
        this.index = index;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}