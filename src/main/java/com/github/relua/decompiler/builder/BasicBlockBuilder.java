package com.github.relua.decompiler.builder;

import java.security.cert.PKIXRevocationChecker.Option;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.relua.decompiler.BasicBlock;
import com.github.relua.decompiler.BytecodeFormatter;
import com.github.relua.decompiler.DecompilerPipeline;
import com.github.relua.decompiler.InstructionHandler;
import com.github.relua.log.Logger;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Instruction.Opcode;

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
            } else if (opcode == Opcode.EQ || opcode == Opcode.LT || opcode == Opcode.LE) {
                if (inst.getC() != 0) {
                    int jumpTarget = i + 1 + inst.getC();
                    if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                        isJumpTarget[jumpTarget] = true;
                    }
                }
            } else if (opcode == Opcode.FORLOOP) {
                // FORLOOP指令：pc += sBx
                int jumpTarget = i + 1 + inst.getSBx();
                if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                    isJumpTarget[jumpTarget] = true;
                }
            } else if (opcode == Opcode.FORPREP) {
                // FORPREP指令：pc += sBx
                int jumpTarget = i + 1 + inst.getSBx();
                if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                    isJumpTarget[jumpTarget] = true;
                }
            } else if (opcode == Opcode.TFORLOOP) {
                // TFORLOOP指令：如果条件满足，pc += sBx
                int jumpTarget = i + 1 + inst.getSBx();
                if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                    isJumpTarget[jumpTarget] = true;
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
            if (isBlockEndInstruction(opcode)) {
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
        return opcode == Opcode.JMP || opcode == Opcode.RETURN ||
                opcode == Opcode.FORLOOP || opcode == Opcode.FORPREP ||
                opcode == Opcode.TFORLOOP || opcode == Opcode.TAILCALL ||
                opcode == Opcode.EQ || opcode == Opcode.LT || opcode == Opcode.LE;
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
