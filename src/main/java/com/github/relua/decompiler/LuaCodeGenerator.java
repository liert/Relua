package com.github.relua.decompiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.relua.log.Logger;
import com.github.relua.model.Chunk;
import com.github.relua.model.CodeLine;
import com.github.relua.model.FromType;
import com.github.relua.model.Register;
import com.github.relua.model.ValueType;

/**
 * Lua代码生成器，作为总控类，负责协调各个代码生成器
 */
public class LuaCodeGenerator {
    // private InstructionHandler instructionHandler;
    private AstCodeEmitter astCodeEmitter;
    private List<CodeGeneratorContext> contexts = new ArrayList<>();
    private Map<String, InstructionHandler> handlers = new HashMap<>();

    /**
     * 构造函数
     * 
     * @param codeGenContext 代码生成上下文
     */
    public LuaCodeGenerator(Chunk chunk) {
        this.astCodeEmitter = new AstCodeEmitter();

        // 初始化所有代码块上下文
        initializeContexts(chunk);
    }

    /**
     * 初始化代码块的上下文
     * 
     * @param chunk
     * @param register
     * @return
     */
    private void initializeContexts(Chunk chunk) {
        Register register = new Register();
        for (int i = 0; i < chunk.getNumParams(); i++) {
            register.setRegisterEntity(i, "a" + i, ValueType.OBJECT, FromType.GLOBAL);
        }
        CodeGeneratorContext context = new CodeGeneratorContext(chunk, register);
        contexts.add(context);
        handlers.put(chunk.getFunction(), new InstructionHandler(this, context));
        for (Chunk subChunk : chunk.getSubChunks()) {
            initializeContexts(subChunk);
        }
    }

    /**
     * 生成Lua代码
     * 
     * @param chunk 代码块
     * @return 生成的Lua代码
     */
    public String generate(Chunk chunk, Register register) {
        // 创建代码生成上下文
        Logger.debug("当前处理的Chunk函数名: " + chunk.getFunction());
        InstructionHandler handler = handlers.get(chunk.getFunction());
        CodeGeneratorContext context = handler.getContext();

        // System.out.println("=== 开始处理Chunk ===");
        System.out.println("Chunk信息: lineDefined=" + chunk.getLineDefined() + ", lastLineDefined="
                + chunk.getLastLineDefined() + ", numParams=" + chunk.getNumParams() + ", isVararg="
                + chunk.getIsVararg() + ", maxStackSize=" + chunk.getMaxStackSize());

        // 先让指令处理器处理代码块，建立控制流和变量映射
        handler.process(chunk);

        // 生成代码块头部信息
        if (chunk.getFunction().equals("main")) {
            System.out.println("生成代码块头部信息...");
            generateChunkHeader(chunk, context);
        }

        // 生成指令代码（使用AST）
        System.out.println("生成AST代码...");
        astCodeEmitter.emitAst(chunk, context, handler);

        // 关闭所有未结束的控制流结构
        // System.out.println("关闭所有未结束的控制流结构...");
        context.closeAllControlFlow();

        System.out.println("=== Chunk处理完成 ===");

        contexts.add(context);

        for (Chunk subChunk : chunk.getSubChunks()) {
            Register temp = new Register();
            for (int i = 0; i < subChunk.getNumParams(); i++) {
                temp.setRegisterEntity(i, "a" + i, ValueType.OBJECT, FromType.GLOBAL);
            }
            generate(subChunk, temp);
        }

        if (chunk.getFunction().equals("main")) {
            StringBuilder code = new StringBuilder();
            for (CodeGeneratorContext ctx : contexts) {
                code.append("function ").append(ctx.getChunk().getFunction()).append("(");
                for (int i = 0; i < ctx.getChunk().getNumParams(); i++) {
                    code.append("a").append(i);
                    if (i < ctx.getChunk().getNumParams() - 1) {
                        code.append(", ");
                    }
                }
                code.append(") {\n");
                code.append(ctx.generateCode());
                code.append("}\n\n");
            }
            return code.toString();
        }
        return "";
    }

    /**
     * 生成Lua代码
     * 
     * @param chunk 代码块
     * @return 生成的Lua代码
     */
    // public String generate(Chunk chunk, CodeGeneratorContext context) {
    // // 创建代码生成上下文
    // this.instructionHandler = new InstructionHandler(context);

    // System.out.println("=== 开始处理Chunk ===");
    // System.out.println("Chunk信息: lineDefined=" + chunk.getLineDefined() + ",
    // lastLineDefined="
    // + chunk.getLastLineDefined() + ", numParams=" + chunk.getNumParams() + ",
    // isVararg="
    // + chunk.getIsVararg() + ", maxStackSize=" + chunk.getMaxStackSize());

    // // 先让指令处理器处理代码块，建立控制流和变量映射
    // instructionHandler.process(chunk);

    // // 生成代码块头部信息
    // if (chunk.getFunction().equals("main")) {
    // System.out.println("生成代码块头部信息...");
    // generateChunkHeader(chunk, context);
    // }

    // // 生成指令代码（使用AST）
    // System.out.println("生成AST代码...");
    // astCodeEmitter.emitAst(chunk, context, instructionHandler);

    // // 关闭所有未结束的控制流结构
    // // System.out.println("关闭所有未结束的控制流结构...");
    // context.closeAllControlFlow();

    // System.out.println("=== Chunk处理完成 ===");

    // contexts.add(context);
    // return "";
    // }

    /**
     * 生成代码块头部信息
     * 
     * @param chunk   代码块
     * @param context 代码生成上下文
     */
    private void generateChunkHeader(Chunk chunk, CodeGeneratorContext context) {
        // 对于主代码块，添加一些元信息注释
        context.addCodeLine("-- Decompiled Lua code", CodeLine.CodeType.COMMENT);
        context.addCodeLine("-- Generated by Relua", CodeLine.CodeType.COMMENT);
        context.addEmptyLine();
    }

    public InstructionHandler getInstructionHandler(String function) {
        return handlers.get(function);
    }
}