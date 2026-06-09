package com.github.relua.opcode;

import static com.github.relua.opcode.OpcodeFormat.iABC;
import static com.github.relua.opcode.OpcodeFormat.iABx;
import static com.github.relua.opcode.OpcodeFormat.iAsBx;
import static com.github.relua.opcode.OperandType.A;
import static com.github.relua.opcode.OperandType.B;
import static com.github.relua.opcode.OperandType.Bx;
import static com.github.relua.opcode.OperandType.C;
import static com.github.relua.opcode.OperandType.None;
import static com.github.relua.opcode.OperandType.SBx;

public class Lua51OpcodeTable implements OpcodeTable {
    private static final OpcodeProps[] TABLE = new OpcodeProps[] {
            op("MOVE", iABC, A, B, None, true, false, false, false, false, false),
            op("LOADK", iABx, A, Bx, None, true, false, false, false, true, false),
            op("LOADBOOL", iABC, A, B, C, true, false, false, false, false, false),
            op("LOADNIL", iABC, A, B, None, true, false, false, false, false, false),
            op("GETUPVAL", iABC, A, B, None, true, false, false, false, false, true),
            op("GETGLOBAL", iABx, A, Bx, None, true, false, false, false, true, false),
            op("GETTABLE", iABC, A, B, C, true, false, false, false, false, false),
            op("SETGLOBAL", iABx, A, Bx, None, false, false, false, false, true, false),
            op("SETUPVAL", iABC, A, B, None, false, false, false, false, false, true),
            op("SETTABLE", iABC, A, B, C, false, false, false, false, false, false),
            op("NEWTABLE", iABC, A, B, C, true, false, false, false, false, false),
            op("SELF", iABC, A, B, C, true, false, false, false, false, false),
            op("ADD", iABC, A, B, C, true, false, false, false, false, false),
            op("SUB", iABC, A, B, C, true, false, false, false, false, false),
            op("MUL", iABC, A, B, C, true, false, false, false, false, false),
            op("DIV", iABC, A, B, C, true, false, false, false, false, false),
            op("MOD", iABC, A, B, C, true, false, false, false, false, false),
            op("POW", iABC, A, B, C, true, false, false, false, false, false),
            op("UNM", iABC, A, B, None, true, false, false, false, false, false),
            op("NOT", iABC, A, B, None, true, false, false, false, false, false),
            op("LEN", iABC, A, B, None, true, false, false, false, false, false),
            op("CONCAT", iABC, A, B, C, true, false, false, false, false, false),
            jmp("JMP", iAsBx, None, SBx, None),
            jmp("EQ", iABC, A, B, C),
            jmp("LT", iABC, A, B, C),
            jmp("LE", iABC, A, B, C),
            jmp("TEST", iABC, A, None, C),
            jmp("TESTSET", iABC, A, B, C),
            op("CALL", iABC, A, B, C, true, false, true, false, false, false),
            op("TAILCALL", iABC, A, B, C, false, false, true, true, false, false),
            op("RETURN", iABC, A, B, None, false, false, false, true, false, false),
            jmp("FORLOOP", iAsBx, A, SBx, None),
            jmp("FORPREP", iAsBx, A, SBx, None),
            jmp("TFORLOOP", iABC, A, None, C),
            op("SETLIST", iABC, A, B, C, false, false, false, false, false, false),
            op("CLOSE", iABC, A, None, None, false, false, false, false, false, false),
            op("CLOSURE", iABx, A, Bx, None, true, false, false, false, false, false),
            op("VARARG", iABC, A, B, None, true, false, false, false, false, false)
    };

    private static OpcodeProps op(String name, OpcodeFormat format, OperandType opA, OperandType opB, OperandType opC,
            boolean assign, boolean isJump, boolean isCall, boolean isReturn,
            boolean readsConstants, boolean readsUpvalue) {
        return new OpcodeProps(name, format, opA, opB, opC, assign, isJump, isCall, isReturn,
                readsConstants, readsUpvalue, null);
    }

    private static OpcodeProps jmp(String name, OpcodeFormat format, OperandType opA, OperandType opB, OperandType opC) {
        return new OpcodeProps(name, format, opA, opB, opC, false, true, false, false,
                false, false, (ins, proto) -> ins.getSBx());
    }

    @Override
    public OpcodeProps get(int opcodeId) {
        if (opcodeId < 0 || opcodeId >= TABLE.length) {
            return new OpcodeProps("UNKNOWN_" + opcodeId, iABC, A, B, C, false, false, false, false,
                    false, false, null);
        }
        return TABLE[opcodeId];
    }

    @Override
    public int opcodeCount() {
        return TABLE.length;
    }
}
