package com.github.relua.ast;

public abstract class AstNode {
    public final NodeType type;
    public final SourcePos pos;
    
    public AstNode(NodeType type, SourcePos pos) {
        this.type = type;
        this.pos = pos;
    }
    
    public abstract <R> R accept(AstVisitor<R> visitor);

    public enum NodeType {
        PROGRAM, BLOCK,
        // statements
        LOCAL_ASSIGN, ASSIGN, IF, WHILE, REPEAT, FORNUM, FORIN, FUNC_DECL,
        RETURN, EXPR_STMT, BREAK, GOTO, LABEL,
        // expressions
        NIL, BOOL, NUMBER, STRING, NAME, INDEX, MEMBER, FUNC_CALL, FUNC_LIT,
        TABLE_CONSTR, UNARY_OP, BINARY_OP, VARARG, MULTI
    }
}