package com.github.relua.parser;

public class StandardLua51FormatProfile extends AbstractLua51FormatProfile {
    private static final byte[] LUA_MAGIC = { 0x1B, 'L', 'u', 'a' };

    @Override
    public String getName() {
        return "Lua 5.1";
    }

    @Override
    public boolean matches(byte[] firstBytes) {
        if (firstBytes == null || firstBytes.length < LUA_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < LUA_MAGIC.length; i++) {
            if (firstBytes[i] != LUA_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }
}
