package com.github.relua.gui.utils;

import com.github.relua.gui.views.GraphVisualizationView;
import com.github.relua.gui.views.GraphVisualizationView.NodeType;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.LuacFile;
import com.github.relua.decompiler.BasicBlock;
import com.github.relua.decompiler.InstructionHandler;
import com.github.relua.decompiler.LuaCodeGenerator;

import java.util.List;

/**
 * CFG到图形的转换器，用于将Relua的控制流图转换为JavaFX Canvas的图形表示
 */
public class CFGGraphConverter {
    // 图形可视化视图
    private final GraphVisualizationView graphView;
    
    /**
     * 构造函数
     * @param graphView 图形可视化视图
     */
    public CFGGraphConverter(GraphVisualizationView graphView) {
        this.graphView = graphView;
    }
    
    /**
     * 将CFG转换为图形
     * @param chunk 代码块
     */
    public void convertToGraph(Chunk chunk) {
        // 清空现有图形
        graphView.clearGraph();
        // 切换到图形视图模式
        graphView.switchToGraphMode();
        
        System.out.println("CFGGraphConverter.convertToGraph(Chunk): chunk = " + chunk);
        
        if (chunk != null) {
            System.out.println("CFGGraphConverter.convertToGraph(Chunk): chunk.getInstructions().size() = " + chunk.getInstructions().size());

            LuaCodeGenerator generator = new LuaCodeGenerator(chunk);
            InstructionHandler handler = generator.getInstructionHandler(chunk.getFunction());
            if (handler == null) {
                graphView.addNode("No CFG handler found\nPlease check if the input file is valid", GraphVisualizationView.NodeType.NORMAL);
                graphView.applyLayout();
                return;
            }

            handler.process(chunk);
            List<BasicBlock> basicBlocks = handler.getBasicBlocks(chunk);
            System.out.println("CFGGraphConverter.convertToGraph(Chunk): basicBlocks.size() = " + basicBlocks.size());

            convertCFGToGraph(basicBlocks, chunk);
        }
    }
    
    /**
     * 将CFG转换为图形
     * @param luacFile Luac文件
     */
    public void convertToGraph(LuacFile luacFile) {
        if (luacFile != null) {
            convertToGraph(luacFile.getMainChunk());
        }
    }
    
    /**
     * 将CFG转换为图形
     * @param basicBlocks 基本块列表
     * @param chunk 代码块
     */
    public void convertCFGToGraph(List<BasicBlock> basicBlocks, Chunk chunk) {
        // 清空现有图形
        graphView.clearGraph();

        System.out.println("CFGGraphConverter.convertCFGToGraph: basicBlocks.size() = " + basicBlocks.size());
        
        if (basicBlocks.isEmpty()) {
            // 如果基本块为空，添加一个提示节点
            graphView.addNode("No basic blocks found\nPlease check if the input file is valid", GraphVisualizationView.NodeType.NORMAL);
            graphView.applyLayout();
            return;
        }
        
        // 为每个基本块创建节点
        for (int i = 0; i < basicBlocks.size(); i++) {
            BasicBlock block = basicBlocks.get(i);
            
            // 生成节点标签
            String label = generateBlockLabel(block, chunk, i);
            
            // 根据基本块类型选择节点类型
            NodeType nodeType = NodeType.NORMAL;
            if (block.isLoopBlock()) {
                nodeType = NodeType.LOOP_BLOCK;
            } else if (block.isElseBlock()) {
                nodeType = NodeType.ELSE_BLOCK;
            } else if (block.isIfBlock()) {
                nodeType = NodeType.IF_BLOCK;
            }
            
            // 添加节点
            graphView.addNode(label, nodeType);
        }
        
        // 构建控制流边
        for (int i = 0; i < basicBlocks.size(); i++) {
            BasicBlock block = basicBlocks.get(i);
            List<BasicBlock> successors = block.getSuccessors();
            
            for (BasicBlock successor : successors) {
                int successorIndex = basicBlocks.indexOf(successor);
                if (successorIndex != -1) {
                    graphView.addEdge(i, successorIndex);
                }
            }
        }
        
        // 应用布局
        graphView.applyLayout();
    }
    
    /**
     * 生成基本块的节点标签
     * @param block 基本块
     * @param chunk 代码块
     * @param blockIndex 基本块索引
     * @return 节点标签
     */
    private String generateBlockLabel(BasicBlock block, Chunk chunk, int blockIndex) {
        StringBuilder label = new StringBuilder();
        
        // 添加基本块ID和范围
        label.append("BB").append(blockIndex).append("\n");
        label.append("[")
             .append(block.getStartIndex())
             .append("-")
             .append(block.getEndIndex())
             .append("]\n");
        label.append("\n");
        
        // 添加字节码指令序列
        List<Instruction> instructions = chunk.getInstructions();
        for (int i = block.getStartIndex(); i <= block.getEndIndex(); i++) {
            if (i < instructions.size()) {
                Instruction inst = instructions.get(i);
                // 使用 BytecodeFormatter 格式化指令，获取包含语义注释的汇编代码，并将制表符替换为空格以保证对齐
                String instStr = com.github.relua.util.BytecodeFormatter.formatInstruction(chunk, inst, i)
                        .replace("\t", "    ");
                label.append(instStr).append("\n");
            }
        }
        
        return label.toString();
    }
}
