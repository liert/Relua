package com.github.relua.parser;

import java.io.IOException;

import com.github.relua.model.Constant;
import com.github.relua.model.Instruction;
import com.github.relua.model.LuacFile;

public interface BytecodeFormatProfile {
    String getName();

    boolean matches(byte[] firstBytes);

    void parseHeader(BinaryReader reader, LuacFile luacFile, byte[] firstBytes) throws IOException;

    Instruction decodeInstruction(int pc, int raw);

    Constant parseConstant(byte type, BinaryReader reader) throws IOException;
}
