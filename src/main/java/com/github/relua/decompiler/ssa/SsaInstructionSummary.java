package com.github.relua.decompiler.ssa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SsaInstructionSummary {
    private final List<Integer> uses = new ArrayList<>();
    private final List<Integer> defs = new ArrayList<>();

    void use(int register) {
        if (register >= 0) {
            uses.add(register);
        }
    }

    void def(int register) {
        if (register >= 0) {
            defs.add(register);
        }
    }

    List<Integer> getUses() {
        return Collections.unmodifiableList(uses);
    }

    List<Integer> getDefs() {
        return Collections.unmodifiableList(defs);
    }
}
