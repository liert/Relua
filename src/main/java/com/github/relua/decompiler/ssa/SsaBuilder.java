package com.github.relua.decompiler.ssa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.relua.decompiler.BasicBlock;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;

public final class SsaBuilder {
    private Chunk chunk;
    private List<BasicBlock> blocks;
    private SsaFunction function;
    private Map<BasicBlock, Set<BasicBlock>> dominators;
    private Map<BasicBlock, BasicBlock> immediateDominators;
    private Map<BasicBlock, List<BasicBlock>> dominatorTree;
    private Map<BasicBlock, Set<BasicBlock>> dominanceFrontiers;
    private Map<BasicBlock, Set<Integer>> blockDefs;
    private Map<Integer, Set<BasicBlock>> definingBlocks;
    private Set<BasicBlock> reachableBlockSet;
    private Map<Integer, Integer> nextVersion;
    private Map<Integer, Deque<SsaValue>> stacks;

    public SsaFunction build(Chunk chunk, List<BasicBlock> blocks) {
        this.chunk = chunk;
        this.blocks = reachableBlocks(blocks != null ? blocks : Collections.<BasicBlock>emptyList());
        this.reachableBlockSet = new LinkedHashSet<>(this.blocks);
        this.function = new SsaFunction(chunk);
        this.nextVersion = new HashMap<>();
        this.stacks = new HashMap<>();

        for (BasicBlock block : this.blocks) {
            function.getOrCreateBlock(block);
        }
        if (this.blocks.isEmpty()) {
            return function;
        }

        collectDefinitions();
        computeDominators();
        computeImmediateDominators();
        computeDominatorTree();
        computeDominanceFrontiers();
        insertPhis();
        initializeParameters();
        rename(this.blocks.get(0), new HashSet<BasicBlock>());
        return function;
    }

    private List<BasicBlock> reachableBlocks(List<BasicBlock> inputBlocks) {
        if (inputBlocks.isEmpty()) {
            return inputBlocks;
        }
        Set<BasicBlock> reachable = new LinkedHashSet<>();
        Deque<BasicBlock> work = new ArrayDeque<>();
        work.add(inputBlocks.get(0));
        reachable.add(inputBlocks.get(0));
        while (!work.isEmpty()) {
            BasicBlock block = work.removeFirst();
            for (BasicBlock successor : block.getSuccessors()) {
                if (reachable.add(successor)) {
                    work.addLast(successor);
                }
            }
        }

        List<BasicBlock> ordered = new ArrayList<>();
        for (BasicBlock block : inputBlocks) {
            if (reachable.contains(block)) {
                ordered.add(block);
            }
        }
        return ordered;
    }

    private void collectDefinitions() {
        blockDefs = new LinkedHashMap<>();
        definingBlocks = new HashMap<>();
        List<Instruction> instructions = chunk.getInstructions();
        for (BasicBlock block : blocks) {
            Set<Integer> defs = new LinkedHashSet<>();
            for (int pc = block.getStartIndex(); pc <= block.getEndIndex() && pc < instructions.size(); pc++) {
                SsaInstructionSummary summary = SsaInstructionSummarizer.summarize(instructions.get(pc));
                for (Integer register : summary.getDefs()) {
                    defs.add(register);
                    Set<BasicBlock> defBlocks = definingBlocks.get(register);
                    if (defBlocks == null) {
                        defBlocks = new LinkedHashSet<>();
                        definingBlocks.put(register, defBlocks);
                    }
                    defBlocks.add(block);
                }
            }
            blockDefs.put(block, defs);
        }
    }

    private void computeDominators() {
        dominators = new LinkedHashMap<>();
        BasicBlock entry = blocks.get(0);
        Set<BasicBlock> all = new LinkedHashSet<>(blocks);
        for (BasicBlock block : blocks) {
            if (block == entry) {
                dominators.put(block, new LinkedHashSet<>(Collections.singleton(block)));
            } else {
                dominators.put(block, new LinkedHashSet<>(all));
            }
        }

        boolean changed;
        do {
            changed = false;
            for (BasicBlock block : blocks) {
                if (block == entry) {
                    continue;
                }
                Set<BasicBlock> newDom = null;
                for (BasicBlock pred : block.getPredecessors()) {
                    if (!dominators.containsKey(pred)) {
                        continue;
                    }
                    if (newDom == null) {
                        newDom = new LinkedHashSet<>(dominators.get(pred));
                    } else {
                        newDom.retainAll(dominators.get(pred));
                    }
                }
                if (newDom == null) {
                    newDom = new LinkedHashSet<>();
                }
                newDom.add(block);
                if (!newDom.equals(dominators.get(block))) {
                    dominators.put(block, newDom);
                    changed = true;
                }
            }
        } while (changed);
    }

    private void computeImmediateDominators() {
        immediateDominators = new LinkedHashMap<>();
        BasicBlock entry = blocks.get(0);
        immediateDominators.put(entry, null);
        for (BasicBlock block : blocks) {
            if (block == entry) {
                continue;
            }
            Set<BasicBlock> strictDominators = new LinkedHashSet<>(dominators.get(block));
            strictDominators.remove(block);
            BasicBlock idom = null;
            for (BasicBlock candidate : strictDominators) {
                boolean dominatesAnotherStrictDominator = false;
                for (BasicBlock other : strictDominators) {
                    if (other != candidate && dominators.get(other).contains(candidate)) {
                        dominatesAnotherStrictDominator = true;
                        break;
                    }
                }
                if (!dominatesAnotherStrictDominator) {
                    idom = candidate;
                    break;
                }
            }
            immediateDominators.put(block, idom);
        }
    }

