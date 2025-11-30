package com.github.relua.parser;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 二进制读取工具类，用于读取Luac文件的二进制数据
 */
public class BinaryReader {
    private DataInputStream dis;
    private boolean isLittleEndian;

    /**
     * 构造函数
     * @param inputStream 输入流
     * @param isLittleEndian 是否为小端模式
     */
    public BinaryReader(InputStream inputStream, boolean isLittleEndian) {
        this.dis = new DataInputStream(inputStream);
        this.isLittleEndian = isLittleEndian;
    }

    /**
     * 构造函数，默认大端模式
     * @param inputStream 输入流
     */
    public BinaryReader(InputStream inputStream) {
        this(inputStream, false);
    }

    /**
     * 读取一个字节
     * @return 字节值
     * @throws IOException IO异常
     */
    public byte readByte() throws IOException {
        return dis.readByte();
    }

    /**
     * 读取一个无符号字节
     * @return 无符号字节值
     * @throws IOException IO异常
     */
    public int readUnsignedByte() throws IOException {
        return dis.readUnsignedByte();
    }

    /**
     * 读取一个短整型
     * @return 短整型值
     * @throws IOException IO异常
     */
    public short readShort() throws IOException {
        short value = dis.readShort();
        if (isLittleEndian) {
            value = swapShort(value);
        }
        return value;
    }

    /**
     * 读取一个无符号短整型
     * @return 无符号短整型值
     * @throws IOException IO异常
     */
    public int readUnsignedShort() throws IOException {
        byte[] bytes = new byte[2];
        dis.readFully(bytes);
        int value;
        if (isLittleEndian) {
            value = (bytes[1] & 0xFF) << 8 | (bytes[0] & 0xFF);
        } else {
            value = (bytes[0] & 0xFF) << 8 | (bytes[1] & 0xFF);
        }
        return value;
    }

    /**
     * 读取一个整型
     * @return 整型值
     * @throws IOException IO异常
     */
    public int readInt() throws IOException {
        int value = dis.readInt();
        if (isLittleEndian) {
            value = swapInt(value);
        }
        return value;
    }

    /**
     * 读取一个长整型
     * @return 长整型值
     * @throws IOException IO异常
     */
    public long readLong() throws IOException {
        long value = dis.readLong();
        if (isLittleEndian) {
            value = swapLong(value);
        }
        return value;
    }

    /**
     * 读取一个Lua字符串
     * @return 字符串值
     * @throws IOException IO异常
     */
    public String readLuaString() throws IOException {
        int length = readInt();
        
        if (length == 0) {
            return "";
        }
        
        // 不读取\x00空终止符
        byte[] bytes = new byte[length - 1];
        dis.readFully(bytes);
        // 跳过\x00空终止符
        dis.readByte();
        return new String(bytes, "UTF-8");
    }

    /**
     * 读取一个Lua数字
     * @return 数字值
     * @throws IOException IO异常
     */
    public double readLuaNumber() throws IOException {
        byte[] bytes = new byte[8];
        dis.readFully(bytes);
        
        // 处理大小端
        if (isLittleEndian) {
            // 小端转大端
            for (int i = 0; i < 4; i++) {
                byte temp = bytes[i];
                bytes[i] = bytes[7 - i];
                bytes[7 - i] = temp;
            }
        }
        
        // 使用DataInputStream从大端字节数组读取double
        try (DataInputStream tempDis = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return tempDis.readDouble();
        }
    }

    /**
     * 跳过指定数量的字节
     * @param n 要跳过的字节数
     * @throws IOException IO异常
     */
    public void skip(long n) throws IOException {
        dis.skip(n);
    }

    /**
     * 关闭输入流
     * @throws IOException IO异常
     */
    public void close() throws IOException {
        dis.close();
    }

    /**
     * 设置大小端模式
     * @param isLittleEndian 是否为小端模式
     */
    public void setLittleEndian(boolean isLittleEndian) {
        this.isLittleEndian = isLittleEndian;
    }

    /**
     * 获取当前大小端模式
     * @return 是否为小端模式
     */
    public boolean isLittleEndian() {
        return isLittleEndian;
    }

    // 字节序转换方法
    private short swapShort(short value) {
        return (short) (((value & 0xFF) << 8) | ((value >> 8) & 0xFF));
    }

    private int swapInt(int value) {
        return ((value & 0xFF) << 24) |
               ((value & 0xFF00) << 8) |
               ((value & 0xFF0000) >>> 8) |
               ((value & 0xFF000000) >>> 24);
    }

    private long swapLong(long value) {
        return ((value & 0xFFL) << 56) |
               ((value & 0xFF00L) << 40) |
               ((value & 0xFF0000L) << 24) |
               ((value & 0xFF000000L) << 8) |
               ((value & 0xFF00000000L) >>> 8) |
               ((value & 0xFF0000000000L) >>> 24) |
               ((value & 0xFF000000000000L) >>> 40) |
               ((value & 0xFF00000000000000L) >>> 56);
    }
}