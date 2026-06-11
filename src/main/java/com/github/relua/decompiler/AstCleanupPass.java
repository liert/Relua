package com.github.relua.decompiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

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
import com.github.relua.ast.GotoStatement;
import com.github.relua.ast.GlobalAssign;
import com.github.relua.ast.IfStatement;
import com.github.relua.ast.IndexExpr;
import com.github.relua.ast.LabelStatement;
import com.github.relua.ast.LocalAssign;
import com.github.relua.ast.MemberExpr;
import com.github.relua.ast.Name;
import com.github.relua.ast.RepeatStatement;
import com.github.relua.ast.ReturnStatement;
import com.github.relua.ast.Statement;
import com.github.relua.ast.TableConstructor;
import com.github.relua.ast.UnaryOp;
import com.github.relua.ast.WhileStatement;
import com.github.relua.decompiler.analysis.DataFlowAnalyzer;
import com.github.relua.decompiler.analysis.StructureRestorer;

public class AstCleanupPass {
    public void cleanup(Block block) {
        if (block == null) {
            return;
        }

        // 1. 数据流变量内联及点号/冒号语法糖还原
        new DataFlowAnalyzer().optimize(block);

        // 1.5 清理空的if块（nil-guard等模式产生的空条件分支）
        removeEmptyIfBlocks(block);

        // 2. 结构化控制流还原与 GOTO/Label 消解
        new StructureRestorer().restructure(block);

        // 2.5 再次清理空的if块（结构恢复可能产生新的空分支）
        removeEmptyIfBlocks(block);

        Set<TableConstructor> consumedTables = Collections.newSetFromMap(new IdentityHashMap<>());
        collectConsumedTables(block, consumedTables, true);
        removeConsumedTemporaryTables(block, consumedTables);
        removeJoinGotos(block);

        // 3. 把函数体顶层第一次出现的寄存器赋值恢复为 local
        declareTopLevelLocals(block);
    }

