package com.github.relua.decompiler;

import com.github.relua.ast.Block;
import com.github.relua.decompiler.pass.DecompilationPass;
import com.github.relua.decompiler.pass.PassContext;

/**
 * 冗余 NaN / 类型检查简化 Pass
 */
public class NanCheckSimplificationPass implements DecompilationPass {
    private final AstCleanupPass parent;

    public NanCheckSimplificationPass(AstCleanupPass parent) {
        this.parent = parent;
    }

    @Override
    public void execute(Block block, PassContext context) {
        parent.simplifyRedundantNanChecks(block);
    }

    @Override
    public String getName() {
        return "nan_checks_simplified";
    }
}
