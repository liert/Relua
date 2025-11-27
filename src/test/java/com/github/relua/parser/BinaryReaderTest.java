package com.github.relua.parser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BinaryReader测试用例
 */
class BinaryReaderTest {

    @Test
    void testReadByte() throws IOException {
        byte[] data = {0x42};
        BinaryReader reader = new BinaryReader(new ByteArrayInputStream(data));
        assertEquals(0x42, reader.readByte());
    }

    @Test
    void testReadUnsignedByte() throws IOException {
        byte[] data = {(byte) 0xFF};
        BinaryReader reader = new BinaryReader(new ByteArrayInputStream(data));
        assertEquals(255, reader.readUnsignedByte());
    }

    @Test
    void testReadShort() throws IOException {
        // 大端模式下的0x1234
        byte[] data = {0x12, 0x34};
        BinaryReader reader = new BinaryReader(new ByteArrayInputStream(data), false);
        assertEquals(0x1234, reader.readShort());
        
        // 小端模式下的0x1234
        reader = new BinaryReader(new ByteArrayInputStream(data), true);
        assertEquals(0x3412, reader.readShort());
    }

    @Test
    void testReadInt() throws IOException {
        // 大端模式下的0x12345678
        byte[] data = {0x12, 0x34, 0x56, 0x78};
        BinaryReader reader = new BinaryReader(new ByteArrayInputStream(data), false);
        assertEquals(0x12345678, reader.readInt());
        
        // 小端模式下的0x12345678
        reader = new BinaryReader(new ByteArrayInputStream(data), true);
        assertEquals(0x78563412, reader.readInt());
    }

    @Test
    void testReadLuaString() throws IOException {
        // Lua字符串格式：长度(4字节) + 内容 + 空字节
        // 长度应该包括末尾的空字节，所以"Hello"的长度是6
        byte[] data = {0x00, 0x00, 0x00, 0x06, 0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x00};
        BinaryReader reader = new BinaryReader(new ByteArrayInputStream(data));
        assertEquals("Hello", reader.readLuaString());
    }

    @Test
    void testEndianness() throws IOException {
        // 直接测试int值的大小端读取，不包含长度前缀
        byte[] data = {0x12, 0x34, 0x56, 0x78};
        BinaryReader reader = new BinaryReader(new ByteArrayInputStream(data));
        
        // 默认大端模式
        assertFalse(reader.isLittleEndian());
        assertEquals(0x12345678, reader.readInt());
        
        // 设置为小端模式
        reader = new BinaryReader(new ByteArrayInputStream(data));
        reader.setLittleEndian(true);
        assertTrue(reader.isLittleEndian());
        assertEquals(0x78563412, reader.readInt());
    }
}