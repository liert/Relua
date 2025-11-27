package com.github.relua.parser;

import com.github.relua.model.Chunk;
import com.github.relua.model.Constant;
import com.github.relua.model.Instruction;

import java.io.EOFException;
import java.io.IOException;

/**
 * 代码块解析器
 */
public class ChunkParser {
    private BinaryReader reader;

    /**
     * 构造函数
     * @param reader 二进制读取器
     */
    public ChunkParser(BinaryReader reader) {
        this.reader = reader;
    }

    /**
     * 解析代码块
     * @return 解析后的代码块
     * @throws IOException IO异常
     */
    public Chunk parse() throws IOException {
        Chunk chunk = new Chunk();
        
        // 解析函数定义行号
        int source = reader.readInt();
        chunk.setLineDefined(reader.readInt());
        chunk.setLastLineDefined(reader.readInt());
        
        int nups = reader.readUnsignedByte();
        int numParams = reader.readUnsignedByte();
        int isVararg = reader.readUnsignedByte();
        int maxStackSize = reader.readUnsignedByte();
        System.out.println("nups: " + nups);
        System.out.println("numParams: " + numParams);
        System.out.println("isVararg: " + isVararg);
        System.out.println("maxStackSize: " + maxStackSize);
        
        chunk.setNumParams(numParams);
        chunk.setIsVararg(isVararg);
        chunk.setMaxStackSize(maxStackSize);
        
        // 解析指令列表
        parseInstructions(chunk);
        
        // 解析常量表
        parseConstants(chunk);
        
        // 解析子代码块
        parseSubChunks(chunk);
        
        // 解析局部变量表
        parseLocalVars(chunk);
        
        // 解析行号表
        parseLineNumbers(chunk);
        
        return chunk;
    }

    /**
     * 解析指令列表
     * @param chunk 代码块
     * @throws IOException IO异常
     */
    private void parseInstructions(Chunk chunk) throws IOException {
        try {
            // 读取指令数量，使用4字节int
            int instructionCount = reader.readInt();
            
            for (int i = 0; i < instructionCount; i++) {
                // 读取指令，使用4字节int
                int code = reader.readInt();
                Instruction instruction = new Instruction(code);
                chunk.addInstruction(instruction);
            }
        } catch (EOFException e) {
            // 处理文件末尾异常，可能是解析逻辑与实际文件格式不匹配
            System.err.println("Warning: Reached end of file while parsing instructions. This may be due to incompatible Luac file format.");
        }
    }

    /**
     * 解析常量表
     * @param chunk 代码块
     * @throws IOException IO异常
     */
    private void parseConstants(Chunk chunk) throws IOException {
        try {
            int constantCount = reader.readInt();
            
            for (int i = 0; i < constantCount; i++) {
                try {
                    byte type = reader.readByte();
                    Constant constant = parseConstant(type);
                    chunk.addConstant(constant);
                } catch (EOFException e) {
                    System.err.println("Warning: Reached end of file while parsing constant " + i + ".");
                    break;
                }
            }
        } catch (EOFException e) {
            System.err.println("Warning: Reached end of file while parsing constant count.");
        }
    }

    /**
     * 解析单个常量
     * @param type 常量类型
     * @return 常量
     * @throws IOException IO异常
     */
    private Constant parseConstant(byte type) throws IOException {
        try {
            switch (type) {
                case 0: // nil
                    return Constant.nil();
                case 1: // boolean
                    boolean boolValue = reader.readByte() != 0;
                    return Constant.booleanConstant(boolValue);
                case 3: // number
                    double numValue = reader.readLuaNumber();
                    return Constant.number(numValue);
                case 4: // string
                    String strValue = reader.readLuaString();
                    return Constant.string(strValue);
                case 9: // int <openwrt>
                    int intValue = reader.readInt();
                    return Constant.number(intValue);
                default:
                    throw new IOException("Unknown constant type: " + type);
            }
        } catch (EOFException e) {
            System.err.println("Warning: Reached end of file while parsing constant of type " + type + ".");
            return Constant.nil(); // 返回默认值
        }
    }

    /**
     * 解析子代码块
     * @param chunk 代码块
     * @throws IOException IO异常
     */
    private void parseSubChunks(Chunk chunk) throws IOException {
        try {
            int subChunkCount = reader.readInt();
            
            for (int i = 0; i < subChunkCount; i++) {
                try {
                    Chunk subChunk = parse();
                    chunk.addSubChunk(subChunk);
                } catch (EOFException e) {
                    System.err.println("Warning: Reached end of file while parsing sub-chunk " + i + ".");
                    break;
                }
            }
        } catch (EOFException e) {
            System.err.println("Warning: Reached end of file while parsing sub-chunk count.");
        }
    }

    /**
     * 解析局部变量表
     * @param chunk 代码块
     * @throws IOException IO异常
     */
    private void parseLocalVars(Chunk chunk) throws IOException {
        try {
            int localVarCount = reader.readInt();
            
            for (int i = 0; i < localVarCount; i++) {
                try {
                    String name = reader.readLuaString();
                    int startPC = reader.readInt();
                    int endPC = reader.readInt();
                    
                    Chunk.LocalVar localVar = new Chunk.LocalVar(name, startPC, endPC);
                    chunk.addLocalVar(localVar);
                } catch (EOFException e) {
                    System.err.println("Warning: Reached end of file while parsing local variable " + i + ".");
                    break;
                }
            }
        } catch (EOFException e) {
            System.err.println("Warning: Reached end of file while parsing local variable count.");
        }
    }

    /**
     * 解析行号表
     * @param chunk 代码块
     * @throws IOException IO异常
     */
    private void parseLineNumbers(Chunk chunk) throws IOException {
        try {
            int lineNumberCount = reader.readInt();
            
            for (int i = 0; i < lineNumberCount; i++) {
                try {
                    int lineNumber = reader.readInt();
                    chunk.addLineNumber(lineNumber);
                } catch (EOFException e) {
                    System.err.println("Warning: Reached end of file while parsing line number " + i + ".");
                    break;
                }
            }
        } catch (EOFException e) {
            System.err.println("Warning: Reached end of file while parsing line number count.");
        }
    }
}