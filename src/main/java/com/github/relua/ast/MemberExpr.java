package com.github.relua.ast;

public class MemberExpr extends Expression {
    public final Expression table;
    public final String member;
    
    public MemberExpr(Expression table, String member, SourcePos pos) {
        super(NodeType.MEMBER, pos);
        this.table = table;
        this.member = member;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}