package com.github.relua.gui.services;

import com.github.relua.ast.*;
import com.github.relua.decompiler.pipeline.DecompilerResult;
import com.github.relua.gui.model.AstTreeNode;
import com.github.relua.gui.utils.LuaVariableUtils;
import com.github.relua.model.Chunk;
import com.github.relua.model.LineRange;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 后台转换 AST；完整覆盖语句、表达式与表字段，独立于 Canvas。 */
public final class AstTreeService {
    public static final class FunctionTree {
        public final String name, displayName;
        public final LineRange range;
        public final AstTreeNode root;
        public FunctionTree(String name, String displayName, LineRange range, AstTreeNode root) {
            this.name = name; this.displayName = displayName; this.range = range; this.root = root;
        }
        @Override public String toString() { return displayName; }
    }
    private static final Pattern FUNCTION = Pattern.compile("(?:local\\s+)?function\\s+([^\\s(]+)");
    private final Map<String, String> renames;
    private final Map<String, List<Integer>> sourceLines = new HashMap<>();

    public AstTreeService(String source, Map<String, String> renames) {
        this.renames = new LinkedHashMap<>(renames);
        String[] lines = source.split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            sourceLines.computeIfAbsent(lines[i].trim(), k -> new ArrayList<>()).add(i + 1);
        }
    }

    public List<FunctionTree> build(DecompilerResult result, Map<String, LineRange> ranges) {
        List<FunctionTree> functions = new ArrayList<>();
        if (result.getLuacFile() != null && result.getLuacFile().getMainChunk() != null) {
            addFunction(result.getLuacFile().getMainChunk(), result, ranges, functions);
        }
        return Collections.unmodifiableList(functions);
    }

    private void addFunction(Chunk chunk, DecompilerResult result, Map<String, LineRange> ranges, List<FunctionTree> functions) {
        if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
        Block block = result.getChunkAsts().get(chunk.getFunction());
        LineRange range = ranges.get(chunk.getFunction());
        String name = chunk.getFunction();
        String display = name;
        if (range != null) {
            for (Map.Entry<String, List<Integer>> entry : sourceLines.entrySet()) {
                if (entry.getValue().contains(range.startLine)) {
                    Matcher matcher = FUNCTION.matcher(entry.getKey());
                    if (matcher.find()) display = matcher.group(1);
                    break;
                }
            }
        }
        if (block != null) {
            int start = range == null ? 1 : range.startLine;
            int end = range == null ? Integer.MAX_VALUE : range.endLine;
            functions.add(new FunctionTree(name, display, range, convert(block, "", name, start, end)));
        }
        for (Chunk child : chunk.getSubChunks()) addFunction(child, result, ranges, functions);
    }

    public AstTreeNode convert(AstNode node, String role, String id, int start, int end) {
        if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
        List<AstTreeNode> children = new ArrayList<>();
        String text = summary(node);
        int line = findSourceLine(node, text, start, end);
        int childStart = line > 0 ? line : start;
        if (node instanceof Block) {
            int cursor = start;
            List<Statement> statements = ((Block) node).statements;
            for (int i = 0; i < statements.size(); i++) {
                AstTreeNode child = convert(statements.get(i), "statements[" + i + "]", id + "/" + i, cursor, end);
                children.add(child);
                if (child.sourceLine > 0) cursor = child.sourceLine + 1;
            }
        } else if (node instanceof Assign) {
            list(children, ((Assign)node).left, "left", id, childStart, end);
            list(children, ((Assign)node).right, "right", id, childStart, end);
        } else if (node instanceof LocalAssign) {
            list(children, ((LocalAssign)node).right, "values", id, childStart, end);
        } else if (node instanceof GlobalAssign) {
            list(children, ((GlobalAssign)node).right, "values", id, childStart, end);
        } else if (node instanceof IfStatement) {
            IfStatement value = (IfStatement)node;
            list(children, value.conditions, "conditions", id, childStart, end);
            list(children, value.blocks, "branches", id, childStart, end);
            child(children, value.elseBlock, "else", id, childStart, end);
        } else if (node instanceof WhileStatement) {
            child(children, ((WhileStatement)node).condition, "condition", id, childStart, end);
            child(children, ((WhileStatement)node).body, "body", id, childStart, end);
        } else if (node instanceof RepeatStatement) {
            child(children, ((RepeatStatement)node).body, "body", id, childStart, end);
            child(children, ((RepeatStatement)node).condition, "condition", id, childStart, end);
        } else if (node instanceof ForNumeric) {
            ForNumeric value = (ForNumeric)node;
            child(children, value.start, "start", id, childStart, end);
            child(children, value.end, "end", id, childStart, end);
            child(children, value.step, "step", id, childStart, end);
            child(children, value.body, "body", id, childStart, end);
        } else if (node instanceof ForIn) {
            list(children, ((ForIn)node).iterators, "iterators", id, childStart, end);
            child(children, ((ForIn)node).body, "body", id, childStart, end);
        } else if (node instanceof FunctionDeclaration) {
            child(children, ((FunctionDeclaration)node).func, "function", id, childStart, end);
        } else if (node instanceof FunctionLiteral) {
            child(children, ((FunctionLiteral)node).body, "body", id, childStart, end);
        } else if (node instanceof ReturnStatement) {
            list(children, ((ReturnStatement)node).values, "values", id, childStart, end);
        } else if (node instanceof ExpressionStatement) {
            child(children, ((ExpressionStatement)node).expression, "expression", id, childStart, end);
        } else if (node instanceof FunctionCall) {
            child(children, ((FunctionCall)node).callee, "callee", id, childStart, end);
            list(children, ((FunctionCall)node).args, "arguments", id, childStart, end);
        } else if (node instanceof BinaryOp) {
            child(children, ((BinaryOp)node).left, "left", id, childStart, end);
            child(children, ((BinaryOp)node).right, "right", id, childStart, end);
        } else if (node instanceof UnaryOp) {
            child(children, ((UnaryOp)node).expr, "operand", id, childStart, end);
        } else if (node instanceof IndexExpr) {
            child(children, ((IndexExpr)node).table, "table", id, childStart, end);
            child(children, ((IndexExpr)node).index, "index", id, childStart, end);
        } else if (node instanceof MemberExpr) {
            child(children, ((MemberExpr)node).table, "table", id, childStart, end);
        } else if (node instanceof TableConstructor) {
            List<TableField> fields = ((TableConstructor)node).fields;
            for (int i = 0; i < fields.size(); i++) {
                child(children, fields.get(i).key, "fields[" + i + "].key", id, childStart, end);
                child(children, fields.get(i).value, "fields[" + i + "].value", id, childStart, end);
            }
        } else if (node instanceof MultiVal) {
            list(children, ((MultiVal)node).values, "values", id, childStart, end);
        }
        return new AstTreeNode(id, role, node.getClass().getSimpleName(), text,
                node.pos == null ? -1 : node.pos.pc, node.pos == null ? -1 : node.pos.line, line, children);
    }

    private void list(List<AstTreeNode> target, List<? extends AstNode> nodes, String role, String id, int start, int end) {
        for (int i = 0; i < nodes.size(); i++) child(target, nodes.get(i), role + "[" + i + "]", id, start, end);
    }
    private void child(List<AstTreeNode> target, AstNode node, String role, String id, int start, int end) {
        if (node != null) target.add(convert(node, role, id + "/" + role, start, end));
    }
    private String summary(AstNode node) {
        String text;
        if (node instanceof Block) text = ((Block)node).statements.size() + " 条语句";
        else if (node instanceof FunctionLiteral) text = "function(" + String.join(", ", ((FunctionLiteral)node).params)
                + (((FunctionLiteral)node).vararg ? (((FunctionLiteral)node).params.isEmpty() ? "..." : ", ...") : "") + ")";
        else if (node instanceof FunctionDeclaration) text = (((FunctionDeclaration)node).isLocal ? "local " : "")
                + "function " + ((FunctionDeclaration)node).name + summary(((FunctionDeclaration)node).func).substring(8);
        else if (node instanceof IfStatement) text = "if " + ((IfStatement)node).conditions.get(0).accept(new AstPrinter()) + " then";
        else if (node instanceof WhileStatement) text = "while " + ((WhileStatement)node).condition.accept(new AstPrinter()) + " do";
        else if (node instanceof RepeatStatement) text = "repeat … until " + ((RepeatStatement)node).condition.accept(new AstPrinter());
        else if (node instanceof ForNumeric) text = "for " + ((ForNumeric)node).name + " = " + ((ForNumeric)node).start.accept(new AstPrinter()) + ", " + ((ForNumeric)node).end.accept(new AstPrinter()) + " do";
        else if (node instanceof ForIn) text = "for " + String.join(", ", ((ForIn)node).names) + " in … do";
        else if (node instanceof TableConstructor) text = "{ " + ((TableConstructor)node).fields.size() + " 个字段 }";
        else text = node.accept(new AstPrinter()).split("\\r?\\n", 2)[0];
        for (Map.Entry<String, String> rename : renames.entrySet()) {
            List<LuaVariableUtils.SymbolMatch> matches = LuaVariableUtils.findVariableOccurrences(text, rename.getKey(), -1);
            StringBuilder renamed = new StringBuilder(text);
            for (int i = matches.size() - 1; i >= 0; i--) renamed.replace(matches.get(i).start, matches.get(i).end, rename.getValue());
            text = renamed.toString();
        }
        return text;
    }
    private int findSourceLine(AstNode node, String text, int start, int end) {
        // 仅唯一且完整匹配生成源码中的语句行；重复语句不猜测位置。
        if (!(node instanceof Statement) || node instanceof Block) return -1;
        List<Integer> matches = sourceLines.get(text.trim());
        int found = -1;
        if (matches != null) for (int line : matches) if (line >= start && line <= end) {
            if (found > 0) return -1;
            found = line;
        }
        return found;
    }
}
