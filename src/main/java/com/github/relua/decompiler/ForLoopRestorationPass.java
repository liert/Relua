package com.github.relua.decompiler;

import com.github.relua.ast.Block;
import com.github.relua.decompiler.pass.DecompilationPass;
import com.github.relua.decompiler.pass.PassContext;
import com.github.relua.decompiler.analysis.StructureRestorer;

/**
 * 循环结构恢复 Pass，使用 StructureRestorer 恢复 numeric/generic For 循环
 */
public class ForLoopRestorationPass implements DecompilationPass {
    private final int index;

    public ForLoopRestorationPass(int index) {
        this.index = index;
    }

    @Override
    public void execute(Block block, PassContext context) {
        new StructureRestorer(context.getContext() != null ? context.getContext().getChunk() : null)
                .restoreNumericForLoops(block);
    }

    @Override
    public String getName() {
        return "numeric_for_restored_" + index;
    }
}
