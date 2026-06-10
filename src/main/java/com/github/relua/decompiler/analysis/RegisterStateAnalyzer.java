package com.github.relua.decompiler.analysis;

import java.util.ArrayList;
import java.util.List;
import com.github.relua.decompiler.BasicBlock;
import com.github.relua.decompiler.DecompilerPipeline;
import com.github.relua.log.Logger;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Register;
import com.github.relua.model.Register.RegisterEntity;
import com.github.relua.util.RegisterUtils;

public class RegisterStateAnalyzer {
    private final DecompilerPipeline pipeline;
    private final List<Register> inStates = new ArrayList<>(); // 每条指令执行前的寄存器状态
    private final List<Register> outStates = new ArrayList<>(); // 每条指令执行后的寄存器状态

    public RegisterStateAnalyzer(DecompilerPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public void analyze(Chunk chunk) {
        iterativeDataFlowAnalysis(chunk);
    }

    /**
     * 执行迭代数据流分析
     * 
     * @param chunk 代码块
     */
    private void iterativeDataFlowAnalysis(Chunk chunk) {
        List<Instruction> instructions = chunk.getInstructions();
        int numInstructions = instructions.size();

        // 初始化所有指令的输入和输出状态
        resetRegisterStates(numInstructions);

        boolean changed = true;
        int maxIterations = 20; // 避免死循环
        int iter = 0;

        while (changed && iter < maxIterations) {
            changed = false;
            iter++;

            // 遍历所有基本块
            for (BasicBlock block : pipeline.getBasicBlocks(chunk.getFunction())) {
                // 合并前驱块的输出状态作为当前块的输入状态
                Register mergedInput = RegisterUtils.mergePredecessors(block);

                // 先更新块的输入状态（如果有变化）
                if (!mergedInput.equals(block.getInputState())) {
                    block.setInputState(mergedInput);
                    changed = true;
                }

                // 使用更新后的输入状态来处理块内指令
                Register currentState = new Register(block.getInputState());
                // 当为第一条指令时设置初始寄存器状态
                if (block.getStartIndex() == 0) {
                    Register register = inStates.get(0);
                    for (RegisterEntity entity : register.getRegisterEntities()) {
                        currentState.setRegisterEntity(entity.getIndex(), entity.getValue(), entity.getType(), entity.getFromType());
                    }
                }
                
                // 处理块内指令
                for (int i = block.getStartIndex(); i <= block.getEndIndex();) {
                    if (i < numInstructions) {
                        int originalIndex = i;
                        // 更新指令i的输入状态
                        if (!currentState.equals(inStates.get(i))) {
                            inStates.set(i, new Register(currentState));
                            changed = true;
                        }

                        // 处理指令，更新当前状态
                        int nextI = pipeline.processInstruction(chunk, instructions.get(i), i, currentState);

                        // 更新指令i的输出状态
                        if (!currentState.equals(outStates.get(originalIndex))) {
                            outStates.set(originalIndex, new Register(currentState));
                            changed = true;
                        }
                        
                        i = nextI;
                    } else {
                        break;
                    }
                }

                // 更新块的输出状态
                if (!currentState.equals(block.getOutputState())) {
                    block.setOutputState(currentState);
                    changed = true;
                }
            }
        }
    }

    private void resetRegisterStates(int numInstructions) {
        inStates.clear();
        outStates.clear();
        // 初始化所有指令的输入和输出状态

        for (int i = 0; i < numInstructions; i++) {
            inStates.add(new Register());
            outStates.add(new Register());
        }

        Register initRegister = pipeline.getContext().getRegister();
        // Logger.debug("初始寄存器状态: " + initRegister);
        if (initRegister == null) {
            return;
        }
        for (int i = 0; i < initRegister.getRegisterCount(); i++) {
            RegisterEntity entity = initRegister.getRegisterEntity(i);
            inStates.get(0).setRegisterEntity(i, entity.getValue(), entity.getType(), entity.getFromType());
        }
        // Logger.debug("入口寄存器状态: " + inStates.get(0));
    }

    /**
     * 根据指令索引获取寄存器状态
     * 
     * @param instructionIndex 指令索引
     * @return 该指令对应的寄存器
     */
    public Register getRegisterByInstructionIndex(int instructionIndex) {
        if (instructionIndex >= 0 && instructionIndex < inStates.size()) {
            return new Register(inStates.get(instructionIndex));
        }
        return new Register();
    }
}
