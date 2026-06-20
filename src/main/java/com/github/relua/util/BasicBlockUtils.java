package com.github.relua.util;

import java.util.List;

import com.github.relua.decompiler.BasicBlock;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;

public class BasicBlockUtils {
    /**
     * 获取基本块的最后一条指令
     */
    public static Instruction getLastInstruction(BasicBlock block, Chunk chunk) {
        if (block == null)
            return null;
        List<Instruction> instructions = chunk.getInstructions();
        int endIndex = block.getEndIndex();
        if (endIndex >= 0 && endIndex < instructions.size()) {
            return instructions.get(endIndex);
        }
        return null;
    }
}
