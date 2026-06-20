package com.github.relua.decompiler;

import com.github.relua.ast.Block;
import com.github.relua.decompiler.pass.DecompilationPass;
import com.github.relua.decompiler.pass.PassContext;
import com.github.relua.decompiler.analysis.DataFlowAnalyzer;

/**
 * 数据流分析与内联优化 Pass，消除死代码并折叠单定值单引用变量
 */
public class DataFlowOptimizationPass implements DecompilationPass {
    private final int index;

    public DataFlowOptimizationPass(int index) {
        this.index = index;
    }

    @Override
    public void execute(Block block, PassContext context) {
        new DataFlowAnalyzer().optimize(block, context.getParentDeclared(),
                context.getOptimizerUpvalues(), context.getCapturedLocalDefinitionPcs());
    }

    @Override
    public String getName() {
        return "dataflow_opt_" + index;
    }
}
