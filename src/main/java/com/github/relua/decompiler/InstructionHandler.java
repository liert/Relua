package com.github.relua.decompiler;

import com.github.relua.ast.*;
import com.github.relua.decompiler.ssa.SsaAstNameResolver;
import com.github.relua.decompiler.ssa.SsaValue;
import com.github.relua.log.Logger;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;
import com.github.relua.model.Register;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Deque;
import java.util.ArrayDeque;

/**
 * 指令处理器，负责处理指令并构建中间表示
 */
public class InstructionHandler {
    private CodeGeneratorContext codeGenContext; // 代码生成上下文
    private final DecompilerPipeline pipeline;
    private final SsaAstNameResolver ssaNameResolver = new SsaAstNameResolver();
    private Set<BasicBlock> reachableBlocks;

    /**
     * 构造函数
     * 
     * @param codeGenContext 代码生成上下文
     */
    public InstructionHandler(LuaCodeGenerator generator, CodeGeneratorContext codeGenContext) {
        this.codeGenContext = codeGenContext;
        this.pipeline = new DecompilerPipeline(generator, this);
    }

    /**
     * 处理代码块的指令
     * 
     * @param chunk 代码块
     */
    public void process(Chunk chunk) {
        pipeline.processChunk(chunk);
    }

    public List<BasicBlock> getBasicBlocks(Chunk chunk) {
        if (chunk == null) {
            return java.util.Collections.emptyList();
        }
        return pipeline.getBasicBlocks(chunk.getFunction());
    }

    /**
     * 检查是否是无条件跳转
     * 
     * @param inst 指令
     * @return 是否是无条件跳转
     */
    private boolean isUnconditionalJump(Instruction inst) {
        // JMP指令如果没有设置条件位，则是无条件跳转
        return inst.getOpcode() == Opcode.JMP && inst.getA() == 0;
    }

    // getter方法，供代码生成器使用
    // public List<BasicBlock> getBasicBlocks() {
    // return basicBlocks;
    // }

    // public Map<Integer, BasicBlock> getInstructionToBlockMap() {
    // return instructionToBlockMap;
    // }

    /**
     * 获取基本块的第一条指令
     */
    public Instruction getFirstInstruction(BasicBlock block, Chunk chunk) {
        if (block == null)
            return null;
        List<Instruction> instructions = chunk.getInstructions();
        int startIndex = block.getStartIndex();
        if (startIndex >= 0 && startIndex < instructions.size()) {
            return instructions.get(startIndex);
        }
        return null;
    }

    /**
     * 获取基本块的下一个基本块（按索引顺序）
     */
    public BasicBlock getNextBlock(Chunk chunk, BasicBlock block) {
        if (block == null)
            return null;
        List<BasicBlock> basicBlocks = pipeline.getBasicBlocks(chunk.getFunction());
        int currentIndex = basicBlocks.indexOf(block);
        if (currentIndex >= 0 && currentIndex < basicBlocks.size() - 1) {
            return basicBlocks.get(currentIndex + 1);
        }
        return null;
    }

    public String getRegisterName(int register, int instructionIndex) {
        Chunk chunk = codeGenContext.getChunk();
        String function = chunk.getFunction();
        Register registerState = pipeline.getRegisterByInstructionIndex(instructionIndex);
        SsaValue definition = pipeline.getSsaDefinition(function, instructionIndex, register);
        if (definition != null) {
            return ssaNameResolver.nameForDefinition(definition, register, registerState, chunk.getNumParams());
        }
        SsaValue use = pipeline.getSsaUse(function, instructionIndex, register);
        if (use != null) {
            return ssaNameResolver.nameForUse(use, register, registerState, chunk.getNumParams());
        }
        throw new IllegalStateException("Missing SSA value for " + function + " pc=" + instructionIndex
                + " R" + register);
    }

    /**
     * 获取寄存器名，使用当前寄存器状态
     * 
     * @param register 寄存器号
     * @return 寄存器名或变量名
     */
    public String getRegisterName(int register) {
        return getRegisterName(register, 0);
    }

    /**
     * 计算所有基本块的支配关系
     * 
     * @param blocks 基本块列表
     * @param entry  入口基本块
     * @return 每个基本块的支配者集合
     */
    public Map<BasicBlock, Set<BasicBlock>> computeDominators(List<BasicBlock> blocks, BasicBlock entry) {
        Map<BasicBlock, Set<BasicBlock>> dom = new HashMap<>();

        // 初始化：入口基本块的支配者只有自己，其他基本块的支配者是所有基本块
        for (BasicBlock b : blocks) {
            if (b == entry) {
                Set<BasicBlock> entryDom = new HashSet<>();
                entryDom.add(b);
                dom.put(b, entryDom);
            } else {
                dom.put(b, new HashSet<>(blocks));
            }
        }

        boolean changed;
        do {
            changed = false;
            for (BasicBlock b : blocks) {
                if (b == entry) {
                    continue;
                }

                // 计算所有前驱支配者的交集
                Set<BasicBlock> newDom = intersectPredecessors(dom, b.getPredecessors());
                newDom.add(b);

                if (!newDom.equals(dom.get(b))) {
                    dom.put(b, newDom);
                    changed = true;
                }
            }
        } while (changed);

        return dom;
    }

    /**
     * 计算所有前驱基本块支配者的交集
     * 
     * @param dom          支配关系映射
     * @param predecessors 前驱基本块列表
     * @return 前驱支配者的交集
     */
    private Set<BasicBlock> intersectPredecessors(Map<BasicBlock, Set<BasicBlock>> dom, List<BasicBlock> predecessors) {
        if (predecessors.isEmpty()) {
            return new HashSet<>();
        }

        // 初始化结果为第一个前驱的支配者集合
        Set<BasicBlock> result = new HashSet<>(dom.get(predecessors.get(0)));

        // 与其他前驱的支配者集合求交集
        for (int i = 1; i < predecessors.size(); i++) {
            result.retainAll(dom.get(predecessors.get(i)));
        }

        return result;
    }

