package com.github.relua.decompiler.ssa;

import com.github.relua.model.Register;
import com.github.relua.util.RegisterNamePolicy;

/**
 * Centralizes the source-name policy used by SSA-backed AST lowering. The
 * current policy keeps physical-register-shaped temporaries so existing cleanup
 * passes can reason about live ranges; phi lowering and richer local naming
 * should evolve here instead of inside opcode converters.
 */
public final class SsaAstNameResolver {
    public String nameForDefinition(SsaValue value, int physicalRegister, Register registerState, int parameterCount) {
        return sourceName(physicalRegister, registerState, parameterCount);
    }

    public String nameForUse(SsaValue value, int physicalRegister, Register registerState, int parameterCount) {
        return sourceName(physicalRegister, registerState, parameterCount);
    }

    private String sourceName(int physicalRegister, Register registerState, int parameterCount) {
        if (physicalRegister >= 0 && physicalRegister < parameterCount) {
            return RegisterNamePolicy.parameterName(physicalRegister);
        }
        if (registerState != null) {
            return registerState.getRegisterEntity(physicalRegister).getName();
        }
        return RegisterNamePolicy.physicalRegisterName(physicalRegister);
    }
}
