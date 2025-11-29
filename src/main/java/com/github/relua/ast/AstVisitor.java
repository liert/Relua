package com.github.relua.ast;

public interface AstVisitor<R> {
    // Statements
    R visit(Block node);
    R visit(Assign node);
    R visit(LocalAssign node);
    R visit(GlobalAssign node);
    R visit(IfStatement node);
    R visit(WhileStatement node);
    R visit(RepeatStatement node);
    R visit(ForNumeric node);
    R visit(ForIn node);
    R visit(FunctionDeclaration node);
    R visit(ReturnStatement node);
    R visit(ExpressionStatement node);
    R visit(BreakStatement node);
    R visit(GotoStatement node);
    R visit(LabelStatement node);
    
    // Expressions
    R visit(NilConst node);
    R visit(BooleanConst node);
    R visit(NumberConst node);
    R visit(StringConst node);
    R visit(Name node);
    R visit(IndexExpr node);
    R visit(MemberExpr node);
    R visit(FunctionCall node);
    R visit(FunctionLiteral node);
    R visit(TableConstructor node);
    R visit(UnaryOp node);
    R visit(BinaryOp node);
    R visit(Vararg node);
    R visit(MultiVal node);
}