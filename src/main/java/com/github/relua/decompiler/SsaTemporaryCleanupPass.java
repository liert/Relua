package com.github.relua.decompiler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.relua.ast.Assign;
import com.github.relua.ast.AstNode;
import com.github.relua.ast.BinaryOp;
import com.github.relua.ast.Block;
import com.github.relua.ast.BooleanConst;
import com.github.relua.ast.Expression;
import com.github.relua.ast.ExpressionStatement;
import com.github.relua.ast.ForIn;
import com.github.relua.ast.ForNumeric;
import com.github.relua.ast.FunctionCall;
import com.github.relua.ast.FunctionDeclaration;
import com.github.relua.ast.FunctionLiteral;
import com.github.relua.ast.GlobalAssign;
import com.github.relua.ast.GotoStatement;
import com.github.relua.ast.IfStatement;
import com.github.relua.ast.IndexExpr;
import com.github.relua.ast.LabelStatement;
import com.github.relua.ast.LocalAssign;
import com.github.relua.ast.MemberExpr;
import com.github.relua.ast.Name;
import com.github.relua.ast.NilConst;
import com.github.relua.ast.NumberConst;
import com.github.relua.ast.RepeatStatement;
import com.github.relua.ast.ReturnStatement;
import com.github.relua.ast.SourcePos;
import com.github.relua.ast.Statement;
import com.github.relua.ast.StringConst;
import com.github.relua.ast.TableConstructor;
import com.github.relua.ast.TableField;
import com.github.relua.ast.UnaryOp;
import com.github.relua.ast.WhileStatement;
import com.github.relua.decompiler.ssa.SsaBlock;
import com.github.relua.decompiler.ssa.SsaExpressionAnalysis;
import com.github.relua.decompiler.ssa.SsaFunction;
import com.github.relua.decompiler.ssa.SsaInstruction;
import com.github.relua.decompiler.ssa.SsaPhi;
import com.github.relua.decompiler.ssa.SsaValue;
import com.github.relua.decompiler.ssa.SsaValueKind;
import com.github.relua.decompiler.ssa.SsaValueSummary;
import com.github.relua.util.RegisterNamePolicy;