    /**
     * 查找所有回边
     * 
     * @param blocks 基本块列表
     * @param dom    支配关系映射
     * @return 回边列表，每个回边是一个包含两个基本块的数组 [u, v]，其中 u->v 是回边
     */
    public List<BasicBlock[]> findBackEdges(List<BasicBlock> blocks, Map<BasicBlock, Set<BasicBlock>> dom) {
        List<BasicBlock[]> backEdges = new ArrayList<>();

        for (BasicBlock u : blocks) {
            for (BasicBlock v : u.getSuccessors()) {
                // 如果 v 支配 u，并且 u != v，则 u->v 是回边
                // 排除自环，只处理真正的循环回边
                if (u != v && dom.get(v).contains(u)) {
                    backEdges.add(new BasicBlock[] { u, v });
                }
            }
        }

        return backEdges;
    }

    /**
     * 构建自然循环
     * 
     * @param u 回边的起始节点
     * @param v 回边的目标节点（循环头）
     * @return 自然循环包含的基本块集合
     */
    public Set<BasicBlock> buildNaturalLoop(BasicBlock u, BasicBlock v) {
        Set<BasicBlock> loop = new HashSet<>();
        Deque<BasicBlock> stack = new ArrayDeque<>();

        // 循环头总是在循环中
        loop.add(v);

        // 只有当u != v时，才构建循环
        if (u != v) {
            stack.push(u);
        }

        while (!stack.isEmpty()) {
            BasicBlock n = stack.pop();
            if (!loop.contains(n)) {
                loop.add(n);

                // 将所有前驱加入栈中，除非它们已经在循环中
                for (BasicBlock p : n.getPredecessors()) {
                    if (!loop.contains(p)) {
                        stack.push(p);
                    }
                }
            }
        }

        // 确保循环至少包含两个块，避免将单个块误识别为循环
        if (loop.size() < 2) {
            return new HashSet<>();
        }

        // 检查循环是否包含真正的循环结构（即循环头有一个后继指向循环内部）
        boolean hasLoopSuccessor = false;
        for (BasicBlock successor : v.getSuccessors()) {
            if (loop.contains(successor) && successor != v) {
                hasLoopSuccessor = true;
                break;
            }
        }

        if (!hasLoopSuccessor) {
            return new HashSet<>();
        }

        return loop;
    }

    /**
     * 计算所有基本块的后支配关系
     * 
     * @param blocks 基本块列表
     * @param exit   出口基本块
     * @return 每个基本块的后支配者集合
     */
    public Map<BasicBlock, Set<BasicBlock>> computePostDominators(List<BasicBlock> blocks, BasicBlock exit) {
        // 反转CFG的边，计算后支配关系
        Map<BasicBlock, List<BasicBlock>> reversedEdges = new HashMap<>();
        for (BasicBlock block : blocks) {
            reversedEdges.put(block, new ArrayList<>());
        }

        // 反转边：将原边 u->v 变为 v->u
        for (BasicBlock u : blocks) {
            for (BasicBlock v : u.getSuccessors()) {
                reversedEdges.get(v).add(u);
            }
        }

        Map<BasicBlock, Set<BasicBlock>> postDom = new HashMap<>();

        // 初始化：出口基本块的后支配者只有自己，其他基本块的后支配者是所有基本块
        for (BasicBlock b : blocks) {
            if (b == exit) {
                Set<BasicBlock> exitPostDom = new HashSet<>();
                exitPostDom.add(b);
                postDom.put(b, exitPostDom);
            } else {
                postDom.put(b, new HashSet<>(blocks));
            }
        }

        boolean changed;
        do {
            changed = false;
            for (BasicBlock b : blocks) {
                if (b == exit) {
                    continue;
                }

                // 计算所有后继后支配者的交集（在反转图中是前驱）
                Set<BasicBlock> newPostDom = intersectPostDominators(postDom, reversedEdges.get(b));
                newPostDom.add(b);

                if (!newPostDom.equals(postDom.get(b))) {
                    postDom.put(b, newPostDom);
                    changed = true;
                }
            }
        } while (changed);

        return postDom;
    }

    /**
     * 计算所有后继基本块后支配者的交集
     * 
     * @param postDom    后支配关系映射
     * @param successors 后继基本块列表（在反转图中是前驱）
     * @return 后继后支配者的交集
     */
    private Set<BasicBlock> intersectPostDominators(Map<BasicBlock, Set<BasicBlock>> postDom,
            List<BasicBlock> successors) {
        if (successors.isEmpty()) {
            return new HashSet<>();
        }

        // 初始化结果为第一个后继的后支配者集合
        Set<BasicBlock> result = new HashSet<>(postDom.get(successors.get(0)));

        // 与其他后继的后支配者集合求交集
        for (int i = 1; i < successors.size(); i++) {
            result.retainAll(postDom.get(successors.get(i)));
        }

        return result;
    }

    /**
     * 查找出口基本块
     * 
     * @param blocks 基本块列表
     * @return 出口基本块
     */
    public BasicBlock findExitBlock(List<BasicBlock> blocks) {
        // 出口基本块是没有后继的块，或者包含RETURN指令的块
        for (BasicBlock block : blocks) {
            if (block.getSuccessors().isEmpty()) {
                return block;
            }
        }

        // 如果没有没有后继的块，返回最后一个块
        return blocks.get(blocks.size() - 1);
    }

