package com.github.relua.ast;

import java.util.List;
import java.util.stream.Collectors;

public class AstPrinter implements AstVisitor<String> {
    private int indentLevel = 0;
    private final String indent = "    ";
    
    private String getIndent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            sb.append(indent);
        }
        return sb.toString();
    }
    
    private void indent() {
        indentLevel++;
    }
    
    private void dedent() {
        if (indentLevel > 0) {
            indentLevel--;
        }
    }
    
    private String join(List<?> list, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }
    
    // Statements
    @Override
    public String visit(Block node) {
        StringBuilder sb = new StringBuilder();
        for (Statement stmt : node.statements) {
            sb.append(getIndent());
            sb.append(stmt.accept(this));
            sb.append("\n");
        }
        return sb.toString();
    }
    
    @Override
    public String visit(LocalAssign node) {
        StringBuilder sb = new StringBuilder();
        sb.append("local ");
        sb.append(join(node.names, ", "));
        if (!node.right.isEmpty()) {
            sb.append(" = ");
            sb.append(join(node.right.stream().map(expr -> expr.accept(this)).collect(Collectors.toList()), ", "));
        }
        return sb.toString();
    }
    
    @Override
    public String visit(Assign node) {
        // 全局变量暂时先不显示在伪代码中
        // return "";
        StringBuilder sb = new StringBuilder();
        sb.append(join(node.left.stream().map(expr -> expr.accept(this)).collect(Collectors.toList()), ", "));
        sb.append(" = ");
        sb.append(join(node.right.stream().map(expr -> expr.accept(this)).collect(Collectors.toList()), ", ")); 
        return sb.toString();
    }

    @Override
    public String visit(GlobalAssign node) {
        StringBuilder sb = new StringBuilder();
        sb.append(join(node.names, ", "));
        if (!node.right.isEmpty()) {
            sb.append(" = ");
            sb.append(join(node.right.stream().map(expr -> expr.accept(this)).collect(Collectors.toList()), ", "));
        }
        return sb.toString();
    }
    
    @Override
    public String visit(IfStatement node) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < node.conditions.size(); i++) {
            if (i == 0) {
                sb.append("if ");
            } else {
                sb.append("elseif ");
            }
            sb.append(node.conditions.get(i).accept(this));
            sb.append(" then\n");
            indent();
            String thanString = node.blocks.get(i).accept(this);
            sb.append(thanString);
            dedent();
        }
        if (node.elseBlock != null) {
            sb.append(getIndent());
            sb.append("else\n");
            indent();
            sb.append(node.elseBlock.accept(this));
            dedent();
        }
        sb.append(getIndent());
        sb.append("end");
        return sb.toString();
    }
    
    @Override
    public String visit(WhileStatement node) {
        StringBuilder sb = new StringBuilder();
        sb.append("while ");
        sb.append(node.condition.accept(this));
        sb.append(" do\n");
        indent();
        sb.append(node.body.accept(this));
        dedent();
        sb.append(getIndent());
        sb.append("end");
        return sb.toString();
    }
    
    @Override
    public String visit(RepeatStatement node) {
        StringBuilder sb = new StringBuilder();
        sb.append("repeat\n");
        indent();
        sb.append(node.body.accept(this));
        dedent();
        sb.append(getIndent());
        sb.append("until ");
        sb.append(node.condition.accept(this));
        return sb.toString();
    }
    
    @Override
    public String visit(ForNumeric node) {
        StringBuilder sb = new StringBuilder();
        sb.append("for ");
        sb.append(node.name);
        sb.append(" = ");
        sb.append(node.start.accept(this));
        sb.append(", ");
        sb.append(node.end.accept(this));
        if (node.step != null) {
            sb.append(", ");
            sb.append(node.step.accept(this));
        }
        sb.append(" do\n");
        indent();
        sb.append(node.body.accept(this));
        dedent();
        sb.append(getIndent());
        sb.append("end");
        return sb.toString();
    }
    
    @Override
    public String visit(ForIn node) {
        StringBuilder sb = new StringBuilder();
        sb.append("for ");
        sb.append(join(node.names, ", "));
        sb.append(" in ");
        sb.append(join(node.iterators.stream().map(expr -> expr.accept(this)).collect(Collectors.toList()), ", "));
        sb.append(" do\n");
        indent();
        sb.append(node.body.accept(this));
        dedent();
        sb.append(getIndent());
        sb.append("end");
        return sb.toString();
    }
    
    @Override
    public String visit(FunctionDeclaration node) {
        StringBuilder sb = new StringBuilder();
        if (node.isLocal) {
            sb.append("local ");
        }
        sb.append("function ");
        if (node.name != null) {
            sb.append(node.name);
        }
        sb.append(node.func.accept(this));
        return sb.toString();
    }
    
    @Override
    public String visit(ReturnStatement node) {
        StringBuilder sb = new StringBuilder();
        sb.append("return");
        if (!node.values.isEmpty()) {
            sb.append(" ");
            sb.append(join(node.values.stream().map(expr -> expr.accept(this)).collect(Collectors.toList()), ", "));
        }
        return sb.toString();
    }
    
    @Override
    public String visit(ExpressionStatement node) {
        return node.expression.accept(this);
    }
    
    @Override
    public String visit(BreakStatement node) {
        return "break";
    }
    
    @Override
    public String visit(GotoStatement node) {
        // dedent();
        return "goto " + node.label;
    }
    
    @Override
    public String visit(LabelStatement node) {
        return "::" + node.label + "::";
    }
    
    // Expressions
    @Override
    public String visit(NilConst node) {
        return "nil";
    }
    
    @Override
    public String visit(BooleanConst node) {
        return node.value ? "true" : "false";
    }
    
    @Override
    public String visit(NumberConst node) {
        if (node.value % 1 == 0) {
            return String.valueOf((long) node.value);
        }
        return String.valueOf(node.value);
    }
    
    @Override
    public String visit(StringConst node) {
        return "\"" + escapeLuaString(node.value) + "\"";
    }

    private String escapeLuaString(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 32) {
                        sb.append(String.format("\\%03d", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }
    
    @Override
    public String visit(Name node) {
        return node.name;
    }
    
    @Override
    public String visit(IndexExpr node) {
        if (node.index instanceof StringConst) {
            String key = ((StringConst) node.index).value;
            if (key.startsWith("\"") && key.endsWith("\"") && key.length() >= 2) {
                key = key.substring(1, key.length() - 1);
            }
            if (key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                return node.table.accept(this) + "." + key;
            }
        }
        return node.table.accept(this) + "[" + node.index.accept(this) + "]";
    }
    
    @Override
    public String visit(MemberExpr node) {
        return node.table.accept(this) + "." + node.member;
    }
    
    @Override
    public String visit(FunctionCall node) {
        StringBuilder sb = new StringBuilder();
        
        if (node.isMethodCall && node.callee instanceof MemberExpr) {
            // 方法调用，使用冒号语法
            MemberExpr memberExpr = (MemberExpr) node.callee;
            sb.append(memberExpr.table.accept(this));
            sb.append(":");
            sb.append(memberExpr.member);
        } else {
            // 普通函数调用
            sb.append(node.callee.accept(this));
        }
        
        sb.append("(");
        List<Expression> printArgs = node.args;
        if (node.isMethodCall && node.callee instanceof MemberExpr && !printArgs.isEmpty()) {
            printArgs = printArgs.subList(1, printArgs.size());
        }
        sb.append(join(printArgs.stream().map(expr -> expr.accept(this)).collect(Collectors.toList()), ", "));
        sb.append(")");
        return sb.toString();
    }
    
    @Override
    public String visit(FunctionLiteral node) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        if (!node.params.isEmpty()) {
            sb.append(join(node.params, ", "));
            if (node.vararg) {
                sb.append(", ...");
            }
        } else if (node.vararg) {
            sb.append("...");
        }
        sb.append(")\n");
        indent();
        if (node.getChunk() != null) {
            sb.append(getIndent()).append("--[=[RELUA_CHUNK_START:").append(node.getChunk().getFunction()).append("]=]\n");
        }
        sb.append(node.body.accept(this));
        if (node.getChunk() != null) {
            sb.append(getIndent()).append("--[=[RELUA_CHUNK_END:").append(node.getChunk().getFunction()).append("]=]\n");
        }
        dedent();
        sb.append("end\n");
        return sb.toString();
    }
    
    @Override
    public String visit(TableConstructor node) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (!node.fields.isEmpty()) {
            sb.append(" ");
            sb.append(join(node.fields.stream().map(field -> {
                if (field.key != null) {
                    return field.key.accept(this) + " = " + field.value.accept(this);
                } else {
                    return field.value.accept(this);
                }
            }).collect(Collectors.toList()), ", "));
            sb.append(" ");
        }
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public String visit(UnaryOp node) {
        return node.op + " " + node.expr.accept(this);
    }
    
    @Override
    public String visit(BinaryOp node) {
        return node.left.accept(this) + " " + node.op + " " + node.right.accept(this);
    }
    
    @Override
    public String visit(Vararg node) {
        return "...";
    }
    
    @Override
    public String visit(MultiVal node) {
        return join(node.values.stream().map(expr -> expr.accept(this)).collect(Collectors.toList()), ", ");
    }
}
