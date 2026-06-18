package com.github.relua.decompiler.ssa;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.relua.decompiler.BasicBlock;

public final class SsaPhi {
    private final BasicBlock block;
    private final int register;
    private SsaValue target;
    private final Map<BasicBlock, SsaValue> incoming = new LinkedHashMap<>();

    public SsaPhi(BasicBlock block, int register) {
        this.block = block;
        this.register = register;
    }

    public BasicBlock getBlock() {
        return block;
    }

    public int getRegister() {
        return register;
    }

    public SsaValue getTarget() {
        return target;
    }

    public void setTarget(SsaValue target) {
        this.target = target;
    }

    public void addIncoming(BasicBlock predecessor, SsaValue value) {
        incoming.put(predecessor, value);
    }

    public Map<BasicBlock, SsaValue> getIncoming() {
        return Collections.unmodifiableMap(incoming);
    }

    @Override
    public String toString() {
        return target + " = phi" + incoming.values();
    }
}
