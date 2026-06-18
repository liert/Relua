package com.github.relua.decompiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import com.github.relua.ast.AstNode;
import com.github.relua.ast.TableField;
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
import com.github.relua.debug.DecompilerDebugger;
import com.github.relua.decompiler.analysis.DataFlowAnalyzer;
import com.github.relua.decompiler.analysis.StructureRestorer;
import com.github.relua.ast.NilConst;
import com.github.relua.ast.BooleanConst;
import com.github.relua.ast.NumberConst;
import com.github.relua.ast.StringConst;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;
import com.github.relua.util.TransformUtils;
import com.github.relua.model.Chunk;
import com.github.relua.model.Constant;
import com.github.relua.log.Logger;
import com.github.relua.util.RegisterNamePolicy;

public class AstCleanupPass {
    public void cleanup(Block block) {
        cleanup(block, null, java.util.Collections.emptySet(), java.util.Collections.emptySet());
    }

    public Set<String> cleanup(Block block, Set<String> parentDeclared) {
        return cleanup(block, null, parentDeclared, java.util.Collections.emptySet());
    }

    public Set<String> cleanup(Block block, Set<String> parentDeclared, Set<String> upvalueNames) {
        return cleanup(block, null, parentDeclared, upvalueNames);
    }

    public Set<String> cleanup(Block block, CodeGeneratorContext context) {
        return cleanup(block, context, java.util.Collections.emptySet(), java.util.Collections.emptySet());
    }

    public Set<String> cleanup(Block block, CodeGeneratorContext context, Set<String> parentDeclared) {
        return cleanup(block, context, parentDeclared, java.util.Collections.emptySet());
    }

