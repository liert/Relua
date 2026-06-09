package com.github.relua.decode;

import com.github.relua.model.Instruction;
import com.github.relua.model.Proto;
import com.github.relua.opcode.OpcodeProps;
import com.github.relua.opcode.OpcodeTable;

public class InstructionDecoder {
    public Instruction decode(int pc, int raw, OpcodeTable table, Proto proto) {
        int opcodeId = raw & 0x3F;
        OpcodeProps props = table.get(opcodeId);
        return new Instruction(pc, raw, props, proto);
    }
}
