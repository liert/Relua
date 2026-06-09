package com.github.relua.format;

import com.github.relua.binary.LuaHeader;
import com.github.relua.io.LuaReader;
import com.github.relua.model.Instruction;
import com.github.relua.model.Proto;
import com.github.relua.opcode.Lua51OpcodeTable;
import com.github.relua.opcode.OpcodeTable;

public class OpenWrtLuaFormat implements LuaBytecodeFormat {
    private final OpcodeTable opcodeTable = new Lua51OpcodeTable();

    @Override
    public LuaHeader parseHeader(LuaReader reader) {
        reader.position(12);
        return new LuaHeader();
    }

    @Override
    public void applyHeaderConfig(LuaHeader header) {
    }

    @Override
    public Proto parseProto(LuaReader reader) {
        return new Proto();
    }

    @Override
    public Instruction decodeInstruction(int pc, int raw, Proto proto) {
        return new Instruction(pc, raw);
    }

    @Override
    public OpcodeTable getOpcodeTable() {
        return opcodeTable;
    }

    @Override
    public boolean hasCustomFields() {
        return false;
    }

    @Override
    public void parseCustomFields(LuaReader reader, Proto proto) {
    }
}
