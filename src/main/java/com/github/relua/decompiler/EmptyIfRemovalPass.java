package com.github.relua.decompiler;

import com.github.relua.ast.Block;
import com.github.relua.decompiler.pass.DecompilationPass;
import com.github.relua.decompiler.pass.PassContext;

/**
 * 空条件分支消除 Pass，消除 nil-guard 等模式产生的无意义 If 条件结构
 */
public class EmptyIfRemovalPass implements DecompilationPass {
    private final int index;
    private final AstCleanupPass parent;

    public EmptyIfRemovalPass(int index, AstCleanupPass parent) {
        this.index = index;
        this.parent = parent;
    }

    @Override
    public void execute(Block block, PassContext context) {
        parent.removeEmptyIfBlocks(block);
    }

    @Override
    public String getName() {
        return "empty_if_removed_" + index;
    }
}
