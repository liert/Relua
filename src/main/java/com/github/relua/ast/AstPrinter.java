package com.github.relua.ast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.github.relua.util.LuaStringEscapeUtils;

public class AstPrinter implements AstVisitor<String> {
    public static final int PREC_LOWEST = 0;
    public static final int PREC_OR = 1;
    public static final int PREC_AND = 2;
    public static final int PREC_COMPARE = 3;
    public static final int PREC_CONCAT = 4;
    public static final int PREC_ADD_SUB = 5;
    public static final int PREC_MUL_DIV_MOD = 6;
    public static final int PREC_UNARY = 7;
    public static final int PREC_POWER = 8;
    public static final int PREC_POSTFIX = 9;
    public static final int PREC_PRIMARY = 10;

    private static final Set<String> LUA_KEYWORDS = new HashSet<>(Arrays.asList(
        "and", "break", "do", "else", "elseif", "end", "false", "for", "function",
        "goto", "if", "in", "local", "nil", "not", "or", "repeat", "return", "then", "true", "until", "while"
    ));

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
                sb.append(getIndent());
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
        sb.append(renderFunctionLiteral(node.func, false));
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
        if (node.isForLoop()) {
            return "if " + node.getForCondition() + " then\n" + 
                   getIndent() + "    " + node.getForAssignLeft() + " = " + node.getForAssignRight() + "\n" +
                   getIndent() + "    goto " + node.label + "\n" +
                   getIndent() + "end";
        }
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
        return "\"" + LuaStringEscapeUtils.escape(node.value) + "\"";
    }
    
    @Override
    public String visit(Name node) {
        return node.name;
    }
    
    @Override
    public String visit(IndexExpr node) {
        String tableStr = formatPostfixReceiver(node.table);
        if (node.index instanceof StringConst) {
            String key = ((StringConst) node.index).value;
            if (key.startsWith("\"") && key.endsWith("\"") && key.length() >= 2) {
                key = key.substring(1, key.length() - 1);
            }
            if (isValidLuaIdentifier(key)) {
                return tableStr + "." + key;
            }
        }
        return tableStr + "[" + node.index.accept(this) + "]";
    }
    
    @Override
    public String visit(MemberExpr node) {
        return formatPostfixReceiver(node.table) + "." + node.member;
    }
    
    @Override
    public String visit(FunctionCall node) {
        StringBuilder sb = new StringBuilder();
        
        if (node.isMethodCall && node.callee instanceof MemberExpr) {
            // 方法调用，使用冒号语法
            MemberExpr memberExpr = (MemberExpr) node.callee;
            sb.append(formatPostfixReceiver(memberExpr.table));
            sb.append(":");
            sb.append(memberExpr.member);
        } else {
            // 普通函数调用
            sb.append(formatPostfixReceiver(node.callee));
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
        return renderFunctionLiteral(node, true);
    }

    private String renderFunctionLiteral(FunctionLiteral node, boolean includeFunctionKeyword) {
        StringBuilder sb = new StringBuilder();
        if (includeFunctionKeyword) {
            sb.append("function");
        }
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
        sb.append(getIndent()).append("end");
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
        String expr = formatUnaryChild(node.expr, node);
        if (node.op != null && !node.op.isEmpty() && Character.isLetter(node.op.charAt(0))) {
            return node.op + " " + expr;
        }
        if ("-".equals(node.op) && expr.startsWith("-")) {
            return "- " + expr;
        }
        return node.op + expr;
    }

    private String formatUnaryChild(Expression child, UnaryOp parent) {
        String rendered = child.accept(this);
        if (child instanceof BinaryOp) {
            return "(" + rendered + ")";
        }
        if ("-".equals(parent.op) && child instanceof UnaryOp && "-".equals(((UnaryOp) child).op)) {
            return "(" + rendered + ")";
        }
        int childPrec = getPrecedence(child);
        if (childPrec < PREC_UNARY) {
            return "(" + rendered + ")";
        }
        return rendered;
    }
    
    @Override
    public String visit(BinaryOp node) {
        return formatBinaryChild(node.left, node, true) + " " + node.op + " " + formatBinaryChild(node.right, node, false);
    }

    private String formatBinaryChild(Expression child, BinaryOp parent, boolean isLeft) {
        String rendered = child.accept(this);
        if (child instanceof BinaryOp) {
            BinaryOp childBinary = (BinaryOp) child;
            int childPrecedence = getBinaryOpPrecedence(childBinary.op);
            int parentPrecedence = getBinaryOpPrecedence(parent.op);
            if (childPrecedence < parentPrecedence
                    || (childPrecedence == parentPrecedence && needsParensForEqualPrecedence(parent.op, childBinary.op, isLeft))) {
                return "(" + rendered + ")";
            }
            return rendered;
        }

        int childPrecedence = getPrecedence(child);
        int parentPrecedence = getBinaryOpPrecedence(parent.op);
        if (childPrecedence < parentPrecedence) {
            return "(" + rendered + ")";
        }
        return rendered;
    }

    private boolean needsParensForEqualPrecedence(String parentOp, String childOp, boolean isLeft) {
        if (isComparison(parentOp)) {
            return true;
        }
        if ("..".equals(parentOp)) {
            return false;
        }
        if ("^".equals(parentOp)) {
            return isLeft;
        }
        if (isLeft) {
            return false;
        }
        return !isAssociative(parentOp) || !parentOp.equals(childOp);
    }

    private boolean isRightAssociative(String op) {
        return "^".equals(op) || "..".equals(op);
    }

    private boolean isComparison(String op) {
        return "<".equals(op) || ">".equals(op) || "<=".equals(op)
                || ">=".equals(op) || "~=".equals(op) || "==".equals(op);
    }

    private boolean isAssociative(String op) {
        return "+".equals(op) || "*".equals(op) || "and".equals(op) || "or".equals(op);
    }

    private String formatPostfixReceiver(Expression receiver) {
        String rendered = receiver.accept(this);
        if (isValidPrefixExp(receiver)) {
            return rendered;
        }
        return "(" + rendered + ")";
    }

    private boolean isValidPrefixExp(Expression expr) {
        return expr instanceof Name
                || expr instanceof MemberExpr
                || expr instanceof IndexExpr
                || expr instanceof FunctionCall;
    }

    private boolean isValidLuaIdentifier(String str) {
        return str != null && str.matches("[A-Za-z_][A-Za-z0-9_]*") && !LUA_KEYWORDS.contains(str);
    }

    public static int getPrecedence(Expression expr) {
        if (expr instanceof BinaryOp) {
            return getBinaryOpPrecedence(((BinaryOp) expr).op);
        }
        if (expr instanceof UnaryOp) {
            return PREC_UNARY;
        }
        if (expr instanceof MemberExpr || expr instanceof IndexExpr || expr instanceof FunctionCall) {
            return PREC_POSTFIX;
        }
        if (expr instanceof Name || expr instanceof NilConst || expr instanceof BooleanConst
                || expr instanceof NumberConst || expr instanceof StringConst || expr instanceof Vararg
                || expr instanceof TableConstructor || expr instanceof FunctionLiteral) {
            return PREC_PRIMARY;
        }
        return PREC_LOWEST;
    }

    public static int getBinaryOpPrecedence(String op) {
        switch (op) {
            case "or":
                return PREC_OR;
            case "and":
                return PREC_AND;
            case "<":
            case ">":
            case "<=":
            case ">=":
            case "~=":
            case "==":
                return PREC_COMPARE;
            case "..":
                return PREC_CONCAT;
            case "+":
            case "-":
                return PREC_ADD_SUB;
            case "*":
            case "/":
            case "%":
                return PREC_MUL_DIV_MOD;
            case "^":
                return PREC_POWER;
            default:
                return PREC_LOWEST;
        }
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
