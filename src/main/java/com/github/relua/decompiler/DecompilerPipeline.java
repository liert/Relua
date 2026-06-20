package com.github.relua.decompiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.relua.decompiler.analysis.ControlFlowAnalyzer;
import com.github.relua.decompiler.builder.BasicBlockBuilder;
import com.github.relua.decompiler.cfg.ControlFlowGraphBuilder;
import com.github.relua.decompiler.ssa.SsaExpressionAnalysis;
import com.github.relua.decompiler.ssa.SsaExpressionAnalyzer;
import com.github.relua.decompiler.ssa.SsaBuilder;
import com.github.relua.decompiler.ssa.SsaFunction;
import com.github.relua.decompiler.ssa.SsaInstruction;
import com.github.relua.decompiler.ssa.SsaRegisterSnapshot;
import com.github.relua.decompiler.ssa.SsaValue;
import com.github.relua.decompiler.ssa.SsaVerifier;
import com.github.relua.log.Logger;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.util.RegisterNamePolicy;

public class DecompilerPipeline {
    private final LuaCodeGenerator generator;
    private final InstructionHandler instructionHandler;
    private final Map<String, BasicBlockBuilder> basicBlockBuilders = new HashMap<>();
    private final ControlFlowGraphBuilder cfgBuilder;
    private final ControlFlowAnalyzer controlFlowAnalyzer;
    private final SsaBuilder ssaBuilder;
    private final SsaExpressionAnalyzer ssaExpressionAnalyzer;
    private final Map<String, SsaFunction> ssaFunctions = new HashMap<>();
    private final Map<String, SsaExpressionAnalysis> ssaExpressionAnalyses = new HashMap<>();

    public DecompilerPipeline(LuaCodeGenerator generator, InstructionHandler instructionHandler) {
        this.generator = generator;
        this.instructionHandler = instructionHandler;
        // this.basicBlockBuilder = new BasicBlockBuilder(this);
        this.cfgBuilder = new ControlFlowGraphBuilder(this);
        this.controlFlowAnalyzer = new ControlFlowAnalyzer(this);
        this.ssaBuilder = new SsaBuilder();
        this.ssaExpressionAnalyzer = new SsaExpressionAnalyzer();
    }

    public LuaCodeGenerator getGenerator() {
        return generator;
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
        // System.out.println("\n===== 基本块信息 =====");
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
            // System.out.println("块 " + i + ": [" + block.getStartIndex() + "-" + block.getEndIndex() + "]");
            // System.out.println("  类型: " + blockType);
        }

        // 构建控制流图
        cfgBuilder.build(chunk);
        // 打印CFG结构
        // System.out.println("\n===== 控制流图结构 =====");
        for (int i = 0; i < basicBlockBuilder.getBasicBlocks().size(); i++) {
            BasicBlock block = basicBlockBuilder.getBasicBlocks().get(i);
            // System.out.println("块 " + i + " -> 后继:");
            for (BasicBlock successor : block.getSuccessors()) {
                // 查找后继块的索引
                int succIndex = basicBlockBuilder.getBasicBlocks().indexOf(successor);
                // System.out.println("  -> 块 " + succIndex + ": [" + successor.getStartIndex() + "-"
                //         + successor.getEndIndex() + "]");
            }

            // System.out.println("块 " + i + " <- 前驱:");
            for (BasicBlock predecessor : block.getPredecessors()) {
                // 查找前驱块的索引
                int predIndex = basicBlockBuilder.getBasicBlocks().indexOf(predecessor);
                // System.out.println("  <- 块 " + predIndex + ": [" + predecessor.getStartIndex() + "-"
                //         + predecessor.getEndIndex() + "]");
            }
        }

        SsaFunction ssaFunction = ssaBuilder.build(chunk, basicBlockBuilder.getBasicBlocks());
        ssaFunctions.put(chunk.getFunction(), ssaFunction);
        List<String> ssaErrors = SsaVerifier.verify(ssaFunction);
        if (!ssaErrors.isEmpty()) {
            for (com.github.relua.decompiler.pipeline.PipelineDebugListener listener : generator.getDebugListeners()) {
                listener.onSsaVerifyFailed(chunk, ssaErrors);
            }
            String message = "SSA verification failed for " + chunk.getFunction() + ": " + ssaErrors;
            Logger.error(message);
            throw new IllegalStateException(message);
        }
        SsaExpressionAnalysis ssaExpressionAnalysis = ssaExpressionAnalyzer.analyze(ssaFunction);
        ssaExpressionAnalyses.put(chunk.getFunction(), ssaExpressionAnalysis);

