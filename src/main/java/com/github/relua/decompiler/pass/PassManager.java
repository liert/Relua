package com.github.relua.decompiler.pass;

import com.github.relua.ast.Block;
import com.github.relua.log.Logger;
import com.github.relua.decompiler.pipeline.PipelineDebugListener;

import java.util.ArrayList;
import java.util.List;

/**
 * 优化 Pass 管理器，负责链式执行各个 AST 优化与结构还原 pass
 */
public class PassManager {
    private final List<DecompilationPass> passes = new ArrayList<>();

    public void addPass(DecompilationPass pass) {
        if (pass != null) {
            passes.add(pass);
        }
    }

    /**
     * 顺序执行所有注册的 pass
     * @param block AST 根 Block
     * @param context Pass 运行上下文
     */
    public void execute(Block block, PassContext context) {
        if (block == null || context == null) {
            return;
        }

        String funcName = (context.getContext() != null && context.getContext().getChunk() != null)
                ? context.getContext().getChunk().getFunction()
                : "unknown";

        for (DecompilationPass pass : passes) {
            long start = System.currentTimeMillis();
            try {
                pass.execute(block, context);
                long duration = System.currentTimeMillis() - start;
                
                // 广播阶段结束事件给所有 debug 监听器
                if (context.getPipeline() != null && context.getPipeline().getGenerator() != null) {
                    for (PipelineDebugListener listener : context.getPipeline().getGenerator().getDebugListeners()) {
                        listener.onStageFinished(pass.getName(), funcName, block);
                    }
                }
                
                Logger.debug(String.format("Executed pass %s on chunk %s in %d ms", pass.getName(), funcName, duration));
            } catch (Exception e) {
                Logger.error(String.format("Error executing pass %s on chunk %s: %s", pass.getName(), funcName, e.getMessage()));
                throw e;
            }
        }
    }
}
