package com.github.relua.decompiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.relua.ast.BinaryOp;
import com.github.relua.ast.Block;
import com.github.relua.ast.Expression;
import com.github.relua.ast.ExpressionStatement;
import com.github.relua.ast.FunctionCall;
import com.github.relua.ast.IfStatement;
import com.github.relua.ast.IndexExpr;
import com.github.relua.ast.Name;
import com.github.relua.ast.SourcePos;
import com.github.relua.ast.ReturnStatement;
import com.github.relua.ast.Statement;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;
import com.github.relua.model.Register;

public class StructuredPatternEmitter {
    private final DecompilerPipeline pipeline;

    public StructuredPatternEmitter(DecompilerPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public Block emitIfStructured(Chunk chunk) {
        List<Instruction> instructions = chunk.getInstructions();
        Block block = new Block(new SourcePos(0, -1));
        InstructionToASTConverter converter = new InstructionToASTConverter(chunk, pipeline);
        boolean matched = false;

        for (int pc = 0; pc < instructions.size(); pc++) {
            ShortCircuitAndIfPattern pattern = matchShortCircuitAndIf(chunk, pc);
            if (pattern != null) {
                matched = true;
                block.statements.add(pattern.toIfStatement(chunk, converter));
                pc = pattern.bodyEnd;
                continue;
            }

            Object node = converter.convertInstructionToAST(instructions.get(pc), pc);
            if (node instanceof ClosureSkipResult) {
                ClosureSkipResult csr = (ClosureSkipResult) node;
                appendConverted(block, csr.astNode);
                pc += csr.skipCount;
                continue;
            }
            if (node instanceof ReturnStatement) {
                if (converter.tryOptimizeAssignReturn(block, (ReturnStatement) node, pc)) {
                    // Optimized
                } else {
                    block.statements.add((ReturnStatement) node);
                }
            } else {
                appendConverted(block, node);
            }
        }

        return matched ? block : null;
    }

    private ShortCircuitAndIfPattern matchShortCircuitAndIf(Chunk chunk, int pc) {
        List<Instruction> ins = chunk.getInstructions();
        if (pc + 6 >= ins.size()) {
            return null;
        }

        Instruction firstGet = ins.get(pc);
        Instruction test = ins.get(pc + 1);
        Instruction firstJmp = ins.get(pc + 2);
        Instruction secondGetBase = ins.get(pc + 3);
        Instruction secondGetField = ins.get(pc + 4);
        Instruction compare = ins.get(pc + 5);
        Instruction secondJmp = ins.get(pc + 6);

        if (firstGet.getOpcode() != Opcode.GETTABLE
                || test.getOpcode() != Opcode.TEST
                || firstJmp.getOpcode() != Opcode.JMP
                || secondGetBase.getOpcode() != Opcode.GETTABLE
                || secondGetField.getOpcode() != Opcode.GETTABLE
                || compare.getOpcode() != Opcode.EQ
                || secondJmp.getOpcode() != Opcode.JMP) {
            return null;
        }

        if (firstGet.getA() != test.getA() || test.getC() != 0) {
            return null;
        }
        if (sameGetTable(firstGet, secondGetBase) == false) {
            return null;
        }
        if (secondGetBase.getA() != secondGetField.getB()
                || secondGetField.getA() != compare.getB()
                || compare.getA() != 0) {
            return null;
        }

        int firstTarget = jumpTarget(pc + 2, firstJmp);
        int secondTarget = jumpTarget(pc + 6, secondJmp);
        if (firstTarget != secondTarget || secondTarget <= pc + 7 || secondTarget > ins.size()) {
            return null;
        }

        return new ShortCircuitAndIfPattern(pc, pc + 7, secondTarget - 1,
                firstGet, secondGetField, compare);
    }

    private boolean sameGetTable(Instruction left, Instruction right) {
        return left.getA() == right.getA()
                && left.getB() == right.getB()
                && left.getC() == right.getC();
    }

    private int jumpTarget(int pc, Instruction jump) {
        return pc + 1 + jump.getSBx();
    }

    private void appendConverted(Block block, Object node) {
        if (node instanceof Statement) {
            block.statements.add((Statement) node);
        } else if (node instanceof Expression) {
            Expression expression = (Expression) node;
            block.statements.add(new ExpressionStatement(expression, expression.pos));
        }
    }

    private Expression getTableExpression(InstructionToASTConverter converter, Instruction instruction, int pc) {
        SourcePos pos = new SourcePos(pc, -1);
        Register register = pipeline.getRegisterByInstructionIndex(pc);
        Expression table = converter.resolveRegisterExpression(instruction.getB(), pc, register);
        Expression index = converter.resolveRkExpression(register, instruction.getC(), pos);
        return new IndexExpr(table, index, pos);
    }

    private class ShortCircuitAndIfPattern {
        private final int startPc;
        private final int bodyStart;
        private final int bodyEnd;
        private final Instruction firstGet;
        private final Instruction secondGetField;
        private final Instruction compare;

        private ShortCircuitAndIfPattern(int startPc, int bodyStart, int bodyEnd,
                Instruction firstGet, Instruction secondGetField, Instruction compare) {
            this.startPc = startPc;
            this.bodyStart = bodyStart;
            this.bodyEnd = bodyEnd;
            this.firstGet = firstGet;
            this.secondGetField = secondGetField;
            this.compare = compare;
        }

        private IfStatement toIfStatement(Chunk chunk, InstructionToASTConverter converter) {
            SourcePos pos = new SourcePos(startPc, -1);
            Register compareRegister = pipeline.getRegisterByInstructionIndex(startPc + 5);

            Expression left = getTableExpression(converter, firstGet, startPc);
            Expression compareLeft = nestedCompareLeft(converter);
            Expression compareRight = converter.resolveRkExpression(compareRegister, compare.getC(), pos);
            Expression right = new BinaryOp("==", compareLeft, compareRight, pos);
            Expression condition = new BinaryOp("and", left, right, pos);

            Block body = new Block(new SourcePos(bodyStart, -1));
            for (int pc = bodyStart; pc <= bodyEnd; pc++) {
                Statement callStatement = tryEmitTableCall(chunk, pc);
                if (callStatement != null) {
                    body.statements.add(callStatement);
                    pc++;
                    continue;
                }
                Object node = converter.convertInstructionToAST(chunk.getInstructions().get(pc), pc);
                if (node instanceof ClosureSkipResult) {
                    ClosureSkipResult csr = (ClosureSkipResult) node;
                    appendConverted(body, csr.astNode);
                    pc += csr.skipCount;
                    continue;
                }
                if (node instanceof ReturnStatement) {
                    if (converter.tryOptimizeAssignReturn(body, (ReturnStatement) node, pc)) {
                        // Optimized
                    } else {
                        body.statements.add((ReturnStatement) node);
                    }
                } else {
                    appendConverted(body, node);
                }
            }

            return new IfStatement(condition, body, null, pos);
        }

        private Expression nestedCompareLeft(InstructionToASTConverter converter) {
            SourcePos pos = new SourcePos(startPc + 4, -1);
            Register register = pipeline.getRegisterByInstructionIndex(startPc + 4);
            return new IndexExpr(
                    getTableExpression(converter, firstGet, startPc),
                    converter.resolveRkExpression(register, secondGetField.getC(), pos),
                    pos);
        }
    }

    private Statement tryEmitTableCall(Chunk chunk, int pc) {
        List<Instruction> ins = chunk.getInstructions();
        if (pc + 1 >= ins.size()) {
            return null;
        }

        Instruction get = ins.get(pc);
        Instruction call = ins.get(pc + 1);
        if (get.getOpcode() != Opcode.GETTABLE
                || call.getOpcode() != Opcode.CALL
                || get.getA() != call.getA()
                || call.getB() != 1
                || call.getC() != 1) {
            return null;
        }

        SourcePos pos = new SourcePos(pc, -1);
        InstructionToASTConverter converter = new InstructionToASTConverter(chunk, pipeline);
        FunctionCall functionCall = new FunctionCall(
                getTableExpression(converter, get, pc),
                Collections.emptyList(),
                false,
                pos);
        return new ExpressionStatement(functionCall, pos);
    }
}
