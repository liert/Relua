package com.github.relua.decompiler.ssa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.relua.decompiler.BasicBlock;

public final class SsaBlock {
    private final BasicBlock basicBlock;
    private final Map<Integer, SsaPhi> phis = new LinkedHashMap<>();
    private final List<SsaInstruction> instructions = new ArrayList<>();

    public SsaBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    public SsaPhi getOrCreatePhi(int register) {
        SsaPhi phi = phis.get(register);
        if (phi == null) {
            phi = new SsaPhi(basicBlock, register);
            phis.put(register, phi);
        }
        return phi;
    }

    public SsaPhi getPhi(int register) {
        return phis.get(register);
    }

    public Collection<SsaPhi> getPhis() {
        return Collections.unmodifiableCollection(phis.values());
    }

    public void addInstruction(SsaInstruction instruction) {
        instructions.add(instruction);
    }

    public List<SsaInstruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }
}
