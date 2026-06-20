package com.github.relua.decompiler;

import com.github.relua.ast.Block;
import com.github.relua.decompiler.pass.DecompilationPass;
import com.github.relua.decompiler.pass.PassContext;

/**
 * 返回模式窥孔优化 Pass，合并临时寄存器定值与其后的 Return 语句
 */
public class ReturnPatternOptimizationPass implements DecompilationPass {
    private final AstCleanupPass parent;

    public ReturnPatternOptimizationPass(AstCleanupPass parent) {
        this.parent = parent;
    }

    @Override
    public void execute(Block block, PassContext context) {
        parent.optimizeReturnPatterns(block, context.getContext(), context.getOptimizerUpvalues());
    }

    @Override
    public String getName() {
        return "return_opt";
    }
}
