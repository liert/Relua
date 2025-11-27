package com.github.relua.decompiler;

import com.github.relua.model.LuacFile;
import com.github.relua.parser.LuacParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Decompiler测试用例
 */
class DecompilerTest {

    @Test
    void testDecompileVersionLua() throws IOException {
        // 直接使用文件路径
        String filePath = "src/test/resources/version.lua";
        File file = new File(filePath);
        assertNotNull(file, "version.lua file not found");
        assertTrue(file.exists(), "version.lua file does not exist");
        
        // 创建解析器和反编译器
        LuacParser parser = new LuacParser();
        Decompiler decompiler = new Decompiler();
        
        // 解析Luac文件
        LuacFile luacFile = parser.parse(filePath);
        assertNotNull(luacFile, "Failed to parse version.lua");
        
        // 反编译
        String luaCode = decompiler.decompile(luacFile);
        assertNotNull(luaCode, "Failed to decompile version.lua");
        assertFalse(luaCode.isEmpty(), "Decompiled code is empty");
        
        // 打印反编译结果
        System.out.println("Decompiled version.lua:");
        System.out.println(luaCode);
    }
}