package com.github.relua.decompiler.ssa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.relua.model.Instruction;

public final class SsaInstruction {
    private final Instruction instruction;
    private final int pc;
    private final List<SsaValue> uses = new ArrayList<>();
    private final List<SsaValue> defs = new ArrayList<>();
    private final Map<Integer, List<SsaValue>> usesByRegister = new LinkedHashMap<>();
    private final Map<Integer, List<SsaValue>> defsByRegister = new LinkedHashMap<>();

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
        addByRegister(usesByRegister, value);
    }

    public void addDef(SsaValue value) {
        defs.add(value);
        addByRegister(defsByRegister, value);
    }

    public List<SsaValue> getUses() {
        return Collections.unmodifiableList(uses);
    }

    public List<SsaValue> getDefs() {
        return Collections.unmodifiableList(defs);
    }

    public List<SsaValue> getUsesForRegister(int register) {
        List<SsaValue> values = usesByRegister.get(register);
        return values != null ? Collections.unmodifiableList(values) : Collections.<SsaValue>emptyList();
    }

    public List<SsaValue> getDefsForRegister(int register) {
        List<SsaValue> values = defsByRegister.get(register);
        return values != null ? Collections.unmodifiableList(values) : Collections.<SsaValue>emptyList();
    }

    public SsaValue getFirstUseForRegister(int register) {
        List<SsaValue> values = usesByRegister.get(register);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    public SsaValue getFirstDefForRegister(int register) {
        List<SsaValue> values = defsByRegister.get(register);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    private void addByRegister(Map<Integer, List<SsaValue>> valuesByRegister, SsaValue value) {
        if (value == null) {
            return;
        }
        List<SsaValue> values = valuesByRegister.get(value.getRegister());
        if (values == null) {
            values = new ArrayList<>();
            valuesByRegister.put(value.getRegister(), values);
        }
        values.add(value);
    }

    @Override
    public String toString() {
        return pc + ": " + defs + " <- " + instruction.getOpcode() + " " + uses;
    }
}