    private void computeDominatorTree() {
        dominatorTree = new LinkedHashMap<>();
        for (BasicBlock block : blocks) {
            dominatorTree.put(block, new ArrayList<BasicBlock>());
        }
        for (BasicBlock block : blocks) {
            BasicBlock idom = immediateDominators.get(block);
            if (idom != null) {
                dominatorTree.get(idom).add(block);
            }
        }
    }

    private void computeDominanceFrontiers() {
        dominanceFrontiers = new LinkedHashMap<>();
        for (BasicBlock block : blocks) {
            dominanceFrontiers.put(block, new LinkedHashSet<BasicBlock>());
        }
        for (BasicBlock block : blocks) {
            if (block.getPredecessors().size() < 2) {
                continue;
            }
            for (BasicBlock pred : block.getPredecessors()) {
                if (!reachableBlockSet.contains(pred)) {
                    continue;
                }
                BasicBlock runner = pred;
                while (runner != null && runner != immediateDominators.get(block)) {
                    Set<BasicBlock> frontier = dominanceFrontiers.get(runner);
                    if (frontier == null) {
                        break;
                    }
                    frontier.add(block);
                    runner = immediateDominators.get(runner);
                }
            }
        }
    }

    private void insertPhis() {
        for (Map.Entry<Integer, Set<BasicBlock>> entry : definingBlocks.entrySet()) {
            int register = entry.getKey();
            Deque<BasicBlock> work = new ArrayDeque<>(entry.getValue());
            Set<BasicBlock> hasAlready = new HashSet<>();
            Set<BasicBlock> everOnWork = new HashSet<>(entry.getValue());

            while (!work.isEmpty()) {
                BasicBlock block = work.removeFirst();
                for (BasicBlock frontier : dominanceFrontiers.get(block)) {
                    if (hasAlready.add(frontier)) {
                        function.getOrCreateBlock(frontier).getOrCreatePhi(register);
                        if (!everOnWork.contains(frontier)) {
                            work.addLast(frontier);
                            everOnWork.add(frontier);
                        }
                    }
                }
            }
        }
    }

    private void initializeParameters() {
        for (int register = 0; register < chunk.getNumParams(); register++) {
            push(register, newValue(register, true));
        }
    }

    private void rename(BasicBlock block, Set<BasicBlock> visited) {
        if (!visited.add(block)) {
            return;
        }

        List<Integer> pushedRegisters = new ArrayList<>();
        SsaBlock ssaBlock = function.getOrCreateBlock(block);
        for (SsaPhi phi : ssaBlock.getPhis()) {
            SsaValue value = newValue(phi.getRegister(), false);
            phi.setTarget(value);
            push(phi.getRegister(), value);
            pushedRegisters.add(phi.getRegister());
        }

        List<Instruction> instructions = chunk.getInstructions();
        for (int pc = block.getStartIndex(); pc <= block.getEndIndex() && pc < instructions.size(); pc++) {
            Instruction instruction = instructions.get(pc);
            SsaInstructionSummary summary = SsaInstructionSummarizer.summarize(instruction);
            SsaInstruction ssaInstruction = new SsaInstruction(instruction, pc);
            for (Integer register : summary.getUses()) {
                ssaInstruction.addUse(peekOrCreateImplicit(register));
            }
            for (Integer register : summary.getDefs()) {
                SsaValue value = newValue(register, false);
                ssaInstruction.addDef(value);
                push(register, value);
                pushedRegisters.add(register);
            }
            ssaBlock.addInstruction(ssaInstruction);
            function.addInstruction(ssaInstruction);
        }

        for (BasicBlock successor : block.getSuccessors()) {
            SsaBlock successorBlock = function.getBlock(successor);
            if (successorBlock == null) {
                continue;
            }
            for (SsaPhi phi : successorBlock.getPhis()) {
                phi.addIncoming(block, peekOrCreateImplicit(phi.getRegister()));
            }
        }

        List<BasicBlock> children = dominatorTree.get(block);
        if (children != null) {
            for (BasicBlock child : children) {
                rename(child, visited);
            }
        }

        for (int i = pushedRegisters.size() - 1; i >= 0; i--) {
            pop(pushedRegisters.get(i));
        }
    }

    private SsaValue newValue(int register, boolean implicit) {
        int version = nextVersion.containsKey(register) ? nextVersion.get(register) : 0;
        nextVersion.put(register, version + 1);
        return new SsaValue(register, version, implicit);
    }

    private void push(int register, SsaValue value) {
        Deque<SsaValue> stack = stacks.get(register);
        if (stack == null) {
            stack = new ArrayDeque<>();
            stacks.put(register, stack);
        }
        stack.push(value);
    }

    private void pop(int register) {
        Deque<SsaValue> stack = stacks.get(register);
        if (stack != null && !stack.isEmpty()) {
            stack.pop();
        }
    }

    private SsaValue peekOrCreateImplicit(int register) {
        Deque<SsaValue> stack = stacks.get(register);
        if (stack == null || stack.isEmpty()) {
            SsaValue value = newValue(register, true);
            push(register, value);
            return value;
        }
        return stack.peek();
    }
}
