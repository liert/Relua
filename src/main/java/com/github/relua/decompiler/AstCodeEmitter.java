package com.github.relua.decompiler;

import com.github.relua.ast.AstNode;
import com.github.relua.ast.AstPrinter;
import com.github.relua.model.Chunk;

/**
 * AST代码生成器，负责将AST转换为Lua代码
 */
public class AstCodeEmitter {
    /**
     * 生成AST代码
     * 
     * @param chunk   代码块
     * @param context 代码生成上下文
     * @param handler 指令处理器
     */
    public void emitAst(Chunk chunk, CodeGeneratorContext context, InstructionHandler handler) {
        // 获取指令处理器生成的AST
        AstNode ast = handler.generateASTFromChunk(chunk);

        // 创建AST打印机
        AstPrinter astPrinter = new AstPrinter();

        // 生成Lua代码
        String luaCode = ast.accept(astPrinter);

        // 将Lua代码添加到上下文
        String[] lines = luaCode.split("\\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                context.addCodeLine(line);
            }
        }
    }
}