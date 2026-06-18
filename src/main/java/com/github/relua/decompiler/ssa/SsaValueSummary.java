package com.github.relua.decompiler.ssa;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SsaValueSummary {
    private final SsaValue value;
    private SsaValueKind kind = SsaValueKind.UNKNOWN;
    private SsaValue copySource;
    private Object constantValue;
    private final Set<SsaEffect> effects = new LinkedHashSet<>();

    public SsaValueSummary(SsaValue value) {
        this.value = value;
    }

    public SsaValue getValue() {
        return value;
    }

    public SsaValueKind getKind() {
        return kind;
    }

    public void setKind(SsaValueKind kind) {
        this.kind = kind != null ? kind : SsaValueKind.UNKNOWN;
    }

    public SsaValue getCopySource() {
        return copySource;
    }

    public void setCopySource(SsaValue copySource) {
        this.copySource = copySource;
    }

    public Object getConstantValue() {
        return constantValue;
    }

    public void setConstantValue(Object constantValue) {
        this.constantValue = constantValue;
    }

    public void addEffect(SsaEffect effect) {
        if (effect != null && effect != SsaEffect.NONE) {
            effects.add(effect);
        }
    }

    public Set<SsaEffect> getEffects() {
        return Collections.unmodifiableSet(effects);
    }

    public boolean isPure() {
        return effects.isEmpty()
                && kind != SsaValueKind.CALL_RESULT
                && kind != SsaValueKind.VARARG
                && kind != SsaValueKind.UNKNOWN;
    }

    @Override
    public String toString() {
        return value + " " + kind + (copySource != null ? " <- " + copySource : "")
                + (constantValue != null ? " = " + constantValue : "")
                + (effects.isEmpty() ? "" : " effects=" + effects);
    }
}
