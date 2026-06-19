package com.github.relua.decompiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;

public final class VariableArityResolver {
    private VariableArityResolver() {
    }

    public static List<Integer> callArgumentRegisters(Chunk chunk, int pc) {
        Instruction instruction = chunk.getInstruction(pc);
        int a = instruction.getA();
        int b = instruction.getB();
        if (b == 1) {
            return Collections.emptyList();
        }
        if (b > 1) {
            return range(a + 1, a + b - 1);
        }

        int openTop = findOpenResultStart(chunk, pc);
        if (openTop < a + 1) {
            return Collections.emptyList();
        }
        return range(a + 1, openTop);
    }

    public static List<Integer> returnRegisters(Chunk chunk, int pc) {
        Instruction instruction = chunk.getInstruction(pc);
        int a = instruction.getA();
        int b = instruction.getB();
        if (b > 0) {
            return range(a, a + b - 2);
        }

        int openTop = findOpenResultStart(chunk, pc);
        if (openTop < a) {
            return Collections.singletonList(a);
        }
        return range(a, openTop);
    }

    public static int findOpenResultStart(Chunk chunk, int pc) {
        int producerPc = findOpenResultProducerPc(chunk, pc);
        if (producerPc < 0) {
            return -1;
        }
        return chunk.getInstruction(producerPc).getA();
    }

    public static int findOpenResultProducerPc(Chunk chunk, int pc) {
        for (int i = pc - 1; i >= 0; i--) {
            Instruction previous = chunk.getInstruction(i);
            if (previous.getOpcode() == Opcode.CALL && previous.getC() == 0) {
                return i;
            }
            if (previous.getOpcode() == Opcode.VARARG && previous.getB() == 0) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isOpenResultProducer(Chunk chunk, int producerPc) {
        Instruction producer = chunk.getInstruction(producerPc);
        if (producer == null) {
            return false;
        }
        if (producer.getOpcode() != Opcode.CALL || producer.getC() != 0) {
            return false;
        }
        for (int i = producerPc + 1; i < chunk.getInstructions().size(); i++) {
            Instruction inst = chunk.getInstruction(i);
            if (inst == null) {
                break;
            }
            if (inst.getOpcode() == Opcode.CALL && inst.getB() == 0) {
                if (findOpenResultProducerPc(chunk, i) == producerPc) {
                    return true;
                }
            }
            if (inst.getOpcode() == Opcode.RETURN && inst.getB() == 0) {
                if (findOpenResultProducerPc(chunk, i) == producerPc) {
                    return true;
                }
            }
            if (inst.getOpcode() == Opcode.CALL && inst.getC() == 0) {
                break;
            }
            if (inst.getOpcode() == Opcode.VARARG && inst.getB() == 0) {
                break;
            }
        }
        return false;
    }


    private static List<Integer> range(int startInclusive, int endInclusive) {
        if (endInclusive < startInclusive) {
            return Collections.emptyList();
        }
        List<Integer> registers = new ArrayList<>();
        for (int register = startInclusive; register <= endInclusive; register++) {
            registers.add(register);
        }
        return registers;
    }
}
