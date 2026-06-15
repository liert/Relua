package com.github.relua.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.github.relua.model.Constant;

class LuaStringEscapeUtilsTest {

    @Test
    void testEscapeNull() {
        assertEquals("", LuaStringEscapeUtils.escape(null));
    }

    @Test
    void testEscapeEmpty() {
        assertEquals("", LuaStringEscapeUtils.escape(""));
    }

    @Test
    void testEscapeNormal() {
        assertEquals("hello world", LuaStringEscapeUtils.escape("hello world"));
    }

    @Test
    void testEscapeSpecialCharacters() {
        assertEquals("hello\\\\world", LuaStringEscapeUtils.escape("hello\\world"));
        assertEquals("hello\\\"world", LuaStringEscapeUtils.escape("hello\"world"));
        assertEquals("hello\\nworld", LuaStringEscapeUtils.escape("hello\nworld"));
        assertEquals("hello\\rworld", LuaStringEscapeUtils.escape("hello\rworld"));
        assertEquals("hello\\tworld", LuaStringEscapeUtils.escape("hello\tworld"));
    }

    @Test
    void testEscapeControlCharacters() {
        // Character < 32 should be escaped as \ddd
        assertEquals("hello\\001world", LuaStringEscapeUtils.escape("hello\001world"));
        assertEquals("hello\\031world", LuaStringEscapeUtils.escape("hello\u001fworld"));
    }

    @Test
    void testBackgroundIssueExample() {
        // Background issue: "[`;|$&\n]" should be escaped to "[`;|$&\\n]"
        assertEquals("[`;|$&\\n]", LuaStringEscapeUtils.escape("[`;|$&\n]"));
    }

    @Test
    void testConstantStringToString() {
        Constant c = Constant.string("[`;|$&\n]");
        assertEquals("\"[`;|$&\\n]\"", c.toString());
    }
}
