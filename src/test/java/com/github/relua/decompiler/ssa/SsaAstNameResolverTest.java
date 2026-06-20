package com.github.relua.decompiler.ssa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class SsaAstNameResolverTest {
    @Test
    void resolvesNamesThroughCentralPolicy() {
        SsaAstNameResolver resolver = new SsaAstNameResolver();
        SsaValue value = new SsaValue(3, 1, false);

        assertEquals("a0", resolver.nameForUse(new SsaValue(0, 0, true), 0, null, 2));
        assertEquals("R3_1", resolver.nameForDefinition(value, 3, null, 0));

        assertEquals("chunk_R3_1", resolver.nameForUse(value, 3, "chunk_", 0));
    }

    @Test
    void resolvesParameterListThroughCentralPolicy() {
        SsaAstNameResolver resolver = new SsaAstNameResolver();

        assertEquals(Arrays.asList("a0", "a1", "a2"), resolver.parameterNames(3));
    }
}
