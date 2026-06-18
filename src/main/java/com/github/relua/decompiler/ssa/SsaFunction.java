package com.github.relua.decompiler.ssa;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.relua.decompiler.BasicBlock;
import com.github.relua.model.Chunk;

public final class SsaFunction {
    private final Chunk chunk;
    private final Map<BasicBlock, SsaBlock> blocks = new LinkedHashMap<>();
    private final Map<Integer, SsaInstruction> instructionByPc = new LinkedHashMap<>();

    public SsaFunction(Chunk chunk) {
        this.chunk = chunk;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public SsaBlock getOrCreateBlock(BasicBlock basicBlock) {
        SsaBlock block = blocks.get(basicBlock);
        if (block == null) {
            block = new SsaBlock(basicBlock);
            blocks.put(basicBlock, block);
        }
        return block;
    }

    public SsaBlock getBlock(BasicBlock basicBlock) {
        return blocks.get(basicBlock);
    }

    public Collection<SsaBlock> getBlocks() {
        return Collections.unmodifiableCollection(blocks.values());
    }

    public void addInstruction(SsaInstruction instruction) {
        instructionByPc.put(instruction.getPc(), instruction);
    }

    public SsaInstruction getInstruction(int pc) {
        return instructionByPc.get(pc);
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("SSA Chunk: ").append(chunk.getFunction()).append('\n');
        for (SsaBlock block : blocks.values()) {
            BasicBlock bb = block.getBasicBlock();
            sb.append("block ").append(bb.getStartIndex()).append("..").append(bb.getEndIndex()).append('\n');
            for (SsaPhi phi : block.getPhis()) {
                sb.append("  ").append(phi).append('\n');
            }
            for (SsaInstruction instruction : block.getInstructions()) {
                sb.append("  ").append(instruction).append('\n');
            }
        }
        return sb.toString();
    }
}
