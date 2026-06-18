package com.github.relua.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegisterNamePolicyTest {
    @Test
    void distinguishesPhysicalAndPrefixedTemporaryRegisterNames() {
        assertEquals("R7", RegisterNamePolicy.physicalRegisterName(7));
        assertEquals("chunk_R7", RegisterNamePolicy.prefixedRegisterName("chunk_", 7));

        assertTrue(RegisterNamePolicy.isTemporaryRegisterName("R7"));
        assertTrue(RegisterNamePolicy.isTemporaryRegisterName("chunk_R7"));
        assertTrue(RegisterNamePolicy.isTemporaryRegisterName("module_R7"));

        assertTrue(RegisterNamePolicy.isPhysicalRegisterName("R7"));
        assertFalse(RegisterNamePolicy.isPhysicalRegisterName("chunk_R7"));
        assertFalse(RegisterNamePolicy.isPhysicalRegisterName("module_R7"));
    }
}
