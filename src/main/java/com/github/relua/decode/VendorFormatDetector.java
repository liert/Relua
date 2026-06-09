package com.github.relua.decode;

import java.util.List;

import com.github.relua.format.LuaBytecodeFormat;
import com.github.relua.io.LuaReader;

public final class VendorFormatDetector {
    private static final byte[] LUA_MAGIC = new byte[] { 0x1b, 'L', 'u', 'a' };

    private VendorFormatDetector() {
    }

    public static LuaBytecodeFormat detect(LuaReader reader, List<LuaBytecodeFormat> formats) {
        if (!reader.startsWith(LUA_MAGIC) || formats == null || formats.isEmpty()) {
            return null;
        }
        reader.position(0);
        return formats.get(0);
    }
}
