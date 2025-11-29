package com.github.relua.ast;

public class AstTransformer implements AstVisitor<AstNode> {
    // Statements
    @Override
    public AstNode visit(Block node) {
        return node;
    }
    
    @Override
    public AstNode visit(Assign node) {
        return node;
    }

    @Override
    public AstNode visit(LocalAssign node) {
        return node;
    }

    @Override
    public AstNode visit(GlobalAssign node) {
        return node;
    }
    
    @Override
    public AstNode visit(IfStatement node) {
        return node;
    }
    
    @Override
    public AstNode visit(WhileStatement node) {
        return node;
    }
    
    @Override
    public AstNode visit(RepeatStatement node) {
        return node;
    }
    
    @Override
    public AstNode visit(ForNumeric node) {
        return node;
    }
    
    @Override
    public AstNode visit(ForIn node) {
        return node;
    }
    
    @Override
    public AstNode visit(FunctionDeclaration node) {
        return node;
    }
    
    @Override
    public AstNode visit(ReturnStatement node) {
        return node;
    }
    
    @Override
    public AstNode visit(ExpressionStatement node) {
        return node;
    }
    
    @Override
    public AstNode visit(BreakStatement node) {
        return node;
    }
    
    @Override
    public AstNode visit(GotoStatement node) {
        return node;
    }
    
    @Override
    public AstNode visit(LabelStatement node) {
        return node;
    }
    
    // Expressions
    @Override
    public AstNode visit(NilConst node) {
        return node;
    }
    
    @Override
    public AstNode visit(BooleanConst node) {
        return node;
    }
    
    @Override
    public AstNode visit(NumberConst node) {
        return node;
    }
    
    @Override
    public AstNode visit(StringConst node) {
        return node;
    }
    
    @Override
    public AstNode visit(Name node) {
        return node;
    }
    
    @Override
    public AstNode visit(IndexExpr node) {
        return node;
    }
    
    @Override
    public AstNode visit(MemberExpr node) {
        return node;
    }
    
    @Override
    public AstNode visit(FunctionCall node) {
        return node;
    }
    
    @Override
    public AstNode visit(FunctionLiteral node) {
        return node;
    }
    
    @Override
    public AstNode visit(TableConstructor node) {
        return node;
    }
    
    @Override
    public AstNode visit(UnaryOp node) {
        return node;
    }
    
    @Override
    public AstNode visit(BinaryOp node) {
        return node;
    }
    
    @Override
    public AstNode visit(Vararg node) {
        return node;
    }
    
    @Override
    public AstNode visit(MultiVal node) {
        return node;
    }
}