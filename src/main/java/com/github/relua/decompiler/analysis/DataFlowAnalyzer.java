package com.github.relua.decompiler.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.relua.ast.*;

public class DataFlowAnalyzer {
    private Block topBlock;

    public void optimize(Block block) {
        optimize(block, true);
    }

    private void optimize(Block block, boolean isTopLevel) {
        if (block == null || block.statements == null) {
            return;
        }
        if (this.topBlock == null) {
            this.topBlock = block;
        }

        // 1. 递归优化所有嵌套的 Block
        for (Statement stmt : block.statements) {
            optimizeNestedBlocks(stmt, isTopLevel);
        }

        // 2. 先消除死寄存器赋值（被后续赋值覆盖且未被读取的寄存器赋值）
        // 必须在内联之前执行，否则内联会消耗覆盖赋值导致死赋值无法被检测
        removeDeadRegisterAssignments(block, isTopLevel);

        // 3. 在当前 Block 层级执行局部变量内联 (Expression Inlining)
        inlineBlockVariables(block, isTopLevel);

        // 4. 自底向上重写当前 Block 里的所有表达式（还原点号和冒号语法糖）
        rewriteBlockExpressions(block);
    }

    private void optimizeNestedBlocks(Statement statement, boolean isTopLevel) {
        if (statement instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) statement;
            for (Block nested : ifStmt.blocks) {
                optimize(nested, false);
            }
            optimize(ifStmt.elseBlock, false);
        } else if (statement instanceof FunctionDeclaration) {
            optimize(((FunctionDeclaration) statement).func.body, true);
        } else if (statement instanceof WhileStatement) {
            optimize(((WhileStatement) statement).body, false);
        } else if (statement instanceof RepeatStatement) {
            optimize(((RepeatStatement) statement).body, false);
        } else if (statement instanceof ForNumeric) {
            optimize(((ForNumeric) statement).body, false);
        } else if (statement instanceof ForIn) {
            optimize(((ForIn) statement).body, false);
        }
    }

    /**
     * 对 Block 执行数据流变量内联优化
     */
    private void inlineBlockVariables(Block block, boolean isTopLevel) {
        List<Statement> stmts = block.statements;
        boolean changed = true;

        while (changed) {
            changed = false;
            for (int i = 0; i < stmts.size(); i++) {
                Statement stmt = stmts.get(i);
                if (isRegisterAssign(stmt)) {
                    String regName = getAssignedRegisterName(stmt);
                    Expression defExpr;
                    if (stmt instanceof Assign) {
                        defExpr = ((Assign) stmt).right.get(0);
                    } else {
                        LocalAssign la = (LocalAssign) stmt;
                        if (la.right == null || la.right.isEmpty()) {
                            continue;
                        }
                        defExpr = la.right.get(0);
                    }

                    // 收集定义表达式中依赖的所有变量名
                    Set<String> dependencies = new HashSet<>();
                    collectReferencedVariables(defExpr, dependencies);

                    // 寻找该寄存器的下一个定义点与被使用点
                    int useCount = 0;
                    int useIndex = -1;
                    boolean dependencyBroken = false;
                    boolean escaped = false;

                    for (int j = i + 1; j < stmts.size(); j++) {
                        Statement nextStmt = stmts.get(j);

                        // 如果在定义和使用之间有任何控制流标签或跳转语句，这表示有控制流分叉/汇合，在此之后内联是不安全的
                        if (nextStmt instanceof GotoStatement || nextStmt instanceof LabelStatement) {
                            escaped = true;
                        }

                        // 检查在这期间依赖 of 变量是否被重新定值
                        if (hasAssignmentTo(nextStmt, dependencies)) {
                            dependencyBroken = true;
                        }

                        // 检查 R_x 是否被用作表写入目标（如 R0["key"] = value），
                        // 此时 R_x 指向的表发生了变异，R_x 不再等价于最初的定值表达式。
                        if (registerUsedAsMutatedTable(nextStmt, regName)) {
                            dependencyBroken = true;
                        }

                        // 检查 R_x 自身是否被重新定值
                        if (hasAssignmentTo(nextStmt, regName)) {
                            // 遇到 R_x 的下一个定值点，在此之后的 Use 与当前定值无关，停止扫描
                            break;
                        }

                        // 统计 R_x 的使用
                        int usesInStmt = countVariableUses(nextStmt, regName);
                        if (usesInStmt > 0) {
                            useCount += usesInStmt;
                            useIndex = j;
                            // 如果使用发生在复杂的控制流或嵌套块内部，为保证安全性，不进行内联
                            if (isComplexControlFlow(nextStmt)) {
                                escaped = true;
                            }
                        }
                    }

                    // 只有当该寄存器在当前作用域内仅被读取 1 次，且在此之前依赖未被破坏、没有进入复杂控制流时，才允许内联
                    if (useCount == 1 && !dependencyBroken && !escaped && useIndex != -1) {
                        Statement useStmt = stmts.get(useIndex);
                        Statement newUseStmt = (Statement) replaceVariableWithExpression(useStmt, regName, defExpr);
                        newUseStmt = (Statement) rewriteNode(newUseStmt);
                        stmts.set(useIndex, newUseStmt);
                        stmts.remove(i);
                        changed = true;
                        break;
                    }

                    // 如果使用次数为 0，且右侧表达式是没有副作用的常量/别名，可以直接安全删除该赋值语句
                    // 安全删除条件：全局无引用（nextDefIdx == stmts.size()）或者被覆盖前无 Goto 跳转
                    int nextDefIdx = findNextDefinitionIndex(stmts, i + 1, regName);
                    boolean safeToDelete = false;
                    if (useCount == 0) {
                        int totalUses = countTotalUsesInBlock(topBlock, regName);
                        if (totalUses == 0) {
                            safeToDelete = true;
                        } else if (nextDefIdx < stmts.size() && !hasGotoStatement(stmts, i + 1, nextDefIdx)) {
                            safeToDelete = true;
                        }
                    }
                    if (safeToDelete) {
                        if (defExpr instanceof StringConst 
                                || defExpr instanceof NumberConst 
                                || defExpr instanceof BooleanConst 
                                || defExpr instanceof NilConst 
                                || defExpr instanceof Name) {
                            stmts.remove(i);
                            changed = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    private int findNextDefinitionIndex(List<Statement> stmts, int start, String regName) {
        for (int j = start; j < stmts.size(); j++) {
            if (hasAssignmentTo(stmts.get(j), regName)) {
                return j;
            }
        }
        return stmts.size();
    }

    /**
     * 消除死寄存器赋值：如果某个寄存器的赋值在其后被同一寄存器的另一次赋值覆盖，
     * 且期间没有任何语句读取该寄存器，则该赋值是死赋值，可以安全移除。
     * 
     * 典型场景：GETTABLE R3 ... ; CALL R3 1 2 ; SETTABLE ... R3
     * GETTABLE的结果在CALL之前未被读取，被CALL的返回值覆盖，因此GETTABLE是死赋值。
     * 
     * 当死赋值的寄存器在重定义表达式中被引用时（如 R19 = string.len(R5) 被 R19 = R19 + 1 覆盖），
     * 需要先将死值代入重定义表达式（R19 = string.len(R5) + 1），再移除死赋值，
     * 避免后续内联时出现未定义的寄存器引用。
     */
    private void removeDeadRegisterAssignments(Block block, boolean isTopLevel) {
        List<Statement> stmts = block.statements;
        boolean changed = true;

        while (changed) {
            changed = false;
            for (int i = 0; i < stmts.size(); i++) {
                Statement stmt = stmts.get(i);
                String regName = getAssignedRegisterName(stmt);
                if (regName == null) {
                    continue;
                }

                // 提取死赋值的右侧定义表达式
                Expression deadExpr;
                if (stmt instanceof Assign) {
                    deadExpr = ((Assign) stmt).right.get(0);
                } else {
                    LocalAssign la = (LocalAssign) stmt;
                    if (la.right == null || la.right.isEmpty()) {
                        continue;
                    }
                    deadExpr = la.right.get(0);
                }

                boolean foundUse = false;
                int redefineIndex = -1;

                for (int j = i + 1; j < stmts.size(); j++) {
                    Statement nextStmt = stmts.get(j);

                    // 检查该寄存器是否被重新定值
                    if (hasAssignmentTo(nextStmt, regName)) {
                        redefineIndex = j;
                        break;
                    }

                    // 检查该寄存器是否被读取
                    int uses = countVariableUses(nextStmt, regName);
                    if (uses > 0) {
                        foundUse = true;
                        break;
                    }
                }

                // 寄存器在未被读取的情况下被重新定值，当前赋值为死赋值，且在此期间不包含 Goto 语句
                if (redefineIndex != -1 && !foundUse && !hasGotoStatement(stmts, i + 1, redefineIndex)) {
                    // 如果重定义表达式引用了该寄存器，先将死值代入
                    Statement redefineStmt = stmts.get(redefineIndex);
                    if (countVariableUses(redefineStmt, regName) > 0) {
                        Statement newRedefine = (Statement) replaceVariableWithExpression(
                                redefineStmt, regName, deadExpr);
                        stmts.set(redefineIndex, newRedefine);
                    }
                    stmts.remove(i);
                    changed = true;
                    break;
                }
            }
        }
    }

    /**
     * 获取语句中赋值的寄存器名称（支持 Assign 和 LocalAssign）
     * 如果不是寄存器赋值则返回 null
     */
    private String getAssignedRegisterName(Statement stmt) {
        if (stmt instanceof Assign) {
            Assign assign = (Assign) stmt;
            if (assign.left.size() == 1 && assign.right.size() == 1
                    && assign.left.get(0) instanceof Name) {
                String name = ((Name) assign.left.get(0)).name;
                if (name.matches("R\\d+")) {
                    return name;
                }
            }
        } else if (stmt instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) stmt;
            if (local.names.size() == 1) {
                String name = local.names.get(0);
                if (name.matches("R\\d+")) {
                    return name;
                }
            }
        }
        return null;
    }

    private boolean isRegisterAssign(Statement stmt) {
        return getAssignedRegisterName(stmt) != null;
    }

    private boolean isComplexControlFlow(Statement stmt) {
        return stmt instanceof IfStatement 
            || stmt instanceof WhileStatement 
            || stmt instanceof RepeatStatement 
            || stmt instanceof ForNumeric 
            || stmt instanceof ForIn 
            || stmt instanceof FunctionDeclaration
            || stmt instanceof GotoStatement
            || stmt instanceof LabelStatement;
    }

    /**
     * 检查语句是否对指定寄存器进行了表字段写入（如 R0["key"] = value）。
     * 这种操作会修改寄存器指向的表内容，即寄存器发生了"别名逃逸"，
     * 此时不能将寄存器简单的内联为初始定值。
     */
    private boolean registerUsedAsMutatedTable(Statement stmt, String regName) {
        if (stmt instanceof Assign) {
            Assign assign = (Assign) stmt;
            for (Expression left : assign.left) {
                if (left instanceof IndexExpr) {
                    IndexExpr idx = (IndexExpr) left;
                    if (idx.table instanceof Name && ((Name) idx.table).name.equals(regName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void collectReferencedVariables(Expression expr, Set<String> vars) {
        if (expr == null) return;
        if (expr instanceof Name) {
            vars.add(((Name) expr).name);
        } else if (expr instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) expr;
            collectReferencedVariables(binary.left, vars);
            collectReferencedVariables(binary.right, vars);
        } else if (expr instanceof UnaryOp) {
            collectReferencedVariables(((UnaryOp) expr).expr, vars);
        } else if (expr instanceof IndexExpr) {
            IndexExpr index = (IndexExpr) expr;
            collectReferencedVariables(index.table, vars);
            collectReferencedVariables(index.index, vars);
        } else if (expr instanceof MemberExpr) {
            collectReferencedVariables(((MemberExpr) expr).table, vars);
        } else if (expr instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) expr;
            collectReferencedVariables(call.callee, vars);
            for (Expression arg : call.args) {
                collectReferencedVariables(arg, vars);
            }
        } else if (expr instanceof TableConstructor) {
            TableConstructor tc = (TableConstructor) expr;
            if (tc.fields != null) {
                for (TableField field : tc.fields) {
                    collectReferencedVariables(field.key, vars);
                    collectReferencedVariables(field.value, vars);
                }
            }
        }
    }

    private boolean hasAssignmentTo(Statement stmt, Set<String> vars) {
        if (stmt instanceof Assign) {
            Assign assign = (Assign) stmt;
            for (Expression left : assign.left) {
                if (left instanceof Name && vars.contains(((Name) left).name)) {
                    return true;
                }
            }
        } else if (stmt instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) stmt;
            for (String name : local.names) {
                if (vars.contains(name)) {
                    return true;
                }
            }
        } else if (stmt instanceof GlobalAssign) {
            GlobalAssign glob = (GlobalAssign) stmt;
            for (String name : glob.names) {
                if (vars.contains(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasAssignmentTo(Statement stmt, String varName) {
        Set<String> set = new HashSet<>();
        set.add(varName);
        return hasAssignmentTo(stmt, set);
    }

    private int countVariableUses(AstNode node, String varName) {
        if (node == null) return 0;
        int count = 0;
        
        if (node instanceof Name) {
            if (((Name) node).name.equals(varName)) {
                return 1;
            }
        } else if (node instanceof Assign) {
            Assign assign = (Assign) node;
            for (Expression left : assign.left) {
                if (!(left instanceof Name)) {
                    count += countVariableUses(left, varName);
                }
            }
            for (Expression right : assign.right) {
                count += countVariableUses(right, varName);
            }
        } else if (node instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) node;
            for (Expression right : local.right) {
                count += countVariableUses(right, varName);
            }
        } else if (node instanceof GlobalAssign) {
            GlobalAssign glob = (GlobalAssign) node;
            for (Expression right : glob.right) {
                count += countVariableUses(right, varName);
            }
        } else if (node instanceof ExpressionStatement) {
            count += countVariableUses(((ExpressionStatement) node).expression, varName);
        } else if (node instanceof ReturnStatement) {
            for (Expression val : ((ReturnStatement) node).values) {
                count += countVariableUses(val, varName);
            }
        } else if (node instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) node;
            for (Expression cond : ifStmt.conditions) {
                count += countVariableUses(cond, varName);
            }
            for (Block nested : ifStmt.blocks) {
                count += countVariableUses(nested, varName);
            }
            count += countVariableUses(ifStmt.elseBlock, varName);
        } else if (node instanceof WhileStatement) {
            WhileStatement wh = (WhileStatement) node;
            count += countVariableUses(wh.condition, varName);
            count += countVariableUses(wh.body, varName);
        } else if (node instanceof RepeatStatement) {
            RepeatStatement rep = (RepeatStatement) node;
            count += countVariableUses(rep.condition, varName);
            count += countVariableUses(rep.body, varName);
        } else if (node instanceof ForNumeric) {
            ForNumeric fn = (ForNumeric) node;
            count += countVariableUses(fn.start, varName);
            count += countVariableUses(fn.end, varName);
            count += countVariableUses(fn.step, varName);
            count += countVariableUses(fn.body, varName);
        } else if (node instanceof ForIn) {
            ForIn fi = (ForIn) node;
            for (Expression exp : fi.iterators) {
                count += countVariableUses(exp, varName);
            }
            count += countVariableUses(fi.body, varName);
        } else if (node instanceof FunctionDeclaration) {
            count += countVariableUses(((FunctionDeclaration) node).func, varName);
        } else if (node instanceof Block) {
            for (Statement stmt : ((Block) node).statements) {
                count += countVariableUses(stmt, varName);
            }
        } else if (node instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) node;
            count += countVariableUses(binary.left, varName);
            count += countVariableUses(binary.right, varName);
        } else if (node instanceof UnaryOp) {
            count += countVariableUses(((UnaryOp) node).expr, varName);
        } else if (node instanceof IndexExpr) {
            IndexExpr idx = (IndexExpr) node;
            count += countVariableUses(idx.table, varName);
            count += countVariableUses(idx.index, varName);
        } else if (node instanceof MemberExpr) {
            count += countVariableUses(((MemberExpr) node).table, varName);
        } else if (node instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) node;
            count += countVariableUses(call.callee, varName);
            for (Expression arg : call.args) {
                count += countVariableUses(arg, varName);
            }
        } else if (node instanceof TableConstructor) {
            TableConstructor tc = (TableConstructor) node;
            if (tc.fields != null) {
                for (TableField field : tc.fields) {
                    count += countVariableUses(field.key, varName);
                    count += countVariableUses(field.value, varName);
                }
            }
        } else if (node instanceof FunctionLiteral) {
            count += countVariableUses(((FunctionLiteral) node).body, varName);
        }
        
        return count;
    }

    private AstNode replaceVariableWithExpression(AstNode root, String varName, Expression replacement) {
        if (root == null) return null;

        if (root instanceof Name) {
            if (((Name) root).name.equals(varName)) {
                return replacement;
            }
            return root;
        }

        if (root instanceof Assign) {
            Assign assign = (Assign) root;
            List<Expression> newLeft = new ArrayList<>();
            for (Expression left : assign.left) {
                if (!(left instanceof Name)) {
                    newLeft.add((Expression) replaceVariableWithExpression(left, varName, replacement));
                } else {
                    newLeft.add(left);
                }
            }
            List<Expression> newRight = new ArrayList<>();
            for (Expression right : assign.right) {
                newRight.add((Expression) replaceVariableWithExpression(right, varName, replacement));
            }
            return new Assign(newLeft, newRight, assign.pos);
        }

        if (root instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) root;
            List<Expression> newRight = new ArrayList<>();
            for (Expression right : local.right) {
                newRight.add((Expression) replaceVariableWithExpression(right, varName, replacement));
            }
            return new LocalAssign(local.names, newRight, local.pos);
        }

        if (root instanceof GlobalAssign) {
            GlobalAssign glob = (GlobalAssign) root;
            List<Expression> newRight = new ArrayList<>();
            for (Expression right : glob.right) {
                newRight.add((Expression) replaceVariableWithExpression(right, varName, replacement));
            }
            return new GlobalAssign(glob.names, newRight, glob.pos);
        }

        if (root instanceof ExpressionStatement) {
            ExpressionStatement es = (ExpressionStatement) root;
            return new ExpressionStatement(
                    (Expression) replaceVariableWithExpression(es.expression, varName, replacement),
                    es.pos);
        }

        if (root instanceof ReturnStatement) {
            ReturnStatement rs = (ReturnStatement) root;
            List<Expression> newVals = new ArrayList<>();
            for (Expression val : rs.values) {
                newVals.add((Expression) replaceVariableWithExpression(val, varName, replacement));
            }
            return new ReturnStatement(newVals, rs.pos);
        }

        if (root instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) root;
            List<Expression> newConds = new ArrayList<>();
            for (Expression cond : ifStmt.conditions) {
                newConds.add((Expression) replaceVariableWithExpression(cond, varName, replacement));
            }
            List<Block> newBlocks = new ArrayList<>();
            for (Block block : ifStmt.blocks) {
                newBlocks.add((Block) replaceVariableWithExpression(block, varName, replacement));
            }
            Block newElse = (Block) replaceVariableWithExpression(ifStmt.elseBlock, varName, replacement);
            return new IfStatement(newConds, newBlocks, newElse, ifStmt.pos);
        }

        if (root instanceof WhileStatement) {
            WhileStatement wh = (WhileStatement) root;
            Expression newCond = (Expression) replaceVariableWithExpression(wh.condition, varName, replacement);
            Block newBody = (Block) replaceVariableWithExpression(wh.body, varName, replacement);
            return new WhileStatement(newCond, newBody, wh.pos);
        }

        if (root instanceof RepeatStatement) {
            RepeatStatement rep = (RepeatStatement) root;
            Block newBody = (Block) replaceVariableWithExpression(rep.body, varName, replacement);
            Expression newCond = (Expression) replaceVariableWithExpression(rep.condition, varName, replacement);
            return new RepeatStatement(newBody, newCond, rep.pos);
        }

        if (root instanceof ForNumeric) {
            ForNumeric fn = (ForNumeric) root;
            Expression newStart = (Expression) replaceVariableWithExpression(fn.start, varName, replacement);
            Expression newEnd = (Expression) replaceVariableWithExpression(fn.end, varName, replacement);
            Expression newStep = (Expression) replaceVariableWithExpression(fn.step, varName, replacement);
            Block newBody = (Block) replaceVariableWithExpression(fn.body, varName, replacement);
            return new ForNumeric(fn.name, newStart, newEnd, newStep, newBody, fn.pos);
        }

        if (root instanceof ForIn) {
            ForIn fi = (ForIn) root;
            List<Expression> newIterators = new ArrayList<>();
            for (Expression exp : fi.iterators) {
                newIterators.add((Expression) replaceVariableWithExpression(exp, varName, replacement));
            }
            Block newBody = (Block) replaceVariableWithExpression(fi.body, varName, replacement);
            return new ForIn(fi.names, newIterators, newBody, fi.pos);
        }

        if (root instanceof Block) {
            Block block = (Block) root;
            List<Statement> newStmts = new ArrayList<>();
            for (Statement stmt : block.statements) {
                newStmts.add((Statement) replaceVariableWithExpression(stmt, varName, replacement));
            }
            Block newBlock = new Block(block.pos);
            newBlock.statements.addAll(newStmts);
            return newBlock;
        }

        if (root instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) root;
            Expression newLeft = (Expression) replaceVariableWithExpression(binary.left, varName, replacement);
            Expression newRight = (Expression) replaceVariableWithExpression(binary.right, varName, replacement);
            return new BinaryOp(binary.op, newLeft, newRight, binary.pos);
        }

        if (root instanceof UnaryOp) {
            UnaryOp unary = (UnaryOp) root;
            Expression newExpr = (Expression) replaceVariableWithExpression(unary.expr, varName, replacement);
            return new UnaryOp(unary.op, newExpr, unary.pos);
        }

        if (root instanceof IndexExpr) {
            IndexExpr idx = (IndexExpr) root;
            Expression newTable = (Expression) replaceVariableWithExpression(idx.table, varName, replacement);
            Expression newIdx = (Expression) replaceVariableWithExpression(idx.index, varName, replacement);
            return new IndexExpr(newTable, newIdx, idx.pos);
        }

        if (root instanceof MemberExpr) {
            MemberExpr mem = (MemberExpr) root;
            Expression newTable = (Expression) replaceVariableWithExpression(mem.table, varName, replacement);
            return new MemberExpr(newTable, mem.member, mem.pos);
        }

        if (root instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) root;
            Expression newCallee = (Expression) replaceVariableWithExpression(call.callee, varName, replacement);
            List<Expression> newArgs = new ArrayList<>();
            for (Expression arg : call.args) {
                newArgs.add((Expression) replaceVariableWithExpression(arg, varName, replacement));
            }
            return new FunctionCall(newCallee, newArgs, call.isMethodCall, call.returns, call.pos);
        }

        if (root instanceof TableConstructor) {
            TableConstructor tc = (TableConstructor) root;
            List<TableField> newFields = new ArrayList<>();
            if (tc.fields != null) {
                for (TableField field : tc.fields) {
                    Expression newKey = (Expression) replaceVariableWithExpression(field.key, varName, replacement);
                    Expression newVal = (Expression) replaceVariableWithExpression(field.value, varName, replacement);
                    newFields.add(new TableField(newKey, newVal));
                }
            }
            return new TableConstructor(newFields, tc.pos);
        }

        if (root instanceof FunctionLiteral) {
            FunctionLiteral fl = (FunctionLiteral) root;
            Block newBody = (Block) replaceVariableWithExpression(fl.body, varName, replacement);
            return new FunctionLiteral(fl.params, fl.vararg, newBody, fl.pos);
        }

        return root;
    }

    /**
     * 自底向上对 Block 内部所有表达式做重写（还原冒号/点号语法糖）
     */
    private void rewriteBlockExpressions(Block block) {
        if (block == null || block.statements == null) {
            return;
        }
        for (int i = 0; i < block.statements.size(); i++) {
            block.statements.set(i, (Statement) rewriteNode(block.statements.get(i)));
        }
    }

    private AstNode rewriteNode(AstNode node) {
        if (node == null) return null;

        // 1. 递归重写子节点
        if (node instanceof Assign) {
            Assign assign = (Assign) node;
            List<Expression> newRight = new ArrayList<>();
            for (Expression right : assign.right) {
                newRight.add((Expression) rewriteNode(right));
            }
            return new Assign(assign.left, newRight, assign.pos);
        } else if (node instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) node;
            List<Expression> newRight = new ArrayList<>();
            for (Expression right : local.right) {
                newRight.add((Expression) rewriteNode(right));
            }
            return new LocalAssign(local.names, newRight, local.pos);
        } else if (node instanceof GlobalAssign) {
            GlobalAssign glob = (GlobalAssign) node;
            List<Expression> newRight = new ArrayList<>();
            for (Expression right : glob.right) {
                newRight.add((Expression) rewriteNode(right));
            }
            return new GlobalAssign(glob.names, newRight, glob.pos);
        } else if (node instanceof ExpressionStatement) {
            ExpressionStatement es = (ExpressionStatement) node;
            return new ExpressionStatement((Expression) rewriteNode(es.expression), es.pos);
        } else if (node instanceof ReturnStatement) {
            ReturnStatement rs = (ReturnStatement) node;
            List<Expression> newVals = new ArrayList<>();
            for (Expression val : rs.values) {
                newVals.add((Expression) rewriteNode(val));
            }
            return new ReturnStatement(newVals, rs.pos);
        } else if (node instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) node;
            List<Expression> newConds = new ArrayList<>();
            for (Expression cond : ifStmt.conditions) {
                newConds.add((Expression) rewriteNode(cond));
            }
            List<Block> newBlocks = new ArrayList<>();
            for (Block b : ifStmt.blocks) {
                rewriteBlockExpressions(b);
                newBlocks.add(b);
            }
            rewriteBlockExpressions(ifStmt.elseBlock);
            return new IfStatement(newConds, newBlocks, ifStmt.elseBlock, ifStmt.pos);
        } else if (node instanceof WhileStatement) {
            WhileStatement wh = (WhileStatement) node;
            Expression newCond = (Expression) rewriteNode(wh.condition);
            rewriteBlockExpressions(wh.body);
            return new WhileStatement(newCond, wh.body, wh.pos);
        } else if (node instanceof RepeatStatement) {
            RepeatStatement rep = (RepeatStatement) node;
            rewriteBlockExpressions(rep.body);
            Expression newCond = (Expression) rewriteNode(rep.condition);
            return new RepeatStatement(rep.body, newCond, rep.pos);
        } else if (node instanceof ForNumeric) {
            ForNumeric fn = (ForNumeric) node;
            Expression newStart = (Expression) rewriteNode(fn.start);
            Expression newEnd = (Expression) rewriteNode(fn.end);
            Expression newStep = (Expression) rewriteNode(fn.step);
            rewriteBlockExpressions(fn.body);
            return new ForNumeric(fn.name, newStart, newEnd, newStep, fn.body, fn.pos);
        } else if (node instanceof ForIn) {
            ForIn fi = (ForIn) node;
            List<Expression> newIterators = new ArrayList<>();
            for (Expression exp : fi.iterators) {
                newIterators.add((Expression) rewriteNode(exp));
            }
            rewriteBlockExpressions(fi.body);
            return new ForIn(fi.names, newIterators, fi.body, fi.pos);
        } else if (node instanceof FunctionDeclaration) {
            FunctionDeclaration fd = (FunctionDeclaration) node;
            rewriteBlockExpressions(fd.func.body);
            return fd;
        } else if (node instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) node;
            Expression newLeft = (Expression) rewriteNode(binary.left);
            Expression newRight = (Expression) rewriteNode(binary.right);
            return new BinaryOp(binary.op, newLeft, newRight, binary.pos);
        } else if (node instanceof UnaryOp) {
            UnaryOp unary = (UnaryOp) node;
            Expression newExpr = (Expression) rewriteNode(unary.expr);
            return new UnaryOp(unary.op, newExpr, unary.pos);
        } else if (node instanceof IndexExpr) {
            IndexExpr idx = (IndexExpr) node;
            Expression newTable = (Expression) rewriteNode(idx.table);
            Expression newIdx = (Expression) rewriteNode(idx.index);
            // 优化：IndexExpr 转换为 MemberExpr（符合点号访问规范）
            if (newIdx instanceof StringConst) {
                String key = ((StringConst) newIdx).value;
                if (key.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
                    return new MemberExpr(newTable, key, idx.pos);
                }
            }
            return new IndexExpr(newTable, newIdx, idx.pos);
        } else if (node instanceof MemberExpr) {
            MemberExpr mem = (MemberExpr) node;
            Expression newTable = (Expression) rewriteNode(mem.table);
            return new MemberExpr(newTable, mem.member, mem.pos);
        } else if (node instanceof TableConstructor) {
            TableConstructor tc = (TableConstructor) node;
            List<TableField> newFields = new ArrayList<>();
            if (tc.fields != null) {
                for (TableField field : tc.fields) {
                    Expression newKey = (Expression) rewriteNode(field.key);
                    Expression newVal = (Expression) rewriteNode(field.value);
                    newFields.add(new TableField(newKey, newVal));
                }
            }
            return new TableConstructor(newFields, tc.pos);
        } else if (node instanceof FunctionLiteral) {
            FunctionLiteral fl = (FunctionLiteral) node;
            rewriteBlockExpressions(fl.body);
            return fl;
        } else if (node instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) node;
            Expression newCallee = (Expression) rewriteNode(call.callee);
            List<Expression> newArgs = new ArrayList<>();
            for (Expression arg : call.args) {
                newArgs.add((Expression) rewriteNode(arg));
            }

            // 优化：通用冒号方法还原 obj.method(obj, ...) -> obj:method(...)
            if (!call.isMethodCall && newArgs.size() >= 1 && (newCallee instanceof MemberExpr || newCallee instanceof IndexExpr)) {
                Expression firstArg = newArgs.get(0);
                Expression table = null;
                MemberExpr memberCallee = null;

                if (newCallee instanceof MemberExpr) {
                    memberCallee = (MemberExpr) newCallee;
                    table = memberCallee.table;
                } else {
                    IndexExpr indexCallee = (IndexExpr) newCallee;
                    table = indexCallee.table;
                    if (indexCallee.index instanceof StringConst) {
                        String key = ((StringConst) indexCallee.index).value;
                        if (key.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
                            memberCallee = new MemberExpr(table, key, indexCallee.pos);
                        }
                    }
                }



                if (memberCallee != null && isSameExpression(firstArg, table)) {
                    return new FunctionCall(memberCallee, newArgs, true, call.returns, call.pos);
                }
            }

            return new FunctionCall(newCallee, newArgs, call.isMethodCall, call.returns, call.pos);
        }

        return node;
    }

    private boolean isSameExpression(Expression e1, Expression e2) {
        if (e1 == e2) return true;
        if (e1 == null || e2 == null) return false;
        if (e1.getClass() != e2.getClass()) return false;

        if (e1 instanceof Name) {
            return ((Name) e1).name.equals(((Name) e2).name);
        }
        if (e1 instanceof MemberExpr) {
            MemberExpr m1 = (MemberExpr) e1;
            MemberExpr m2 = (MemberExpr) e2;
            return m1.member.equals(m2.member) && isSameExpression(m1.table, m2.table);
        }
        if (e1 instanceof IndexExpr) {
            IndexExpr i1 = (IndexExpr) e1;
            IndexExpr i2 = (IndexExpr) e2;
            return isSameExpression(i1.table, i2.table) && isSameExpression(i1.index, i2.index);
        }
        if (e1 instanceof StringConst) {
            return ((StringConst) e1).value.equals(((StringConst) e2).value);
        }
        if (e1 instanceof NumberConst) {
            return ((NumberConst) e1).value == ((NumberConst) e2).value;
        }
        if (e1 instanceof BooleanConst) {
            return ((BooleanConst) e1).value == ((BooleanConst) e2).value;
        }
        if (e1 instanceof NilConst) {
            return true;
        }
        if (e1 instanceof FunctionCall) {
            FunctionCall f1 = (FunctionCall) e1;
            FunctionCall f2 = (FunctionCall) e2;
            if (f1.isMethodCall != f2.isMethodCall || !isSameExpression(f1.callee, f2.callee)) {
                return false;
            }
            if (f1.args.size() != f2.args.size()) {
                return false;
            }
            for (int j = 0; j < f1.args.size(); j++) {
                if (!isSameExpression(f1.args.get(j), f2.args.get(j))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean hasGotoStatement(List<Statement> stmts, int start, int end) {
        for (int k = start; k < end && k < stmts.size(); k++) {
            if (countGotos(stmts.get(k)) > 0) {
                return true;
            }
        }
        return false;
    }

    private int countGotos(com.github.relua.ast.AstNode node) {
        if (node == null) return 0;
        int count = 0;
        if (node instanceof GotoStatement) {
            count = 1;
        } else if (node instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) node;
            for (Block block : ifStmt.blocks) {
                count += countGotos(block);
            }
            count += countGotos(ifStmt.elseBlock);
        } else if (node instanceof WhileStatement) {
            count += countGotos(((WhileStatement) node).body);
        } else if (node instanceof RepeatStatement) {
            count += countGotos(((RepeatStatement) node).body);
        } else if (node instanceof ForNumeric) {
            count += countGotos(((ForNumeric) node).body);
        } else if (node instanceof ForIn) {
            count += countGotos(((ForIn) node).body);
        } else if (node instanceof Block) {
            for (Statement stmt : ((Block) node).statements) {
                count += countGotos(stmt);
            }
        } else if (node instanceof FunctionDeclaration) {
            count += countGotos(((FunctionDeclaration) node).func.body);
        } else if (node instanceof FunctionLiteral) {
            count += countGotos(((FunctionLiteral) node).body);
        }
        return count;
    }

    private int countTotalUsesInBlock(Block block, String regName) {
        if (block == null || block.statements == null) {
            return 0;
        }
        int count = 0;
        for (Statement s : block.statements) {
            count += countTotalUsesInStatement(s, regName);
        }
        return count;
    }

    private int countTotalUsesInStatement(Statement stmt, String regName) {
        int count = countVariableUses(stmt, regName);
        if (stmt instanceof IfStatement) {
            IfStatement ifs = (IfStatement) stmt;
            for (Block b : ifs.blocks) {
                count += countTotalUsesInBlock(b, regName);
            }
            count += countTotalUsesInBlock(ifs.elseBlock, regName);
        } else if (stmt instanceof WhileStatement) {
            count += countTotalUsesInBlock(((WhileStatement) stmt).body, regName);
        } else if (stmt instanceof RepeatStatement) {
            count += countTotalUsesInBlock(((RepeatStatement) stmt).body, regName);
        } else if (stmt instanceof ForNumeric) {
            count += countTotalUsesInBlock(((ForNumeric) stmt).body, regName);
        } else if (stmt instanceof ForIn) {
            count += countTotalUsesInBlock(((ForIn) stmt).body, regName);
        }
        return count;
    }
}
