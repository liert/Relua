package com.github.relua.parser;

import com.github.relua.model.LuacFile;
import com.github.relua.model.Chunk;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Luac文件解析器
 */
public class LuacParser {
    // Lua魔数
    private static final byte[] LUA_MAGIC = {0x1B, 'L', 'u', 'a'};

    /**
     * 解析Luac文件
     * @param filePath Luac文件路径
     * @return 解析后的Luac文件对象
     * @throws IOException IO异常
     */
    public LuacFile parse(String filePath) throws IOException {
        try (InputStream is = new FileInputStream(filePath)) {
            return parse(is);
        }
    }

    /**
     * 解析Luac文件
     * @param inputStream Luac文件输入流
     * @return 解析后的Luac文件对象
     * @throws IOException IO异常
     */
    public LuacFile parse(InputStream inputStream) throws IOException {
        BinaryReader reader = new BinaryReader(inputStream);
        LuacFile luacFile = new LuacFile();

        // 解析文件头
        parseHeader(reader, luacFile);

        // 更新reader的大小端设置
        reader.setLittleEndian(luacFile.isLittleEndian());

        // 解析主代码块
        ChunkParser chunkParser = new ChunkParser(reader);
        Chunk mainChunk = chunkParser.parse();
        luacFile.setMainChunk(mainChunk);

        return luacFile;
    }

    /**
     * 解析文件头
     * @param reader 二进制读取器
     * @param luacFile Luac文件对象
     * @throws IOException IO异常
     */
    private void parseHeader(BinaryReader reader, LuacFile luacFile) throws IOException {
        // 读取魔数
        byte[] magicNumber = new byte[4];
        for (int i = 0; i < 4; i++) {
            magicNumber[i] = reader.readByte();
        }
        luacFile.setMagicNumber(magicNumber);

        // 验证魔数
        if (!isValidMagicNumber(magicNumber)) {
            throw new IOException("Invalid Lua magic number");
        }

        // 读取版本号
        byte version = reader.readByte();
        luacFile.setVersion(version);

        // 读取格式标识
        byte format = reader.readByte();
        luacFile.setFormat(format);

        // 读取大小端标识
        byte endianness = reader.readByte();
        luacFile.setEndianness(endianness);

        // 读取int类型大小
        byte intSize = reader.readByte();
        luacFile.setIntSize(intSize);

        // 读取size_t类型大小
        byte sizeTSize = reader.readByte();
        luacFile.setSizeTSize(sizeTSize);

        // 读取指令大小
        byte instructionSize = reader.readByte();
        luacFile.setInstructionSize(instructionSize);

        // 读取Lua数字大小
        byte luaNumberSize = reader.readByte();
        luacFile.setLuaNumberSize(luaNumberSize);

        // 读取是否为整数
        byte integralFlag = reader.readByte();
        luacFile.setIntegralFlag(integralFlag);
    }

    /**
     * 验证魔数是否有效
     * @param magicNumber 魔数
     * @return 是否有效
     */
    private boolean isValidMagicNumber(byte[] magicNumber) {
        if (magicNumber.length != LUA_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < LUA_MAGIC.length; i++) {
            if (magicNumber[i] != LUA_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }
}