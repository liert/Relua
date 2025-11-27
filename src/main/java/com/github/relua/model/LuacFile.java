package com.github.relua.model;

/**
 * Luac文件模型
 */
public class LuacFile {
    // 文件头信息
    private byte[] magicNumber;    // 魔数
    private byte version;          // 版本号
    private byte format;           // 格式标识
    private byte endianness;       // 大小端标识
    private byte intSize;          // int类型大小
    private byte sizeTSize;        // size_t类型大小
    private byte instructionSize;  // 指令大小
    private byte luaNumberSize;    // Lua数字大小
    private byte integralFlag;     // 是否为整数
    
    private Chunk mainChunk;       // 主代码块

    /**
     * 获取魔数
     * @return 魔数
     */
    public byte[] getMagicNumber() {
        return magicNumber;
    }

    /**
     * 设置魔数
     * @param magicNumber 魔数
     */
    public void setMagicNumber(byte[] magicNumber) {
        this.magicNumber = magicNumber;
    }

    /**
     * 获取版本号
     * @return 版本号
     */
    public byte getVersion() {
        return version;
    }

    /**
     * 设置版本号
     * @param version 版本号
     */
    public void setVersion(byte version) {
        this.version = version;
    }

    /**
     * 获取格式标识
     * @return 格式标识
     */
    public byte getFormat() {
        return format;
    }

    /**
     * 设置格式标识
     * @param format 格式标识
     */
    public void setFormat(byte format) {
        this.format = format;
    }

    /**
     * 获取大小端标识
     * @return 大小端标识
     */
    public byte getEndianness() {
        return endianness;
    }

    /**
     * 设置大小端标识
     * @param endianness 大小端标识
     */
    public void setEndianness(byte endianness) {
        this.endianness = endianness;
    }

    /**
     * 获取int类型大小
     * @return int类型大小
     */
    public byte getIntSize() {
        return intSize;
    }

    /**
     * 设置int类型大小
     * @param intSize int类型大小
     */
    public void setIntSize(byte intSize) {
        this.intSize = intSize;
    }

    /**
     * 获取size_t类型大小
     * @return size_t类型大小
     */
    public byte getSizeTSize() {
        return sizeTSize;
    }

    /**
     * 设置size_t类型大小
     * @param sizeTSize size_t类型大小
     */
    public void setSizeTSize(byte sizeTSize) {
        this.sizeTSize = sizeTSize;
    }

    /**
     * 获取指令大小
     * @return 指令大小
     */
    public byte getInstructionSize() {
        return instructionSize;
    }

    /**
     * 设置指令大小
     * @param instructionSize 指令大小
     */
    public void setInstructionSize(byte instructionSize) {
        this.instructionSize = instructionSize;
    }

    /**
     * 获取Lua数字大小
     * @return Lua数字大小
     */
    public byte getLuaNumberSize() {
        return luaNumberSize;
    }

    /**
     * 设置Lua数字大小
     * @param luaNumberSize Lua数字大小
     */
    public void setLuaNumberSize(byte luaNumberSize) {
        this.luaNumberSize = luaNumberSize;
    }

    /**
     * 获取是否为整数
     * @return 是否为整数
     */
    public byte getIntegralFlag() {
        return integralFlag;
    }

    /**
     * 设置是否为整数
     * @param integralFlag 是否为整数
     */
    public void setIntegralFlag(byte integralFlag) {
        this.integralFlag = integralFlag;
    }

    /**
     * 获取主代码块
     * @return 主代码块
     */
    public Chunk getMainChunk() {
        return mainChunk;
    }

    /**
     * 设置主代码块
     * @param mainChunk 主代码块
     */
    public void setMainChunk(Chunk mainChunk) {
        this.mainChunk = mainChunk;
    }

    /**
     * 判断是否为小端模式
     * @return 是否为小端模式
     */
    public boolean isLittleEndian() {
        return endianness == 1;
    }

    /**
     * 获取Lua版本字符串
     * @return Lua版本字符串
     */
    public String getLuaVersion() {
        switch (version) {
            case 0x51:
                return "5.1";
            case 0x52:
                return "5.2";
            case 0x53:
                return "5.3";
            case 0x54:
                return "5.4";
            default:
                return "unknown (0x" + Integer.toHexString(version & 0xFF) + ")";
        }
    }

    @Override
    public String toString() {
        return String.format("LuacFile[version=%s, format=%d, endianness=%s, mainChunk=%s]",
                getLuaVersion(), format, isLittleEndian() ? "little" : "big", mainChunk.toString());
    }
}