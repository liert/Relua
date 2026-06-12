package com.github.relua.decompiler;

import com.github.relua.model.LuacFile;
import com.github.relua.parser.LuacParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Decompiler测试用例（小米 xqdatacenter.lua 反编译）
 */
class DecompilerTest {

    @Test
    void testDecompileXqdatacenter() throws IOException {
        // 小米固件 Fate Lua 字节码
        String filePath = "src/test/resources/xiaomi/xqdatacenter.lua";
        File file = new File(filePath);
        assertTrue(file.exists(), "xqdatacenter.lua file does not exist: " + file.getAbsolutePath());

        // 创建解析器和反编译器
        LuacParser parser = new LuacParser();
        Decompiler decompiler = new Decompiler();

        // 解析
        LuacFile luacFile = parser.parse(filePath);
        assertNotNull(luacFile, "Failed to parse xqdatacenter.lua");

        // 反编译
        String luaCode = decompiler.decompile(luacFile);
        assertNotNull(luaCode, "Failed to decompile");
        assertFalse(luaCode.isEmpty(), "Decompiled code is empty");

        // 写入输出文件
        try (PrintWriter writer = new PrintWriter(new FileWriter("target/xqdatacenter_decompiled.lua"))) {
            writer.print(luaCode);
        }

        // 打印 tunnelRequest 函数内容用于验证
        System.out.println("=== tunnelRequest 函数 ===");
        String[] lines = luaCode.split("\n");
        boolean inTunnel = false;
        int count = 0;
        for (String line : lines) {
            if (line.contains("function tunnelRequest")) {
                inTunnel = true;
                count = 0;
            }
            if (inTunnel) {
                System.out.println(line);
                count++;
                if (count > 30) break;
            }
        }
    }

    @Test
    void testDecompileHttp() throws IOException {
        String filePath = "src/test/resources/xiaomi/http.lua";
        File file = new File(filePath);
        assertTrue(file.exists(), "http.lua file does not exist: " + file.getAbsolutePath());

        LuacParser parser = new LuacParser();
        Decompiler decompiler = new Decompiler();

        LuacFile luacFile = parser.parse(filePath);
        assertNotNull(luacFile, "Failed to parse http.lua");

        String luaCode = decompiler.decompile(luacFile);
        assertNotNull(luaCode, "Failed to decompile");

        try (PrintWriter writer = new PrintWriter(new FileWriter("target/http_decompiled.lua"))) {
            writer.print(luaCode);
        }
    }
}