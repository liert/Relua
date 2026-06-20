package com.github.relua.decompiler;

import com.github.relua.ast.Block;
import com.github.relua.ast.TableConstructor;
import com.github.relua.decompiler.pass.DecompilationPass;
import com.github.relua.decompiler.pass.PassContext;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * 最终 AST 清理 Pass，整合临时表消除、汇合 goto 消除以及未引用 local 声明消除
 */
public class AstCleanupFinalPass implements DecompilationPass {
    private final AstCleanupPass parent;

    public AstCleanupFinalPass(AstCleanupPass parent) {
        this.parent = parent;
    }

    @Override
    public void execute(Block block, PassContext context) {
        Set<TableConstructor> consumedTables = Collections.newSetFromMap(new IdentityHashMap<>());
        parent.collectConsumedTables(block, consumedTables, true);
        parent.removeConsumedTemporaryTables(block, consumedTables);
        parent.removeJoinGotos(block);
        parent.removeUnusedEmptyLocalDeclarations(block);
    }

    @Override
    public String getName() {
        return "ast_cleanup_final";
    }
}
