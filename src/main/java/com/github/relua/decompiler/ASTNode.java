package com.github.relua.decompiler;

import java.util.ArrayList;
import java.util.List;

/**
 * AST节点类，用于表示抽象语法树
 */
public class ASTNode {
    private NodeType type;
    private List<ASTNode> children;
    private Object value;
    
    // 节点类型枚举
    public enum NodeType {
        PROGRAM,           // 程序根节点
        BLOCK,             // 代码块
        IF_STATEMENT,      // if语句
        ELSE_CLAUSE,       // else子句
        WHILE_LOOP,        // while循环
        REPEAT_LOOP,       // repeat-until循环
        FOR_LOOP,          // for循环
        EXPRESSION,        // 表达式
        ASSIGNMENT,        // 赋值语句
        FUNCTION_CALL,     // 函数调用
        RETURN_STATEMENT,  // 返回语句
        BINARY_OP,         // 二元操作
        UNARY_OP,          // 一元操作
        VARIABLE,          // 变量
        CONSTANT,          // 常量
        SEQUENCE           // 顺序结构
    }
    
    public ASTNode(NodeType type) {
        this.type = type;
        this.children = new ArrayList<>();
    }
    
    public ASTNode(NodeType type, Object value) {
        this.type = type;
        this.value = value;
        this.children = new ArrayList<>();
    }
    
    // getter和setter方法
    public NodeType getType() { return type; }
    public void setType(NodeType type) { this.type = type; }
    public List<ASTNode> getChildren() { return children; }
    public void addChild(ASTNode child) { this.children.add(child); }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
}