    public Set<String> cleanup(Block block, CodeGeneratorContext context, Set<String> parentDeclared,
            Set<String> upvalueNames) {
        if (block == null) {
            return java.util.Collections.emptySet();
        }

        String funcName = (context != null && context.getChunk() != null) ? context.getChunk().getFunction()
                : "unknown";

        Set<String> finalUpvalueNames = new HashSet<>(upvalueNames);
        if (context != null && context.getChunk() != null) {
            com.github.relua.model.Chunk chunk = context.getChunk();
            if (chunk.getInstructions() != null) {
                for (com.github.relua.model.Instruction ins : chunk.getInstructions()) {
                    if (ins != null) {
                        if (ins.getOpcode() == com.github.relua.model.Opcode.GETUPVAL
                                || ins.getOpcode() == com.github.relua.model.Opcode.SETUPVAL) {
                            com.github.relua.model.UpValue uv = context.getUpvalue(ins.getB());
                            if (uv != null) {
                                String uvName = getResolvedUpvalueName(uv, ins.getB());
                                if (uvName != null) {
                                    finalUpvalueNames.add(uvName);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Collect the exact local register definitions captured by child closures.
        // The same physical register can hold several unrelated AST names over time,
        // so a captured upvalue must protect the reaching definition version, not
        // every assignment that happens to share the current register name.
        Set<String> capturedLocals = new HashSet<>();
        Map<String, Set<Integer>> capturedLocalDefinitionPcs = new HashMap<>();
        if (context != null && context.getChunk() != null) {
            com.github.relua.model.Chunk chunk = context.getChunk();
            java.util.List<com.github.relua.model.Instruction> insts = chunk.getInstructions();
            if (insts != null) {
                for (int i = 0; i < insts.size(); i++) {
                    com.github.relua.model.Instruction ins = insts.get(i);
                    if (ins != null && ins.getOpcode() == com.github.relua.model.Opcode.CLOSURE) {
                        int bx = ins.getBx();
                        if (bx >= 0 && bx < chunk.getSubChunks().size()) {
                            com.github.relua.model.Chunk subChunk = chunk.getSubChunks().get(bx);
                            int nup = subChunk.getNup();
                            for (int j = 0; j < nup; j++) {
                                int nextIdx = i + 1 + j;
                                if (nextIdx < insts.size()) {
                                    com.github.relua.model.Instruction upvalIns = insts.get(nextIdx);
                                    if (upvalIns != null
                                            && upvalIns.getOpcode() == com.github.relua.model.Opcode.MOVE) {
                                        int b = upvalIns.getB();
                                        String regName = context.getRegister().getRegisterEntity(b).getName();
                                        if (regName != null) {
                                            capturedLocals.add(regName);
                                            int reachingDefPc = findReachingRegisterDefinitionPc(insts, i, b);
                                            if (reachingDefPc != -1) {
                                                capturedLocalDefinitionPcs
                                                        .computeIfAbsent(regName, k -> new HashSet<>())
                                                        .add(reachingDefPc);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Set<String> optimizerUpvalues = new HashSet<>(finalUpvalueNames);
        optimizerUpvalues.addAll(capturedLocals);

        DecompilerDebugger.dump("dataflow_opt_0_" + funcName, block);

        // 1. 数据流变量内联及点号/冒号语法糖还原
        new DataFlowAnalyzer().optimize(block, parentDeclared, optimizerUpvalues, capturedLocalDefinitionPcs);
        DecompilerDebugger.dump("dataflow_opt_1_" + funcName, block);

        // 1.5 清理空的if块（nil-guard等模式产生的空条件分支）
        removeEmptyIfBlocks(block);
        DecompilerDebugger.dump("empty_if_removed_1_" + funcName, block);

        // 2. 结构化控制流还原与 GOTO/Label 消解
        new StructureRestorer(context != null ? context.getChunk() : null).restructure(block);
        DecompilerDebugger.dump("structure_restored_" + funcName, block);

        // 2.2 简化冗余的NaN/类型检查
        simplifyRedundantNanChecks(block);
        DecompilerDebugger.dump("nan_checks_simplified_" + funcName, block);

        // 2.5 再次清理空的if块（结构恢复可能产生新的空分支）
        removeEmptyIfBlocks(block);
        DecompilerDebugger.dump("empty_if_removed_2_" + funcName, block);

        // 2.5.5 再次执行数据流变量内联（消解 GOTO/Label 之后，许多原本因跳转阻碍而无法安全内联的变量现在可以安全内联了）
        new DataFlowAnalyzer().optimize(block, parentDeclared, optimizerUpvalues, capturedLocalDefinitionPcs);
        DecompilerDebugger.dump("dataflow_opt_2_" + funcName, block);

        // 2.6 移除 return 后的死代码
        removeDeadCodeAfterReturn(block);
        DecompilerDebugger.dump("dead_code_removed_1_" + funcName, block);

        // 优化返回模式（Peephole 优化）：合并临时寄存器赋值与其后紧随的返回
        optimizeReturnPatterns(block, context, optimizerUpvalues);
        DecompilerDebugger.dump("return_opt_" + funcName, block);

        // 再次移除合并可能产生的新一轮死代码
        removeDeadCodeAfterReturn(block);
        DecompilerDebugger.dump("dead_code_removed_2_" + funcName, block);

        Set<TableConstructor> consumedTables = Collections.newSetFromMap(new IdentityHashMap<>());
        collectConsumedTables(block, consumedTables, true);
        removeConsumedTemporaryTables(block, consumedTables);
        removeJoinGotos(block);
        removeUnusedEmptyLocalDeclarations(block);
        DecompilerDebugger.dump("ast_cleanup_final_" + funcName, block);

        // 3. 把函数体顶层第一次出现的寄存器赋值恢复为 local，主块无参数
        List<String> params = new ArrayList<>();
        if (context != null && context.getChunk() != null && !"main".equals(context.getChunk().getFunction())) {
            Chunk functionChunk = context.getChunk();
            for (int i = 0; i < functionChunk.getNumParams(); i++) {
                params.add(TransformUtils.transformRegister(context.getRegister().getRegisterEntity(i)));
            }
        }
        Set<String> declared = declareTopLevelLocals(block, params, parentDeclared, finalUpvalueNames);
        DecompilerDebugger.dump("locals_declared_" + funcName, block);
        return declared;
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
                stripTrailingJoinGotos(block, (IfStatement) statement);
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

    private boolean isTerminatingStatement(Statement stmt) {
        if (stmt instanceof ReturnStatement) {
            return true;
        }
        if (stmt instanceof ExpressionStatement) {
            Expression expr = ((ExpressionStatement) stmt).expression;
            if (expr instanceof UnaryOp && "return".equals(((UnaryOp) expr).op)) {
                return true;
            }
        }
        if (stmt instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) stmt;
            if (ifStmt.elseBlock == null || isEffectivelyEmpty(ifStmt.elseBlock)) {
                return false;
            }
            for (Block b : ifStmt.blocks) {
                if (!blockEndsWithTerminator(b)) {
                    return false;
                }
            }
            return blockEndsWithTerminator(ifStmt.elseBlock);
        }
        return false;
    }

    private boolean blockEndsWithTerminator(Block block) {
        if (block == null || block.statements.isEmpty()) {
            return false;
        }
        Statement last = block.statements.get(block.statements.size() - 1);
        return isTerminatingStatement(last);
    }

    private void removeDeadCodeAfterReturn(Block block) {
        if (block == null || block.statements == null) {
            return;
        }

        for (Statement statement : block.statements) {
            if (statement instanceof IfStatement) {
                IfStatement ifStmt = (IfStatement) statement;
                for (Block nested : ifStmt.blocks) {
                    removeDeadCodeAfterReturn(nested);
                }
                removeDeadCodeAfterReturn(ifStmt.elseBlock);
            } else if (statement instanceof FunctionDeclaration) {
                removeDeadCodeAfterReturn(((FunctionDeclaration) statement).func.body);
            } else if (statement instanceof WhileStatement) {
                removeDeadCodeAfterReturn(((WhileStatement) statement).body);
            } else if (statement instanceof ForNumeric) {
                removeDeadCodeAfterReturn(((ForNumeric) statement).body);
            } else if (statement instanceof ForIn) {
                removeDeadCodeAfterReturn(((ForIn) statement).body);
            }
        }

        List<Statement> stmts = block.statements;
        for (int i = 0; i < stmts.size(); i++) {
            Statement s = stmts.get(i);
            if (isTerminatingStatement(s)) {
                if (i + 1 < stmts.size()) {
                    List<Statement> toRemove = new ArrayList<>(stmts.subList(i + 1, stmts.size()));
                    stmts.removeAll(toRemove);
                }
                break;
            }
        }
    }

    private void stripTrailingJoinGotos(Block parentBlock, IfStatement ifStmt) {
        int ifStmtIdx = parentBlock.statements.indexOf(ifStmt);
        if (ifStmtIdx == -1) {
            for (Block b : ifStmt.blocks) {
                if (b != null) {
                    stripTrailingGotosFromBlock(b, java.util.Collections.emptySet());
                }
            }
            if (ifStmt.elseBlock != null) {
                stripTrailingGotosFromBlock(ifStmt.elseBlock, java.util.Collections.emptySet());
            }
            return;
        }

        Set<String> redundantLabels = new HashSet<>();
        for (int k = ifStmtIdx + 1; k < parentBlock.statements.size(); k++) {
            Statement s = parentBlock.statements.get(k);
            if (s instanceof LabelStatement) {
                redundantLabels.add(((LabelStatement) s).label);
            } else {
                break;
            }
        }

        for (Block b : ifStmt.blocks) {
            if (b != null) {
                stripTrailingGotosFromBlock(b, redundantLabels);
            }
        }
        if (ifStmt.elseBlock != null) {
            stripTrailingGotosFromBlock(ifStmt.elseBlock, redundantLabels);
        }
    }

    private void stripTrailingGotosFromBlock(Block block, Set<String> redundantLabels) {
        if (block == null || block.statements.isEmpty()) {
            return;
        }
        for (Statement stmt : block.statements) {
            if (stmt instanceof IfStatement) {
                stripTrailingJoinGotos(block, (IfStatement) stmt);
            }
        }
        while (!block.statements.isEmpty()) {
            Statement last = block.statements.get(block.statements.size() - 1);
            if (last instanceof GotoStatement) {
                String target = ((GotoStatement) last).label;
                if (redundantLabels.contains(target)) {
                    block.statements.remove(block.statements.size() - 1);
                } else {
                    break;
                }
            } else if (last instanceof LabelStatement) {
                block.statements.remove(block.statements.size() - 1);
            } else {
                break;
            }
        }
    }

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
            case "==":
                return "~=";
            case "~=":
                return "==";
            case "<":
                return ">=";
            case ">":
                return "<=";
            case "<=":
                return ">";
            case ">=":
                return "<";
            default:
                return null;
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
        return RegisterNamePolicy.isTemporaryRegisterName(name) && consumedTables.contains(assign.right.get(0));
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

    private void collectFromStatement(Statement statement, Set<TableConstructor> consumedTables,
            boolean skipTempAssignRight) {
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
                && RegisterNamePolicy.isTemporaryRegisterName(((Name) assign.left.get(0)).name)
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

    private boolean containsRegister(AstNode node, String reg) {
        if (node == null) {
            return false;
        }
        Set<String> regs = new HashSet<>();
        collectAllUsedRegisters(node, regs);
        return regs.contains(reg);
    }

    private boolean containsRegister(Statement stmt, String reg) {
        return containsRegister((AstNode) stmt, reg);
    }

    private boolean isValidInlineLocalizableAssignment(Statement stmt, String reg) {
        if (stmt instanceof Assign) {
            Assign assign = (Assign) stmt;
            if (assign.left.size() == 1 && assign.right.size() == 1 && assign.left.get(0) instanceof Name) {
                String name = ((Name) assign.left.get(0)).name;
                if (name.equals(reg)) {
                    Expression right = assign.right.get(0);
                    Set<String> rightVars = new HashSet<>();
                    collectAllUsedRegisters(right, rightVars);
                    if (!rightVars.contains(reg)) {
                        return shouldDeclareLocal(reg, right);
                    }
                }
            }
        } else if (stmt instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) stmt;
            return local.names.contains(reg);
        }
        return false;
    }

    private Set<String> declareTopLevelLocals(Block block, List<String> params, Set<String> parentDeclared,
            Set<String> upvalueNames) {
        if (block == null || block.statements == null) {
            return java.util.Collections.emptySet();
        }

        Logger.debug("[DEBUG] declareTopLevelLocals block, params: " + params + ", parentDeclared: " + parentDeclared);

        Set<String> declared = new HashSet<>(parentDeclared);
        Set<String> hoistedRegisters = new HashSet<>();

        boolean isFunction = (params != null);
        if (isFunction) {
            declared.addAll(params);

            collectAllUsedRegisters(block, hoistedRegisters);
            Logger.debug("[DEBUG] Collected hoistedRegisters before remove: " + hoistedRegisters);
            hoistedRegisters.removeAll(params);

            // Names loaded through GETUPVAL belong to an enclosing scope. They must
            // never be hoisted as missing locals in the current function, otherwise
            // the generated local shadows the captured value and changes semantics.
            hoistedRegisters.removeAll(upvalueNames);
            Logger.debug("[DEBUG] Collected hoistedRegisters after remove: " + hoistedRegisters);
        }

        List<String> missing = new ArrayList<>();
        if (isFunction && !hoistedRegisters.isEmpty()) {
            for (String reg : hoistedRegisters) {
                if (!declared.contains(reg)) {
                    Statement firstOccurrence = null;
                    for (Statement stmt : block.statements) {
                        if (containsRegister(stmt, reg)) {
                            firstOccurrence = stmt;
                            break;
                        }
                    }
                    if (firstOccurrence == null || !isValidInlineLocalizableAssignment(firstOccurrence, reg)) {
                        missing.add(reg);
                    }
                }
            }
            if (!missing.isEmpty()) {
                missing.sort(this::compareTemporaryRegisterNames);
                declared.addAll(missing);
            }
        }
        Logger.debug("[DEBUG] computed declared (with missing): " + declared);

        Set<String> actuallyHoisted = new HashSet<>(missing);

        List<Statement> rewritten = new ArrayList<>();
        for (Statement statement : block.statements) {
            declareStatementLocalIfNeeded(statement, declared, true, actuallyHoisted, rewritten);
        }

        block.statements.clear();
        block.statements.addAll(rewritten);

        if (isFunction && !missing.isEmpty()) {
            Map<Statement, List<String>> insertMap = new IdentityHashMap<>();
            List<String> unplacedDeclarations = new ArrayList<>();

            for (String reg : missing) {
                Statement firstStmt = null;
                for (Statement stmt : block.statements) {
                    if (containsRegister(stmt, reg)) {
                        firstStmt = stmt;
                        break;
                    }
                }
                if (firstStmt != null) {
                    insertMap.computeIfAbsent(firstStmt, k -> new ArrayList<>()).add(reg);
                } else {
                    unplacedDeclarations.add(reg);
                }
            }

            List<Statement> newStmts = new ArrayList<>();
            if (!unplacedDeclarations.isEmpty()) {
                unplacedDeclarations.sort(this::compareTemporaryRegisterNames);
                newStmts.add(new LocalAssign(unplacedDeclarations, new ArrayList<>(), block.pos));
            }

            for (Statement stmt : block.statements) {
                List<String> regsToDeclare = insertMap.get(stmt);
                if (regsToDeclare != null && !regsToDeclare.isEmpty()) {
                    regsToDeclare.sort(this::compareTemporaryRegisterNames);
                    newStmts.add(new LocalAssign(regsToDeclare, new ArrayList<>(), block.pos));
                }
                newStmts.add(stmt);
            }

            block.statements.clear();
            block.statements.addAll(newStmts);
        }
        return declared;
    }

    private int compareTemporaryRegisterNames(String left, String right) {
        int leftIndex = RegisterNamePolicy.temporaryRegisterIndex(left);
        int rightIndex = RegisterNamePolicy.temporaryRegisterIndex(right);
        if (leftIndex != -1 && rightIndex != -1) {
            return Integer.compare(leftIndex, rightIndex);
        }
        if (leftIndex != -1) {
            return -1;
        }
        if (rightIndex != -1) {
            return 1;
        }
        return left.compareTo(right);
    }

    private boolean isRegisterOrObj(String name) {
        if (name == null) {
            return false;
        }
        return RegisterNamePolicy.isTemporaryRegisterName(name) || name.endsWith("Obj");
    }

    private void collectAllUsedRegisters(AstNode node, Set<String> registers) {
        if (node == null) {
            return;
        }
        if (node instanceof Name) {
            String name = ((Name) node).name;
            if (isRegisterOrObj(name)) {
                registers.add(name);
            }
            return;
        }
        if (node instanceof Block) {
            Block block = (Block) node;
            if (block.statements != null) {
                for (Statement stmt : block.statements) {
                    collectAllUsedRegisters(stmt, registers);
                }
            }
        } else if (node instanceof Assign) {
            Assign assign = (Assign) node;
            for (Expression expr : assign.left) {
                collectAllUsedRegisters(expr, registers);
            }
            for (Expression expr : assign.right) {
                collectAllUsedRegisters(expr, registers);
            }
        } else if (node instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) node;
            for (String name : local.names) {
                if (isRegisterOrObj(name)) {
                    registers.add(name);
                }
            }
            if (local.right != null) {
                for (Expression expr : local.right) {
                    collectAllUsedRegisters(expr, registers);
                }
            }
        } else if (node instanceof GlobalAssign) {
            GlobalAssign glob = (GlobalAssign) node;
            for (String name : glob.names) {
                if (isRegisterOrObj(name)) {
                    registers.add(name);
                }
            }
            if (glob.right != null) {
                for (Expression expr : glob.right) {
                    collectAllUsedRegisters(expr, registers);
                }
            }
        } else if (node instanceof IfStatement) {
            IfStatement ifs = (IfStatement) node;
            if (ifs.conditions != null) {
                for (Expression cond : ifs.conditions) {
                    collectAllUsedRegisters(cond, registers);
                }
            }
            if (ifs.blocks != null) {
                for (Block blk : ifs.blocks) {
                    collectAllUsedRegisters(blk, registers);
                }
            }
            collectAllUsedRegisters(ifs.elseBlock, registers);
        } else if (node instanceof WhileStatement) {
            WhileStatement ws = (WhileStatement) node;
            collectAllUsedRegisters(ws.condition, registers);
            collectAllUsedRegisters(ws.body, registers);
        } else if (node instanceof RepeatStatement) {
            RepeatStatement rs = (RepeatStatement) node;
            collectAllUsedRegisters(rs.condition, registers);
            collectAllUsedRegisters(rs.body, registers);
        } else if (node instanceof ForNumeric) {
            ForNumeric fn = (ForNumeric) node;
            if (isRegisterOrObj(fn.name))
                registers.add(fn.name);
            collectAllUsedRegisters(fn.start, registers);
            collectAllUsedRegisters(fn.end, registers);
            collectAllUsedRegisters(fn.step, registers);
            collectAllUsedRegisters(fn.body, registers);
        } else if (node instanceof ForIn) {
            ForIn fi = (ForIn) node;
            for (String n : fi.names) {
                if (isRegisterOrObj(n))
                    registers.add(n);
            }
            if (fi.iterators != null) {
                for (Expression expr : fi.iterators) {
                    collectAllUsedRegisters(expr, registers);
                }
            }
            collectAllUsedRegisters(fi.body, registers);
        } else if (node instanceof FunctionDeclaration) {
            FunctionDeclaration fd = (FunctionDeclaration) node;
            if (isRegisterOrObj(fd.name)) {
                registers.add(fd.name);
            }
        } else if (node instanceof FunctionLiteral) {
            // 匿名闭包的变量独立，不收集内层
        } else if (node instanceof BinaryOp) {
            BinaryOp bo = (BinaryOp) node;
            collectAllUsedRegisters(bo.left, registers);
            collectAllUsedRegisters(bo.right, registers);
        } else if (node instanceof UnaryOp) {
            collectAllUsedRegisters(((UnaryOp) node).expr, registers);
        } else if (node instanceof IndexExpr) {
            IndexExpr ie = (IndexExpr) node;
            collectAllUsedRegisters(ie.table, registers);
            collectAllUsedRegisters(ie.index, registers);
        } else if (node instanceof MemberExpr) {
            collectAllUsedRegisters(((MemberExpr) node).table, registers);
        } else if (node instanceof FunctionCall) {
            FunctionCall fc = (FunctionCall) node;
            collectAllUsedRegisters(fc.callee, registers);
            if (fc.args != null) {
                for (Expression arg : fc.args) {
                    collectAllUsedRegisters(arg, registers);
                }
            }
        } else if (node instanceof TableConstructor) {
            TableConstructor tc = (TableConstructor) node;
            if (tc.fields != null) {
                for (TableField tf : tc.fields) {
                    collectAllUsedRegisters(tf.key, registers);
                    collectAllUsedRegisters(tf.value, registers);
                }
            }
        } else if (node instanceof ReturnStatement) {
            ReturnStatement rs = (ReturnStatement) node;
            if (rs.values != null) {
                for (Expression val : rs.values) {
                    collectAllUsedRegisters(val, registers);
                }
            }
        } else if (node instanceof ExpressionStatement) {
            collectAllUsedRegisters(((ExpressionStatement) node).expression, registers);
        }
    }

    private void declareStatementLocalIfNeeded(Statement statement, Set<String> declared, boolean allowNewLocal,
            Set<String> hoistedRegisters, List<Statement> result) {
        if (statement instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) statement;

            List<String> hoistedInLocal = new ArrayList<>();
            List<String> remainInLocal = new ArrayList<>();
            for (String name : local.names) {
                if (isRegisterOrObj(name) && hoistedRegisters != null && hoistedRegisters.contains(name)) {
                    hoistedInLocal.add(name);
                } else {
                    remainInLocal.add(name);
                }
            }

            if (hoistedInLocal.isEmpty()) {
                declared.addAll(local.names);
                result.add(local);
                return;
            }

            declared.addAll(remainInLocal);

            if (!remainInLocal.isEmpty()) {
                List<Expression> remainRight = new ArrayList<>();
                for (String rName : remainInLocal) {
                    int idx = local.names.indexOf(rName);
                    if (idx < local.right.size()) {
                        remainRight.add(local.right.get(idx));
                    }
                }
                result.add(new LocalAssign(remainInLocal, remainRight, local.pos));
            }

            for (String hName : hoistedInLocal) {
                int idx = local.names.indexOf(hName);
                if (idx < local.right.size()) {
                    Expression rightExpr = local.right.get(idx);
                    List<Expression> leftList = new ArrayList<>();
                    leftList.add(new Name(hName, local.pos));
                    List<Expression> rightList = new ArrayList<>();
                    rightList.add(rightExpr);
                    result.add(new Assign(leftList, rightList, local.pos));
                }
            }
            return;
        }

        if (statement instanceof GlobalAssign) {
            result.add(statement);
            return;
        }

        if (statement instanceof Assign) {
            Assign assign = (Assign) statement;

            if (allowNewLocal) {
                boolean allNames = true;
                List<String> names = new ArrayList<>();
                for (Expression left : assign.left) {
                    if (left instanceof Name) {
                        names.add(((Name) left).name);
                    } else {
                        allNames = false;
                        break;
                    }
                }
                if (allNames && !names.isEmpty()) {
                    boolean noneDeclared = true;
                    for (String name : names) {
                        if (declared.contains(name)) {
                            noneDeclared = false;
                            break;
                        }
                    }
                    if (noneDeclared) {
                        boolean allShouldDeclare = true;
                        for (int k = 0; k < names.size(); k++) {
                            String name = names.get(k);
                            Expression right = k < assign.right.size() ? assign.right.get(k) : null;
                            if (!shouldDeclareLocal(name, right)) {
                                allShouldDeclare = false;
                                break;
                            }
                        }
                        if (allShouldDeclare) {
                            if (hoistedRegisters != null) {
                                boolean hasHoisted = false;
                                for (String name : names) {
                                    if (hoistedRegisters.contains(name)) {
                                        hasHoisted = true;
                                        break;
                                    }
                                }
                                if (hasHoisted) {
                                    result.add(statement);
                                    return;
                                }
                            }
                            declared.addAll(names);
                            result.add(new LocalAssign(names, assign.right, assign.pos));
                            return;
                        }
                    }
                }
            }

            result.add(statement);
            return;
        }

        if (statement instanceof IfStatement) {
            IfStatement ifStatement = (IfStatement) statement;

            for (Block nested : ifStatement.blocks) {
                rewriteNestedBlockWithoutNewLocals(nested, declared, hoistedRegisters);
            }

            rewriteNestedBlockWithoutNewLocals(ifStatement.elseBlock, declared, hoistedRegisters);
            result.add(statement);
            return;
        }

        if (statement instanceof WhileStatement) {
            rewriteNestedBlockWithoutNewLocals(((WhileStatement) statement).body, declared, hoistedRegisters);
            result.add(statement);
            return;
        }

        if (statement instanceof RepeatStatement) {
            rewriteNestedBlockWithoutNewLocals(((RepeatStatement) statement).body, declared, hoistedRegisters);
            result.add(statement);
            return;
        }

        if (statement instanceof ForNumeric) {
            rewriteNestedBlockWithoutNewLocals(((ForNumeric) statement).body, declared, hoistedRegisters);
            result.add(statement);
            return;
        }

        if (statement instanceof ForIn) {
            rewriteNestedBlockWithoutNewLocals(((ForIn) statement).body, declared, hoistedRegisters);
            result.add(statement);
            return;
        }

        if (statement instanceof FunctionDeclaration) {
            declareTopLevelLocals(((FunctionDeclaration) statement).func.body,
                    ((FunctionDeclaration) statement).func.params, declared, java.util.Collections.emptySet());
            result.add(statement);
            return;
        }

        result.add(statement);
    }

    private void rewriteNestedBlockWithoutNewLocals(Block block, Set<String> declared, Set<String> hoistedRegisters) {
        if (block == null || block.statements == null) {
            return;
        }

        List<Statement> rewritten = new ArrayList<>();

        for (Statement statement : block.statements) {
            declareStatementLocalIfNeeded(statement, declared, false, hoistedRegisters, rewritten);
        }

        block.statements.clear();
        block.statements.addAll(rewritten);
    }

    private void removeUnusedEmptyLocalDeclarations(Block block) {
        if (block == null || block.statements == null) {
            return;
        }

        List<Statement> rewritten = new ArrayList<>();
        for (Statement statement : block.statements) {
            if (statement instanceof LocalAssign) {
                LocalAssign local = (LocalAssign) statement;
                if (local.right == null || local.right.isEmpty()) {
                    List<String> keptNames = new ArrayList<>();
                    for (String name : local.names) {
                        if (countVariableReads(block, name) > 0) {
                            keptNames.add(name);
                        }
                    }
                    if (!keptNames.isEmpty()) {
                        rewritten.add(new LocalAssign(keptNames, new ArrayList<>(), local.pos));
                    }
                    continue;
                }
            }

            removeUnusedEmptyLocalDeclarationsInNested(statement);
            rewritten.add(statement);
        }

        block.statements.clear();
        block.statements.addAll(rewritten);
    }

    private void removeUnusedEmptyLocalDeclarationsInNested(Statement statement) {
        if (statement instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) statement;
            for (Block nested : ifStmt.blocks) {
                removeUnusedEmptyLocalDeclarations(nested);
            }
            removeUnusedEmptyLocalDeclarations(ifStmt.elseBlock);
        } else if (statement instanceof WhileStatement) {
            removeUnusedEmptyLocalDeclarations(((WhileStatement) statement).body);
        } else if (statement instanceof RepeatStatement) {
            removeUnusedEmptyLocalDeclarations(((RepeatStatement) statement).body);
        } else if (statement instanceof ForNumeric) {
            removeUnusedEmptyLocalDeclarations(((ForNumeric) statement).body);
        } else if (statement instanceof ForIn) {
            removeUnusedEmptyLocalDeclarations(((ForIn) statement).body);
        } else if (statement instanceof FunctionDeclaration) {
            removeUnusedEmptyLocalDeclarations(((FunctionDeclaration) statement).func.body);
        }
    }

    private int countVariableReads(AstNode node, String varName) {
        if (node == null || varName == null) {
            return 0;
        }
        int count = 0;

        if (node instanceof Name) {
            return varName.equals(((Name) node).name) ? 1 : 0;
        } else if (node instanceof Block) {
            for (Statement stmt : ((Block) node).statements) {
                count += countVariableReads(stmt, varName);
            }
        } else if (node instanceof Assign) {
            Assign assign = (Assign) node;
            for (Expression left : assign.left) {
                if (!(left instanceof Name)) {
                    count += countVariableReads(left, varName);
                }
            }
            for (Expression right : assign.right) {
                count += countVariableReads(right, varName);
            }
        } else if (node instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) node;
            if (local.right != null) {
                for (Expression right : local.right) {
                    count += countVariableReads(right, varName);
                }
            }
        } else if (node instanceof GlobalAssign) {
            GlobalAssign global = (GlobalAssign) node;
            if (global.right != null) {
                for (Expression right : global.right) {
                    count += countVariableReads(right, varName);
                }
            }
        } else if (node instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) node;
            for (Expression condition : ifStmt.conditions) {
                count += countVariableReads(condition, varName);
            }
            for (Block nested : ifStmt.blocks) {
                count += countVariableReads(nested, varName);
            }
            count += countVariableReads(ifStmt.elseBlock, varName);
        } else if (node instanceof WhileStatement) {
            WhileStatement whileStmt = (WhileStatement) node;
            count += countVariableReads(whileStmt.condition, varName);
            count += countVariableReads(whileStmt.body, varName);
        } else if (node instanceof RepeatStatement) {
            RepeatStatement repeat = (RepeatStatement) node;
            count += countVariableReads(repeat.condition, varName);
            count += countVariableReads(repeat.body, varName);
        } else if (node instanceof ForNumeric) {
            ForNumeric numeric = (ForNumeric) node;
            count += countVariableReads(numeric.start, varName);
            count += countVariableReads(numeric.end, varName);
            count += countVariableReads(numeric.step, varName);
            count += countVariableReads(numeric.body, varName);
        } else if (node instanceof ForIn) {
            ForIn forIn = (ForIn) node;
            for (Expression iterator : forIn.iterators) {
                count += countVariableReads(iterator, varName);
            }
            count += countVariableReads(forIn.body, varName);
        } else if (node instanceof FunctionDeclaration) {
            count += countVariableReads(((FunctionDeclaration) node).func.body, varName);
        } else if (node instanceof FunctionLiteral) {
            count += countVariableReads(((FunctionLiteral) node).body, varName);
        } else if (node instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) node;
            count += countVariableReads(binary.left, varName);
            count += countVariableReads(binary.right, varName);
        } else if (node instanceof UnaryOp) {
            count += countVariableReads(((UnaryOp) node).expr, varName);
        } else if (node instanceof IndexExpr) {
            IndexExpr index = (IndexExpr) node;
            count += countVariableReads(index.table, varName);
            count += countVariableReads(index.index, varName);
        } else if (node instanceof MemberExpr) {
            count += countVariableReads(((MemberExpr) node).table, varName);
        } else if (node instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) node;
            count += countVariableReads(call.callee, varName);
            for (Expression arg : call.args) {
                count += countVariableReads(arg, varName);
            }
        } else if (node instanceof TableConstructor) {
            TableConstructor table = (TableConstructor) node;
            if (table.fields != null) {
                for (TableField field : table.fields) {
                    count += countVariableReads(field.key, varName);
                    count += countVariableReads(field.value, varName);
                }
            }
        } else if (node instanceof ReturnStatement) {
            ReturnStatement ret = (ReturnStatement) node;
            for (Expression value : ret.values) {
                count += countVariableReads(value, varName);
            }
        } else if (node instanceof ExpressionStatement) {
            count += countVariableReads(((ExpressionStatement) node).expression, varName);
        }

        return count;
    }

    private boolean shouldDeclareLocal(String name, Expression right) {
        if (isRegisterOrObj(name)) {
            return true;
        }
        return isRequireCall(right);
    }

    private boolean isRequireCall(Expression expression) {
        if (!(expression instanceof FunctionCall)) {
            return false;
        }

        FunctionCall call = (FunctionCall) expression;
        return call.callee instanceof Name && "require".equals(((Name) call.callee).name);
    }

    private static class VerifiedPattern {
        final int reg;
        final Instruction assignInst;
        final Instruction returnInst;
        final Expression constExpr;

        VerifiedPattern(int reg, Instruction assignInst, Instruction returnInst, Expression constExpr) {
            this.reg = reg;
            this.assignInst = assignInst;
            this.returnInst = returnInst;
            this.constExpr = constExpr;
        }
    }

    private boolean isConstantExpr(Expression expr) {
        return expr instanceof NilConst || expr instanceof BooleanConst || expr instanceof NumberConst
                || expr instanceof StringConst;
    }

    private boolean areConstantsEqual(Expression e1, Expression e2) {
        if (e1 instanceof NilConst && e2 instanceof NilConst) {
            return true;
        }
        if (e1 instanceof BooleanConst && e2 instanceof BooleanConst) {
            return ((BooleanConst) e1).value == ((BooleanConst) e2).value;
        }
        if (e1 instanceof StringConst && e2 instanceof StringConst) {
            return ((StringConst) e1).value.equals(((StringConst) e2).value);
        }
        if (e1 instanceof NumberConst && e2 instanceof NumberConst) {
            return Double.compare(((NumberConst) e1).value, ((NumberConst) e2).value) == 0;
        }
        return false;
    }

    private VerifiedPattern verifyAssignReturnConstPattern(Assign assign, ReturnStatement ret,
            CodeGeneratorContext context, Set<String> upvalueNames) {
        if (assign.left.size() != 1 || assign.right.size() != 1 || ret.values.size() != 1) {
            return null;
        }
        Expression leftExpr = assign.left.get(0);
        Expression rightExpr = assign.right.get(0);

        if (!(leftExpr instanceof Name)) {
            return null;
        }
        Name leftName = (Name) leftExpr;
        int reg = RegisterNamePolicy.temporaryRegisterIndex(leftName.name);
        if (reg == -1) {
            return null;
        }

        // 基础安全校验：PC 有效性、LabelPC 校验、Upvalue 校验
        if (assign.pos == null || ret.pos == null ||
                assign.pos.pc < 0 || ret.pos.pc < 0 ||
                context.isLabelPC(assign.pos.pc) ||
                context.isLabelPC(ret.pos.pc) ||
                (upvalueNames != null && upvalueNames.contains(leftName.name))) {
            return null;
        }

        // 获取指令并验证一致性
        List<Instruction> instructions = context.getChunk().getInstructions();
        if (assign.pos.pc >= instructions.size() || ret.pos.pc >= instructions.size()) {
            return null;
        }
        Instruction assignInst = instructions.get(assign.pos.pc);
        Instruction returnInst = instructions.get(ret.pos.pc);

        if (returnInst.getOpcode() != Opcode.RETURN ||
                returnInst.getB() != 2 ||
                returnInst.getA() != reg ||
                assignInst.getA() != reg) {
            return null;
        }

        // 验证常量与指令的一致性
        Opcode assignOp = assignInst.getOpcode();
        boolean verified = false;

        if (assignOp == Opcode.LOADNIL) {
            if (assignInst.getA() == assignInst.getB() && rightExpr instanceof NilConst) {
                verified = true;
            }
        } else if (assignOp == Opcode.LOADBOOL) {
            if (rightExpr instanceof BooleanConst) {
                boolean astValue = ((BooleanConst) rightExpr).value;
                boolean instValue = assignInst.getB() != 0;
                if (astValue == instValue) {
                    verified = true;
                }
            }
        } else if (assignOp == Opcode.LOADK) {
            Constant k = context.getChunk().getConstant(assignInst.getBx());
            if (k != null) {
                Object kVal = k.getValue();
                if (rightExpr instanceof StringConst) {
                    String astStr = ((StringConst) rightExpr).value;
                    if (astStr.equals(kVal)) {
                        verified = true;
                    }
                } else if (rightExpr instanceof NumberConst) {
                    if (kVal instanceof Number) {
                        double astNum = ((NumberConst) rightExpr).value;
                        double kNum = ((Number) kVal).doubleValue();
                        if (Double.compare(astNum, kNum) == 0) {
                            verified = true;
                        }
                    }
                }
            }
        }

        if (verified) {
            return new VerifiedPattern(reg, assignInst, returnInst, rightExpr);
        }
        return null;
    }

    void optimizeReturnPatterns(Block block, CodeGeneratorContext context, Set<String> upvalueNames) {
        if (block == null || block.statements == null || context == null) {
            return;
        }

        // 递归处理嵌套块
        for (Statement statement : block.statements) {
            if (statement instanceof IfStatement) {
                IfStatement ifStmt = (IfStatement) statement;
                for (Block nested : ifStmt.blocks) {
                    optimizeReturnPatterns(nested, context, upvalueNames);
                }
                optimizeReturnPatterns(ifStmt.elseBlock, context, upvalueNames);
            } else if (statement instanceof FunctionDeclaration) {
                optimizeReturnPatterns(((FunctionDeclaration) statement).func.body, context, upvalueNames);
            } else if (statement instanceof WhileStatement) {
                optimizeReturnPatterns(((WhileStatement) statement).body, context, upvalueNames);
            } else if (statement instanceof ForNumeric) {
                optimizeReturnPatterns(((ForNumeric) statement).body, context, upvalueNames);
            } else if (statement instanceof ForIn) {
                optimizeReturnPatterns(((ForIn) statement).body, context, upvalueNames);
            }
        }

        List<Statement> stmts = block.statements;
        List<Statement> optimized = new ArrayList<>();

        for (int i = 0; i < stmts.size(); i++) {
            Statement s = stmts.get(i);

            if (s instanceof Assign && i + 1 < stmts.size() && stmts.get(i + 1) instanceof ReturnStatement) {
                Assign assign = (Assign) s;
                ReturnStatement ret = (ReturnStatement) stmts.get(i + 1);

                VerifiedPattern pattern = verifyAssignReturnConstPattern(assign, ret, context, upvalueNames);
                if (pattern != null) {
                    Expression retExpr = ret.values.get(0);

                    // 模式 A：Rn = const; return Rn
                    if (retExpr instanceof Name && ((Name) retExpr).name.equals(((Name) assign.left.get(0)).name)) {
                        ReturnStatement newRet = new ReturnStatement(assign.right, ret.pos);
                        optimized.add(newRet);
                        i++; // 跳过下一个已经合并的 ReturnStatement
                        continue;
                    }

                    // 模式 B：Rn = const; return const
                    if (isConstantExpr(retExpr) && areConstantsEqual(pattern.constExpr, retExpr)) {
                        // 只删除 Assign（即不将当前 assign s 加入到 optimized，保留 ret 在下一个循环迭代中加入）
                        continue;
                    }
                }
            }
            optimized.add(s);
        }

        block.statements.clear();
        block.statements.addAll(optimized);
    }

    private void simplifyRedundantNanChecks(Block block) {
        simplifyRedundantNanChecks(block, new HashSet<>(), new HashMap<>());
    }

    private void simplifyRedundantNanChecks(Block block, Set<String> nonNumberVars, Map<String, String> typeVarMap) {
        if (block == null || block.statements == null) {
            return;
        }

        List<Statement> rewritten = new ArrayList<>();
        for (Statement stmt : block.statements) {
            if (stmt instanceof Assign) {
                Assign assign = (Assign) stmt;
                if (assign.left.size() == 1 && assign.right.size() == 1) {
                    Expression left = assign.left.get(0);
                    Expression right = assign.right.get(0);
                    if (left instanceof Name) {
                        String leftName = ((Name) left).name;
                        if (right instanceof FunctionCall) {
                            FunctionCall call = (FunctionCall) right;
                            if (call.callee instanceof Name && ((Name) call.callee).name.equals("type")
                                    && call.args.size() == 1 && call.args.get(0) instanceof Name) {
                                typeVarMap.put(leftName, ((Name) call.args.get(0)).name);
                            } else {
                                typeVarMap.remove(leftName);
                            }
                        } else {
                            typeVarMap.remove(leftName);
                        }
                    }
                }
            } else if (stmt instanceof LocalAssign) {
                LocalAssign local = (LocalAssign) stmt;
                if (local.names.size() == 1 && local.right != null && local.right.size() == 1) {
                    String leftName = local.names.get(0);
                    Expression right = local.right.get(0);
                    if (right instanceof FunctionCall) {
                        FunctionCall call = (FunctionCall) right;
                        if (call.callee instanceof Name && ((Name) call.callee).name.equals("type")
                                && call.args.size() == 1 && call.args.get(0) instanceof Name) {
                            typeVarMap.put(leftName, ((Name) call.args.get(0)).name);
                        } else {
                            typeVarMap.remove(leftName);
                        }
                    } else {
                        typeVarMap.remove(leftName);
                    }
                }
            }

            if (stmt instanceof IfStatement) {
                IfStatement ifStmt = (IfStatement) stmt;

                Statement simplified = trySimplifyNanCheck(ifStmt, nonNumberVars);
                if (simplified != null) {
                    if (simplified instanceof Block) {
                        simplifyRedundantNanChecks((Block) simplified, nonNumberVars, typeVarMap);
                        rewritten.addAll(((Block) simplified).statements);
                    } else {
                        Block tempBlock = new Block(simplified.pos);
                        tempBlock.statements.add(simplified);
                        simplifyRedundantNanChecks(tempBlock, nonNumberVars, typeVarMap);
                        rewritten.addAll(tempBlock.statements);
                    }
                    continue;
                }

                List<Expression> newConditions = new ArrayList<>();
                List<Block> newBlocks = new ArrayList<>();

                for (int i = 0; i < ifStmt.conditions.size(); i++) {
                    Expression cond = ifStmt.conditions.get(i);
                    Block body = ifStmt.blocks.get(i);

                    String nonNumVar = getNonNumberVariableFromCondition(cond, true, typeVarMap);
                    Set<String> bodyNonNumberVars = new HashSet<>(nonNumberVars);
                    if (nonNumVar != null) {
                        bodyNonNumberVars.add(nonNumVar);
                    }

                    simplifyRedundantNanChecks(body, bodyNonNumberVars, new HashMap<>(typeVarMap));
                    newConditions.add(cond);
                    newBlocks.add(body);
                }

                Block newElseBlock = null;
                if (ifStmt.elseBlock != null) {
                    Set<String> elseNonNumberVars = new HashSet<>(nonNumberVars);
                    if (ifStmt.conditions.size() == 1) {
                        String nonNumVar = getNonNumberVariableFromCondition(ifStmt.conditions.get(0), false,
                                typeVarMap);
                        if (nonNumVar != null) {
                            elseNonNumberVars.add(nonNumVar);
                        }
                    }
                    newElseBlock = ifStmt.elseBlock;
                    simplifyRedundantNanChecks(newElseBlock, elseNonNumberVars, new HashMap<>(typeVarMap));
                }

                IfStatement newIf = new IfStatement(newConditions, newBlocks, newElseBlock, ifStmt.pos);
                rewritten.add(newIf);
            } else if (stmt instanceof WhileStatement) {
                WhileStatement wh = (WhileStatement) stmt;
                simplifyRedundantNanChecks(wh.body, nonNumberVars, new HashMap<>(typeVarMap));
                rewritten.add(wh);
            } else if (stmt instanceof RepeatStatement) {
                RepeatStatement rep = (RepeatStatement) stmt;
                simplifyRedundantNanChecks(rep.body, nonNumberVars, new HashMap<>(typeVarMap));
                rewritten.add(rep);
            } else if (stmt instanceof ForNumeric) {
                ForNumeric fn = (ForNumeric) stmt;
                simplifyRedundantNanChecks(fn.body, nonNumberVars, new HashMap<>(typeVarMap));
                rewritten.add(fn);
            } else if (stmt instanceof ForIn) {
                ForIn fi = (ForIn) stmt;
                simplifyRedundantNanChecks(fi.body, nonNumberVars, new HashMap<>(typeVarMap));
                rewritten.add(fi);
            } else if (stmt instanceof FunctionDeclaration) {
                FunctionDeclaration fd = (FunctionDeclaration) stmt;
                simplifyRedundantNanChecks(fd.func.body, new HashSet<>(), new HashMap<>());
                rewritten.add(fd);
            } else {
                rewritten.add(stmt);
            }
        }
        block.statements.clear();
        block.statements.addAll(rewritten);
    }

    private Statement trySimplifyNanCheck(Statement stmt, Set<String> nonNumberVars) {
        if (stmt instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) stmt;
            if (ifStmt.conditions.size() == 1) {
                Expression cond = ifStmt.conditions.get(0);
                if (cond instanceof BinaryOp) {
                    BinaryOp bin = (BinaryOp) cond;
                    if (bin.op.equals("~=") && bin.left instanceof Name && bin.right instanceof Name) {
                        String leftName = ((Name) bin.left).name;
                        String rightName = ((Name) bin.right).name;
                        if (leftName.equals(rightName) && nonNumberVars.contains(leftName)) {
                            if (ifStmt.elseBlock != null) {
                                return ifStmt.elseBlock;
                            } else {
                                return new Block(stmt.pos);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private String getNonNumberVariableFromCondition(Expression cond, boolean thenBranch,
            Map<String, String> typeVarMap) {
        if (cond instanceof BinaryOp) {
            BinaryOp bin = (BinaryOp) cond;
            Expression typeExpr = bin.left;
            Expression constExpr = bin.right;
            if (bin.right instanceof FunctionCall) {
                typeExpr = bin.right;
                constExpr = bin.left;
            }
            if (constExpr instanceof StringConst) {
                String typeStr = ((StringConst) constExpr).value;
                String varName = null;
                if (typeExpr instanceof FunctionCall) {
                    FunctionCall call = (FunctionCall) typeExpr;
                    if (call.callee instanceof Name && ((Name) call.callee).name.equals("type")
                            && call.args.size() == 1 && call.args.get(0) instanceof Name) {
                        varName = ((Name) call.args.get(0)).name;
                    }
                } else if (typeExpr instanceof Name) {
                    String name = ((Name) typeExpr).name;
                    if (typeVarMap != null && typeVarMap.containsKey(name)) {
                        varName = typeVarMap.get(name);
                    }
                }
                if (varName != null) {
                    if (bin.op.equals("==")) {
                        if (thenBranch) {
                            if (!typeStr.equals("number")) {
                                return varName;
                            }
                        } else {
                            if (typeStr.equals("number")) {
                                return varName;
                            }
                        }
                    } else if (bin.op.equals("~=")) {
                        if (thenBranch) {
                            if (typeStr.equals("number")) {
                                return varName;
                            }
                        } else {
                            if (!typeStr.equals("number")) {
                                return varName;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private String getResolvedUpvalueName(com.github.relua.model.UpValue upvalue, int b) {
        if (upvalue != null
                && (upvalue.getFromType() == com.github.relua.model.FromType.CONSTANT
                        || upvalue.getFromType() == com.github.relua.model.FromType.GLOBAL)
                && upvalue.getValue() != null) {
            Object val = upvalue.getValue();
            if (upvalue.getFromType() == com.github.relua.model.FromType.GLOBAL) {
                String globalName = val.toString();
                if (!RegisterNamePolicy.isTemporaryRegisterName(globalName)) {
                    if (globalName.contains(".")) {
                        return globalName.split("\\.")[0];
                    }
                    return globalName;
                }
            }
        }
        return (upvalue != null) ? upvalue.getName() : ("upvalue_" + b);
    }

    private int findReachingRegisterDefinitionPc(List<com.github.relua.model.Instruction> instructions,
            int beforeInstructionIndex, int registerIndex) {
        if (instructions == null) {
            return -1;
        }
        for (int i = beforeInstructionIndex - 1; i >= 0; i--) {
            com.github.relua.model.Instruction instruction = instructions.get(i);
            if (instructionDefinesRegister(instruction, registerIndex)) {
                return instruction.getPc();
            }
        }
        return -1;
    }

    private boolean instructionDefinesRegister(com.github.relua.model.Instruction instruction, int registerIndex) {
        if (instruction == null) {
            return false;
        }
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();
        switch (instruction.getOpcode()) {
            case MOVE:
            case LOADK:
            case LOADBOOL:
            case GETUPVAL:
            case GETGLOBAL:
            case GETTABLE:
            case NEWTABLE:
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
            case POW:
            case UNM:
            case NOT:
            case LEN:
            case CONCAT:
            case CLOSURE:
            case TESTSET:
                return registerIndex == a;
            case LOADNIL:
                return registerIndex >= a && registerIndex <= b;
            case SELF:
                return registerIndex == a || registerIndex == a + 1;
            case CALL:
                if (c == 0) {
                    return registerIndex >= a;
                }
                return registerIndex >= a && registerIndex <= a + c - 2;
            case TAILCALL:
                return false;
            case VARARG:
                if (b == 0) {
                    return registerIndex >= a;
                }
                return registerIndex >= a && registerIndex <= a + b - 1;
            case FORLOOP:
                return registerIndex == a || registerIndex == a + 3;
            case FORPREP:
                return registerIndex == a;
            case TFORLOOP:
                return c == 0 ? registerIndex >= a + 3 : registerIndex >= a + 3 && registerIndex <= a + 2 + c;
            default:
                return false;
        }
    }
}
