package com.github.relua.parser;

import com.github.relua.model.LuacFile;
import com.github.relua.model.Chunk;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Luac文件解析器
 */
public class LuacParser {
    private final List<BytecodeFormatProfile> formatProfiles;

    public LuacParser() {
        this(Arrays.asList(
                new XiaomiFateLua51FormatProfile(),
                new StandardLua51FormatProfile()));
    }

    public LuacParser(List<BytecodeFormatProfile> formatProfiles) {
        this.formatProfiles = formatProfiles;
    }

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
        BytecodeFormatProfile formatProfile = parseHeader(reader, luacFile);

        // 更新reader的大小端设置
        reader.setLittleEndian(luacFile.isLittleEndian());

        // 解析主代码块
        ChunkParser chunkParser = new ChunkParser(reader, formatProfile);
        Chunk mainChunk = chunkParser.parse("main");
        luacFile.setMainChunk(mainChunk);
        // System.exit(0);
        return luacFile;
    }

    /**
     * 解析文件头
     * @param reader 二进制读取器
     * @param luacFile Luac文件对象
     * @throws IOException IO异常
     */
    private BytecodeFormatProfile parseHeader(BinaryReader reader, LuacFile luacFile) throws IOException {
        // 跳过 shebang 行（如 #!/usr/bin/lua\n）
        byte firstByte = reader.readByte();
        if (firstByte == '#') {
            // 读取直到换行符结束
            while (true) {
                byte b = reader.readByte();
                if (b == '\n' || b == '\r') {
                    break;
                }
            }
            // 读取真正的魔数第一个字节
            firstByte = reader.readByte();
        }

        // 读取魔数（firstByte + 后续3字节）
        byte[] magicNumber = new byte[4];
        magicNumber[0] = firstByte;
        for (int i = 1; i < 4; i++) {
            magicNumber[i] = reader.readByte();
        }

        for (BytecodeFormatProfile formatProfile : formatProfiles) {
            if (formatProfile.matches(magicNumber)) {
                formatProfile.parseHeader(reader, luacFile, magicNumber);
                return formatProfile;
            }
        }

        throw new IOException("Unknown Lua bytecode format");
    }
}
