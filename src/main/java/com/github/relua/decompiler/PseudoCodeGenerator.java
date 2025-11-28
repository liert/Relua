package com.github.relua.decompiler;

/**
 * 伪代码生成器，负责将AST转换为类似源码的伪代码
 * 实现ASTVisitor接口，使用访问者模式遍历AST节点
 */
public class PseudoCodeGenerator implements ASTVisitor {
    private StringBuilder codeBuilder; // 用于构建伪代码
    private int indentLevel; // 缩进级别
    private static final String INDENT = "    "; // 缩进字符串
    
    /**
     * 构造函数
     */
    public PseudoCodeGenerator() {
        this.codeBuilder = new StringBuilder();
        this.indentLevel = 0;
    }
    
    /**
     * 生成伪代码
     * @param root AST根节点
     * @return 生成的伪代码
     */
    public String generatePseudoCode(ASTNode root) {
        // 清空代码构建器
        codeBuilder.setLength(0);
        indentLevel = 0;
        
        // 访问根节点
        visit(root);
        
        return codeBuilder.toString();
    }
    
    /**
     * 访问AST节点
     * @param node AST节点
     */
    private void visit(ASTNode node) {
        switch (node.getType()) {
            case PROGRAM:
                visitProgram(node);
                break;
            case BLOCK:
                visitBlock(node);
                break;
            case IF_STATEMENT:
                visitIfStatement(node);
                break;
            case ELSE_CLAUSE:
                visitElseClause(node);
                break;
            case WHILE_LOOP:
                visitWhileLoop(node);
                break;
            case REPEAT_LOOP:
                visitRepeatLoop(node);
                break;
            case FOR_LOOP:
                visitForLoop(node);
                break;
            case EXPRESSION:
                visitExpression(node);
                break;
            case ASSIGNMENT:
                visitAssignment(node);
                break;
            case FUNCTION_CALL:
                visitFunctionCall(node);
                break;
            case RETURN_STATEMENT:
                visitReturnStatement(node);
                break;
            case BINARY_OP:
                visitBinaryOp(node);
                break;
            case UNARY_OP:
                visitUnaryOp(node);
                break;
            case VARIABLE:
                visitVariable(node);
                break;
            case CONSTANT:
                visitConstant(node);
                break;
            case SEQUENCE:
                visitSequence(node);
                break;
            default:
                // 未知节点类型
                codeBuilder.append("// 未知节点类型: ").append(node.getType()).append("\n");
                break;
        }
    }
    
