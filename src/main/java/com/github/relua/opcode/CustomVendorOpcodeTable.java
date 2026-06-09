package com.github.relua.opcode;

public class CustomVendorOpcodeTable implements OpcodeTable {
    private final OpcodeTable fallback;

    public CustomVendorOpcodeTable() {
        this(new Lua51OpcodeTable());
    }

    public CustomVendorOpcodeTable(OpcodeTable fallback) {
        this.fallback = fallback;
    }

    @Override
    public OpcodeProps get(int opcodeId) {
        return fallback.get(opcodeId);
    }

    @Override
    public int opcodeCount() {
        return fallback.opcodeCount();
    }
}
