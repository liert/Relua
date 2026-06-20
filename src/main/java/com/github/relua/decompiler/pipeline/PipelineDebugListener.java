package com.github.relua.decompiler.pipeline;

import com.github.relua.model.Chunk;
import com.github.relua.decompiler.BasicBlock;
import com.github.relua.decompiler.ssa.SsaFunction;
import com.github.relua.decompiler.ssa.SsaExpressionAnalysis;
import com.github.relua.ast.AstNode;
import com.github.relua.ast.Block;

import java.util.List;

/**
 * 反编译流水线调试监听接口，用于解耦调试信息的输出与存储
 */
public interface PipelineDebugListener {
    void onCFGBuilt(Chunk chunk, List<BasicBlock> basicBlocks, String cfgFormat);
    
    void onSSABuilt(Chunk chunk, SsaFunction ssaFunction);
    
    void onSsaExprAnalyzed(Chunk chunk, SsaExpressionAnalysis ssaExpressionAnalysis);
    
    void onSsaVerifyFailed(Chunk chunk, List<String> errors);
    
    void onAstConstructed(Chunk chunk, AstNode ast);
    
    void onStageFinished(String stageName, String chunkName, Object state);
}
