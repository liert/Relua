package com.github.relua.format;

import com.github.relua.binary.LuaHeader;
import com.github.relua.io.LuaReader;
import com.github.relua.model.Instruction;
import com.github.relua.model.Proto;
import com.github.relua.opcode.OpcodeTable;

public interface LuaBytecodeFormat {
    // 解析文件头（支持标准或厂商魔改结构）
    LuaHeader parseHeader(LuaReader reader);

    // 从 header 中读取全局编译配置（Upvalue 标志位、Number格式、Endianness等）
    void applyHeaderConfig(LuaHeader header);

    // 解析 Proto，包括：
    // - code 数组（大小、类型）
    // - 常量池（常量类型编码）
    // - Upvalue 结构体格式
    // - Debug 信息（LocVar、行号等）
    Proto parseProto(LuaReader reader);

    // 解码指令格式（Lua 5.1/5.2/5.3/BX、SBX 是否变化）
    Instruction decodeInstruction(int pc, int raw, Proto proto);

    // 每个 opcode 的元信息（名称、参数类型、描述）
    OpcodeTable getOpcodeTable();

    // 提供非常见的字段，如厂商特有：Proto Flags、Hash 值、加密字段等
    boolean hasCustomFields();

    void parseCustomFields(LuaReader reader, Proto proto);
}
