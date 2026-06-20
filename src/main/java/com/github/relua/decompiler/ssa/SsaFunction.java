package com.github.relua.decompiler.ssa;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.relua.decompiler.BasicBlock;
import com.github.relua.model.Chunk;

public final class SsaFunction {
    private final Chunk chunk;
    private final Map<BasicBlock, SsaBlock> blocks = new LinkedHashMap<>();
    private final Map<Integer, SsaInstruction> instructionByPc = new LinkedHashMap<>();
    private final Map<SsaValue, SsaInstruction> definingInstructionByValue = new HashMap<>();
    private final Map<SsaValue, SsaPhi> definingPhiByValue = new HashMap<>();
    private final Map<SsaValue, List<SsaInstruction>> instructionUsesByValue = new HashMap<>();
    private final Map<SsaValue, List<SsaPhi>> phiUsesByValue = new HashMap<>();

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

    public void rebuildDefUse() {
        definingInstructionByValue.clear();
        definingPhiByValue.clear();
        instructionUsesByValue.clear();
        phiUsesByValue.clear();

        for (SsaBlock block : blocks.values()) {
            for (SsaPhi phi : block.getPhis()) {
                if (phi.getTarget() != null) {
                    definingPhiByValue.put(phi.getTarget(), phi);
                }
                for (SsaValue incoming : phi.getIncoming().values()) {
                    addPhiUse(incoming, phi);
                }
            }
            for (SsaInstruction instruction : block.getInstructions()) {
                for (SsaValue def : instruction.getDefs()) {
                    definingInstructionByValue.put(def, instruction);
                }
                for (SsaValue use : instruction.getUses()) {
                    addInstructionUse(use, instruction);
                }
            }
        }
    }

    public SsaInstruction getDefiningInstruction(SsaValue value) {
        return definingInstructionByValue.get(value);
    }

    public SsaPhi getDefiningPhi(SsaValue value) {
        return definingPhiByValue.get(value);
    }

    public List<SsaInstruction> getInstructionUses(SsaValue value) {
        List<SsaInstruction> uses = instructionUsesByValue.get(value);
        return uses != null ? Collections.unmodifiableList(uses) : Collections.<SsaInstruction>emptyList();
    }

    public List<SsaPhi> getPhiUses(SsaValue value) {
        List<SsaPhi> uses = phiUsesByValue.get(value);
        return uses != null ? Collections.unmodifiableList(uses) : Collections.<SsaPhi>emptyList();
    }

    public int getUseCount(SsaValue value) {
        return getInstructionUses(value).size() + getPhiUses(value).size();
    }

    private void addInstructionUse(SsaValue value, SsaInstruction instruction) {
        if (value == null) {
            return;
        }
        List<SsaInstruction> uses = instructionUsesByValue.get(value);
        if (uses == null) {
            uses = new ArrayList<>();
            instructionUsesByValue.put(value, uses);
        }
        uses.add(instruction);
    }

    private void addPhiUse(SsaValue value, SsaPhi phi) {
        if (value == null) {
            return;
        }
        List<SsaPhi> uses = phiUsesByValue.get(value);
        if (uses == null) {
            uses = new ArrayList<>();
            phiUsesByValue.put(value, uses);
        }
        uses.add(phi);
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