    /**
     * 检测SESE区域
     * 
     * @param blocks  基本块列表
     * @param dom     支配关系映射
     * @param postDom 后支配关系映射
     * @param chunk   代码块，用于指令分析
     * @return SESE区域列表
     */
    public List<SESERegion> detectSESERegions(List<BasicBlock> blocks, Map<BasicBlock, Set<BasicBlock>> dom,
            Map<BasicBlock, Set<BasicBlock>> postDom, Chunk chunk) {
        List<SESERegion> regions = new ArrayList<>();

        // 首先检测简单的if-then-else结构
        for (BasicBlock block : blocks) {
            // 检测if-else结构
            IfElsePattern pattern = pipeline.getControlFlowAnalyzer().detectIfElse(block, chunk);
            if (pattern != null) {
                // 检查是否形成SESE区域
                Set<BasicBlock> regionBlocks = new HashSet<>();
                regionBlocks.add(pattern.testBlock);
                regionBlocks.add(pattern.thenBlock);
                regionBlocks.add(pattern.elseBlock);
                regionBlocks.add(pattern.endBlock);

                // 检查是否满足SESE条件
                if (isSESE(regionBlocks, pattern.testBlock, pattern.endBlock, dom, postDom)) {
                    SESERegion region = new SESERegion(pattern.testBlock, pattern.endBlock, regionBlocks,
                            SESERegion.RegionType.IF_THEN_ELSE);
                    regions.add(region);
                }
            }
        }

        // 检测顺序结构
        detectSequenceRegions(blocks, regions, dom, postDom);

        return regions;
    }

    /**
     * 检测顺序结构的SESE区域
     * 
     * @param blocks  基本块列表
     * @param regions 区域列表，用于添加检测到的区域
     * @param dom     支配关系映射
     * @param postDom 后支配关系映射
     */
    private void detectSequenceRegions(List<BasicBlock> blocks, List<SESERegion> regions,
            Map<BasicBlock, Set<BasicBlock>> dom, Map<BasicBlock, Set<BasicBlock>> postDom) {
        // 顺序结构是指一系列连续的基本块，每个块只有一个前驱和一个后继
        Set<BasicBlock> processed = new HashSet<>();

        for (BasicBlock block : blocks) {
            if (processed.contains(block)) {
                continue;
            }

            // 检查是否是顺序结构的起始块
            if (block.getPredecessors().size() <= 1 && block.getSuccessors().size() == 1) {
                List<BasicBlock> sequence = new ArrayList<>();
                BasicBlock current = block;

                // 收集顺序结构的所有块
                while (current != null && current.getSuccessors().size() == 1 && !processed.contains(current)) {
                    sequence.add(current);
                    processed.add(current);

                    // 检查下一个块是否只有一个前驱
                    BasicBlock next = current.getSuccessors().get(0);
                    if (next.getPredecessors().size() != 1) {
                        break;
                    }

                    current = next;
                }

                // 如果顺序结构包含多个块，添加到区域列表
                if (sequence.size() > 1) {
                    Set<BasicBlock> regionBlocks = new HashSet<>(sequence);
                    BasicBlock entry = sequence.get(0);
                    BasicBlock exit = sequence.get(sequence.size() - 1);

                    if (isSESE(regionBlocks, entry, exit, dom, postDom)) {
                        SESERegion region = new SESERegion(entry, exit, regionBlocks, SESERegion.RegionType.SEQUENCE);
                        regions.add(region);
                    }
                }
            }
        }
    }

