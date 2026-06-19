package com.github.relua.decompiler.ssa;

import com.github.relua.decompiler.VariableArityResolver;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;

final class SsaInstructionSummarizer {
    private SsaInstructionSummarizer() {
    }

    static SsaInstructionSummary summarize(Chunk chunk, int pc) {
        return summarize(chunk.getInstruction(pc), chunk, pc);
    }

    static SsaInstructionSummary summarize(Instruction instruction) {
        return summarize(instruction, null, -1);
    }

    private static SsaInstructionSummary summarize(Instruction instruction, Chunk chunk, int pc) {
        SsaInstructionSummary summary = new SsaInstructionSummary();
        Opcode opcode = instruction.getOpcode();
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();

        switch (opcode) {
            case MOVE:
                summary.use(b);
                summary.def(a);
                break;
            case LOADK:
            case GETGLOBAL:
            case NEWTABLE:
            case CLOSURE:
                summary.def(a);
                break;
            case LOADBOOL:
                summary.def(a);
                break;
            case LOADNIL:
                for (int reg = a; reg <= b; reg++) {
                    summary.def(reg);
                }
                break;
            case GETUPVAL:
                summary.def(a);
                break;
            case VARARG:
                {
                    int varargB = instruction.getB();
                    if (varargB > 1) {
                        for (int reg = a; reg <= a + varargB - 2; reg++) {
                            summary.def(reg);
                        }
                    } else {
                        summary.def(a);
                    }
                }
                break;
            case SETGLOBAL:
                summary.use(a);
                break;
            case SETUPVAL:
                summary.use(a);
                break;
            case GETTABLE:
                summary.use(b);
                useRk(summary, c);
                summary.def(a);
                break;
            case SETTABLE:
                summary.use(a);
                useRk(summary, b);
                useRk(summary, c);
                break;
            case SELF:
                summary.use(b);
                useRk(summary, c);
                summary.def(a);
                summary.def(a + 1);
                break;
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
            case POW:
                useRk(summary, b);
                useRk(summary, c);
                summary.def(a);
                break;
            case UNM:
            case NOT:
            case LEN:
                summary.use(b);
                summary.def(a);
                break;
            case CONCAT:
                for (int reg = b; reg <= c; reg++) {
                    summary.use(reg);
                }
                summary.def(a);
                break;
            case EQ:
            case LT:
            case LE:
                useRk(summary, b);
                useRk(summary, c);
                break;
            case TEST:
                summary.use(a);
                break;
            case TESTSET:
                summary.use(b);
                summary.def(a);
                break;
            case CALL:
            case TAILCALL:
                summarizeCall(summary, instruction, chunk, pc);
                break;
            case RETURN:
                summarizeReturn(summary, instruction, chunk, pc);
                break;
            case FORPREP:
                summary.use(a);
                summary.use(a + 1);
                summary.use(a + 2);
                summary.def(a);
                break;
            case FORLOOP:
                summary.use(a);
                summary.use(a + 1);
                summary.use(a + 2);
                summary.def(a);
                summary.def(a + 3);
                break;
            case TFORLOOP:
                summary.use(a);
                summary.use(a + 1);
                summary.use(a + 2);
                for (int reg = 1; reg <= c; reg++) {
                    summary.def(a + 2 + reg);
                }
                break;
            case SETLIST:
                summary.use(a);
                for (int reg = 1; reg <= b; reg++) {
                    summary.use(a + reg);
                }
                break;
            case CLOSE:
            case JMP:
                break;
            case UNKNOWN:
            default:
                throw new IllegalArgumentException("Unhandled opcode in SSA summarizer: " + opcode);
        }

        return summary;
    }

    private static void useRk(SsaInstructionSummary summary, int rk) {
        if (rk >= 0 && rk < 256) {
            summary.use(rk);
        }
    }

    private static void summarizeCall(SsaInstructionSummary summary, Instruction instruction, Chunk chunk, int pc) {
        int a = instruction.getA();
        int c = instruction.getC();

        summary.use(a);
        if (chunk != null && pc >= 0) {
            for (Integer reg : VariableArityResolver.callArgumentRegisters(chunk, pc)) {
                summary.use(reg);
            }
        } else {
            int b = instruction.getB();
            if (b > 0) {
                for (int reg = a + 1; reg <= a + b - 1; reg++) {
                    summary.use(reg);
                }
            }
        }

        if (instruction.getOpcode() == Opcode.TAILCALL) {
            return;
        }
        if (c == 0) {
            summary.def(a);
        } else {
            for (int reg = a; reg <= a + c - 2; reg++) {
                summary.def(reg);
            }
        }
    }

    private static void summarizeReturn(SsaInstructionSummary summary, Instruction instruction, Chunk chunk, int pc) {
        int a = instruction.getA();
        int b = instruction.getB();
        if (chunk != null && pc >= 0) {
            for (Integer reg : VariableArityResolver.returnRegisters(chunk, pc)) {
                summary.use(reg);
            }
        } else {
            if (b > 0) {
                for (int reg = a; reg <= a + b - 2; reg++) {
                    summary.use(reg);
                }
            } else {
                summary.use(a);
            }
        }
    }
}
