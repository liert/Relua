package com.github.relua.decompiler.pass;

import com.github.relua.ast.Block;

/**
 * 优化和还原 AST 的单一 Pass 接口
 */
public interface DecompilationPass {
    /**
     * 执行优化 pass
     * @param block 待优化的 AST Block
     * @param context 优化运行上下文
     */
    void execute(Block block, PassContext context);

    /**
     * 获取 Pass 名称
     */
    String getName();
}
