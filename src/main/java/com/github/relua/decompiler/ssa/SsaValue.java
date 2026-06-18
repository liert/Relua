package com.github.relua.decompiler.ssa;

import java.util.Objects;

/**
 * A single SSA definition of one physical Lua register.
 */
public final class SsaValue {
    private final int register;
    private final int version;
    private final boolean implicit;

    public SsaValue(int register, int version, boolean implicit) {
        this.register = register;
        this.version = version;
        this.implicit = implicit;
    }

    public int getRegister() {
        return register;
    }

    public int getVersion() {
        return version;
    }

    public boolean isImplicit() {
        return implicit;
    }

    public String getName() {
        return "R" + register + "_" + version;
    }

    @Override
    public String toString() {
        return getName() + (implicit ? "?" : "");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SsaValue)) {
            return false;
        }
        SsaValue other = (SsaValue) obj;
        return register == other.register && version == other.version && implicit == other.implicit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(register, version, implicit);
    }
}
