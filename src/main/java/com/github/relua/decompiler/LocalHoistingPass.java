package com.github.relua.decompiler;

import com.github.relua.ast.Block;
import com.github.relua.decompiler.pass.DecompilationPass;
import com.github.relua.decompiler.pass.PassContext;
import com.github.relua.decompiler.ssa.SsaAstNameResolver;
import com.github.relua.model.Chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 局部变量声明提升 Pass，推导并插入 local 声明关键字以符合 Lua 作用域语义
 */
public class LocalHoistingPass implements DecompilationPass {
    private final AstCleanupPass parent;
    private final SsaAstNameResolver ssaNameResolver = new SsaAstNameResolver();
    private final SsaTemporaryCleanupPass ssaTemporaryCleanup = new SsaTemporaryCleanupPass();

    public LocalHoistingPass(AstCleanupPass parent) {
        this.parent = parent;
    }

    @Override
    public void execute(Block block, PassContext context) {
        parent.removeUnusedEmptyLocalDeclarations(block);
        
        List<String> params = new ArrayList<>();
        if (context.getContext() != null && context.getContext().getChunk() != null 
                && !"main".equals(context.getContext().getChunk().getFunction())) {
            Chunk functionChunk = context.getContext().getChunk();
            params.addAll(ssaNameResolver.parameterNames(functionChunk.getNumParams()));
        }
        
        Set<String> declared = parent.declareTopLevelLocals(block, params, context.getParentDeclared(), context.getUpvalueNames());
        context.setDeclaredVariables(declared);

        ssaTemporaryCleanup.inlineSingleUse(block, context.getContext(), context.getPipeline(),
                context.getCapturedLocalDefinitionPcs());
        ssaTemporaryCleanup.deleteDead(block, context.getContext(), context.getPipeline(),
                context.getCapturedLocalDefinitionPcs(), true);
    }

    @Override
    public String getName() {
        return "locals_declared";
    }
}