final class SsaTemporaryCleanupPass {
    void inlineSingleUse(Block block, CodeGeneratorContext context, DecompilerPipeline pipeline,
            Map<String, Set<Integer>> protectedDefinitionPcs) {
        if (!hasSsaContext(block, context, pipeline)) {
            return;
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < block.statements.size(); i++) {
                Statement statement = block.statements.get(i);
                inlineSingleUseInNested(statement, context, pipeline, protectedDefinitionPcs);

                SsaTemporaryAssignment assignment = ssaTemporaryAssignment(block.statements, i, context, pipeline,
                        protectedDefinitionPcs);
                if (assignment == null || isSelfCopy(assignment)) {
                    continue;
                }

                boolean isPure = isDiscardableSsaTemporarySummary(assignment.analysis.getSummary(assignment.definition))
                        && isAstPureExpression(assignment.right);

                int useIndex = findSingleLinearUseIndex(block.statements, i, assignment);
                if (useIndex < 0 || !canInlineIntoStatement(block.statements.get(useIndex))) {
                    continue;
                }
                
                int readsOutside = countVariableReadsOutsideExpression(block.statements.get(useIndex), assignment.name,
                        assignment.right);
                if (readsOutside == 0) {
                    block.statements.remove(i);
                    changed = true;
                    break;
                }

                if (!isPure) {
                    continue;
                }

                Statement rewritten = replaceVariableReadInStatement(block.statements.get(useIndex), assignment.name,
                        assignment.right);
                block.statements.set(useIndex, rewritten);
                block.statements.remove(i);
                changed = true;
                break;
            }
        }
    }

    void deleteDead(Block block, CodeGeneratorContext context, DecompilerPipeline pipeline,
            Map<String, Set<Integer>> protectedDefinitionPcs, boolean deleteAtBlockExitWhenNoAstUse) {
        if (!hasSsaContext(block, context, pipeline)) {
            return;
        }
        Set<SsaValue> livePhis = computeLivePhis(pipeline.requireSsaExpressionAnalysis(context.getChunk().getFunction()).getFunction());
        deleteDead(block, context, pipeline, protectedDefinitionPcs, deleteAtBlockExitWhenNoAstUse, livePhis);
    }

    private void deleteDead(Block block, CodeGeneratorContext context, DecompilerPipeline pipeline,
            Map<String, Set<Integer>> protectedDefinitionPcs, boolean deleteAtBlockExitWhenNoAstUse,
            Set<SsaValue> livePhis) {
        if (!hasSsaContext(block, context, pipeline)) {
            return;
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < block.statements.size(); i++) {
                Statement statement = block.statements.get(i);
                deleteDeadInNested(statement, context, pipeline, protectedDefinitionPcs, livePhis);

                SsaTemporaryAssignment assignment = ssaTemporaryAssignment(block.statements, i, context, pipeline,
                        protectedDefinitionPcs);
                if (assignment == null) {
                    AstTemporaryAssignment ast = astTemporaryAssignment(statement);
                    if (canDeleteAstDeadTemporaryDefinition(block.statements, i, ast)) {
                        block.statements.remove(i);
                        changed = true;
                        break;
                    }
                    continue;
                }
                if (isSelfCopy(assignment)
                        || canDeleteSsaTemporaryDefinition(block.statements, i, assignment,
                                deleteAtBlockExitWhenNoAstUse, livePhis)
                        || canDeleteAstDeadTemporaryDefinition(block.statements, i,
                                new AstTemporaryAssignment(assignment.name, assignment.right))) {
                    block.statements.remove(i);
                    changed = true;
                    break;
                }
            }
        }
    }

    private boolean hasSsaContext(Block block, CodeGeneratorContext context, DecompilerPipeline pipeline) {
        return block != null && block.statements != null && context != null && context.getChunk() != null
                && pipeline != null;
    }

    private void deleteDeadInNested(Statement statement, CodeGeneratorContext context, DecompilerPipeline pipeline,
            Map<String, Set<Integer>> protectedDefinitionPcs, Set<SsaValue> livePhis) {
        forEachNestedBlock(statement, block -> deleteDead(block, context, pipeline, protectedDefinitionPcs,
                statement instanceof FunctionDeclaration, livePhis));
    }

    private void inlineSingleUseInNested(Statement statement, CodeGeneratorContext context, DecompilerPipeline pipeline,
            Map<String, Set<Integer>> protectedDefinitionPcs) {
        forEachNestedBlock(statement, block -> inlineSingleUse(block, context, pipeline, protectedDefinitionPcs));
    }

    private void forEachNestedBlock(Statement statement, java.util.function.Consumer<Block> consumer) {
        if (statement instanceof IfStatement) {
            IfStatement ifStatement = (IfStatement) statement;
            for (Block nested : ifStatement.blocks) {
                consumer.accept(nested);
            }
            consumer.accept(ifStatement.elseBlock);
        } else if (statement instanceof WhileStatement) {
            consumer.accept(((WhileStatement) statement).body);
        } else if (statement instanceof RepeatStatement) {
            consumer.accept(((RepeatStatement) statement).body);
        } else if (statement instanceof ForNumeric) {
            consumer.accept(((ForNumeric) statement).body);
        } else if (statement instanceof ForIn) {
            consumer.accept(((ForIn) statement).body);
        } else if (statement instanceof FunctionDeclaration) {
            consumer.accept(((FunctionDeclaration) statement).func.body);
        }
    }

    private SsaTemporaryAssignment ssaTemporaryAssignment(List<Statement> statements, int index,
            CodeGeneratorContext context, DecompilerPipeline pipeline, Map<String, Set<Integer>> protectedDefinitionPcs) {
        Statement statement = statements.get(index);
        SsaTemporaryAssignment direct = ssaTemporaryAssignment(statement, context, pipeline, protectedDefinitionPcs);
        if (direct != null) {
            return direct;
        }
        AstTemporaryAssignment ast = astTemporaryAssignment(statement);
        if (ast == null) {
            return null;
        }
        int pc = inferTemporaryDefinitionPc(statements, index, ast, context, pipeline, protectedDefinitionPcs);
        return pc >= 0 ? ssaTemporaryAssignment(ast, statement, pc, context, pipeline, protectedDefinitionPcs) : null;
    }

    private SsaTemporaryAssignment ssaTemporaryAssignment(Statement statement, CodeGeneratorContext context,
            DecompilerPipeline pipeline, Map<String, Set<Integer>> protectedDefinitionPcs) {
        AstTemporaryAssignment ast = astTemporaryAssignment(statement);
        SourcePos pos = statement.pos;
        if (ast == null || pos == null || pos.pc < 0) {
            return null;
        }
        return ssaTemporaryAssignment(ast, statement, pos.pc, context, pipeline, protectedDefinitionPcs);
    }

    private SsaTemporaryAssignment ssaTemporaryAssignment(AstTemporaryAssignment ast, Statement statement, int pc,
            CodeGeneratorContext context, DecompilerPipeline pipeline, Map<String, Set<Integer>> protectedDefinitionPcs) {
        if (ast == null || pc < 0 || !RegisterNamePolicy.isTemporaryRegisterName(ast.name)) {
            return null;
        }
        int register = RegisterNamePolicy.temporaryRegisterIndex(ast.name);
        if (register < 0) {
            return null;
        }
        SsaInstruction instruction = pipeline.requireSsaInstruction(context.getChunk().getFunction(), pc);
        SsaValue definition = instruction.getFirstDefForRegister(register);
        if (definition == null || isProtectedTemporaryDefinition(ast.name, pc, protectedDefinitionPcs)) {
            return null;
        }
        SsaExpressionAnalysis analysis = pipeline.requireSsaExpressionAnalysis(context.getChunk().getFunction());
        SsaValueSummary summary = analysis.getSummary(definition);
        if (summary == null) {
            return null;
        }
        boolean allowedSummary = isDiscardableSsaTemporarySummary(summary) || summary.getKind() == SsaValueKind.CALL_RESULT;
        boolean allowedAst = isAstPureExpression(ast.right) || (ast.right instanceof FunctionCall);
        if (!allowedSummary || !allowedAst || !matchesSsaSummary(ast.right, summary)) {
            return null;
        }
        return new SsaTemporaryAssignment(ast.name, ast.right, definition, analysis);
    }

    private AstTemporaryAssignment astTemporaryAssignment(Statement statement) {
        if (statement instanceof Assign) {
            Assign assign = (Assign) statement;
            if (assign.left.size() == 1 && assign.right.size() == 1 && assign.left.get(0) instanceof Name) {
                return new AstTemporaryAssignment(((Name) assign.left.get(0)).name, assign.right.get(0));
            }
        } else if (statement instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) statement;
            if (local.names.size() == 1 && local.right != null && local.right.size() == 1) {
                return new AstTemporaryAssignment(local.names.get(0), local.right.get(0));
            }
        }
        return null;
    }

    private int inferTemporaryDefinitionPc(List<Statement> statements, int index, AstTemporaryAssignment ast,
            CodeGeneratorContext context, DecompilerPipeline pipeline, Map<String, Set<Integer>> protectedDefinitionPcs) {
        if (context == null || context.getChunk() == null || ast == null
                || !RegisterNamePolicy.isTemporaryRegisterName(ast.name)) {
            return -1;
        }
        int register = RegisterNamePolicy.temporaryRegisterIndex(ast.name);
        if (register < 0) {
            return -1;
        }

        int lower = 0;
        for (int i = index - 1; i >= 0; i--) {
            SourcePos pos = statements.get(i).pos;
            if (pos != null && pos.pc >= 0) {
                lower = pos.pc + 1;
                break;
            }
        }
        int upper = context.getChunk().getInstructions().size() - 1;
        for (int i = index + 1; i < statements.size(); i++) {
            SourcePos pos = statements.get(i).pos;
            if (pos != null && pos.pc >= 0) {
                upper = pos.pc - 1;
                break;
            }
        }

        int matchedPc = -1;
        SsaExpressionAnalysis analysis = pipeline.requireSsaExpressionAnalysis(context.getChunk().getFunction());
        for (int pc = lower; pc <= upper; pc++) {
            if (isProtectedTemporaryDefinition(ast.name, pc, protectedDefinitionPcs)) {
                continue;
            }
            SsaInstruction instruction = pipeline.requireSsaInstruction(context.getChunk().getFunction(), pc);
            SsaValue definition = instruction.getFirstDefForRegister(register);
            if (definition == null) {
                continue;
            }
            SsaValueSummary summary = analysis.getSummary(definition);
            if (summary != null && isDiscardableSsaTemporarySummary(summary) && matchesSsaSummary(ast.right, summary)) {
                if (matchedPc >= 0) {
                    return -1;
                }
                matchedPc = pc;
            }
        }
        return matchedPc;
    }

    private boolean canDeleteAstDeadTemporaryDefinition(List<Statement> statements, int index,
            AstTemporaryAssignment assignment) {
        if (assignment == null || !RegisterNamePolicy.isTemporaryRegisterName(assignment.name)
                || !isAstPureExpression(assignment.right)) {
            return false;
        }
        for (int i = index + 1; i < statements.size(); i++) {
            Statement next = statements.get(i);
            if (countVariableReads(next, assignment.name) > 0) {
                return false;
            }
            if (definesTemporary(next, assignment.name)) {
                return true;
            }
            if (containsUnresolvedTransfer(next)) {
                return false;
            }
        }
        return true;
    }

    private boolean canDeleteSsaTemporaryDefinition(List<Statement> statements, int index,
            SsaTemporaryAssignment assignment, boolean deleteAtBlockExitWhenNoAstUse,
            Set<SsaValue> livePhis) {
        Set<Integer> astConsumedUsePcs = new HashSet<>();
        int lastLinearPc = -1;
        for (int i = index + 1; i < statements.size(); i++) {
            Statement next = statements.get(i);
            if (countVariableReads(next, assignment.name) > 0) {
                return false;
            }
            if (next.pos != null && next.pos.pc >= 0) {
                astConsumedUsePcs.add(next.pos.pc);
                lastLinearPc = Math.max(lastLinearPc, next.pos.pc);
            }
            if (definesTemporary(next, assignment.name)) {
                return true;
            }
        }
        boolean isPure = isDiscardableSsaTemporarySummary(assignment.analysis.getSummary(assignment.definition))
                && isAstPureExpression(assignment.right);
        List<SsaInstruction> ssaUses = assignment.analysis.getFunction().getInstructionUses(assignment.definition);

        if (deleteAtBlockExitWhenNoAstUse || assignment.analysis.getFunction().getUseCount(assignment.definition) == 0) {
            return isPure;
        }
        if (!isPure) {
            if (ssaUses.size() != 1) {
                return false;
            }
        }
        return hasOnlyAstConsumedLinearSsaUses(assignment, astConsumedUsePcs, lastLinearPc, livePhis);
    }

    private boolean hasOnlyAstConsumedLinearSsaUses(SsaTemporaryAssignment assignment, Set<Integer> astConsumedUsePcs,
            int lastLinearPc, Set<SsaValue> livePhis) {
        for (SsaPhi phi : assignment.analysis.getFunction().getPhiUses(assignment.definition)) {
            if (livePhis.contains(phi.getTarget())) {
                return false;
            }
        }
        SsaInstruction definitionInstruction = assignment.analysis.getFunction()
                .getDefiningInstruction(assignment.definition);
        int definitionPc = definitionInstruction != null ? definitionInstruction.getPc() : -1;
        List<SsaInstruction> uses = assignment.analysis.getFunction().getInstructionUses(assignment.definition);
        for (SsaInstruction use : uses) {
            if (use == null) {
                return false;
            }
            boolean exactAstUse = astConsumedUsePcs.contains(use.getPc());
            boolean rangeAstUse = definitionPc >= 0 && lastLinearPc >= definitionPc
                    && use.getPc() > definitionPc && use.getPc() <= lastLinearPc;
            if (!exactAstUse && !rangeAstUse) {
                return false;
            }
        }
        return true;
    }

    private int findSingleLinearUseIndex(List<Statement> statements, int definitionIndex,
            SsaTemporaryAssignment assignment) {
        Set<String> dependencies = new HashSet<>();
        collectTemporaryNameReads(assignment.right, dependencies);
        dependencies.remove(assignment.name);

        int useIndex = -1;
        int totalReads = 0;
        for (int i = definitionIndex + 1; i < statements.size(); i++) {
            Statement next = statements.get(i);
            if (containsUnresolvedTransfer(next)) {
                return -1;
            }
            int reads = countVariableReads(next, assignment.name);
            if (reads > 0) {
                totalReads += reads;
                if (totalReads > 1) {
                    return -1;
                }
                useIndex = i;
                if (definesAnyTemporary(next, dependencies) && !writesDependenciesAfterReadableOperands(next)) {
                    return -1;
                }
            } else if (hasSideEffectBeforeUse(next, dependencies)) {
                return -1;
            }
            if (definesTemporary(next, assignment.name)) {
                return totalReads == 1 ? useIndex : -1;
            }
            if (definesAnyTemporary(next, dependencies)) {
                return -1;
            }
        }
        return totalReads == 1 ? useIndex : -1;
    }

    private boolean hasSideEffectBeforeUse(Statement statement, Set<String> dependencies) {
        return dependencies != null && !dependencies.isEmpty() && statementHasSideEffect(statement);
    }

    private boolean writesDependenciesAfterReadableOperands(Statement statement) {
        if (statement instanceof Assign) {
            for (Expression left : ((Assign) statement).left) {
                if (!(left instanceof Name)) {
                    return false;
                }
            }
            return true;
        }
        return statement instanceof LocalAssign || statement instanceof GlobalAssign;
    }

    private boolean canInlineIntoStatement(Statement statement) {
        return statement instanceof Assign || statement instanceof LocalAssign || statement instanceof GlobalAssign
                || statement instanceof ExpressionStatement || statement instanceof ReturnStatement;
    }

    private Statement replaceVariableReadInStatement(Statement statement, String name, Expression replacement) {
        return (Statement) replaceVariableRead(statement, name, replacement);
    }

    private AstNode replaceVariableRead(AstNode node, String name, Expression replacement) {
        if (node == null) {
            return null;
        }
        if (node instanceof Name) {
            return name.equals(((Name) node).name) ? replacement : node;
        }
        if (node instanceof Assign) {
            Assign assign = (Assign) node;
            List<Expression> newLeft = new ArrayList<>();
            for (Expression left : assign.left) {
                newLeft.add(left instanceof Name ? left : (Expression) replaceVariableRead(left, name, replacement));
            }
            List<Expression> newRight = new ArrayList<>();
            for (Expression right : assign.right) {
                newRight.add((Expression) replaceVariableRead(right, name, replacement));
            }
            return new Assign(newLeft, newRight, assign.pos);
        }
        if (node instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) node;
            List<Expression> newRight = new ArrayList<>();
            if (local.right != null) {
                for (Expression right : local.right) {
                    newRight.add((Expression) replaceVariableRead(right, name, replacement));
                }
            }
            return new LocalAssign(local.names, newRight, local.pos);
        }
        if (node instanceof GlobalAssign) {
            GlobalAssign global = (GlobalAssign) node;
            List<Expression> newRight = new ArrayList<>();
            if (global.right != null) {
                for (Expression right : global.right) {
                    newRight.add((Expression) replaceVariableRead(right, name, replacement));
                }
            }
            return new GlobalAssign(global.names, newRight, global.pos);
        }
        if (node instanceof ExpressionStatement) {
            ExpressionStatement expr = (ExpressionStatement) node;
            return new ExpressionStatement((Expression) replaceVariableRead(expr.expression, name, replacement),
                    expr.pos);
        }
        if (node instanceof ReturnStatement) {
            ReturnStatement ret = (ReturnStatement) node;
            List<Expression> values = new ArrayList<>();
            for (Expression value : ret.values) {
                values.add((Expression) replaceVariableRead(value, name, replacement));
            }
            return new ReturnStatement(values, ret.pos);
        }
        if (node instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) node;
            return new BinaryOp(binary.op, (Expression) replaceVariableRead(binary.left, name, replacement),
                    (Expression) replaceVariableRead(binary.right, name, replacement), binary.pos);
        }
        if (node instanceof UnaryOp) {
            UnaryOp unary = (UnaryOp) node;
            return new UnaryOp(unary.op, (Expression) replaceVariableRead(unary.expr, name, replacement), unary.pos);
        }
        if (node instanceof IndexExpr) {
            IndexExpr index = (IndexExpr) node;
            return new IndexExpr((Expression) replaceVariableRead(index.table, name, replacement),
                    (Expression) replaceVariableRead(index.index, name, replacement), index.pos);
        }
        if (node instanceof MemberExpr) {
            MemberExpr member = (MemberExpr) node;
            return new MemberExpr((Expression) replaceVariableRead(member.table, name, replacement), member.member,
                    member.pos);
        }
        if (node instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) node;
            List<Expression> args = new ArrayList<>();
            for (Expression arg : call.args) {
                args.add((Expression) replaceVariableRead(arg, name, replacement));
            }
            return new FunctionCall((Expression) replaceVariableRead(call.callee, name, replacement), args,
                    call.isMethodCall, call.returns, call.pos);
        }
        if (node instanceof TableConstructor) {
            TableConstructor table = (TableConstructor) node;
            List<TableField> fields = new ArrayList<>();
            if (table.fields != null) {
                for (TableField field : table.fields) {
                    fields.add(new TableField((Expression) replaceVariableRead(field.key, name, replacement),
                            (Expression) replaceVariableRead(field.value, name, replacement)));
                }
            }
            return new TableConstructor(fields, table.pos);
        }
        return node;
    }

    private boolean matchesSsaSummary(Expression expression, SsaValueSummary summary) {
        if (summary == null) {
            return false;
        }
        if (summary.getKind() == SsaValueKind.UPVALUE) {
            return true;
        }
        if (expression instanceof StringConst) {
            Object constant = summary.getConstantValue();
            return summary.getKind() == SsaValueKind.CONSTANT && constant instanceof String
                    && constant.equals(((StringConst) expression).value);
        }
        if (expression instanceof NumberConst) {
            Object constant = summary.getConstantValue();
            return summary.getKind() == SsaValueKind.CONSTANT && constant instanceof Number
                    && Double.compare(((Number) constant).doubleValue(), ((NumberConst) expression).value) == 0;
        }
        if (expression instanceof BooleanConst) {
            Object constant = summary.getConstantValue();
            return summary.getKind() == SsaValueKind.CONSTANT && constant instanceof Boolean
                    && constant.equals(((BooleanConst) expression).value);
        }
        if (expression instanceof NilConst) {
            return summary.getKind() == SsaValueKind.CONSTANT && summary.getConstantValue() == null;
        }
        if (expression instanceof Name) {
            return summary.getKind() == SsaValueKind.COPY || summary.getKind() == SsaValueKind.UPVALUE
                    || summary.isPure();
        }
        if (expression instanceof FunctionCall && summary.getKind() == SsaValueKind.CALL_RESULT) {
            return true;
        }
        return summary.isPure();
    }

    private boolean isDiscardableSsaTemporarySummary(SsaValueSummary summary) {
        return summary.isPure() || summary.getKind() == SsaValueKind.UPVALUE;
    }

    private boolean isProtectedTemporaryDefinition(String name, int pc, Map<String, Set<Integer>> protectedDefinitionPcs) {
        if (protectedDefinitionPcs == null || name == null || pc < 0) {
            return false;
        }
        Set<Integer> pcs = protectedDefinitionPcs.get(name);
        return pcs != null && pcs.contains(pc);
    }

    private boolean isSelfCopy(SsaTemporaryAssignment assignment) {
        return assignment.right instanceof Name && assignment.name.equals(((Name) assignment.right).name);
    }

    private boolean definesTemporary(Statement statement, String name) {
        if (statement instanceof Assign) {
            Assign assign = (Assign) statement;
            return assign.left.size() == 1 && assign.left.get(0) instanceof Name
                    && name.equals(((Name) assign.left.get(0)).name);
        }
        if (statement instanceof LocalAssign) {
            return ((LocalAssign) statement).names.contains(name);
        }
        return false;
    }

    private boolean definesAnyTemporary(Statement statement, Set<String> names) {
        if (statement == null || names == null || names.isEmpty()) {
            return false;
        }
        for (String name : names) {
            if (definesTemporary(statement, name)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsUnresolvedTransfer(AstNode node) {
        if (node == null) {
            return false;
        }
        if (node instanceof GotoStatement || node instanceof LabelStatement) {
            return true;
        }
        if (node instanceof IfStatement) {
            IfStatement ifStatement = (IfStatement) node;
            for (Block nested : ifStatement.blocks) {
                if (containsUnresolvedTransfer(nested)) {
                    return true;
                }
            }
            return containsUnresolvedTransfer(ifStatement.elseBlock);
        }
        if (node instanceof Block) {
            for (Statement statement : ((Block) node).statements) {
                if (containsUnresolvedTransfer(statement)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAstPureExpression(Expression expression) {
        if (expression == null) {
            return true;
        }
        if (expression instanceof NilConst || expression instanceof BooleanConst || expression instanceof NumberConst
                || expression instanceof StringConst || expression instanceof Name) {
            return true;
        }
        if (expression instanceof IndexExpr) {
            IndexExpr idx = (IndexExpr) expression;
            return isAstPureExpression(idx.table) && isAstPureExpression(idx.index);
        }
        if (expression instanceof MemberExpr) {
            MemberExpr mem = (MemberExpr) expression;
            return isAstPureExpression(mem.table);
        }
        if (expression instanceof UnaryOp) {
            return isAstPureExpression(((UnaryOp) expression).expr);
        }
        if (expression instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) expression;
            return isAstPureExpression(binary.left) && isAstPureExpression(binary.right);
        }
        if (expression instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) expression;
            if (!isPureStringFormatCall(call)) {
                return false;
            }
            for (Expression arg : call.args) {
                if (!isAstPureExpression(arg)) {
                    return false;
                }
            }
            return true;
        }
        return expression instanceof TableConstructor && ((TableConstructor) expression).isEmpty();
    }

    private boolean isPureStringFormatCall(FunctionCall call) {
        return call != null && !call.isMethodCall && call.callee instanceof MemberExpr
                && "format".equals(((MemberExpr) call.callee).member)
                && ((MemberExpr) call.callee).table instanceof Name
                && "string".equals(((Name) ((MemberExpr) call.callee).table).name);
    }

    private void collectTemporaryNameReads(AstNode node, Set<String> names) {
        if (node == null || names == null) {
            return;
        }
        if (node instanceof Name) {
            String name = ((Name) node).name;
            if (RegisterNamePolicy.isTemporaryRegisterName(name)) {
                names.add(name);
            }
        } else if (node instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) node;
            collectTemporaryNameReads(binary.left, names);
            collectTemporaryNameReads(binary.right, names);
        } else if (node instanceof UnaryOp) {
            collectTemporaryNameReads(((UnaryOp) node).expr, names);
        } else if (node instanceof IndexExpr) {
            IndexExpr index = (IndexExpr) node;
            collectTemporaryNameReads(index.table, names);
            collectTemporaryNameReads(index.index, names);
        } else if (node instanceof MemberExpr) {
            collectTemporaryNameReads(((MemberExpr) node).table, names);
        } else if (node instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) node;
            collectTemporaryNameReads(call.callee, names);
            for (Expression arg : call.args) {
                collectTemporaryNameReads(arg, names);
            }
        } else if (node instanceof TableConstructor) {
            TableConstructor table = (TableConstructor) node;
            if (table.fields != null) {
                for (TableField field : table.fields) {
                    collectTemporaryNameReads(field.key, names);
                    collectTemporaryNameReads(field.value, names);
                }
            }
        }
    }

    private boolean statementHasSideEffect(Statement statement) {
        if (statement == null) {
            return false;
        }
        if (statement instanceof Assign) {
            Assign assign = (Assign) statement;
            for (Expression left : assign.left) {
                if (!(left instanceof Name) || !RegisterNamePolicy.isTemporaryRegisterName(((Name) left).name)) {
                    return true;
                }
            }
            for (Expression right : assign.right) {
                if (!isAstPureExpression(right)) {
                    return true;
                }
            }
            return false;
        }
        if (statement instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) statement;
            if (local.right != null) {
                for (Expression right : local.right) {
                    if (!isAstPureExpression(right)) {
                        return true;
                    }
                }
            }
            return false;
        }
        return statement instanceof GlobalAssign || statement instanceof ExpressionStatement
                || statement instanceof ReturnStatement || statement instanceof IfStatement
                || statement instanceof WhileStatement || statement instanceof RepeatStatement
                || statement instanceof ForNumeric || statement instanceof ForIn
                || statement instanceof FunctionDeclaration;
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

    private int countVariableReadsOutsideExpression(AstNode node, String varName, Expression consumedExpression) {
        if (node == null || varName == null) {
            return 0;
        }
        if (node instanceof Expression && isConsumedExpression((Expression) node, consumedExpression, varName)) {
            return 0;
        }
        int count = 0;
        if (node instanceof Name) {
            return varName.equals(((Name) node).name) ? 1 : 0;
        } else if (node instanceof Block) {
            for (Statement stmt : ((Block) node).statements) {
                count += countVariableReadsOutsideExpression(stmt, varName, consumedExpression);
            }
        } else if (node instanceof Assign) {
            Assign assign = (Assign) node;
            for (Expression left : assign.left) {
                if (!(left instanceof Name)) {
                    count += countVariableReadsOutsideExpression(left, varName, consumedExpression);
                }
            }
            for (Expression right : assign.right) {
                count += countVariableReadsOutsideExpression(right, varName, consumedExpression);
            }
        } else if (node instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) node;
            if (local.right != null) {
                for (Expression right : local.right) {
                    count += countVariableReadsOutsideExpression(right, varName, consumedExpression);
                }
            }
        } else if (node instanceof GlobalAssign) {
            GlobalAssign global = (GlobalAssign) node;
            if (global.right != null) {
                for (Expression right : global.right) {
                    count += countVariableReadsOutsideExpression(right, varName, consumedExpression);
                }
            }
        } else if (node instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) node;
            for (Expression condition : ifStmt.conditions) {
                count += countVariableReadsOutsideExpression(condition, varName, consumedExpression);
            }
            for (Block nested : ifStmt.blocks) {
                count += countVariableReadsOutsideExpression(nested, varName, consumedExpression);
            }
            count += countVariableReadsOutsideExpression(ifStmt.elseBlock, varName, consumedExpression);
        } else if (node instanceof WhileStatement) {
            WhileStatement whileStmt = (WhileStatement) node;
            count += countVariableReadsOutsideExpression(whileStmt.condition, varName, consumedExpression);
            count += countVariableReadsOutsideExpression(whileStmt.body, varName, consumedExpression);
        } else if (node instanceof RepeatStatement) {
            RepeatStatement repeat = (RepeatStatement) node;
            count += countVariableReadsOutsideExpression(repeat.condition, varName, consumedExpression);
            count += countVariableReadsOutsideExpression(repeat.body, varName, consumedExpression);
        } else if (node instanceof ForNumeric) {
            ForNumeric numeric = (ForNumeric) node;
            count += countVariableReadsOutsideExpression(numeric.start, varName, consumedExpression);
            count += countVariableReadsOutsideExpression(numeric.end, varName, consumedExpression);
            count += countVariableReadsOutsideExpression(numeric.step, varName, consumedExpression);
            count += countVariableReadsOutsideExpression(numeric.body, varName, consumedExpression);
        } else if (node instanceof ForIn) {
            ForIn forIn = (ForIn) node;
            for (Expression iterator : forIn.iterators) {
                count += countVariableReadsOutsideExpression(iterator, varName, consumedExpression);
            }
            count += countVariableReadsOutsideExpression(forIn.body, varName, consumedExpression);
        } else if (node instanceof FunctionDeclaration) {
            count += countVariableReadsOutsideExpression(((FunctionDeclaration) node).func.body, varName,
                    consumedExpression);
        } else if (node instanceof FunctionLiteral) {
            count += countVariableReadsOutsideExpression(((FunctionLiteral) node).body, varName, consumedExpression);
        } else if (node instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) node;
            count += countVariableReadsOutsideExpression(binary.left, varName, consumedExpression);
            count += countVariableReadsOutsideExpression(binary.right, varName, consumedExpression);
        } else if (node instanceof UnaryOp) {
            count += countVariableReadsOutsideExpression(((UnaryOp) node).expr, varName, consumedExpression);
        } else if (node instanceof IndexExpr) {
            IndexExpr index = (IndexExpr) node;
            count += countVariableReadsOutsideExpression(index.table, varName, consumedExpression);
            count += countVariableReadsOutsideExpression(index.index, varName, consumedExpression);
        } else if (node instanceof MemberExpr) {
            count += countVariableReadsOutsideExpression(((MemberExpr) node).table, varName, consumedExpression);
        } else if (node instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) node;
            count += countVariableReadsOutsideExpression(call.callee, varName, consumedExpression);
            for (Expression arg : call.args) {
                count += countVariableReadsOutsideExpression(arg, varName, consumedExpression);
            }
        } else if (node instanceof TableConstructor) {
            TableConstructor table = (TableConstructor) node;
            if (table.fields != null) {
                for (TableField field : table.fields) {
                    count += countVariableReadsOutsideExpression(field.key, varName, consumedExpression);
                    count += countVariableReadsOutsideExpression(field.value, varName, consumedExpression);
                }
            }
        } else if (node instanceof ReturnStatement) {
            ReturnStatement ret = (ReturnStatement) node;
            for (Expression value : ret.values) {
                count += countVariableReadsOutsideExpression(value, varName, consumedExpression);
            }
        } else if (node instanceof ExpressionStatement) {
            count += countVariableReadsOutsideExpression(((ExpressionStatement) node).expression, varName,
                    consumedExpression);
        }
        return count;
    }

    private boolean isSameExpression(Expression first, Expression second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null || first.getClass() != second.getClass()) {
            return false;
        }
        if (first instanceof Name) {
            return ((Name) first).name.equals(((Name) second).name);
        }
        if (first instanceof StringConst) {
            return ((StringConst) first).value.equals(((StringConst) second).value);
        }
        if (first instanceof NumberConst) {
            return Double.compare(((NumberConst) first).value, ((NumberConst) second).value) == 0;
        }
        if (first instanceof BooleanConst) {
            return ((BooleanConst) first).value == ((BooleanConst) second).value;
        }
        if (first instanceof NilConst) {
            return true;
        }
        if (first instanceof BinaryOp) {
            BinaryOp left = (BinaryOp) first;
            BinaryOp right = (BinaryOp) second;
            return left.op.equals(right.op) && isSameExpression(left.left, right.left)
                    && isSameExpression(left.right, right.right);
        }
        if (first instanceof UnaryOp) {
            UnaryOp left = (UnaryOp) first;
            UnaryOp right = (UnaryOp) second;
            return left.op.equals(right.op) && isSameExpression(left.expr, right.expr);
        }
        if (first instanceof IndexExpr) {
            IndexExpr left = (IndexExpr) first;
            IndexExpr right = (IndexExpr) second;
            return isSameExpression(left.table, right.table) && isSameExpression(left.index, right.index);
        }
        if (first instanceof MemberExpr) {
            MemberExpr left = (MemberExpr) first;
            MemberExpr right = (MemberExpr) second;
            return left.member.equals(right.member) && isSameExpression(left.table, right.table);
        }
        if (first instanceof FunctionCall) {
            FunctionCall left = (FunctionCall) first;
            FunctionCall right = (FunctionCall) second;
            if (left.isMethodCall != right.isMethodCall || !isSameExpression(left.callee, right.callee)
                    || left.args.size() != right.args.size()) {
                return false;
            }
            for (int i = 0; i < left.args.size(); i++) {
                if (!isSameExpression(left.args.get(i), right.args.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean isConsumedExpression(Expression useExpression, Expression consumedExpression, String assignedName) {
        if (isSameExpression(useExpression, consumedExpression)) {
            return true;
        }
        if (useExpression instanceof BinaryOp && consumedExpression instanceof BinaryOp) {
            BinaryOp use = (BinaryOp) useExpression;
            BinaryOp consumed = (BinaryOp) consumedExpression;
            return use.op.equals(consumed.op) && isSameExpression(use.left, consumed.left)
                    && isAssignedName(use.right, assignedName);
        }
        if (useExpression instanceof UnaryOp && consumedExpression instanceof UnaryOp) {
            UnaryOp use = (UnaryOp) useExpression;
            UnaryOp consumed = (UnaryOp) consumedExpression;
            return use.op.equals(consumed.op) && isAssignedName(use.expr, assignedName);
        }
        if (useExpression instanceof FunctionCall && consumedExpression instanceof FunctionCall) {
            FunctionCall use = (FunctionCall) useExpression;
            FunctionCall consumed = (FunctionCall) consumedExpression;
            if (use.isMethodCall != consumed.isMethodCall || !isSameExpression(use.callee, consumed.callee)
                    || use.args.size() != consumed.args.size()) {
                return false;
            }
            boolean hasAssignedArgument = false;
            for (int i = 0; i < use.args.size(); i++) {
                Expression useArg = use.args.get(i);
                Expression consumedArg = consumed.args.get(i);
                if (isAssignedName(useArg, assignedName)) {
                    hasAssignedArgument = true;
                } else if (!isSameExpression(useArg, consumedArg)) {
                    return false;
                }
            }
            return hasAssignedArgument;
        }
        return false;
    }

    private boolean isAssignedName(Expression expression, String assignedName) {
        return expression instanceof Name && assignedName.equals(((Name) expression).name);
    }

    private Set<SsaValue> computeLivePhis(SsaFunction function) {
        Set<SsaValue> livePhis = new HashSet<>();
        List<SsaValue> worklist = new ArrayList<>();
        
        for (SsaBlock block : function.getBlocks()) {
            for (SsaPhi phi : block.getPhis()) {
                SsaValue target = phi.getTarget();
                if (target != null) {
                    if (!function.getInstructionUses(target).isEmpty()) {
                        livePhis.add(target);
                        worklist.add(target);
                    }
                }
            }
        }
        
        int head = 0;
        while (head < worklist.size()) {
            SsaValue liveValue = worklist.get(head++);
            SsaPhi definingPhi = function.getDefiningPhi(liveValue);
            if (definingPhi != null) {
                for (SsaValue incoming : definingPhi.getIncoming().values()) {
                    if (incoming != null && function.getDefiningPhi(incoming) != null) {
                        if (livePhis.add(incoming)) {
                            worklist.add(incoming);
                        }
                    }
                }
            }
        }
        return livePhis;
    }

    private static final class AstTemporaryAssignment {
        final String name;
        final Expression right;

        AstTemporaryAssignment(String name, Expression right) {
            this.name = name;
            this.right = right;
        }
    }

    private static final class SsaTemporaryAssignment {
        final String name;
        final Expression right;
        final SsaValue definition;
        final SsaExpressionAnalysis analysis;

        SsaTemporaryAssignment(String name, Expression right, SsaValue definition, SsaExpressionAnalysis analysis) {
            this.name = name;
            this.right = right;
            this.definition = definition;
            this.analysis = analysis;
        }
    }
}
