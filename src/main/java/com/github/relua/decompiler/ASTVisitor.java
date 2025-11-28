package com.github.relua.decompiler;

/**
 * AST访问者接口，用于遍历和处理AST节点
 * 实现访问者模式，便于扩展不同的处理逻辑
 */
public interface ASTVisitor {
    
    /**
     * 访问程序节点
     * @param node 程序节点
     */
    void visitProgram(ASTNode node);
    
    /**
     * 访问代码块节点
     * @param node 代码块节点
     */
    void visitBlock(ASTNode node);
    
    /**
     * 访问if语句节点
     * @param node if语句节点
     */
    void visitIfStatement(ASTNode node);
    
    /**
     * 访问else子句节点
     * @param node else子句节点
     */
    void visitElseClause(ASTNode node);
    
    /**
     * 访问while循环节点
     * @param node while循环节点
     */
    void visitWhileLoop(ASTNode node);
    
    /**
     * 访问repeat-until循环节点
     * @param node repeat-until循环节点
     */
    void visitRepeatLoop(ASTNode node);
    
    /**
     * 访问for循环节点
     * @param node for循环节点
     */
    void visitForLoop(ASTNode node);
    
    /**
     * 访问表达式节点
     * @param node 表达式节点
     */
    void visitExpression(ASTNode node);
    
    /**
     * 访问赋值语句节点
     * @param node 赋值语句节点
     */
    void visitAssignment(ASTNode node);
    
    /**
     * 访问函数调用节点
     * @param node 函数调用节点
     */
    void visitFunctionCall(ASTNode node);
    
    /**
     * 访问返回语句节点
     * @param node 返回语句节点
     */
    void visitReturnStatement(ASTNode node);
    
    /**
     * 访问二元操作节点
     * @param node 二元操作节点
     */
    void visitBinaryOp(ASTNode node);
    
    /**
     * 访问一元操作节点
     * @param node 一元操作节点
     */
    void visitUnaryOp(ASTNode node);
    
    /**
     * 访问变量节点
     * @param node 变量节点
     */
    void visitVariable(ASTNode node);
    
    /**
     * 访问常量节点
     * @param node 常量节点
     */
    void visitConstant(ASTNode node);
    
    /**
     * 访问顺序结构节点
     * @param node 顺序结构节点
     */
    void visitSequence(ASTNode node);
}