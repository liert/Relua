package com.github.relua.opcode;

public class OpcodeProps {
    public final String name;
    public final OpcodeFormat format;
    public final OperandType opA;
    public final OperandType opB;
    public final OperandType opC;

    public final boolean assign; // 写入寄存器
    public final boolean isJump; // 是否是跳转
    public final boolean isCall; // 是否是函数调用
    public final boolean isReturn; // RETURN
    public final boolean readsConstants; // 是否访问常量表（LOADK）
    public final boolean readsUpvalue; // GETUPVAL 等

    // 针对跳转指令的偏移提取逻辑（可扩展魔改逻辑）
    public final JumpOffsetExtractor jumpExtractor;

    public OpcodeProps(String name, OpcodeFormat format, OperandType opA, OperandType opB, OperandType opC,
            boolean assign, boolean isJump, boolean isCall, boolean isReturn,
            boolean readsConstants, boolean readsUpvalue, JumpOffsetExtractor jumpExtractor) {
        this.name = name;
        this.format = format;
        this.opA = opA;
        this.opB = opB;
        this.opC = opC;
        this.assign = assign;
        this.isJump = isJump;
        this.isCall = isCall;
        this.isReturn = isReturn;
        this.readsConstants = readsConstants;
        this.readsUpvalue = readsUpvalue;
        this.jumpExtractor = jumpExtractor;
    }

    public interface JumpOffsetExtractor {
        int extract(com.github.relua.model.Instruction instruction, com.github.relua.model.Proto proto);
    }
}
