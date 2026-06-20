package com.github.relua.decompiler;

import com.github.relua.ast.Block;
import com.github.relua.decompiler.pass.DecompilationPass;
import com.github.relua.decompiler.pass.PassContext;
import com.github.relua.decompiler.analysis.StructureRestorer;

/**
 * 结构化控制流还原 Pass，消除 GOTO/Label 并组织 while, repeat-until 和 if-else
 */
public class ControlFlowRestructuringPass implements DecompilationPass {
    @Override
    public void execute(Block block, PassContext context) {
        new StructureRestorer(context.getContext() != null ? context.getContext().getChunk() : null)
                .restructure(block);
    }

    @Override
    public String getName() {
        return "structure_restored";
    }
}
