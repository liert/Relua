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
        // sb.append("do\n");
        // indent();
        for (Statement stmt : node.statements) {
            sb.append(getIndent());
            sb.append(stmt.accept(this));
            sb.append("\n");
        }
        // dedent();
        // sb.append(getIndent());
        // sb.append("end");
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
            System.out.println("thenString: " + thanString);
            sb.append(thanString);
            sb.append("\n");
            dedent();
        }
        if (node.elseBlock != null) {
            
            
            sb.append(getIndent());
            sb.append("else\n");
            indent();
            sb.append(node.elseBlock.accept(this));
            sb.append("\n");
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
        sb.append(getIndent());
        sb.append(node.body.accept(this));
        sb.append("\n");
        dedent();
        sb.append("end");
        return sb.toString();
    }
    
    @Override
    public String visit(RepeatStatement node) {
        StringBuilder sb = new StringBuilder();
        sb.append("repeat\n");
        indent();
        sb.append(getIndent());
        sb.append(node.body.accept(this));
        sb.append("\n");
        dedent();
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
        sb.append(getIndent());
        sb.append(node.body.accept(this));
        sb.append("\n");
        dedent();
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
        sb.append(getIndent());
        sb.append(node.body.accept(this));
        sb.append("\n");
        dedent();
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
        return String.valueOf(node.value);
    }
    
    @Override
    public String visit(StringConst node) {
        return "\"" + node.value + "\"";
    }
    
    @Override
    public String visit(Name node) {
        return node.name;
    }
    
    @Override
    public String visit(IndexExpr node) {
        return node.table.accept(this) + "[" + node.index.accept(this) + "]";
    }
    
    @Override
    public String visit(MemberExpr node) {
        return node.table.accept(this) + "." + node.member;
    }
    
    @Override
    public String visit(FunctionCall node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.callee.accept(this));
        sb.append("(");
        sb.append(join(node.args.stream().map(expr -> expr.accept(this)).collect(Collectors.toList()), ", "));
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
        sb.append(getIndent());
        sb.append(node.body.accept(this));
        sb.append("\n");
        dedent();
        sb.append("end");
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