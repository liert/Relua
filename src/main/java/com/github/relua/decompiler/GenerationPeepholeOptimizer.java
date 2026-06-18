package com.github.relua.decompiler;

import com.github.relua.ast.Assign;
import com.github.relua.ast.Block;
import com.github.relua.ast.BooleanConst;
import com.github.relua.ast.Expression;
import com.github.relua.ast.Name;
import com.github.relua.ast.NilConst;
import com.github.relua.ast.NumberConst;
import com.github.relua.ast.ReturnStatement;
import com.github.relua.ast.Statement;
import com.github.relua.ast.StringConst;
import com.github.relua.model.Chunk;
import com.github.relua.model.Constant;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;
import com.github.relua.model.UpValue;
import com.github.relua.util.RegisterNamePolicy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 生成阶段的局部 peephole 优化。
 */
final class GenerationPeepholeOptimizer {
    private GenerationPeepholeOptimizer() {
    }

    /**
     * 尝试在生成 ReturnStatement 时折叠前置寄存器赋值与常量返回。
     * 例如将:
     *   R4 = nil
     *   return nil
     * 折叠为:
     *   return nil
     */
    static boolean tryOptimizeAssignReturn(Block block, ReturnStatement ret, int returnPC, Chunk chunk,
            CodeGeneratorContext context) {
        if (block == null || block.statements == null || block.statements.isEmpty()) {
            return false;
        }

        Statement lastStmt = block.statements.get(block.statements.size() - 1);
        if (!(lastStmt instanceof Assign)) {
            return false;
        }

        Assign assign = (Assign) lastStmt;
        if (assign.left.size() != 1 || assign.right.size() != 1 || ret.values.size() != 1) {
            return false;
        }

        Expression leftExpr = assign.left.get(0);
        Expression rightExpr = assign.right.get(0);
        Expression retExpr = ret.values.get(0);

        if (!(leftExpr instanceof Name)) {
            return false;
        }
        Name leftName = (Name) leftExpr;
        int reg = RegisterNamePolicy.temporaryRegisterIndex(leftName.name);
        if (reg == -1 || !RegisterNamePolicy.isPhysicalRegisterName(leftName.name)) {
            return false;
        }

        // 基础安全校验：PC 有效性、LabelPC 校验、Upvalue 校验
        if (assign.pos == null || ret.pos == null ||
            assign.pos.pc < 0 || returnPC < 0 ||
            context.isLabelPC(assign.pos.pc) ||
            context.isLabelPC(returnPC)) {
            return false;
        }

        if (isUpvalueName(leftName.name, context)) {
            return false;
        }

        List<Instruction> instructions = chunk.getInstructions();
        if (assign.pos.pc >= instructions.size() || returnPC >= instructions.size()) {
            return false;
        }
        Instruction assignInst = instructions.get(assign.pos.pc);
        Instruction returnInst = instructions.get(returnPC);

        if (returnInst.getOpcode() != Opcode.RETURN ||
            returnInst.getB() != 2 ||
            returnInst.getA() != reg ||
            assignInst.getA() != reg) {
            return false;
        }

        if (!isVerifiedConstantAssign(assignInst, rightExpr, chunk)) {
            return false;
        }

        if (!isConstantExpression(retExpr) || !constantExpressionsEqual(rightExpr, retExpr)) {
            return false;
        }

        block.statements.remove(block.statements.size() - 1);
        block.statements.add(ret);
        return true;
    }

    private static boolean isUpvalueName(String name, CodeGeneratorContext context) {
        Set<String> upvalueNames = new HashSet<>();
        for (UpValue uv : context.getUpvalues()) {
            if (uv != null && uv.getName() != null) {
                upvalueNames.add(uv.getName());
            }
        }
        return upvalueNames.contains(name);
    }

    private static boolean isVerifiedConstantAssign(Instruction assignInst, Expression rightExpr, Chunk chunk) {
        Opcode assignOp = assignInst.getOpcode();

        if (assignOp == Opcode.LOADNIL) {
            return assignInst.getA() == assignInst.getB() && rightExpr instanceof NilConst;
        }

        if (assignOp == Opcode.LOADBOOL) {
            if (!(rightExpr instanceof BooleanConst)) {
                return false;
            }
            boolean astValue = ((BooleanConst) rightExpr).value;
            boolean instValue = assignInst.getB() != 0;
            return astValue == instValue;
        }

        if (assignOp == Opcode.LOADK) {
            Constant k = chunk.getConstant(assignInst.getBx());
            return k != null && constantMatchesAst(k.getValue(), rightExpr);
        }

        return false;
    }

    private static boolean constantMatchesAst(Object constantValue, Expression expression) {
        if (expression instanceof StringConst) {
            return ((StringConst) expression).value.equals(constantValue);
        }

        if (expression instanceof NumberConst && constantValue instanceof Number) {
            double astNum = ((NumberConst) expression).value;
            double kNum = ((Number) constantValue).doubleValue();
            return Double.compare(astNum, kNum) == 0;
        }

        return false;
    }

    private static boolean isConstantExpression(Expression expression) {
        return expression instanceof NilConst ||
                expression instanceof BooleanConst ||
                expression instanceof NumberConst ||
                expression instanceof StringConst;
    }

    private static boolean constantExpressionsEqual(Expression left, Expression right) {
        if (left instanceof NilConst && right instanceof NilConst) {
            return true;
        }
        if (left instanceof BooleanConst && right instanceof BooleanConst) {
            return ((BooleanConst) left).value == ((BooleanConst) right).value;
        }
        if (left instanceof StringConst && right instanceof StringConst) {
            return ((StringConst) left).value.equals(((StringConst) right).value);
        }
        if (left instanceof NumberConst && right instanceof NumberConst) {
            return Double.compare(((NumberConst) left).value, ((NumberConst) right).value) == 0;
        }
        return false;
    }
}
