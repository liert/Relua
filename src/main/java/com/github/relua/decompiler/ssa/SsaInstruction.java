package com.github.relua.decompiler.ssa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.relua.model.Instruction;

public final class SsaInstruction {
    private final Instruction instruction;
    private final int pc;
    private final List<SsaValue> uses = new ArrayList<>();
    private final List<SsaValue> defs = new ArrayList<>();

    public SsaInstruction(Instruction instruction, int pc) {
        this.instruction = instruction;
        this.pc = pc;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    public int getPc() {
        return pc;
    }

    public void addUse(SsaValue value) {
        uses.add(value);
    }

    public void addDef(SsaValue value) {
        defs.add(value);
    }

    public List<SsaValue> getUses() {
        return Collections.unmodifiableList(uses);
    }

    public List<SsaValue> getDefs() {
        return Collections.unmodifiableList(defs);
    }

    @Override
    public String toString() {
        return pc + ": " + defs + " <- " + instruction.getOpcode() + " " + uses;
    }
}
