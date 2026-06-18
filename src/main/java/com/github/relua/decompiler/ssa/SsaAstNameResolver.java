package com.github.relua.decompiler.ssa;

import com.github.relua.model.Register;

/**
 * Centralizes the temporary source-name policy used while AST lowering is being
 * migrated to SSA. The current policy preserves legacy register-looking output
 * so existing cleanup passes keep working; later phi lowering and live-range
 * naming should evolve here instead of inside opcode converters.
 */
public final class SsaAstNameResolver {
    public String nameForDefinition(SsaValue value, int physicalRegister, Register registerState, int parameterCount) {
        return legacyCompatibleName(physicalRegister, registerState, parameterCount);
    }

    public String nameForUse(SsaValue value, int physicalRegister, Register registerState, int parameterCount) {
        return legacyCompatibleName(physicalRegister, registerState, parameterCount);
    }

    private String legacyCompatibleName(int physicalRegister, Register registerState, int parameterCount) {
        if (physicalRegister >= 0 && physicalRegister < parameterCount) {
            return "a" + physicalRegister;
        }
        if (registerState != null) {
            return registerState.getRegisterEntity(physicalRegister).getName();
        }
        return "R" + physicalRegister;
    }
}