        for (com.github.relua.decompiler.pipeline.PipelineDebugListener listener : generator.getDebugListeners()) {
            listener.onCFGBuilt(chunk, basicBlockBuilder.getBasicBlocks(), formatCFG(chunk, basicBlockBuilder));
            listener.onSSABuilt(chunk, ssaFunction);
            listener.onSsaExprAnalyzed(chunk, ssaExpressionAnalysis);
        }
    }

    private String formatCFG(Chunk chunk, BasicBlockBuilder builder) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Chunk: ").append(chunk.getFunction()).append(" =====\n");
        sb.append("Instructions count: ").append(chunk.getInstructions().size()).append("\n\n");
        
        List<BasicBlock> blocks = builder.getBasicBlocks();
        if (blocks == null || blocks.isEmpty()) {
            sb.append("No basic blocks.\n");
            return sb.toString();
        }
        
        sb.append("--- Basic Blocks ---\n");
        for (int i = 0; i < blocks.size(); i++) {
            BasicBlock block = blocks.get(i);
            String blockType = "NORMAL";
            if (block.isIfBlock()) blockType = "IF";
            else if (block.isLoopBlock()) blockType = "LOOP";
            else if (block.isElseBlock()) blockType = "ELSE";
            
            sb.append(String.format("Block %d: [%d - %d] Type: %s\n", 
                    i, block.getStartIndex(), block.getEndIndex(), blockType));
            
            sb.append("  Predecessors:");
            for (BasicBlock pred : block.getPredecessors()) {
                sb.append(" Block ").append(blocks.indexOf(pred));
            }
            sb.append("\n");
            
            sb.append("  Successors:");
            for (BasicBlock succ : block.getSuccessors()) {
                sb.append(" Block ").append(blocks.indexOf(succ));
            }
            sb.append("\n\n");
        }
        return sb.toString();
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

    public SsaRegisterSnapshot getRegisterByInstructionIndex(int instructionIndex) {
        Chunk chunk = getContext().getChunk();
        return new SsaRegisterSnapshot(this, chunk, instructionIndex, getContext().getRegisterPrefix());
    }

    public SsaFunction getSsaFunction(String function) {
        return ssaFunctions.get(function);
    }

    public SsaInstruction requireSsaInstruction(String function, int pc) {
        SsaFunction ssaFunction = ssaFunctions.get(function);
        if (ssaFunction == null) {
            throw new IllegalStateException("Missing SSA function for " + function);
        }
        SsaInstruction instruction = ssaFunction.getInstruction(pc);
        if (instruction == null) {
            throw new IllegalStateException("Missing SSA instruction for " + function + " pc=" + pc);
        }
        return instruction;
    }

    public SsaValue requireSsaDefinition(String function, int pc, int register) {
        SsaValue value = requireSsaInstruction(function, pc).getFirstDefForRegister(register);
        if (value == null) {
            throw new IllegalStateException("Missing SSA definition for " + function
                    + " pc=" + pc + " " + RegisterNamePolicy.physicalRegisterName(register));
        }
        return value;
    }

    public SsaValue requireSsaUse(String function, int pc, int register) {
        SsaValue value = requireSsaInstruction(function, pc).getFirstUseForRegister(register);
        if (value == null) {
            throw new IllegalStateException("Missing SSA use for " + function
                    + " pc=" + pc + " " + RegisterNamePolicy.physicalRegisterName(register));
        }
        return value;
    }

    public SsaValue requireSsaValue(String function, int pc, int register) {
        SsaInstruction instruction = requireSsaInstruction(function, pc);
        SsaValue value = instruction.getFirstDefForRegister(register);
        if (value != null) {
            return value;
        }
        value = instruction.getFirstUseForRegister(register);
        if (value != null) {
            return value;
        }
        throw new IllegalStateException("Missing SSA value for " + function
                + " pc=" + pc + " " + RegisterNamePolicy.physicalRegisterName(register));
    }

    public SsaExpressionAnalysis requireSsaExpressionAnalysis(String function) {
        SsaExpressionAnalysis analysis = ssaExpressionAnalyses.get(function);
        if (analysis == null) {
            throw new IllegalStateException("Missing SSA expression analysis for " + function);
        }
        return analysis;
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
