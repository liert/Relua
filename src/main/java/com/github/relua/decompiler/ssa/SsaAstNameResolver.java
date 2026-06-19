package com.github.relua.decompiler.ssa;

import java.util.ArrayList;
import java.util.List;

import com.github.relua.model.Register;
import com.github.relua.util.RegisterNamePolicy;

/**
 * Centralizes the source-name policy used by SSA-backed AST lowering. The
 * current policy keeps physical-register-shaped temporaries so existing cleanup
 * passes can reason about live ranges; phi lowering and richer local naming
 * should evolve here instead of inside opcode converters.
 */
public final class SsaAstNameResolver {
    public List<String> parameterNames(int parameterCount) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < parameterCount; i++) {
            names.add(RegisterNamePolicy.parameterName(i));
        }
        return names;
    }

    public String nameForDefinition(SsaValue value, int physicalRegister, Register registerState, int parameterCount) {
        String base = sourceName(physicalRegister, registerState, parameterCount);
        if (physicalRegister >= 0 && physicalRegister < parameterCount) {
            return base;
        }
        if (value == null || value.isImplicit()) {
            return base;
        }
        return base + "_" + value.getVersion();
    }

    public String nameForUse(SsaValue value, int physicalRegister, Register registerState, int parameterCount) {
        String base = sourceName(physicalRegister, registerState, parameterCount);
        if (physicalRegister >= 0 && physicalRegister < parameterCount) {
            return base;
        }
        if (value == null || value.isImplicit()) {
            return base;
        }
        return base + "_" + value.getVersion();
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
