package com.github.relua.decompiler;

import com.github.relua.ast.Block;
import com.github.relua.decompiler.pass.DecompilationPass;
import com.github.relua.decompiler.pass.PassContext;

/**
 * 死代码消除 Pass，裁撤在 return 等终止性语句之后无法触及的 AST 节点
 */
public class DeadCodeEliminationPass implements DecompilationPass {
    private final int index;
    private final AstCleanupPass parent;

    public DeadCodeEliminationPass(int index, AstCleanupPass parent) {
        this.index = index;
        this.parent = parent;
    }

    @Override
    public void execute(Block block, PassContext context) {
        parent.removeDeadCodeAfterReturn(block);
    }

    @Override
    public String getName() {
        return "dead_code_removed_" + index;
    }
}