    private void removeEmptyIfBlocks(Block block) {
        if (block == null || block.statements == null) {
            return;
        }

        // 先递归处理嵌套块中的空if
        for (Statement statement : block.statements) {
            if (statement instanceof IfStatement) {
                IfStatement ifStmt = (IfStatement) statement;
                for (Block nested : ifStmt.blocks) {
                    removeEmptyIfBlocks(nested);
                }
                removeEmptyIfBlocks(ifStmt.elseBlock);
            } else if (statement instanceof FunctionDeclaration) {
                removeEmptyIfBlocks(((FunctionDeclaration) statement).func.body);
            } else if (statement instanceof WhileStatement) {
                removeEmptyIfBlocks(((WhileStatement) statement).body);
            } else if (statement instanceof ForNumeric) {
                removeEmptyIfBlocks(((ForNumeric) statement).body);
            } else if (statement instanceof ForIn) {
                removeEmptyIfBlocks(((ForIn) statement).body);
            }
        }

        // 先清除if块中残留的join-point goto/label（结构恢复的副产物）
        for (Statement statement : block.statements) {
            if (statement instanceof IfStatement) {
                stripTrailingJoinGotos((IfStatement) statement);
            }
        }

        // 处理空的if分支
        List<Statement> rewritten = new ArrayList<>();
        for (Statement statement : block.statements) {
            if (statement instanceof IfStatement) {
                IfStatement ifStmt = (IfStatement) statement;
                boolean allThenEmpty = true;
                for (Block b : ifStmt.blocks) {
                    if (isEffectivelyEmpty(b)) {
                        continue;
                    }
                    allThenEmpty = false;
                    break;
                }

                boolean hasElse = !isEffectivelyEmpty(ifStmt.elseBlock);

                if (allThenEmpty && !hasElse) {
                    // then块全部为空且无else（或else也为空），移除整个if语句
                    continue;
                }

                if (allThenEmpty && hasElse && ifStmt.conditions.size() == 1) {
                    // 单条件：then块为空但else非空 → 取反条件，提升else为then
                    Expression invertedCond = negateExpression(ifStmt.conditions.get(0));
                    List<Expression> newConds = new ArrayList<>();
                    newConds.add(invertedCond);
                    List<Block> newBlocks = new ArrayList<>();
                    newBlocks.add(ifStmt.elseBlock);
                    IfStatement newIf = new IfStatement(newConds, newBlocks, null, ifStmt.pos);
                    rewritten.add(newIf);
                    continue;
                }

                if (!allThenEmpty && hasElse) {
                    // 对于多条件(elseif)链，过滤掉空的分支
                    List<Expression> newConds = new ArrayList<>();
                    List<Block> newBlocks = new ArrayList<>();
                    for (int i = 0; i < ifStmt.conditions.size(); i++) {
                        Block b = ifStmt.blocks.get(i);
                        if (!isEffectivelyEmpty(b)) {
                            newConds.add(ifStmt.conditions.get(i));
                            newBlocks.add(b);
                        }
                    }
                    if (newConds.isEmpty()) {
                        continue;
                    }
                    if (newConds.size() != ifStmt.conditions.size()) {
                        IfStatement newIf = new IfStatement(newConds, newBlocks, ifStmt.elseBlock, ifStmt.pos);
                        rewritten.add(newIf);
                        continue;
                    }
                }

                if (!allThenEmpty && !hasElse && ifStmt.elseBlock != null) {
                    // else块存在但为空（或仅含goto），then非空 → 移除空的else分支
                    if (ifStmt.conditions.size() == 1) {
                        IfStatement newIf = new IfStatement(
                                ifStmt.conditions.get(0),
                                ifStmt.blocks.get(0),
                                null,
                                ifStmt.pos);
                        rewritten.add(newIf);
                        continue;
                    } else {
                        IfStatement newIf = new IfStatement(
                                new ArrayList<>(ifStmt.conditions),
                                new ArrayList<>(ifStmt.blocks),
                                null,
                                ifStmt.pos);
                        rewritten.add(newIf);
                        continue;
                    }
                }

                rewritten.add(statement);
            } else {
                rewritten.add(statement);
            }
        }
        block.statements.clear();
        block.statements.addAll(rewritten);
    }

