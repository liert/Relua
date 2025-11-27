package com.github.relua.decompiler;

import com.github.relua.model.LuacFile;
import com.github.relua.model.Chunk;

/**
 * 反编译器主类
 */
public class Decompiler {
    private InstructionHandler instructionHandler;
    private LuaCodeGenerator codeGenerator;

    /**
     * 构造函数
     */
    public Decompiler() {
        this.instructionHandler = new InstructionHandler();
        this.codeGenerator = new LuaCodeGenerator(instructionHandler);
    }

    /**
     * 反编译Luac文件
     * @param luacFile 解析后的Luac文件对象
     * @return 反编译后的Lua代码
     */
    public String decompile(LuacFile luacFile) {
        if (luacFile == null || luacFile.getMainChunk() == null) {
            throw new IllegalArgumentException("Invalid LuacFile object");
        }

        Chunk mainChunk = luacFile.getMainChunk();
        
        // 处理指令，构建中间表示
        instructionHandler.processChunk(mainChunk);
        
        // 生成Lua代码
        return codeGenerator.generate(mainChunk);
    }

    /**
     * 获取指令处理器
     * @return 指令处理器
     */
    public InstructionHandler getInstructionHandler() {
        return instructionHandler;
    }

    /**
     * 获取代码生成器
     * @return 代码生成器
     */
    public LuaCodeGenerator getCodeGenerator() {
        return codeGenerator;
    }
}