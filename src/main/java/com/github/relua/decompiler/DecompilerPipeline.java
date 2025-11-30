package com.github.relua.decompiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.relua.decompiler.analysis.ControlFlowAnalyzer;
import com.github.relua.decompiler.analysis.RegisterStateAnalyzer;
import com.github.relua.decompiler.builder.BasicBlockBuilder;
import com.github.relua.decompiler.cfg.ControlFlowGraphBuilder;
import com.github.relua.decompiler.ir.IRBuilder;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Register;

public class DecompilerPipeline {
    private final LuaCodeGenerator generator;
    private final InstructionHandler instructionHandler;
    private final Map<String, BasicBlockBuilder> basicBlockBuilders = new HashMap<>();
    private final ControlFlowGraphBuilder cfgBuilder;
    private final ControlFlowAnalyzer controlFlowAnalyzer;
    private final RegisterStateAnalyzer registerStateAnalyzer;
    private final IRBuilder irBuilder;

    public DecompilerPipeline(LuaCodeGenerator generator, InstructionHandler instructionHandler) {
        this.generator = generator;
        this.instructionHandler = instructionHandler;
        // this.basicBlockBuilder = new BasicBlockBuilder(this);
        this.cfgBuilder = new ControlFlowGraphBuilder(this);
        this.controlFlowAnalyzer = new ControlFlowAnalyzer(this);
        this.registerStateAnalyzer = new RegisterStateAnalyzer(this);
        this.irBuilder = new IRBuilder(this);
    }

    /**
     * 处理代码块的指令
     * 
     * @param chunk 代码块
     */
    public void processChunk(Chunk chunk) {
        if (chunk == null) {
            return;
        }

        BasicBlockBuilder basicBlockBuilder = new BasicBlockBuilder(this);
        basicBlockBuilders.put(chunk.getFunction(), basicBlockBuilder);

        // 构建基本块
        basicBlockBuilder.build(chunk);

        // 分析控制流
        controlFlowAnalyzer.analyze(chunk);

        // 打印所有基本块的类型
        System.out.println("\n===== 基本块信息 =====");
        for (int i = 0; i < basicBlockBuilder.getBasicBlocks().size(); i++) {
            BasicBlock block = basicBlockBuilder.getBasicBlocks().get(i);
            String blockType = "普通块";
            if (block.isIfBlock()) {
                blockType = "IF块";
            } else if (block.isLoopBlock()) {
                blockType = "LOOP块";
            } else if (block.isElseBlock()) {
                blockType = "ELSE块";
            }
            System.out.println("块 " + i + ": [" + block.getStartIndex() + "-" + block.getEndIndex() + "]");
            System.out.println("  类型: " + blockType);
        }

        // 构建控制流图
        cfgBuilder.build(chunk);
        // 打印CFG结构
        System.out.println("\n===== 控制流图结构 =====");
        for (int i = 0; i < basicBlockBuilder.getBasicBlocks().size(); i++) {
            BasicBlock block = basicBlockBuilder.getBasicBlocks().get(i);
            System.out.println("块 " + i + " -> 后继:");
            for (BasicBlock successor : block.getSuccessors()) {
                // 查找后继块的索引
                int succIndex = basicBlockBuilder.getBasicBlocks().indexOf(successor);
                System.out.println("  -> 块 " + succIndex + ": [" + successor.getStartIndex() + "-"
                        + successor.getEndIndex() + "]");
            }

            System.out.println("块 " + i + " <- 前驱:");
            for (BasicBlock predecessor : block.getPredecessors()) {
                // 查找前驱块的索引
                int predIndex = basicBlockBuilder.getBasicBlocks().indexOf(predecessor);
                System.out.println("  <- 块 " + predIndex + ": [" + predecessor.getStartIndex() + "-"
                        + predecessor.getEndIndex() + "]");
            }
        }

        // 执行迭代数据流分析
        registerStateAnalyzer.analyze(chunk);
    }

    public int processInstruction(Chunk chunk, Instruction instruction, int index, Register currentState) {
        return irBuilder.processInstruction(chunk, instruction, index, currentState);
    }

    /**
     * 从指令行获取基本块
     * 
     * @return
     */
    public BasicBlock getBasicBlock(String function, int index) {
        return basicBlockBuilders.get(function).getInstructionToBlockMap().get(index);
    }

    public List<BasicBlock> getBasicBlocks(String function) {
        return basicBlockBuilders.get(function).getBasicBlocks();
    }

    public BasicBlock getBlockByStartIndex(String function, int startIndex) {
        return basicBlockBuilders.get(function).getBlockByStartIndex(startIndex);
    }

    public Register getRegisterByInstructionIndex(int instructionIndex) {
        return registerStateAnalyzer.getRegisterByInstructionIndex(instructionIndex);
    }

    /**
     * 获取当前指令处理的上下文
     * 
     * @return 代码生成上下文
     */
    public CodeGeneratorContext getContext() {
        return instructionHandler.getContext();
    }

    /**
     * 获取指定函数的代码生成上下文
     * 
     * @param function 函数名
     * @return 代码生成上下文
     */
    public CodeGeneratorContext getContext(String function) {
        return generator.getInstructionHandler(function).getContext();
    }

    public ControlFlowAnalyzer getControlFlowAnalyzer() {
        return controlFlowAnalyzer;
    }
}
