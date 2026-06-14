package com.github.relua.decompiler.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.relua.decompiler.BasicBlock;
import com.github.relua.decompiler.DecompilerPipeline;
import com.github.relua.log.Logger;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;

public class BasicBlockBuilder {
    private final DecompilerPipeline pipeline;
    private final List<BasicBlock> basicBlocks = new ArrayList<>();
    private final Map<Integer, BasicBlock> instructionToBlockMap = new HashMap<>();

    public BasicBlockBuilder(DecompilerPipeline pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * 构建基本块
     * 
     * @param chunk 代码块
     */
    public void build(Chunk chunk) {
        List<Instruction> instructions = chunk.getInstructions();
        if (instructions.isEmpty()) {
            Logger.debug("未检测到指令");
            return;
        }

        // 首先标记所有跳转目标指令
        boolean[] isJumpTarget = new boolean[instructions.size()];
        for (int i = 0; i < instructions.size(); i++) {
            Instruction inst = instructions.get(i);
            Opcode opcode = inst.getOpcode();

            // 检查所有可能产生跳转的指令
            if (opcode == Opcode.JMP) {
                int jumpTarget = i + 1 + inst.getSBx();
                if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                    isJumpTarget[jumpTarget] = true;
                }
                pipeline.getContext().addLabelPC(jumpTarget);
            } else if (opcode == Opcode.FORLOOP) {
                // FORLOOP指令：pc += sBx
                int jumpTarget = inst.getSBx() == 0 ? inst.getNumericForPrepTarget(instructions, i) : i + 1 + inst.getSBx();
                if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                    isJumpTarget[jumpTarget] = true;
                }
                pipeline.getContext().addLabelPC(jumpTarget);
            } else if (opcode == Opcode.FORPREP) {
                // FORPREP指令：pc += sBx
                int jumpTarget = inst.getSBx() == 0 ? inst.getNumericForLoopTarget(instructions, i) : i + 1 + inst.getSBx();
                if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                    isJumpTarget[jumpTarget] = true;
                }
                pipeline.getContext().addLabelPC(jumpTarget);
            } else if (opcode == Opcode.TFORLOOP) {
                // TFORLOOP指令：如果条件满足，pc += sBx
                int jumpTarget = i + 1 + inst.getSBx();
                if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                    isJumpTarget[jumpTarget] = true;
                }
                // TFORLOOP 的跳转目标也需要注册为 label PC，
                // 使得内层 for-in 的 backward JMP 能找到对应的 LabelStatement
                pipeline.getContext().addLabelPC(jumpTarget);
                // 注册 TFORLOOP 区域 PC，防止被 if-body 吞没
                pipeline.getContext().addTforRegionPC(i);        // TFORLOOP 本身
                pipeline.getContext().addTforRegionPC(jumpTarget); // 跳转目标 label
                if (i + 1 < instructions.size()) {
                    pipeline.getContext().addTforRegionPC(i + 1); // TFORLOOP 后面的 backward JMP
                }
            } else if (opcode == Opcode.LOADBOOL) {
                if (inst.getC() != 0) {
                    int jumpTarget = i + 2;
                    if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                        isJumpTarget[jumpTarget] = true;
                    }
                    pipeline.getContext().addLabelPC(jumpTarget);
                }
            }
        }

        // 创建基本块
        BasicBlock currentBlock = new BasicBlock(0);
        basicBlocks.add(currentBlock);

        for (int i = 0; i < instructions.size(); i++) {
            // Logger.debug(BytecodeFormatter.formatInstruction(chunk, instructions.get(i), i));
            if (instructionToBlockMap.containsKey(i)) {
                currentBlock = instructionToBlockMap.get(i);
            } else {
                // 如果当前指令是跳转目标，创建新的基本块
                if (isJumpTarget[i] && i > 0) {
                    currentBlock = new BasicBlock(i);
                    basicBlocks.add(currentBlock);
                }
                instructionToBlockMap.put(i, currentBlock);
            }
            currentBlock.setEndIndex(i);

            Opcode opcode = instructions.get(i).getOpcode();
            // Logger.debug(String.format("[%s] : %s", i, opcode.toString()));

            // 检查是否是基本块结束指令
            boolean isEnd = isBlockEndInstruction(opcode);
            if (opcode == Opcode.LOADBOOL && instructions.get(i).getC() != 0) {
                isEnd = true;
            }
            if (isEnd) {
                if (i + 1 < instructions.size()) {
                    currentBlock = new BasicBlock(i + 1);
                    basicBlocks.add(currentBlock);
                    instructionToBlockMap.put(i + 1, currentBlock);
                }
            }
        }
    }

    /**
     * 检查是否是基本块结束指令
     * 
     * @param opcode 操作码
     * @return 是否是基本块结束指令
     */
    private boolean isBlockEndInstruction(Opcode opcode) {
        return opcode == Opcode.JMP || 
                opcode == Opcode.FORLOOP || opcode == Opcode.FORPREP ||
                opcode == Opcode.TFORLOOP || 
                opcode == Opcode.EQ || opcode == Opcode.LT || opcode == Opcode.LE ||
                opcode == Opcode.RETURN || opcode == Opcode.TAILCALL;
    }

    public List<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    public Map<Integer, BasicBlock> getInstructionToBlockMap() {
        return instructionToBlockMap;
    }

     /**
     * 根据起始索引获取基本块
     */
    public BasicBlock getBlockByStartIndex(int startIndex) {
        for (BasicBlock block : basicBlocks) {
            if (block.getStartIndex() == startIndex) {
                return block;
            }
        }
        return null;
    }
}

