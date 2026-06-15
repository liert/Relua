package com.github.relua.decompiler;

import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;

import java.util.List;

/**
 * 指令序列上的轻量级局部分析。
 */
final class InstructionFlowAnalyzer {
    private InstructionFlowAnalyzer() {
    }

    static boolean isRegisterConsumedByFollowingCallArgument(List<Instruction> instructions, int registerIndex,
            int instructionIndex) {
        for (int pc = instructionIndex + 1; pc < instructions.size(); pc++) {
            Instruction next = instructions.get(pc);
            Opcode opcode = next.getOpcode();

            if (opcode == Opcode.CALL || opcode == Opcode.TAILCALL) {
                int callA = next.getA();
                int callB = next.getB();

                // B == 1 表示无参数。
                if (callB == 1) {
                    return false;
                }

                // B > 1 表示参数是 R(A+1) ... R(A+B-1)。
                if (callB > 1) {
                    return registerIndex >= callA + 1 && registerIndex <= callA + callB - 1;
                }

                // B == 0 是开放参数列表，这里保守处理。
                return registerIndex > callA;
            }

            if (writesRegister(next, registerIndex) || isForwardScanBarrier(opcode)) {
                return false;
            }
        }

        return false;
    }

    private static boolean writesRegister(Instruction instruction, int registerIndex) {
        Opcode opcode = instruction.getOpcode();
        int a = instruction.getA();

        switch (opcode) {
            case MOVE:
            case LOADK:
            case LOADBOOL:
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
            case GETUPVAL:
            case CLOSURE:
            case VARARG:
                return a == registerIndex;

            case LOADNIL:
                return registerIndex >= instruction.getA() && registerIndex <= instruction.getB();

            case SELF:
                return registerIndex == a || registerIndex == a + 1;

            case CALL:
            case TAILCALL:
                int c = instruction.getC();
                if (c == 1) {
                    return false;
                }
                if (c == 0) {
                    return registerIndex == a;
                }
                return registerIndex >= a && registerIndex <= a + c - 2;

            default:
                return false;
        }
    }

    private static boolean isForwardScanBarrier(Opcode opcode) {
        switch (opcode) {
            case JMP:
            case RETURN:
            case FORPREP:
            case FORLOOP:
            case TFORLOOP:
                return true;
            default:
                return false;
        }
    }
}
