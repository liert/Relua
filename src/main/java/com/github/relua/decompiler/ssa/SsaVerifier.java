package com.github.relua.decompiler.ssa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.relua.decompiler.BasicBlock;

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

    private static String blockName(BasicBlock block) {
        if (block == null) {
            return "<null>";
        }
        return block.getStartIndex() + ".." + block.getEndIndex();
    }
}
