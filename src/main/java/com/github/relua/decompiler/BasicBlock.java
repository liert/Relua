package com.github.relua.decompiler;

import com.github.relua.model.Register;
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
    /** @deprecated Legacy linear register input state; prefer SSA values. */
    @Deprecated
    private Register inputState; // 块输入寄存器状态
    /** @deprecated Legacy linear register output state; prefer SSA values. */
    @Deprecated
    private Register outputState; // 块输出寄存器状态
    
    public BasicBlock(int startIndex) {
        this.startIndex = startIndex;
        this.endIndex = startIndex;
        this.successors = new ArrayList<>();
        this.predecessors = new ArrayList<>();
        this.inputState = new Register();
        this.outputState = new Register();
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
    /** @deprecated Legacy linear register state; prefer SSA values. */
    @Deprecated
    public Register getInputState() { return inputState; }
    /** @deprecated Legacy linear register state; prefer SSA values. */
    @Deprecated
    public void setInputState(Register inputState) { this.inputState = inputState; }
    /** @deprecated Legacy linear register state; prefer SSA values. */
    @Deprecated
    public Register getOutputState() { return outputState; }
    /** @deprecated Legacy linear register state; prefer SSA values. */
    @Deprecated
    public void setOutputState(Register outputState) { this.outputState = outputState; }
    
    public void addSuccessor(BasicBlock block) {
        if (block != null && !successors.contains(block)) {
            successors.add(block);
            block.predecessors.add(this);
        }
    }
}
