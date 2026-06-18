package com.github.relua.decompiler.ssa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.relua.decompiler.BasicBlock;
import com.github.relua.model.Chunk;

public final class SsaVerifier {
    private SsaVerifier() {
    }

    public static List<String> verify(SsaFunction function) {
        List<String> errors = new ArrayList<>();
        Set<SsaValue> definitions = new HashSet<>();

        for (SsaBlock block : function.getBlocks()) {
            for (SsaPhi phi : block.getPhis()) {
                if (phi.getTarget() == null) {
                    errors.add("phi without target in block " + blockName(block.getBasicBlock()));
                } else {
                    definitions.add(phi.getTarget());
                }

                for (BasicBlock predecessor : block.getBasicBlock().getPredecessors()) {
                    if (function.getBlock(predecessor) == null) {
                        continue;
                    }
                    if (!phi.getIncoming().containsKey(predecessor)) {
                        errors.add("phi for R" + phi.getRegister() + " in block "
                                + blockName(block.getBasicBlock()) + " missing predecessor "
                                + blockName(predecessor));
                    }
                }
            }
            for (SsaInstruction instruction : block.getInstructions()) {
                definitions.addAll(instruction.getDefs());
            }
        }

        for (SsaBlock block : function.getBlocks()) {
            for (SsaPhi phi : block.getPhis()) {
                for (SsaValue value : phi.getIncoming().values()) {
                    if (value == null) {
                        errors.add("phi for R" + phi.getRegister() + " in block "
                                + blockName(block.getBasicBlock()) + " has null incoming");
                    } else if (!value.isImplicit() && !definitions.contains(value)) {
                        errors.add("phi incoming " + value + " has no definition");
                    }
                }
            }
            for (SsaInstruction instruction : block.getInstructions()) {
                verifyInstructionSummary(function.getChunk(), instruction, errors);
                for (SsaValue value : instruction.getUses()) {
                    if (value == null) {
                        errors.add("instruction " + instruction.getPc() + " has null use");
                    } else if (!value.isImplicit() && !definitions.contains(value)) {
                        errors.add("instruction " + instruction.getPc() + " uses undefined " + value);
                    }
                }
            }
        }

        return errors;
    }

    private static void verifyInstructionSummary(Chunk chunk, SsaInstruction instruction, List<String> errors) {
        if (chunk == null) {
            return;
        }
        SsaInstructionSummary summary = SsaInstructionSummarizer.summarize(chunk, instruction.getPc());
        List<Integer> actualUses = registersOf(instruction.getUses());
        List<Integer> actualDefs = registersOf(instruction.getDefs());
        if (!summary.getUses().equals(actualUses)) {
            errors.add("instruction " + instruction.getPc() + " SSA uses " + actualUses
                    + " do not match opcode summary " + summary.getUses());
        }
        if (!summary.getDefs().equals(actualDefs)) {
            errors.add("instruction " + instruction.getPc() + " SSA defs " + actualDefs
                    + " do not match opcode summary " + summary.getDefs());
        }
    }

    private static List<Integer> registersOf(List<SsaValue> values) {
        List<Integer> registers = new ArrayList<>();
        for (SsaValue value : values) {
            registers.add(value != null ? value.getRegister() : -1);
        }
        return registers;
    }

    private static String blockName(BasicBlock block) {
        if (block == null) {
            return "<null>";
        }
        return block.getStartIndex() + ".." + block.getEndIndex();
    }
}
