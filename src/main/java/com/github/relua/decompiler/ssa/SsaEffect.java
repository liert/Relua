package com.github.relua.decompiler.ssa;

public enum SsaEffect {
    NONE,
    READ_GLOBAL,
    READ_UPVALUE,
    READ_TABLE,
    WRITE_GLOBAL,
    WRITE_UPVALUE,
    WRITE_TABLE,
    CALL,
    RETURN,
    CONTROL_FLOW,
    CLOSE_UPVALUES
}
