package com.github.relua.decompiler.analysis;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.relua.decompiler.BasicBlock;
import com.github.relua.decompiler.DecompilerPipeline;
import com.github.relua.decompiler.IfElsePattern;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;
import com.github.relua.util.BasicBlockUtils;

public class ControlFlowAnalyzer {
    private final DecompilerPipeline pipeline;

    public ControlFlowAnalyzer(DecompilerPipeline pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * 分析控制流，识别if-else和循环结构
     * 
     * @param chunk 代码块
     */
    public void analyze(Chunk chunk) {
        List<Instruction> instructions = chunk.getInstructions();

        // 首先标记所有包含条件指令的块
        for (BasicBlock block : pipeline.getBasicBlocks(chunk.getFunction())) {
            // 检查块内是否包含条件指令
            boolean hasConditionInstruction = false;
            for (int i = block.getStartIndex(); i <= block.getEndIndex(); i++) {
                if (i < instructions.size()) {
                    Opcode opcode = instructions.get(i).getOpcode();
                    if (opcode == Opcode.TEST || opcode == Opcode.TESTSET ||
                            opcode == Opcode.EQ || opcode == Opcode.LT || opcode == Opcode.LE) {
                        hasConditionInstruction = true;
                        break;
                    }
                }
            }

            // 如果包含条件指令，标记为if块
            if (hasConditionInstruction) {
                block.setIfBlock(true);
            }
        }

        // 使用新的if-else识别算法
        for (BasicBlock block : pipeline.getBasicBlocks(chunk.getFunction())) {
            // 检测if-else结构
            IfElsePattern pattern = detectIfElse(block, chunk);
            if (pattern != null) {
                // 标记为if块
                pattern.testBlock.setIfBlock(true);

                // 标记then块
                pattern.thenBlock.setIfBlock(true);

                // 标记else块
                pattern.elseBlock.setElseBlock(true);

                // 标记end块
                pattern.endBlock.setIfBlock(true);

                // 打印识别到的if-else结构
                System.out.println("识别到if-else结构:");
                System.out.println(
                        "  testBlock: " + pattern.testBlock.getStartIndex() + "-" + pattern.testBlock.getEndIndex());
                System.out.println(
                        "  thenBlock: " + pattern.thenBlock.getStartIndex() + "-" + pattern.thenBlock.getEndIndex());
                System.out.println(
                        "  elseBlock: " + pattern.elseBlock.getStartIndex() + "-" + pattern.elseBlock.getEndIndex());
                System.out.println(
                        "  endBlock: " + pattern.endBlock.getStartIndex() + "-" + pattern.endBlock.getEndIndex());
            }
        }

        // 使用新的循环检测算法
        // if (!basicBlocks.isEmpty()) {
        // // 计算支配关系
        // BasicBlock entry = basicBlocks.get(0); // 假设第一个基本块是入口
        // Map<BasicBlock, Set<BasicBlock>> dom = computeDominators(basicBlocks, entry);

        // // 查找出口基本块
        // BasicBlock exit = findExitBlock(basicBlocks);
        // System.out.println("出口基本块: " + exit.getStartIndex() + "-" +
        // exit.getEndIndex());

        // // 计算后支配关系
        // Map<BasicBlock, Set<BasicBlock>> postDom = computePostDominators(basicBlocks,
        // exit);

        // // 检测SESE区域
        // List<SESERegion> regions = detectSESERegions(basicBlocks, dom, postDom,
        // chunk);

        // // 打印检测到的SESE区域
        // System.out.println("检测到的SESE区域数量: " + regions.size());
        // for (int i = 0; i < regions.size(); i++) {
        // SESERegion region = regions.get(i);
        // System.out.println("SESE区域 " + (i + 1) + ":");
        // System.out.println(" 类型: " + region.getType());
        // System.out
        // .println(" 入口: " + region.getEntry().getStartIndex() + "-" +
        // region.getEntry().getEndIndex());
        // System.out.println(" 出口: " + region.getExit().getStartIndex() + "-" +
        // region.getExit().getEndIndex());
        // System.out.println(" 块数量: " + region.getBlocks().size());
        // System.out.print(" 块范围: ");
        // for (BasicBlock regionBlock : region.getBlocks()) {
        // System.out.print(regionBlock.getStartIndex() + "-" +
        // regionBlock.getEndIndex() + " ");
        // }
        // System.out.println();
        // }

        // // 折叠SESE区域
        // List<SESERegion> collapsedRegions = collapseRegions(regions, dom, postDom);

        // // 打印折叠后的SESE区域
        // System.out.println("折叠后的SESE区域数量: " + collapsedRegions.size());
        // for (int i = 0; i < collapsedRegions.size(); i++) {
        // SESERegion region = collapsedRegions.get(i);
        // System.out.println("折叠后的SESE区域 " + (i + 1) + ":");
        // System.out.println(" 类型: " + region.getType());
        // System.out
        // .println(" 入口: " + region.getEntry().getStartIndex() + "-" +
        // region.getEntry().getEndIndex());
        // System.out.println(" 出口: " + region.getExit().getStartIndex() + "-" +
        // region.getExit().getEndIndex());
        // System.out.println(" 块数量: " + region.getBlocks().size());
        // System.out.print(" 块范围: ");
        // for (BasicBlock regionBlock : region.getBlocks()) {
        // System.out.print(regionBlock.getStartIndex() + "-" +
        // regionBlock.getEndIndex() + " ");
        // }
        // System.out.println();
        // }

        // // 生成AST节点
        // AstNode ast = generateAST(collapsedRegions, chunk);
        // System.out.println("AST生成完成，根节点类型: " + ast.type);
        // if (ast instanceof Block) {
        // System.out.println("AST子节点数量: " + ((Block) ast).statements.size());
        // }

        // // 查找回边
        // List<BasicBlock[]> backEdges = findBackEdges(basicBlocks, dom);

        // // 构建并标记自然循环
        // Set<BasicBlock> allLoopBlocks = new HashSet<>();
        // for (BasicBlock[] backEdge : backEdges) {
        // BasicBlock u = backEdge[0];
        // BasicBlock v = backEdge[1];

        // // 构建自然循环
        // Set<BasicBlock> loop = buildNaturalLoop(u, v);

        // // 只有当循环包含至少两个块时，才标记为循环块
        // if (loop.size() >= 2) {
        // // 标记循环块
        // for (BasicBlock loopBlock : loop) {
        // loopBlock.setLoopBlock(true);
        // allLoopBlocks.add(loopBlock);
        // }

        // // 打印识别到的循环
        // System.out.println("识别到循环:");
        // System.out.println(" 循环头: " + v.getStartIndex() + "-" + v.getEndIndex());
        // System.out.println(" 循环块数量: " + loop.size());
        // System.out.print(" 循环块范围: ");
        // for (BasicBlock loopBlock : loop) {
        // System.out.print(loopBlock.getStartIndex() + "-" + loopBlock.getEndIndex() +
        // " ");
        // }
        // System.out.println();
        // }
        // }

        // // 清除非循环块的循环标记
        // for (BasicBlock block : basicBlocks) {
        // if (!allLoopBlocks.contains(block)) {
        // block.setLoopBlock(false);
        // }
        // }
        // }
    }

    /**
     * 检测if-else结构
     */
    public IfElsePattern detectIfElse(BasicBlock testBlock, Chunk chunk) {
        Instruction last = BasicBlockUtils.getLastInstruction(testBlock, chunk);
        if (last == null || (last.getOpcode() != Opcode.TEST && last.getOpcode() != Opcode.TESTSET)) {
            return null;
        }

        // 检查下一条指令是否是JMP
        int testIndex = testBlock.getEndIndex();
        if (testIndex + 1 >= chunk.getInstructions().size()) {
            return null;
        }

        Instruction nextInst = chunk.getInstructions().get(testIndex + 1);
        if (nextInst.getOpcode() != Opcode.JMP) {
            return null;
        }

        // 获取JMP指令所在的基本块
        BasicBlock jmpBlock = pipeline.getBasicBlock(chunk.getFunction(), testIndex + 1);
        if (jmpBlock == null) {
            return null;
        }

        // JMP块应该有两个后继：then分支和else分支
        List<BasicBlock> jmpSuccessors = jmpBlock.getSuccessors();
        if (jmpSuccessors.size() < 1) {
            return null;
        }

        // 确定thenBlock和elseBlock
        BasicBlock thenBlock = null;
        BasicBlock elseBlock = null;

        // 对于TEST+JMP组合，then分支是testIndex+2的基本块
        int thenIndex = testIndex + 2;
        if (thenIndex < chunk.getInstructions().size()) {
            thenBlock = pipeline.getBasicBlock(chunk.getFunction(), thenIndex);
        }

        // else分支是JMP的跳转目标
        int elseTarget = testIndex + 1 + nextInst.getSBx();
        if (elseTarget >= 0 && elseTarget < chunk.getInstructions().size()) {
            elseBlock = pipeline.getBasicBlock(chunk.getFunction(), elseTarget);
        }

        // 如果无法通过索引确定，则使用JMP块的后继
        if (thenBlock == null && jmpSuccessors.size() >= 1) {
            thenBlock = jmpSuccessors.get(0);
        }
        if (elseBlock == null && jmpSuccessors.size() >= 2) {
            elseBlock = jmpSuccessors.get(1);
        }

        // 确保thenBlock和elseBlock都存在
        if (thenBlock == null || elseBlock == null) {
            return null;
        }

        // 查找共同的后继作为endBlock
        BasicBlock endBlock = findCommonSuccessor(thenBlock, elseBlock);
        if (endBlock == null) {
            return null;
        }

        // 确保endBlock是thenBlock和elseBlock的共同后继
        boolean thenHasEnd = hasPath(thenBlock, endBlock);
        boolean elseHasEnd = hasPath(elseBlock, endBlock);
        if (!thenHasEnd || !elseHasEnd) {
            return null;
        }

        return new IfElsePattern(testBlock, thenBlock, elseBlock, endBlock);
    }

    /**
     * 查找两个块的共同后继
     */
    private BasicBlock findCommonSuccessor(BasicBlock block1, BasicBlock block2) {
        // 使用BFS查找两个块的共同后继
        Set<BasicBlock> visited1 = new HashSet<>();
        Set<BasicBlock> visited2 = new HashSet<>();

        Deque<BasicBlock> queue1 = new ArrayDeque<>();
        Deque<BasicBlock> queue2 = new ArrayDeque<>();

        queue1.offer(block1);
        queue2.offer(block2);

        visited1.add(block1);
        visited2.add(block2);

        while (!queue1.isEmpty() || !queue2.isEmpty()) {
            // 处理第一个队列
            if (!queue1.isEmpty()) {
                BasicBlock current = queue1.poll();
                if (visited2.contains(current)) {
                    return current;
                }
                for (BasicBlock successor : current.getSuccessors()) {
                    if (!visited1.contains(successor)) {
                        visited1.add(successor);
                        queue1.offer(successor);
                    }
                }
            }

            // 处理第二个队列
            if (!queue2.isEmpty()) {
                BasicBlock current = queue2.poll();
                if (visited1.contains(current)) {
                    return current;
                }
                for (BasicBlock successor : current.getSuccessors()) {
                    if (!visited2.contains(successor)) {
                        visited2.add(successor);
                        queue2.offer(successor);
                    }
                }
            }
        }

        return null;
    }

    /**
     * 检查是否存在从block1到block2的路径
     */
    private boolean hasPath(BasicBlock block1, BasicBlock block2) {
        Set<BasicBlock> visited = new HashSet<>();
        Deque<BasicBlock> queue = new ArrayDeque<>();

        queue.offer(block1);
        visited.add(block1);

        while (!queue.isEmpty()) {
            BasicBlock current = queue.poll();
            if (current == block2) {
                return true;
            }

            for (BasicBlock successor : current.getSuccessors()) {
                if (!visited.contains(successor)) {
                    visited.add(successor);
                    queue.offer(successor);
                }
            }
        }

        return false;
    }
}

