package com.github.relua.decode;

import java.util.ArrayList;
import java.util.List;

import com.github.relua.binary.LuaHeader;
import com.github.relua.format.LuaBytecodeFormat;
import com.github.relua.format.OpenWrtLuaFormat;
import com.github.relua.io.LuaReader;
import com.github.relua.model.Proto;

public class BytecodeLoader {
    private final List<LuaBytecodeFormat> formats;

    public BytecodeLoader() {
        formats = new ArrayList<>();
        formats.add(new OpenWrtLuaFormat());
    }

    public Proto load(byte[] data) {
        LuaReader r = new LuaReader(data);

        // 探测格式
        LuaBytecodeFormat format = VendorFormatDetector.detect(r, formats);
        if (format == null) {
            throw new IllegalArgumentException("Unknown Lua bytecode format");
        }

        // 按格式解析
        LuaHeader header = format.parseHeader(r);
        format.applyHeaderConfig(header);

        return format.parseProto(r);
    }
}
