package com.github.relua.decompiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import com.github.relua.ast.Assign;
import com.github.relua.ast.BinaryOp;
import com.github.relua.ast.Block;
import com.github.relua.ast.Expression;
import com.github.relua.ast.ExpressionStatement;
import com.github.relua.ast.ForIn;
import com.github.relua.ast.ForNumeric;
import com.github.relua.ast.FunctionCall;
import com.github.relua.ast.FunctionDeclaration;
import com.github.relua.ast.FunctionLiteral;
import com.github.relua.ast.GlobalAssign;
import com.github.relua.ast.IfStatement;
import com.github.relua.ast.IndexExpr;
import com.github.relua.ast.LocalAssign;
import com.github.relua.ast.MemberExpr;
import com.github.relua.ast.Name;
import com.github.relua.ast.RepeatStatement;
import com.github.relua.ast.ReturnStatement;
import com.github.relua.ast.Statement;
import com.github.relua.ast.TableConstructor;
import com.github.relua.ast.UnaryOp;
import com.github.relua.ast.WhileStatement;

public class AstCleanupPass {
    public void cleanup(Block block) {
        if (block == null) {
            return;
        }

        Set<TableConstructor> consumedTables = Collections.newSetFromMap(new IdentityHashMap<>());
        collectConsumedTables(block, consumedTables, true);
        removeConsumedTemporaryTables(block, consumedTables);
    }

    private void removeConsumedTemporaryTables(Block block, Set<TableConstructor> consumedTables) {
        if (block == null) {
            return;
        }

        List<Statement> rewritten = new ArrayList<>();
        for (Statement statement : block.statements) {
            cleanupNestedBlocks(statement, consumedTables);
            if (!isConsumedTemporaryTableAssign(statement, consumedTables)) {
                rewritten.add(statement);
            }
        }

        block.statements.clear();
        block.statements.addAll(rewritten);
    }

    private boolean isConsumedTemporaryTableAssign(Statement statement, Set<TableConstructor> consumedTables) {
        if (!(statement instanceof Assign)) {
            return false;
        }

        Assign assign = (Assign) statement;
        if (assign.left.size() != 1 || assign.right.size() != 1) {
            return false;
        }
        if (!(assign.left.get(0) instanceof Name) || !(assign.right.get(0) instanceof TableConstructor)) {
            return false;
        }

        String name = ((Name) assign.left.get(0)).name;
        return name.matches("R\\d+") && consumedTables.contains(assign.right.get(0));
    }

    private void cleanupNestedBlocks(Statement statement, Set<TableConstructor> consumedTables) {
        if (statement instanceof IfStatement) {
            IfStatement ifStatement = (IfStatement) statement;
            for (Block nested : ifStatement.blocks) {
                removeConsumedTemporaryTables(nested, consumedTables);
            }
            removeConsumedTemporaryTables(ifStatement.elseBlock, consumedTables);
        } else if (statement instanceof FunctionDeclaration) {
            removeConsumedTemporaryTables(((FunctionDeclaration) statement).func.body, consumedTables);
        } else if (statement instanceof WhileStatement) {
            removeConsumedTemporaryTables(((WhileStatement) statement).body, consumedTables);
        } else if (statement instanceof RepeatStatement) {
            removeConsumedTemporaryTables(((RepeatStatement) statement).body, consumedTables);
        } else if (statement instanceof ForNumeric) {
            removeConsumedTemporaryTables(((ForNumeric) statement).body, consumedTables);
        } else if (statement instanceof ForIn) {
            removeConsumedTemporaryTables(((ForIn) statement).body, consumedTables);
        }
    }

    private void collectConsumedTables(Block block, Set<TableConstructor> consumedTables, boolean skipTempAssignRight) {
        for (Statement statement : block.statements) {
            collectFromStatement(statement, consumedTables, skipTempAssignRight);
        }
    }

    private void collectFromStatement(Statement statement, Set<TableConstructor> consumedTables, boolean skipTempAssignRight) {
        if (statement instanceof Assign) {
            Assign assign = (Assign) statement;
            for (Expression left : assign.left) {
                collectFromExpression(left, consumedTables);
            }
            boolean skipRight = skipTempAssignRight && isTemporaryTableAssign(assign);
            if (!skipRight) {
                for (Expression right : assign.right) {
                    collectFromExpression(right, consumedTables);
                }
            }
        } else if (statement instanceof LocalAssign) {
            for (Expression right : ((LocalAssign) statement).right) {
                collectFromExpression(right, consumedTables);
            }
        } else if (statement instanceof GlobalAssign) {
            for (Expression right : ((GlobalAssign) statement).right) {
                collectFromExpression(right, consumedTables);
            }
        } else if (statement instanceof ExpressionStatement) {
            collectFromExpression(((ExpressionStatement) statement).expression, consumedTables);
        } else if (statement instanceof ReturnStatement) {
            for (Expression value : ((ReturnStatement) statement).values) {
                collectFromExpression(value, consumedTables);
            }
        } else if (statement instanceof IfStatement) {
            IfStatement ifStatement = (IfStatement) statement;
            for (Expression condition : ifStatement.conditions) {
                collectFromExpression(condition, consumedTables);
            }
            for (Block nested : ifStatement.blocks) {
                collectConsumedTables(nested, consumedTables, skipTempAssignRight);
            }
            if (ifStatement.elseBlock != null) {
                collectConsumedTables(ifStatement.elseBlock, consumedTables, skipTempAssignRight);
            }
        } else if (statement instanceof FunctionDeclaration) {
            collectFromExpression(((FunctionDeclaration) statement).func, consumedTables);
        }
    }

    private boolean isTemporaryTableAssign(Assign assign) {
        if (assign.left.size() != 1 || assign.right.size() != 1) {
            return false;
        }
        return assign.left.get(0) instanceof Name
                && ((Name) assign.left.get(0)).name.matches("R\\d+")
                && assign.right.get(0) instanceof TableConstructor;
    }

    private void collectFromExpression(Expression expression, Set<TableConstructor> consumedTables) {
        if (expression == null) {
            return;
        }
        if (expression instanceof TableConstructor) {
            consumedTables.add((TableConstructor) expression);
        } else if (expression instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) expression;
            collectFromExpression(call.callee, consumedTables);
            for (Expression arg : call.args) {
                collectFromExpression(arg, consumedTables);
            }
        } else if (expression instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) expression;
            collectFromExpression(binary.left, consumedTables);
            collectFromExpression(binary.right, consumedTables);
        } else if (expression instanceof UnaryOp) {
            collectFromExpression(((UnaryOp) expression).expr, consumedTables);
        } else if (expression instanceof IndexExpr) {
            IndexExpr index = (IndexExpr) expression;
            collectFromExpression(index.table, consumedTables);
            collectFromExpression(index.index, consumedTables);
        } else if (expression instanceof MemberExpr) {
            collectFromExpression(((MemberExpr) expression).table, consumedTables);
        } else if (expression instanceof FunctionLiteral) {
            collectConsumedTables(((FunctionLiteral) expression).body, consumedTables, true);
        }
    }
}