    /**
     * 添加缩进
     */
    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            codeBuilder.append(INDENT);
        }
    }
    
    /**
     * 增加缩进级别
     */
    private void increaseIndent() {
        indentLevel++;
    }
    
    /**
     * 减少缩进级别
     */
    private void decreaseIndent() {
        if (indentLevel > 0) {
            indentLevel--;
        }
    }
    
    @Override
    public void visitProgram(ASTNode node) {
        codeBuilder.append("-- 生成的Lua代码\n\n");
        
        // 访问所有子节点
        for (ASTNode child : node.getChildren()) {
            visit(child);
        }
    }
    
    @Override
    public void visitBlock(ASTNode node) {
        // 访问所有子节点
        for (ASTNode child : node.getChildren()) {
            visit(child);
        }
    }
    
    @Override
    public void visitIfStatement(ASTNode node) {
        indent();
        codeBuilder.append("if ");
        
        // 访问条件表达式
        if (node.getChildren().size() > 0) {
            visit(node.getChildren().get(0));
        }
        
        codeBuilder.append(" then\n");
        increaseIndent();
        
        // 访问then块
        if (node.getChildren().size() > 1) {
            visit(node.getChildren().get(1));
        }
        
        // 检查是否有else子句
        boolean hasElse = false;
        for (int i = 2; i < node.getChildren().size(); i++) {
            ASTNode child = node.getChildren().get(i);
            if (child.getType() == ASTNode.NodeType.ELSE_CLAUSE) {
                hasElse = true;
                // 访问else子句
                visitElseClause(child);
                break;
            }
        }
        
        decreaseIndent();
        indent();
        codeBuilder.append("end\n");
    }
    
    @Override
    public void visitElseClause(ASTNode node) {
        indent();
        codeBuilder.append("else\n");
        increaseIndent();
        
        // 访问else块
        for (ASTNode child : node.getChildren()) {
            visit(child);
        }
        decreaseIndent();
    }
    
    @Override
    public void visitWhileLoop(ASTNode node) {
        indent();
        codeBuilder.append("while ");
        
        // 访问条件表达式
        if (node.getChildren().size() > 0) {
            visit(node.getChildren().get(0));
        }
        
        codeBuilder.append(" do\n");
        increaseIndent();
        
        // 访问循环体
        if (node.getChildren().size() > 1) {
            visit(node.getChildren().get(1));
        }
        
        decreaseIndent();
        indent();
        codeBuilder.append("end\n");
    }
    
    @Override
    public void visitRepeatLoop(ASTNode node) {
        indent();
        codeBuilder.append("repeat\n");
        increaseIndent();
        
        // 访问循环体
        if (node.getChildren().size() > 1) {
            visit(node.getChildren().get(1));
        }
        
        decreaseIndent();
        indent();
        codeBuilder.append("until ");
        
        // 访问条件表达式
        if (node.getChildren().size() > 0) {
            visit(node.getChildren().get(0));
        }
        
        codeBuilder.append("\n");
    }
    
    @Override
    public void visitForLoop(ASTNode node) {
        indent();
        codeBuilder.append("for ");
        
        // 简化处理：只显示for循环结构
        codeBuilder.append("...");
        
        codeBuilder.append(" do\n");
        increaseIndent();
        
        // 访问循环体
        if (node.getChildren().size() > 1) {
            visit(node.getChildren().get(1));
        }
        
        decreaseIndent();
        indent();
        codeBuilder.append("end\n");
    }
    
    @Override
    public void visitExpression(ASTNode node) {
        if (node.getValue() != null) {
            codeBuilder.append(node.getValue().toString());
        } else {
            codeBuilder.append("expression");
        }
    }
    
    @Override
    public void visitAssignment(ASTNode node) {
        indent();
        
        // 访问目标变量
        if (node.getChildren().size() > 0) {
            visit(node.getChildren().get(0));
        }
        
        codeBuilder.append(" = ");
        
        // 访问赋值表达式
        if (node.getChildren().size() > 1) {
            visit(node.getChildren().get(1));
        }
        
        codeBuilder.append("\n");
    }
    
    @Override
    public void visitFunctionCall(ASTNode node) {
        indent();
        
        // 访问函数
        if (node.getChildren().size() > 0) {
            visit(node.getChildren().get(0));
        }
        
        codeBuilder.append("(");
        
        // 访问所有参数
        for (int i = 1; i < node.getChildren().size(); i++) {
            if (i > 1) {
                codeBuilder.append(", ");
            }
            visit(node.getChildren().get(i));
        }
        
        codeBuilder.append(")\n");
    }
    
    @Override
    public void visitReturnStatement(ASTNode node) {
        indent();
        codeBuilder.append("return");
        
        // 访问返回值
        for (int i = 0; i < node.getChildren().size(); i++) {
            if (i > 0) {
                codeBuilder.append(", ");
            } else {
                codeBuilder.append(" ");
            }
            visit(node.getChildren().get(i));
        }
        
        codeBuilder.append("\n");
    }
    
    @Override
    public void visitBinaryOp(ASTNode node) {
        // 访问左操作数
        if (node.getChildren().size() > 0) {
            visit(node.getChildren().get(0));
        }
        
        // 添加操作符
        codeBuilder.append(" ").append(getOperatorSymbol(node.getValue().toString())).append(" ");
        
        // 访问右操作数
        if (node.getChildren().size() > 1) {
            visit(node.getChildren().get(1));
        }
    }
    
    @Override
    public void visitUnaryOp(ASTNode node) {
        // 添加操作符
        codeBuilder.append(getOperatorSymbol(node.getValue().toString()));
        
        // 访问操作数
        if (node.getChildren().size() > 0) {
            visit(node.getChildren().get(0));
        }
    }
    
    @Override
    public void visitVariable(ASTNode node) {
        if (node.getValue() != null) {
            codeBuilder.append(node.getValue().toString());
        } else {
            codeBuilder.append("variable");
        }
    }
    
    @Override
    public void visitConstant(ASTNode node) {
        if (node.getValue() != null) {
            Object value = node.getValue();
            if (value instanceof String) {
                // 字符串添加引号
                codeBuilder.append("\"").append(value).append("\"");
            } else {
                codeBuilder.append(value.toString());
            }
        } else {
            codeBuilder.append("constant");
        }
    }
    
    @Override
    public void visitSequence(ASTNode node) {
        // 访问所有子节点
        for (ASTNode child : node.getChildren()) {
            visit(child);
        }
    }
    
    /**
     * 获取操作符的符号表示
     * @param opcode 操作码
     * @return 操作符符号
     */
    private String getOperatorSymbol(String opcode) {
        switch (opcode) {
            case "ADD":
                return "+";
            case "SUB":
                return "-";
            case "MUL":
                return "*";
            case "DIV":
                return "/";
            case "MOD":
                return "%";
            case "POW":
                return "^";
            case "UNM":
                return "-";
            case "NOT":
                return "not";
            case "LEN":
                return "#";
            case "CONCAT":
                return "..";
            case "EQ":
                return "==";
            case "LT":
                return "<";
            case "LE":
                return "<=";
            default:
                return opcode;
        }
    }
}