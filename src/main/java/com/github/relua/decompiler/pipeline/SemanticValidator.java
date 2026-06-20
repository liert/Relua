package com.github.relua.decompiler.pipeline;

import com.github.relua.ast.*;
import java.util.*;

/**
 * 语义校验器，在 AST 优化完成后验证 AST 结构的正确性（如 Goto 标签存在性等）
 */
public class SemanticValidator {

    public static List<String> validate(Block block) {
        List<String> errors = new ArrayList<>();
        if (block == null) {
            return errors;
        }

        Set<String> definedLabels = new HashSet<>();
        List<String> targetedLabels = new ArrayList<>();

        collectLabelsAndGotos(block, definedLabels, targetedLabels, errors);

        for (String targeted : targetedLabels) {
            if (!definedLabels.contains(targeted)) {
                errors.add("Goto statement targets missing label: " + targeted);
            }
        }

        return errors;
    }

    private static void collectLabelsAndGotos(AstNode node, Set<String> definedLabels, List<String> targetedLabels, List<String> errors) {
        if (node == null) {
            return;
        }

        if (node instanceof LabelStatement) {
            definedLabels.add(((LabelStatement) node).label);
        } else if (node instanceof GotoStatement) {
            targetedLabels.add(((GotoStatement) node).label);
        } else if (node instanceof Block) {
            for (Statement stmt : ((Block) node).statements) {
                collectLabelsAndGotos(stmt, definedLabels, targetedLabels, errors);
            }
        } else if (node instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) node;
            for (Expression cond : ifStmt.conditions) {
                collectLabelsAndGotos(cond, definedLabels, targetedLabels, errors);
            }
            for (Block b : ifStmt.blocks) {
                collectLabelsAndGotos(b, definedLabels, targetedLabels, errors);
            }
            collectLabelsAndGotos(ifStmt.elseBlock, definedLabels, targetedLabels, errors);
        } else if (node instanceof WhileStatement) {
            WhileStatement wh = (WhileStatement) node;
            collectLabelsAndGotos(wh.condition, definedLabels, targetedLabels, errors);
            collectLabelsAndGotos(wh.body, definedLabels, targetedLabels, errors);
        } else if (node instanceof RepeatStatement) {
            RepeatStatement rep = (RepeatStatement) node;
            collectLabelsAndGotos(rep.condition, definedLabels, targetedLabels, errors);
            collectLabelsAndGotos(rep.body, definedLabels, targetedLabels, errors);
        } else if (node instanceof ForNumeric) {
            ForNumeric fn = (ForNumeric) node;
            collectLabelsAndGotos(fn.start, definedLabels, targetedLabels, errors);
            collectLabelsAndGotos(fn.end, definedLabels, targetedLabels, errors);
            collectLabelsAndGotos(fn.step, definedLabels, targetedLabels, errors);
            collectLabelsAndGotos(fn.body, definedLabels, targetedLabels, errors);
        } else if (node instanceof ForIn) {
            ForIn fi = (ForIn) node;
            for (Expression iterator : fi.iterators) {
                collectLabelsAndGotos(iterator, definedLabels, targetedLabels, errors);
            }
            collectLabelsAndGotos(fi.body, definedLabels, targetedLabels, errors);
        } else if (node instanceof FunctionDeclaration) {
            collectLabelsAndGotos(((FunctionDeclaration) node).func, definedLabels, targetedLabels, errors);
        } else if (node instanceof FunctionLiteral) {
            collectLabelsAndGotos(((FunctionLiteral) node).body, definedLabels, targetedLabels, errors);
        } else if (node instanceof Assign) {
            Assign assign = (Assign) node;
            for (Expression expr : assign.left) {
                collectLabelsAndGotos(expr, definedLabels, targetedLabels, errors);
            }
            for (Expression expr : assign.right) {
                collectLabelsAndGotos(expr, definedLabels, targetedLabels, errors);
            }
        } else if (node instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) node;
            for (Expression expr : local.right) {
                collectLabelsAndGotos(expr, definedLabels, targetedLabels, errors);
            }
        } else if (node instanceof GlobalAssign) {
            GlobalAssign glob = (GlobalAssign) node;
            for (Expression expr : glob.right) {
                collectLabelsAndGotos(expr, definedLabels, targetedLabels, errors);
            }
        } else if (node instanceof ExpressionStatement) {
            collectLabelsAndGotos(((ExpressionStatement) node).expression, definedLabels, targetedLabels, errors);
        } else if (node instanceof ReturnStatement) {
            for (Expression expr : ((ReturnStatement) node).values) {
                collectLabelsAndGotos(expr, definedLabels, targetedLabels, errors);
            }
        } else if (node instanceof BinaryOp) {
            collectLabelsAndGotos(((BinaryOp) node).left, definedLabels, targetedLabels, errors);
            collectLabelsAndGotos(((BinaryOp) node).right, definedLabels, targetedLabels, errors);
        } else if (node instanceof UnaryOp) {
            collectLabelsAndGotos(((UnaryOp) node).expr, definedLabels, targetedLabels, errors);
        } else if (node instanceof IndexExpr) {
            collectLabelsAndGotos(((IndexExpr) node).table, definedLabels, targetedLabels, errors);
            collectLabelsAndGotos(((IndexExpr) node).index, definedLabels, targetedLabels, errors);
        } else if (node instanceof MemberExpr) {
            collectLabelsAndGotos(((MemberExpr) node).table, definedLabels, targetedLabels, errors);
        } else if (node instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) node;
            collectLabelsAndGotos(call.callee, definedLabels, targetedLabels, errors);
            for (Expression arg : call.args) {
                collectLabelsAndGotos(arg, definedLabels, targetedLabels, errors);
            }
        } else if (node instanceof TableConstructor) {
            TableConstructor tc = (TableConstructor) node;
            if (tc.fields != null) {
                for (TableField field : tc.fields) {
                    collectLabelsAndGotos(field.key, definedLabels, targetedLabels, errors);
                    collectLabelsAndGotos(field.value, definedLabels, targetedLabels, errors);
                }
            }
        }
    }
}
