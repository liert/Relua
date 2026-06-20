package com.github.relua.decompiler.pipeline;

import com.github.relua.debug.DecompilerDebugger;
import com.github.relua.model.Chunk;
import com.github.relua.decompiler.BasicBlock;
import com.github.relua.decompiler.ssa.SsaFunction;
import com.github.relua.decompiler.ssa.SsaExpressionAnalysis;
import com.github.relua.ast.AstNode;

import java.util.List;

/**
 * 调试日志适配器，将反编译流水线事件路由给 DecompilerDebugger
 */
public class DecompilerDebugLogger implements PipelineDebugListener {

    @Override
    public void onCFGBuilt(Chunk chunk, List<BasicBlock> basicBlocks, String cfgFormat) {
        DecompilerDebugger.dump("cfg_built_" + chunk.getFunction(), cfgFormat);
    }

    @Override
    public void onSSABuilt(Chunk chunk, SsaFunction ssaFunction) {
        DecompilerDebugger.dump("ssa_built_" + chunk.getFunction(), ssaFunction.format());
    }

    @Override
    public void onSsaExprAnalyzed(Chunk chunk, SsaExpressionAnalysis ssaExpressionAnalysis) {
        DecompilerDebugger.dump("ssa_expr_" + chunk.getFunction(), ssaExpressionAnalysis.format());
    }

    @Override
    public void onSsaVerifyFailed(Chunk chunk, List<String> errors) {
        DecompilerDebugger.dump("ssa_verify_failed_" + chunk.getFunction(), String.join(System.lineSeparator(), errors));
    }

    @Override
    public void onAstConstructed(Chunk chunk, AstNode ast) {
        DecompilerDebugger.dump("ast_constructed_" + chunk.getFunction(), ast);
    }

    @Override
    public void onStageFinished(String stageName, String chunkName, Object state) {
        DecompilerDebugger.dump(stageName + "_" + chunkName, state);
    }
}
