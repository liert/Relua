package com.github.relua.decompiler.ssa;

public enum SsaValueKind {
    UNKNOWN,
    PARAMETER,
    CONSTANT,
    COPY,
    GLOBAL,
    UPVALUE,
    TABLE_READ,
    TABLE_NEW,
    ARITHMETIC,
    UNARY,
    CONCAT,
    CALL_RESULT,
    VARARG,
    CLOSURE,
    PHI
}