    /**
     * 判断块是否为空或仅包含 GotoStatement/LabelStatement（join-point 残留）
     */
    private boolean isEffectivelyEmpty(Block block) {
        if (block == null || block.statements.isEmpty()) {
            return true;
        }
        for (Statement stmt : block.statements) {
            if (!(stmt instanceof GotoStatement) && !(stmt instanceof LabelStatement)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 清除 IfStatement 的 then/else 块末尾的 join-point GotoStatement
     */
    private void stripTrailingJoinGotos(IfStatement ifStmt) {
        for (Block b : ifStmt.blocks) {
            if (b != null) {
                stripTrailingGotosFromBlock(b);
            }
        }
        if (ifStmt.elseBlock != null) {
            stripTrailingGotosFromBlock(ifStmt.elseBlock);
        }
    }

    private void stripTrailingGotosFromBlock(Block block) {
        if (block == null || block.statements.isEmpty()) {
            return;
        }
        // 递归处理嵌套的 if 块
        for (Statement stmt : block.statements) {
            if (stmt instanceof IfStatement) {
                stripTrailingJoinGotos((IfStatement) stmt);
            }
        }
        // 移除块末尾的 GotoStatement 和 LabelStatement
        while (!block.statements.isEmpty()) {
            Statement last = block.statements.get(block.statements.size() - 1);
            if (last instanceof GotoStatement || last instanceof LabelStatement) {
                block.statements.remove(block.statements.size() - 1);
            } else {
                break;
            }
        }
    }

    /**
     * 对条件表达式取反
     */
    private Expression negateExpression(Expression expr) {
        if (expr instanceof UnaryOp && ((UnaryOp) expr).op.equals("not")) {
            return ((UnaryOp) expr).expr;
        }
        if (expr instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) expr;
            String negatedOp = getNegatedOperator(binary.op);
            if (negatedOp != null) {
                return new BinaryOp(negatedOp, binary.left, binary.right, binary.pos);
            }
        }
        return new UnaryOp("not", expr, expr.pos);
    }

    private String getNegatedOperator(String op) {
        switch (op) {
            case "==": return "~=";
            case "~=": return "==";
            case "<":  return ">=";
            case ">":  return "<=";
            case "<=": return ">";
            case ">=": return "<";
            default:   return null;
        }
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

    private void removeJoinGotos(Block block) {
        if (block == null) {
            return;
        }

        for (int i = 0; i + 1 < block.statements.size(); i++) {
            Statement current = block.statements.get(i);
            Statement next = block.statements.get(i + 1);
            if (current instanceof IfStatement && next instanceof LabelStatement) {
                removeTrailingGotoTo((IfStatement) current, ((LabelStatement) next).label);
            }
        }

        for (Statement statement : block.statements) {
            removeJoinGotosFromNestedBlocks(statement);
        }

        removeUnreferencedLabels(block);
    }

    private void removeJoinGotosFromNestedBlocks(Statement statement) {
        if (statement instanceof IfStatement) {
            IfStatement ifStatement = (IfStatement) statement;
            for (Block nested : ifStatement.blocks) {
                removeJoinGotos(nested);
            }
            removeJoinGotos(ifStatement.elseBlock);
        } else if (statement instanceof FunctionDeclaration) {
            removeJoinGotos(((FunctionDeclaration) statement).func.body);
        } else if (statement instanceof WhileStatement) {
            removeJoinGotos(((WhileStatement) statement).body);
        } else if (statement instanceof RepeatStatement) {
            removeJoinGotos(((RepeatStatement) statement).body);
        } else if (statement instanceof ForNumeric) {
            removeJoinGotos(((ForNumeric) statement).body);
        } else if (statement instanceof ForIn) {
            removeJoinGotos(((ForIn) statement).body);
        }
    }

    private void removeTrailingGotoTo(IfStatement ifStatement, String label) {
        for (Block nested : ifStatement.blocks) {
            removeTrailingGotoFromBlock(nested, label);
        }
        removeTrailingGotoFromBlock(ifStatement.elseBlock, label);
    }

    private void removeTrailingGotoFromBlock(Block block, String label) {
        if (block == null || block.statements.isEmpty()) {
            return;
        }

        Statement last = block.statements.get(block.statements.size() - 1);
        if (last instanceof GotoStatement && ((GotoStatement) last).label.equals(label)) {
            block.statements.remove(block.statements.size() - 1);
        } else if (last instanceof IfStatement) {
            removeTrailingGotoTo((IfStatement) last, label);
        }
    }

    private void removeUnreferencedLabels(Block block) {
        Set<String> referencedLabels = new java.util.HashSet<>();
        collectReferencedLabels(block, referencedLabels);
        block.statements.removeIf(statement -> statement instanceof LabelStatement
                && !referencedLabels.contains(((LabelStatement) statement).label));
    }

    private void collectReferencedLabels(Block block, Set<String> referencedLabels) {
        if (block == null) {
            return;
        }
        for (Statement statement : block.statements) {
            if (statement instanceof GotoStatement) {
                referencedLabels.add(((GotoStatement) statement).label);
            } else if (statement instanceof IfStatement) {
                IfStatement ifStatement = (IfStatement) statement;
                for (Block nested : ifStatement.blocks) {
                    collectReferencedLabels(nested, referencedLabels);
                }
                collectReferencedLabels(ifStatement.elseBlock, referencedLabels);
            } else if (statement instanceof FunctionDeclaration) {
                collectReferencedLabels(((FunctionDeclaration) statement).func.body, referencedLabels);
            }
        }
    }

    private void declareTopLevelLocals(Block block) {
        if (block == null || block.statements == null) {
            return;
        }

        Set<String> declared = new HashSet<>();
        List<Statement> rewritten = new ArrayList<>();

        for (Statement statement : block.statements) {
            rewritten.add(declareStatementLocalIfNeeded(statement, declared, true));
        }

        block.statements.clear();
        block.statements.addAll(rewritten);
    }

    private Statement declareStatementLocalIfNeeded(Statement statement, Set<String> declared, boolean allowNewLocal) {
        if (statement instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) statement;
            declared.addAll(local.names);
            return statement;
        }

        if (statement instanceof GlobalAssign) {
            return statement;
        }

        if (statement instanceof Assign) {
            Assign assign = (Assign) statement;

            if (allowNewLocal
                    && assign.left.size() == 1
                    && assign.right.size() == 1
                    && assign.left.get(0) instanceof Name) {
                String name = ((Name) assign.left.get(0)).name;

                if (!declared.contains(name) && shouldDeclareLocal(name, assign.right.get(0))) {
                    declared.add(name);

                    List<String> names = new ArrayList<>();
                    names.add(name);

                    return new LocalAssign(names, assign.right, assign.pos);
                }

                if (declared.contains(name)) {
                    return statement;
                }
            }

            return statement;
        }

        // 不在 if/while/repeat/for 的嵌套块里新建 local，避免把条件分支里的第一次赋值误判成局部声明。
        // 已经在父块声明过的变量，嵌套块里继续保持普通赋值。
        if (statement instanceof IfStatement) {
            IfStatement ifStatement = (IfStatement) statement;

            for (Block nested : ifStatement.blocks) {
                rewriteNestedBlockWithoutNewLocals(nested, declared);
            }

            rewriteNestedBlockWithoutNewLocals(ifStatement.elseBlock, declared);
            return statement;
        }

        if (statement instanceof WhileStatement) {
            rewriteNestedBlockWithoutNewLocals(((WhileStatement) statement).body, declared);
            return statement;
        }

        if (statement instanceof RepeatStatement) {
            rewriteNestedBlockWithoutNewLocals(((RepeatStatement) statement).body, declared);
            return statement;
        }

        if (statement instanceof ForNumeric) {
            rewriteNestedBlockWithoutNewLocals(((ForNumeric) statement).body, declared);
            return statement;
        }

        if (statement instanceof ForIn) {
            rewriteNestedBlockWithoutNewLocals(((ForIn) statement).body, declared);
            return statement;
        }

        if (statement instanceof FunctionDeclaration) {
            // 函数声明本身有自己的作用域，不共享当前函数的 declared 集合
            declareTopLevelLocals(((FunctionDeclaration) statement).func.body);
            return statement;
        }

        return statement;
    }

    private void rewriteNestedBlockWithoutNewLocals(Block block, Set<String> declared) {
        if (block == null || block.statements == null) {
            return;
        }

        List<Statement> rewritten = new ArrayList<>();

        for (Statement statement : block.statements) {
            rewritten.add(declareStatementLocalIfNeeded(statement, declared, false));
        }

        block.statements.clear();
        block.statements.addAll(rewritten);
    }

    private boolean shouldDeclareLocal(String name, Expression right) {
        // R0/R1/R2/R3... 是 Lua VM 函数帧寄存器，第一次显式赋值应恢复成 local
        if (name.matches("R\\d+")) {
            return true;
        }

        // require 返回值虽然被 convertCallInstruction 改名成 luci_util 这种形式，
        // 但本质仍然是当前函数寄存器里的局部值，不是 GETGLOBAL/SETGLOBAL。
        return isRequireCall(right);
    }

    private boolean isRequireCall(Expression expression) {
        if (!(expression instanceof FunctionCall)) {
            return false;
        }

        FunctionCall call = (FunctionCall) expression;
        return call.callee instanceof Name && "require".equals(((Name) call.callee).name);
    }
}
