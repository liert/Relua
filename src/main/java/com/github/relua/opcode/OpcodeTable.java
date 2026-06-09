package com.github.relua.opcode;

public interface OpcodeTable {
    OpcodeProps get(int opcodeId);

    int opcodeCount();
}