    /**
     * 检查一组基本块是否形成SESE区域
     * 
     * @param blocks  基本块集合
     * @param entry   入口基本块
     * @param exit    出口基本块
     * @param dom     支配关系映射
     * @param postDom 后支配关系映射
     * @return 是否形成SESE区域
     */
    private boolean isSESE(Set<BasicBlock> blocks, BasicBlock entry, BasicBlock exit,
            Map<BasicBlock, Set<BasicBlock>> dom, Map<BasicBlock, Set<BasicBlock>> postDom) {
        // 检查入口条件：所有块的入口必须是entry
        for (BasicBlock block : blocks) {
            if (block == entry) {
                continue;
            }

            // 检查block的所有前驱是否都在区域内，或者block是entry
            for (BasicBlock pred : block.getPredecessors()) {
                if (!blocks.contains(pred)) {
                    // 如果前驱不在区域内，那么block必须是entry
                    if (block != entry) {
                        return false;
                    }
                }
            }
        }

        // 检查出口条件：所有块的出口必须是exit
        for (BasicBlock block : blocks) {
            if (block == exit) {
                continue;
            }

            // 检查block的所有后继是否都在区域内，或者block是exit
            for (BasicBlock succ : block.getSuccessors()) {
                if (!blocks.contains(succ)) {
                    // 如果后继不在区域内，那么block必须是exit
                    if (block != exit) {
                        return false;
                    }
                }
            }
        }

        // 检查entry是否支配区域内所有块
        for (BasicBlock block : blocks) {
            if (!dom.get(block).contains(entry)) {
                return false;
            }
        }

        // 检查exit是否后支配区域内所有块
        for (BasicBlock block : blocks) {
            if (!postDom.get(block).contains(exit)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 区域折叠功能，将多个SESE区域合并成更复杂的区域
     * 
     * @param regions 原始SESE区域列表
     * @param dom     支配关系映射
     * @param postDom 后支配关系映射
     * @return 折叠后的SESE区域列表
     */
    public List<SESERegion> collapseRegions(List<SESERegion> regions, Map<BasicBlock, Set<BasicBlock>> dom,
            Map<BasicBlock, Set<BasicBlock>> postDom) {
        List<SESERegion> collapsedRegions = new ArrayList<>();
        Set<BasicBlock> processedBlocks = new HashSet<>();

        // 按区域大小降序排序，优先处理较大的区域
        regions.sort((r1, r2) -> Integer.compare(r2.getBlocks().size(), r1.getBlocks().size()));

        for (SESERegion region : regions) {
            // 如果区域的所有块都未被处理，则将其添加到折叠列表中
            boolean hasProcessedBlock = false;
            for (BasicBlock block : region.getBlocks()) {
                if (processedBlocks.contains(block)) {
                    hasProcessedBlock = true;
                    break;
                }
            }

            if (!hasProcessedBlock) {
                // 标记区域的所有块为已处理
                processedBlocks.addAll(region.getBlocks());
                collapsedRegions.add(region);
            }
        }

        // 处理单个基本块，将未被包含在任何区域中的基本块作为简单块添加
        for (SESERegion region : regions) {
            for (BasicBlock block : region.getBlocks()) {
                if (!processedBlocks.contains(block)) {
                    Set<BasicBlock> singleBlockSet = new HashSet<>();
                    singleBlockSet.add(block);
                    SESERegion singleRegion = new SESERegion(block, block, singleBlockSet,
                            SESERegion.RegionType.SIMPLE_BLOCK);
                    collapsedRegions.add(singleRegion);
                    processedBlocks.add(block);
                }
            }
        }

        return collapsedRegions;
    }

    /**
     * 从SESE区域生成AST节点
     * 
     * @param regions 折叠后的SESE区域列表
     * @param chunk   代码块
     * @return 生成的AST根节点
     */
    public AstNode generateAST(List<SESERegion> regions, Chunk chunk) {
        Block block = new Block(new SourcePos(0, -1));

        // 按入口块的起始索引排序区域
        // System.out.println(" - 按入口块起始索引排序SESE区域");
        regions.sort((r1, r2) -> Integer.compare(r1.getEntry().getStartIndex(), r2.getEntry().getStartIndex()));

        // 为每个区域生成AST节点
        // System.out.println(" - 为每个SESE区域生成AST节点:");
        for (int i = 0; i < regions.size(); i++) {
            SESERegion region = regions.get(i);
            AstNode regionNode = generateRegionAST(region, chunk);
            // System.out.println(" 生成区域AST节点，类型: " + regionNode.type);
            if (regionNode instanceof Statement) {
                block.statements.add((Statement) regionNode);
            } else if (regionNode instanceof Expression) {
                block.statements.add(new ExpressionStatement((Expression) regionNode, regionNode.pos));
            }
        }

        // 如果没有SESE区域，直接添加所有基本块；如果 regions 不为空，也追加所有遗漏的可达基本块
        List<BasicBlock> basicBlocks = pipeline.getBasicBlocks(chunk.getFunction());
        if (regions.isEmpty()) {
            for (int i = 0; i < basicBlocks.size(); i++) {
                BasicBlock basicBlock = basicBlocks.get(i);
                AstNode basicBlockNode = generateBasicBlockAST(basicBlock, chunk);
                if (basicBlockNode != null) {
                    if (basicBlockNode instanceof Block) {
                        block.statements.addAll(((Block) basicBlockNode).statements);
                    }
                }
            }
        } else {
            // 扫描并追加没有被 SESE 区域访问到的可达基本块（例如结尾的 return 块等）
            for (int i = 0; i < basicBlocks.size(); i++) {
                BasicBlock basicBlock = basicBlocks.get(i);
                if (!basicBlock.isVisited()) {
                    AstNode basicBlockNode = generateBasicBlockAST(basicBlock, chunk);
                    if (basicBlockNode != null) {
                        if (basicBlockNode instanceof Block) {
                            block.statements.addAll(((Block) basicBlockNode).statements);
                        }
                    }
                }
            }
        }

        return block;
    }

    /**
     * 从代码块生成AST节点
     * 
     * @param chunk 代码块
     * @return 生成的AST根节点
     */
    public AstNode generateASTFromChunk(Chunk chunk) {
        StructuredPatternEmitter structuredEmitter = new StructuredPatternEmitter(pipeline);
        Block structuredBlock = structuredEmitter.emitIfStructured(chunk);
        if (structuredBlock != null) {
            return structuredBlock;
        }

        // 计算支配关系
        List<BasicBlock> basicBlocks = pipeline.getBasicBlocks(chunk.getFunction());
        BasicBlock entry = basicBlocks.get(0);
        Map<BasicBlock, Set<BasicBlock>> dom = computeDominators(basicBlocks, entry);

        // 查找出口基本块
        BasicBlock exit = findExitBlock(basicBlocks);

        // 计算后支配关系
        Map<BasicBlock, Set<BasicBlock>> postDom = computePostDominators(basicBlocks, exit);

        // 检测SESE区域
        List<SESERegion> regions = detectSESERegions(basicBlocks, dom, postDom, chunk);

        // 折叠SESE区域
        List<SESERegion> collapsedRegions = collapseRegions(regions, dom, postDom);

        // 生成AST
        AstNode ast = generateAST(collapsedRegions, chunk);

        return ast;
    }

    /**
     * 为单个SESE区域生成AST节点
     * 
     * @param region SESE区域
     * @param chunk  代码块
     * @return 生成的AST节点
     */
    private AstNode generateRegionAST(SESERegion region, Chunk chunk) {
        // System.out.println(" - 生成区域AST，类型: " + region.getType());

        AstNode regionNode;
        switch (region.getType()) {
            case IF_THEN:
                // System.out.println(" - 生成IF_THEN AST节点");
                regionNode = generateIfThenAST(region, chunk);
                break;
            case IF_THEN_ELSE:
                // System.out.println(" - 生成IF_THEN_ELSE AST节点");
                regionNode = generateIfThenElseAST(region, chunk);
                break;
            case WHILE_LOOP:
                // System.out.println(" - 生成WHILE_LOOP AST节点");
                regionNode = generateWhileLoopAST(region, chunk);
                break;
            case SEQUENCE:
                // System.out.println(" - 生成SEQUENCE AST节点");
                regionNode = generateSequenceAST(region, chunk);
                break;
            case SIMPLE_BLOCK:
                // System.out.println(" - 生成SIMPLE_BLOCK AST节点");
                regionNode = generateSimpleBlockAST(region, chunk);
                break;
            default:
                // System.out.println(" - 生成默认BLOCK AST节点，类型: " + region.getType());
                // 对于其他类型的区域，生成默认的块节点
                Block defaultBlock = new Block(new SourcePos(0, -1));
                // 添加区域内的所有指令对应的AST节点
                for (BasicBlock block : region.getBlocks()) {
                    // System.out.println(" - 添加基本块到默认BLOCK: " + block.getStartIndex() + "-" +
                    // block.getEndIndex());
                    AstNode basicBlockNode = generateBasicBlockAST(block, chunk);
                    if (basicBlockNode instanceof Block) {
                        defaultBlock.statements.addAll(((Block) basicBlockNode).statements);
                    }
                }
                regionNode = defaultBlock;
                break;
        }

        // System.out.println(
        // " - 区域AST生成完成，类型: " + regionNode.type + ", 子节点数量: "
        // + (regionNode instanceof Block ? ((Block) regionNode).statements.size() :
        // 0));
        return regionNode;
    }

    /**
     * 为if-then结构生成AST节点
     * 
     * @param region SESE区域
     * @param chunk  代码块
     * @return 生成的AST节点
     */
    private AstNode generateIfThenAST(SESERegion region, Chunk chunk) {
        // 简化处理：创建一个布尔常量作为条件，实际应该从条件指令生成准确的条件表达式
        Expression condition = new BooleanConst(true, new SourcePos(0, -1));

        // 创建then块
        Block thenBlock = new Block(new SourcePos(0, -1));

        // 添加then块内的所有指令对应的AST节点
        for (BasicBlock block : region.getBlocks()) {
            if (block != region.getEntry()) { // 跳过条件块
                AstNode basicBlockNode = generateBasicBlockAST(block, chunk);
                if (basicBlockNode instanceof Block) {
                    thenBlock.statements.addAll(((Block) basicBlockNode).statements);
                }
            }
        }

        // 创建IfStatement节点
        IfStatement ifStatement = new IfStatement(condition, thenBlock, null, new SourcePos(0, -1));

        return ifStatement;
    }

    /**
     * 为简单块结构生成AST节点
     * 
     * @param region SESE区域
     * @param chunk  代码块
     * @return 生成的AST节点
     */
    private AstNode generateSimpleBlockAST(SESERegion region, Chunk chunk) {
        Block blockNode = new Block(new SourcePos(0, -1));
        // 添加区域内的所有指令对应的AST节点
        for (BasicBlock block : region.getBlocks()) {
            AstNode basicBlockNode = generateBasicBlockAST(block, chunk);
            if (basicBlockNode instanceof Block) {
                blockNode.statements.addAll(((Block) basicBlockNode).statements);
            }
        }
        return blockNode;
    }

    /**
     * 为if-then-else结构生成AST节点
     * 
     * @param region SESE区域
     * @param chunk  代码块
     * @return 生成的AST节点
     */
    private AstNode generateIfThenElseAST(SESERegion region, Chunk chunk) {
        // 简化处理：创建一个布尔常量作为条件，实际应该从条件指令生成准确的条件表达式
        Expression condition = new BooleanConst(true, new SourcePos(0, -1));

        // 创建then块和else块
        Block thenBlock = new Block(new SourcePos(0, -1));
        Block elseBlock = new Block(new SourcePos(0, -1));

        // 区分then块和else块
        boolean isThenBlock = true;
        for (BasicBlock block : region.getBlocks()) {
            if (block == region.getEntry()) {
                // 跳过条件块
                continue;
            }

            AstNode basicBlockNode = generateBasicBlockAST(block, chunk);
            if (isThenBlock) {
                if (basicBlockNode instanceof Block) {
                    thenBlock.statements.addAll(((Block) basicBlockNode).statements);
                }
                // 如果块有两个后继，说明是then块结束
                if (block.getSuccessors().size() == 2) {
                    isThenBlock = false;
                }
            } else {
                if (basicBlockNode instanceof Block) {
                    elseBlock.statements.addAll(((Block) basicBlockNode).statements);
                }
            }
        }

        // 创建IfStatement节点
        IfStatement ifStatement = new IfStatement(condition, thenBlock, elseBlock, new SourcePos(0, -1));

        return ifStatement;
    }

    /**
     * 为while循环结构生成AST节点
     * 
     * @param region SESE区域
     * @param chunk  代码块
     * @return 生成的AST节点
     */
    private AstNode generateWhileLoopAST(SESERegion region, Chunk chunk) {
        // 简化处理：创建一个布尔常量作为条件，实际应该从条件指令生成准确的条件表达式
        Expression condition = new BooleanConst(true, new SourcePos(0, -1));

        // 创建循环体块
        Block bodyBlock = new Block(new SourcePos(0, -1));

        // 添加循环体内的所有指令对应的AST节点
        for (BasicBlock block : region.getBlocks()) {
            if (block != region.getEntry()) { // 跳过条件块
                AstNode basicBlockNode = generateBasicBlockAST(block, chunk);
                if (basicBlockNode instanceof Block) {
                    bodyBlock.statements.addAll(((Block) basicBlockNode).statements);
                }
            }
        }

        // 创建WhileStatement节点
        WhileStatement whileStatement = new WhileStatement(condition, bodyBlock, new SourcePos(0, -1));

        return whileStatement;
    }

    /**
     * 为顺序结构生成AST节点
     * 
     * @param region SESE区域
     * @param chunk  代码块
     * @return 生成的AST节点
     */
    private AstNode generateSequenceAST(SESERegion region, Chunk chunk) {
        Block sequenceBlock = new Block(new SourcePos(0, -1));

        // 按顺序添加区域内的所有块对应的AST节点
        for (BasicBlock block : region.getBlocks()) {
            AstNode blockNode = generateBasicBlockAST(block, chunk);
            if (blockNode instanceof Block) {
                sequenceBlock.statements.addAll(((Block) blockNode).statements);
            }
        }

        return sequenceBlock;
    }

    /**
     * 为简单基本块生成AST节点
     * 
     * @param block 基本块
     * @param chunk 代码块
     * @return 生成的AST节点
     */
    private AstNode generateBasicBlockAST(BasicBlock block, Chunk chunk) {
        if (reachableBlocks == null || reachableBlocks.isEmpty()) {
            reachableBlocks = getReachableBlocks(chunk);
        }
        // 如果块不可达，直接跳过
        if (!reachableBlocks.contains(block)) {
            return null;
        }
        // 如果块已经被访问过，则跳过
        if (block.isVisited()) {
            // System.out.println(" - 跳过已访问的基本块，范围: " + block.getStartIndex() + "-" +
            // block.getEndIndex());
            return null;
        }
        // 标记块为已访问
        block.setVisited(true);
        // System.out.println(" - 生成基本块AST，范围: " + block.getStartIndex() + "-" +
        // block.getEndIndex());
        Block blockNode = new Block(new SourcePos(block.getStartIndex(), -1));

        // 创建指令到AST的转换器
        InstructionToASTConverter converter = new InstructionToASTConverter(chunk, pipeline);

        List<Instruction> instructions = chunk.getInstructions();
        // 添加块内的所有指令对应的AST节点
        // System.out.println(" - 遍历基本块内的指令:");
        for (int i = block.getStartIndex(); i <= block.getEndIndex(); i++) {
            if (i < instructions.size()) {
                if (codeGenContext.isLabelPC(i)) {
                    blockNode.statements.add(new LabelStatement("L" + i, new SourcePos(i, -1)));
                }
                Instruction instruction = instructions.get(i);
                PendingIf conditionalJump = converter.tryConvertConditionalJump(instructions, i);
                if (conditionalJump != null) {
                    i = appendPendingIf(blockNode, conditionalJump, chunk, converter);
                    continue;
                }
                // Logger.debug(BytecodeFormatter.formatInstruction(chunk, instruction, i));
                // 使用转换器将指令转换为AST节点
                // Register registerState = pipeline.getRegisterByInstructionIndex(i);
                // System.out.println(" 寄存器状态: " + registerState);
                Object result = converter.convertInstructionToAST(instruction, i);
                if (result instanceof ClosureSkipResult) {
                    ClosureSkipResult csr = (ClosureSkipResult) result;
                    if (csr.astNode instanceof Statement) {
                        blockNode.statements.add((Statement) csr.astNode);
                    } else if (csr.astNode instanceof Expression) {
                        Expression expr = (Expression) csr.astNode;
                        blockNode.statements.add(new ExpressionStatement(expr, expr.pos));
                    }
                    i += csr.skipCount;
                    continue;
                }
                if (result instanceof PendingIf) {
                    // 处理待完成的IF节点
                    i = appendPendingIf(blockNode, (PendingIf) result, chunk, converter);
                } else if (result instanceof Statement) {
                    // 如果返回的是 Block（如 LOADNIL），展平以避免双重缩进
                    if (result instanceof Block) {
                        blockNode.statements.addAll(((Block) result).statements);
                    } else {
                        if (result instanceof ReturnStatement) {
                            if (converter.tryOptimizeAssignReturn(blockNode, (ReturnStatement) result, i)) {
                                // Optimized
                            } else {
                                blockNode.statements.add((Statement) result);
                            }
                        } else {
                            blockNode.statements.add((Statement) result);
                        }
                    }

                } else if (result instanceof Expression) {
                    Expression expr = (Expression) result;
                    // System.out.println(" 生成表达式节点，包装为ExpressionStatement");
                    // 将表达式包装为表达式语句
                    blockNode.statements.add(new ExpressionStatement(expr, expr.pos));
                } else {
                    // System.out.println(" 未生成指令AST节点");
                }
            }
        }

        // System.out.println(" - 基本块AST生成完成，子节点数量: " + blockNode.statements.size());
        return blockNode;
    }

    /**
     * 根据指令索引找到对应的基本块
     * 
     * @param instructionIndex 指令索引
     * @return 对应的基本块，如果没有找到则返回null
     */
    public BasicBlock getBlockByInstructionIndex(Chunk chunk, int instructionIndex) {
        List<BasicBlock> basicBlocks = pipeline.getBasicBlocks(chunk.getFunction());
        for (BasicBlock block : basicBlocks) {
            if (instructionIndex >= block.getStartIndex() && instructionIndex <= block.getEndIndex()) {
                return block;
            }
        }
        return null;
    }

    /**
     * 标记指定指令范围内的所有基本块为已访问
     * 
     * @param start 起始指令索引
     * @param end   结束指令索引
     */
    private void markBlocksAsVisited(Chunk chunk, int start, int end) {
        for (int pc = start; pc <= end; pc++) {
            BasicBlock block = getBlockByInstructionIndex(chunk, pc);
            if (block != null && !block.isVisited()) {
                block.setVisited(true);
                // System.out.println(" - 标记块为已访问，范围: " + block.getStartIndex() + "-" +
                // block.getEndIndex());
            }
        }
    }

    /**
     * 调整范围的结束位置以包含 for-in 循环的 TFORLOOP。
     * 如果范围内有 JMP 指向 TFORLOOP（for-in 前向跳转），
     * 将结束位置扩展到包含 TFORLOOP，使 iterator + body + TFORLOOP 在同一块中。
     * backward JMP 不在此范围内，它会被 buildBlock 主循环单独处理，
     * 确保 backward goto 与 iterator 在同一层级。
     * 同时标记 TFORLOOP 所在的基本块为已访问，防止主循环重复处理。
     */
    private int adjustEndForForIn(int start, int end, Chunk chunk) {
        List<Instruction> instructions = chunk.getInstructions();
        int result = end;
        for (int pc = start; pc <= result && pc < instructions.size(); pc++) {
            Instruction inst = instructions.get(pc);
            if (inst.getOpcode() == Opcode.JMP) {
                int target = pc + 1 + inst.getSBx();
                if (target >= 0 && target < instructions.size()
                        && codeGenContext.isTforRegionPC(target)
                        && instructions.get(target).getOpcode() == Opcode.TFORLOOP) {
                    // 找到 for-in 前向跳转，扩展到包含 TFORLOOP（不含 backward JMP）
                    if (target > result) {
                        result = target;
                    }
                    // 标记 TFORLOOP 所在的基本块为已访问，
                    // 防止 appendPendingIf 返回值跳过它后被主循环重复处理
                    markBlocksAsVisited(chunk, target, target);
                }
            }
        }
        return result;
    }

    private int appendPendingIf(Block blockNode, PendingIf pending, Chunk chunk, InstructionToASTConverter converter) {
        Logger.debug(String.format("生成PendingIf节点，条件: %s, then: %d-%d, else: %d-%d, flag: %b, astNodes: %d",
                pending.condition, pending.thenStart, pending.thenEnd, pending.elseStart, pending.elseEnd,
                pending.flag, pending.astNodes.size()));

        int thenStart = pending.thenStart;
        int thenEnd = pending.thenEnd;
        Integer elseStart = pending.elseStart;
        Integer elseEnd = pending.elseEnd;

        // 调整 thenEnd：如果 then 范围内有 for-in 前向跳转（JMP 到 TFORLOOP），
        // 将 thenEnd 扩展到包含 TFORLOOP 和 backward JMP，使整个 for-in 结构在同一块中
        thenEnd = adjustEndForForIn(thenStart, thenEnd, chunk);

        // 调整 else 范围
        if (elseStart != null && elseEnd != null) {
            if (codeGenContext.isTforRegionPC(elseStart)) {
                elseStart = null;
                elseEnd = null;
            } else {
                elseEnd = adjustEndForForIn(elseStart, elseEnd, chunk);
            }
        }

        markBlocksAsVisited(chunk, pending.pos.pc, pending.pos.pc + 1);
        markBlocksAsVisited(chunk, thenStart, thenEnd);
        Block thenBlock = buildBlock(thenStart, thenEnd, chunk, converter);

        // Absorb trailing return/tailcall from gap between thenEnd and elseStart
        // When lastThenBlock shrinks thenEnd, instructions like RETURN may be left in
        // the gap.
        // If the then-block ends with an if(no else) and the gap starts with
        // return/tailcall,
        // move the return into the inner if's then-block so the then-block always
        // terminates.
        if (elseStart != null) {
            List<Instruction> instrs = chunk.getInstructions();
            int gapStart = thenEnd + 1;
            if (gapStart < elseStart && gapStart < instrs.size()) {
                Instruction gapInst = instrs.get(gapStart);
                boolean isReturn = gapInst.getOpcode() == Opcode.RETURN;
                boolean isTailcall = gapInst.getOpcode() == Opcode.TAILCALL;
                if (isReturn || isTailcall) {
                    List<Statement> ts = thenBlock.statements;
                    Statement lastNonLabel = null;
                    for (int k = ts.size() - 1; k >= 0; k--) {
                        if (!(ts.get(k) instanceof LabelStatement)) {
                            lastNonLabel = ts.get(k);
                            break;
                        }
                    }
                    if (lastNonLabel instanceof IfStatement) {
                        IfStatement innerIf = (IfStatement) lastNonLabel;
                        if (innerIf.elseBlock == null && !innerIf.blocks.isEmpty()) {
                            // Convert the return/tailcall instruction
                            InstructionToASTConverter tempConv = new InstructionToASTConverter(chunk, pipeline);
                            Object retNode = tempConv.convertInstructionToAST(gapInst, gapStart);
                            if (retNode instanceof Statement) {
                                innerIf.blocks.get(0).statements.add((Statement) retNode);
                                markBlocksAsVisited(chunk, gapStart, gapStart);
                            }
                        }
                    }
                }
            }
        }

        Block elseBlock = null;
        if (elseStart != null) {
            // 标记 thenEnd+1 到 elseEnd 范围内的所有基本块为已访问，
            // 防止 then 块末尾的死代码（如 return 后的 JMP）和 else 块体
            // 被外层 generateBasicBlockAST 循环重复处理
            markBlocksAsVisited(chunk, thenEnd + 1, elseEnd);
            markBlocksAsVisited(chunk, elseStart, elseEnd);
            elseBlock = buildBlock(elseStart, elseEnd, chunk, converter);
        }

        if (!pending.astNodes.isEmpty()) {
            if (pending.flag) {
                for (AstNode node : pending.astNodes) {
                    if (node instanceof Statement) {
                        thenBlock.statements.add((Statement) node);
                    }
                }
            } else {
                if (elseBlock == null) {
                    elseBlock = new Block(pending.pos);
                }
                for (AstNode node : pending.astNodes) {
                    if (node instanceof Statement) {
                        elseBlock.statements.add((Statement) node);
                    }
                }
            }
        }

        // ========== 重构：当 then 块总是终止时，将 else 代码移到 if 之后 ==========
        // 模式: if cond then ...; return end else B end → if cond then ...; return end; B
        boolean restructured = false;

        if (elseBlock != null && !isEffectivelyEmpty(elseBlock)) {
            if (blockAlwaysTerminates(thenBlock)) {
                // then 块总是终止 → else 代码是后续代码，移到 if 之后
                blockNode.statements.addAll(elseBlock.statements);
                elseBlock = null;
                restructured = true;
            }
        }

        blockNode.statements.add(new IfStatement(pending.condition, thenBlock, elseBlock, pending.pos));

        // 处理 if 语句后多余的裸 return：
        // 当 if 所有分支都有 return 时，后面的裸 return（无返回值）是冗余的
        if (restructured) {
            IfStatement justAdded = null;
            for (int k = blockNode.statements.size() - 1; k >= 0; k--) {
                if (blockNode.statements.get(k) instanceof IfStatement) {
                    justAdded = (IfStatement) blockNode.statements.get(k);
                    break;
                }
            }
            if (justAdded != null && ifAllBranchesTerminate(justAdded)) {
                List<Statement> bs = blockNode.statements;
                int ifIdx = bs.indexOf(justAdded);
                boolean onlyLabelsAndBareReturn = true;
                int bareReturnIdx = -1;
                for (int k = ifIdx + 1; k < bs.size(); k++) {
                    if (bs.get(k) instanceof LabelStatement) {
                        continue;
                    }
                    if (bs.get(k) instanceof ReturnStatement
                            && ((ReturnStatement) bs.get(k)).values.isEmpty()
                            && bareReturnIdx == -1) {
                        bareReturnIdx = k;
                    } else {
                        onlyLabelsAndBareReturn = false;
                        break;
                    }
                }
                if (onlyLabelsAndBareReturn && bareReturnIdx != -1) {
                    // 移除裸 return 及其前面的 label
                    while (bs.size() > ifIdx + 1) {
                        bs.remove(bs.size() - 1);
                    }
                }
            }
        }

        return elseEnd != null ? elseEnd : thenEnd;
    }

    // ========== if-else 重构辅助方法 ==========

    /**
     * 判断块是否以终结语句结尾（return/tailcall/全分支返回的if-else）。
     * 跳过末尾的 LabelStatement。
     */
    private boolean blockAlwaysTerminates(Block block) {
        return blockEndsWithTerminator(block);
    }

    private boolean blockEndsWithTerminator(Block block) {
        if (block == null || block.statements.isEmpty())
            return false;
        for (int i = block.statements.size() - 1; i >= 0; i--) {
            if (!(block.statements.get(i) instanceof LabelStatement)) {
                return isTerminatingStatement(block.statements.get(i));
            }
        }
        return false;
    }

    private boolean isTerminatingStatement(Statement stmt) {
        if (stmt instanceof ReturnStatement)
            return true;
        if (stmt instanceof ExpressionStatement) {
            Expression expr = ((ExpressionStatement) stmt).expression;
            if (expr instanceof UnaryOp && "return".equals(((UnaryOp) expr).op))
                return true;
        }
        if (stmt instanceof IfStatement) {
            return ifAllBranchesTerminate((IfStatement) stmt);
        }
        return false;
    }

    private boolean isEffectivelyEmpty(Block block) {
        if (block == null)
            return true;
        for (Statement s : block.statements) {
            if (!(s instanceof LabelStatement) && !(s instanceof GotoStatement))
                return false;
        }
        return true;
    }

    /**
     * 检查 IfStatement 的所有分支（then/elseif/else）是否都以终结语句结尾。
     */
    private boolean ifAllBranchesTerminate(IfStatement ifStmt) {
        for (Block b : ifStmt.blocks) {
            if (!blockEndsWithTerminator(b))
                return false;
        }
        if (ifStmt.elseBlock == null || isEffectivelyEmpty(ifStmt.elseBlock))
            return false;
        return blockEndsWithTerminator(ifStmt.elseBlock);
    }

    // /**
    // * 收集所有跳转目标PC
    // *
    // * @param chunk 代码块
    // * @return 跳转目标PC的集合
    // */
    // private Set<Integer> collectJumpTargets(Chunk chunk) {
    // Set<Integer> labelPCs = new HashSet<>();
    // List<Instruction> instructions = chunk.getInstructions();

    // for (int pc = 0; pc < instructions.size(); pc++) {
    // Instruction instr = instructions.get(pc);
    // if (instr.getOpcode() == Opcode.JMP) {
    // int sBx = instr.getSBx();
    // int targetPC = pc + 1 + sBx;
    // labelPCs.add(targetPC);
    // System.out.println(" - 收集跳转目标: L" + targetPC + " (PC: " + targetPC + ")");
    // }
    // }

    // return labelPCs;
    // }

    /**
     * 构建指定范围内的指令块AST
     * 
     * @param start     起始指令索引
     * @param end       结束指令索引
     * @param chunk     代码块
     * @param converter 指令到AST的转换器
     * @param labelPCs  跳转目标PC的集合
     * @return 生成的Block节点
     */
    private Block buildBlock(int start, int end, Chunk chunk, InstructionToASTConverter converter) {
        Block block = new Block(new SourcePos(start, -1));
        List<Instruction> instructions = chunk.getInstructions();

        // 清除残留的 pendingTest 状态，防止外层条件指令的状态泄漏到本块
        converter.clearPendingTest();

        for (int pc = start; pc <= end; pc++) {
            if (pc < instructions.size()) {
                if (codeGenContext.isLabelPC(pc)) {
                    block.statements.add(new LabelStatement("L" + pc, new SourcePos(pc, -1)));
                }
                Instruction inst = instructions.get(pc);
                PendingIf conditionalJump = converter.tryConvertConditionalJump(instructions, pc);
                if (conditionalJump != null) {
                    pc = appendPendingIf(block, conditionalJump, chunk, converter);
                    continue;
                }
                Object node = converter.convertInstructionToAST(inst, pc);
                if (node instanceof ClosureSkipResult) {
                    ClosureSkipResult csr = (ClosureSkipResult) node;
                    if (csr.astNode instanceof Statement) {
                        block.statements.add((Statement) csr.astNode);
                    } else if (csr.astNode instanceof Expression) {
                        Expression expr = (Expression) csr.astNode;
                        block.statements.add(new ExpressionStatement(expr, expr.pos));
                    }
                    pc += csr.skipCount;
                    continue;
                }

                if (node instanceof Statement) {
                    // 如果返回的是 Block（如 LOADNIL 生成的多赋值语句块），
                    // 将其内部语句展平到当前块，避免嵌套 Block 导致的双重缩进
                    if (node instanceof Block) {
                        block.statements.addAll(((Block) node).statements);
                    } else {
                        if (node instanceof ReturnStatement) {
                            if (converter.tryOptimizeAssignReturn(block, (ReturnStatement) node, pc)) {
                                // Optimized
                            } else {
                                block.statements.add((Statement) node);
                            }
                        } else {
                            block.statements.add((Statement) node);
                        }
                    }
                } else if (node instanceof Expression) {
                    block.statements.add(new ExpressionStatement((Expression) node, ((Expression) node).pos));
                } else if (node instanceof PendingIf) {
                    // 递归处理嵌套的IF
                    pc = appendPendingIf(block, (PendingIf) node, chunk, converter);
                }
            }
        }

        return block;
    }

    public CodeGeneratorContext getContext() {
        return this.codeGenContext;
    }

    public DecompilerPipeline getPipeline() {
        return this.pipeline;
    }

    private Set<BasicBlock> getReachableBlocks(Chunk chunk) {
        List<BasicBlock> basicBlocks = pipeline.getBasicBlocks(chunk.getFunction());
        Set<BasicBlock> reachable = new HashSet<>();
        if (basicBlocks.isEmpty()) {
            return reachable;
        }
        java.util.Queue<BasicBlock> queue = new java.util.LinkedList<>();
        BasicBlock entry = basicBlocks.get(0);
        reachable.add(entry);
        queue.add(entry);
        while (!queue.isEmpty()) {
            BasicBlock curr = queue.poll();
            for (BasicBlock succ : curr.getSuccessors()) {
                if (reachable.add(succ)) {
                    queue.add(succ);
                }
            }
        }
        return reachable;
    }
}
