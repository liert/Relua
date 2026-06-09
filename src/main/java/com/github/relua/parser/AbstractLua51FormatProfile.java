package com.github.relua.parser;

import java.io.IOException;

import com.github.relua.model.Constant;
import com.github.relua.model.Instruction;
import com.github.relua.model.LuacFile;

public abstract class AbstractLua51FormatProfile implements BytecodeFormatProfile {
    @Override
    public void parseHeader(BinaryReader reader, LuacFile luacFile, byte[] firstBytes) throws IOException {
        luacFile.setMagicNumber(firstBytes);
        luacFile.setVersion(reader.readByte());
        readCommonHeader(reader, luacFile);
    }

    protected void readCommonHeader(BinaryReader reader, LuacFile luacFile) throws IOException {
        luacFile.setFormat(reader.readByte());
        luacFile.setEndianness(reader.readByte());
        luacFile.setIntSize(reader.readByte());
        luacFile.setSizeTSize(reader.readByte());
        luacFile.setInstructionSize(reader.readByte());
        luacFile.setLuaNumberSize(reader.readByte());
        luacFile.setIntegralFlag(reader.readByte());
    }

    @Override
    public Instruction decodeInstruction(int pc, int raw) {
        return new Instruction(pc, raw);
    }

    @Override
    public Constant parseConstant(byte type, BinaryReader reader) throws IOException {
        switch (type) {
            case 0:
                return Constant.nil();
            case 1:
                return Constant.booleanConstant(reader.readByte() != 0);
            case 3:
                return Constant.number(reader.readLuaNumber());
            case 4:
                return Constant.string(reader.readLuaString());
            case 9:
                return Constant.number(reader.readInt());
            default:
                throw new IOException("Unknown constant type: " + (type & 0xFF));
        }
    }
}
