package com.github.relua.gui.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HexFormatterTest {

    @Test
    @DisplayName("测试空数据或null数据的 Hex 格式化输出")
    public void testFormatEmptyData() {
        assertEquals("// 空文件或无二进制数据\n", HexFormatter.formatHex(null));
        assertEquals("// 空文件或无二进制数据\n", HexFormatter.formatHex(new byte[0]));
    }

    @Test
    @DisplayName("测试标准二进制数据的 Hex 格式化对齐输出")
    public void testFormatStandardData() {
        // Lua 头部标志: 0x1B, 'L', 'u', 'a', 0x51
        byte[] data = new byte[]{
                0x1B, 'L', 'u', 'a', 0x51, 0x00, 0x01, 0x04,
                0x08, 0x04, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x61, 0x62, 0x63 // 'a', 'b', 'c' on second row
        };

        String hex = HexFormatter.formatHex(data);
        assertNotNull(hex);
        assertTrue(hex.contains("00000000  "));
        assertTrue(hex.contains("1B 4C 75 61 51 00 01 04  08 04 08 00 00 00 00 00  |.LuaQ...........|"));
        assertTrue(hex.contains("00000010  "));
        assertTrue(hex.contains("61 62 63"));
        assertTrue(hex.contains("|abc|"));
    }

    @Test
    @DisplayName("测试从字符偏移反向计算字节偏移与往返双向转换")
    public void testCharAndByteOffsetMapping() {
        byte[] data = new byte[64];
        for (int i = 0; i < 64; i++) {
            data[i] = (byte) i;
        }

        // 测试第 0 行的前几个字节
        for (int byteIdx = 0; byteIdx < 32; byteIdx++) {
            int charOffset = HexFormatter.calculateCharOffsetFromByteOffset(byteIdx);
            assertTrue(charOffset >= 0);
            int mappedByte = HexFormatter.calculateByteOffsetFromCharOffset(charOffset, data.length);
            assertEquals(byteIdx, mappedByte, "字节偏移往返映射不一致: " + byteIdx);
        }

        // 测试无效与越界情况
        assertEquals(-1, HexFormatter.calculateByteOffsetFromCharOffset(-1, 10));
        assertEquals(-1, HexFormatter.calculateByteOffsetFromCharOffset(0, 0));
    }

    @Test
    @DisplayName("测试二进制数据检查器 (Binary Inspector) 的解析功能")
    public void testInspectOffset() {
        byte[] data = new byte[]{
                (byte) 0x78, (byte) 0x56, (byte) 0x34, (byte) 0x12 // Little-endian 0x12345678 = 305419896
        };

        String inspect0 = HexFormatter.inspectOffset(data, 0);
        assertNotNull(inspect0);
        assertTrue(inspect0.contains("Offset: 0x00000000 (0)"));
        assertTrue(inspect0.contains("UInt8: 120 (0x78)"));
        assertTrue(inspect0.contains("Int16: 22136")); // 0x5678 = 22136
        assertTrue(inspect0.contains("Int32: 305419896")); // 0x12345678

        // 越界保护
        assertEquals("Offset: - | UInt8: - | Int32: -", HexFormatter.inspectOffset(data, -1));
        assertEquals("Offset: - | UInt8: - | Int32: -", HexFormatter.inspectOffset(data, 100));
        assertEquals("Offset: - | UInt8: - | Int32: -", HexFormatter.inspectOffset(null, 0));
    }
}
