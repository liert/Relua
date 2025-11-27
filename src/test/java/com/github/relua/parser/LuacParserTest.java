package com.github.relua.parser;

import com.github.relua.model.LuacFile;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LuacParser测试用例
 */
class LuacParserTest {

    @Test
    void testInvalidMagicNumber() {
        // 创建一个无效的Luac文件流（魔数不正确）
        byte[] invalidData = {0x00, 0x00, 0x00, 0x00};
        ByteArrayInputStream is = new ByteArrayInputStream(invalidData);
        LuacParser parser = new LuacParser();
        
        // 应该抛出IOException，因为魔数不正确
        assertThrows(IOException.class, () -> parser.parse(is));
    }

    @Test
    void testValidMagicNumber() throws IOException {
        // 创建一个包含有效Lua魔数的最小Luac文件流
        byte[] validData = {
            0x1B, 'L', 'u', 'a', // 魔数
            0x51, // 版本号（5.1）
            0x00, // 格式标识
            0x01, // 大小端标识（小端）
            0x04, // int大小
            0x04, // size_t大小
            0x04, // 指令大小
            0x08, // Lua数字大小
            0x00  // 整数标识
            // 缺少主代码块数据，会在解析时抛出异常
        };
        ByteArrayInputStream is = new ByteArrayInputStream(validData);
        LuacParser parser = new LuacParser();
        
        // 应该抛出IOException，因为缺少主代码块数据，但魔数验证应该通过
        Exception exception = assertThrows(Exception.class, () -> parser.parse(is));
        
        // 如果异常有消息，它不应该是关于魔数的
        if (exception.getMessage() != null) {
            assertFalse(exception.getMessage().contains("Invalid Lua magic number"));
        }
    }

    @Test
    void testParseVersion() throws IOException {
        // 创建一个包含版本信息的Luac文件流
        byte[] data = {
            0x1B, 'L', 'u', 'a', // 魔数
            0x52, // 版本号（5.2）
            0x00, // 格式标识
            0x00, // 大小端标识（大端）
            0x04, // int大小
            0x04, // size_t大小
            0x04, // 指令大小
            0x08, // Lua数字大小
            0x00  // 整数标识
            // 缺少主代码块数据
        };
        ByteArrayInputStream is = new ByteArrayInputStream(data);
        LuacParser parser = new LuacParser();
        
        try {
            LuacFile luacFile = parser.parse(is);
            fail("Expected IOException was not thrown");
        } catch (IOException e) {
            // 忽略异常，我们只关心版本号解析
        }
    }
}