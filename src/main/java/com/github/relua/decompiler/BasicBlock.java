package com.github.relua.decompiler;

import java.util.ArrayList;
import java.util.List;

/**
 * 基本块类，用于控制流分析
 */
public class BasicBlock {
    private int startIndex;
    private int endIndex;
    private List<BasicBlock> successors;
    private List<BasicBlock> predecessors;
    private boolean isIfBlock = false;
    private boolean isLoopBlock = false;
    private boolean isElseBlock = false;
    private boolean visited = false;
    private int conditionRegister = -1;
    
    public BasicBlock(int startIndex) {
        this.startIndex = startIndex;
        this.endIndex = startIndex;
        this.successors = new ArrayList<>();
        this.predecessors = new ArrayList<>();
    }
    
    // getter和setter方法
    public int getStartIndex() { return startIndex; }
    public void setEndIndex(int endIndex) { this.endIndex = endIndex; }
    public int getEndIndex() { return endIndex; }
    public List<BasicBlock> getSuccessors() { return successors; }
    public List<BasicBlock> getPredecessors() { return predecessors; }
    public boolean isIfBlock() { return isIfBlock; }
    public void setIfBlock(boolean ifBlock) { isIfBlock = ifBlock; }
    public boolean isLoopBlock() { return isLoopBlock; }
    public void setLoopBlock(boolean loopBlock) { isLoopBlock = loopBlock; }
    public boolean isElseBlock() { return isElseBlock; }
    public void setElseBlock(boolean elseBlock) { isElseBlock = elseBlock; }
    public boolean isVisited() { return visited; }
    public void setVisited(boolean visited) { this.visited = visited; }
    public int getConditionRegister() { return conditionRegister; }
    public void setConditionRegister(int conditionRegister) { this.conditionRegister = conditionRegister; }
    
    public void addSuccessor(BasicBlock block) {
        if (block != null && !successors.contains(block)) {
            successors.add(block);
            block.predecessors.add(this);
        }
    }
}
