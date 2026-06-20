package com.github.relua.decompiler;

import com.github.relua.ast.Block;
import com.github.relua.decompiler.pass.DecompilationPass;
import com.github.relua.decompiler.pass.PassContext;

/**
 * SSA 寄存器清理 Pass，通过 SsaTemporaryCleanupPass 进行变量收缩与死代码清理
 */
public class SsaCleanupPass implements DecompilationPass {
    private final int index;
    private final SsaTemporaryCleanupPass ssaTemporaryCleanup = new SsaTemporaryCleanupPass();

    public SsaCleanupPass(int index) {
        this.index = index;
    }

    @Override
    public void execute(Block block, PassContext context) {
        ssaTemporaryCleanup.inlineSingleUse(block, context.getContext(), context.getPipeline(),
                context.getCapturedLocalDefinitionPcs());
        ssaTemporaryCleanup.deleteDead(block, context.getContext(), context.getPipeline(),
                context.getCapturedLocalDefinitionPcs(), true);
    }

    @Override
    public String getName() {
        return "ssa_cleanup_" + index;
    }
}
