package com.github.relua.decompiler.ssa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.github.relua.decompiler.BasicBlock;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;

class SsaBuilderTest {
    @Test
    void insertsPhiAtBranchMergeAndRenamesUseToPhiValue() {
        Chunk chunk = new Chunk();
        chunk.setFunction("ssa_test");
        chunk.addInstruction(abc(0, Opcode.LOADBOOL, 0, 1, 0));
        chunk.addInstruction(abx(1, Opcode.LOADK, 1, 0));
        chunk.addInstruction(abx(2, Opcode.LOADK, 1, 1));
        chunk.addInstruction(abc(3, Opcode.RETURN, 1, 2, 0));

        BasicBlock entry = block(0, 0);
        BasicBlock thenBlock = block(1, 1);
        BasicBlock elseBlock = block(2, 2);
        BasicBlock merge = block(3, 3);
        entry.addSuccessor(thenBlock);
        entry.addSuccessor(elseBlock);
        thenBlock.addSuccessor(merge);
        elseBlock.addSuccessor(merge);

        SsaFunction function = new SsaBuilder().build(chunk, Arrays.asList(entry, thenBlock, elseBlock, merge));
        SsaPhi phi = function.getBlock(merge).getPhi(1);

        assertNotNull(phi, "R1 must be represented by an explicit phi at the branch merge");
        assertNotNull(phi.getTarget(), "phi must define a new SSA value");
        assertEquals(2, phi.getIncoming().size(), "phi must have one input per predecessor");
        assertFalse(phi.getIncoming().get(thenBlock).equals(phi.getIncoming().get(elseBlock)),
                "branch definitions must remain distinct SSA values");
        assertEquals(phi.getTarget(), function.getInstruction(3).getUses().get(0),
                "return must read the merged phi value, not a physical register fallback");
        assertTrue(SsaVerifier.verify(function).isEmpty(), "synthetic branch SSA must satisfy invariants");
    }

    private static BasicBlock block(int start, int end) {
        BasicBlock block = new BasicBlock(start);
        block.setEndIndex(end);
        return block;
    }

    private static Instruction abc(int pc, Opcode opcode, int a, int b, int c) {
        int code = opcode.ordinal() | (a << 6) | (c << 14) | (b << 23);
        return new Instruction(pc, code);
    }

    private static Instruction abx(int pc, Opcode opcode, int a, int bx) {
        int code = opcode.ordinal() | (a << 6) | (bx << 14);
        return new Instruction(pc, code);
    }
}
