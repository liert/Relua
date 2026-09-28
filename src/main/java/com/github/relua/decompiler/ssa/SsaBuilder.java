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
    private List<BasicBlock> componentRoots;
    private Map<Integer, Integer> nextVersion;
    private Map<Integer, Deque<SsaValue>> stacks;

    public SsaFunction build(Chunk chunk, List<BasicBlock> blocks) {
        this.chunk = chunk;
        this.blocks = blocks != null ? blocks : Collections.<BasicBlock>emptyList();
        this.function = new SsaFunction(chunk);
        this.nextVersion = new HashMap<>();
        this.stacks = new HashMap<>();

        for (BasicBlock block : this.blocks) {
            function.getOrCreateBlock(block);
        }
        if (this.blocks.isEmpty()) {
            return function;
        }

        computeComponents();
        collectDefinitions();

        // Create synthetic entry root to unify multiple components / unreachable code
        BasicBlock syntheticRoot = new BasicBlock(-1);
        syntheticRoot.setEndIndex(-1);
        for (BasicBlock root : componentRoots) {
            syntheticRoot.addSuccessor(root);
        }

        List<BasicBlock> allBlocks = new ArrayList<>();
        allBlocks.add(syntheticRoot);
        allBlocks.addAll(this.blocks);

        computeDominators(syntheticRoot, allBlocks);
        computeImmediateDominators(syntheticRoot, allBlocks);
        computeDominatorTree(allBlocks);
        computeDominanceFrontiers(allBlocks);

        // Clean up temporary synthetic root edges
        for (BasicBlock root : componentRoots) {
            root.getPredecessors().remove(syntheticRoot);
        }

        insertPhis();
        initializeImplicitValues();

        Set<BasicBlock> renamed = new HashSet<BasicBlock>();
        List<BasicBlock> roots = dominatorTree.get(syntheticRoot);
        if (roots != null) {
            for (BasicBlock root : roots) {
                rename(root, renamed);
            }
        }
        function.rebuildDefUse();
        return function;
    }

    private void computeComponents() {
        componentRoots = new ArrayList<>();
        Set<BasicBlock> unvisited = new LinkedHashSet<>(blocks);
        while (!unvisited.isEmpty()) {
            BasicBlock candidateRoot = unvisited.iterator().next();
            componentRoots.add(candidateRoot);

            Set<BasicBlock> visited = new HashSet<>();
            Deque<BasicBlock> work = new ArrayDeque<>();
            work.add(candidateRoot);
            visited.add(candidateRoot);
            unvisited.remove(candidateRoot);

            while (!work.isEmpty()) {
                BasicBlock block = work.removeFirst();
                for (BasicBlock successor : block.getSuccessors()) {
                    if (!blocks.contains(successor)) {
                        continue;
                    }
                    if (visited.add(successor)) {
                        unvisited.remove(successor);
                        work.addLast(successor);
                    }
                }
            }
        }
    }

    private void collectDefinitions() {
        blockDefs = new LinkedHashMap<>();
        definingBlocks = new HashMap<>();
        List<Instruction> instructions = chunk.getInstructions();
        for (BasicBlock block : blocks) {
            Set<Integer> defs = new LinkedHashSet<>();
            for (int pc = block.getStartIndex(); pc <= block.getEndIndex() && pc < instructions.size(); pc++) {
                SsaInstructionSummary summary = SsaInstructionSummarizer.summarize(chunk, pc);
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

    private void computeDominators(BasicBlock syntheticRoot, List<BasicBlock> allBlocks) {
        dominators = new LinkedHashMap<>();
        for (BasicBlock block : allBlocks) {
            if (block == syntheticRoot) {
                dominators.put(block, new LinkedHashSet<>(Collections.singleton(block)));
            } else {
                dominators.put(block, new LinkedHashSet<>(allBlocks));
            }
        }

        boolean changed;
        do {
            changed = false;
            for (BasicBlock block : allBlocks) {
                if (block == syntheticRoot) {
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

    private void computeImmediateDominators(BasicBlock syntheticRoot, List<BasicBlock> allBlocks) {
        immediateDominators = new LinkedHashMap<>();
        immediateDominators.put(syntheticRoot, null);
        for (BasicBlock block : allBlocks) {
            if (block == syntheticRoot) {
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

    private void computeDominatorTree(List<BasicBlock> allBlocks) {
        dominatorTree = new LinkedHashMap<>();
        for (BasicBlock block : allBlocks) {
            dominatorTree.put(block, new ArrayList<BasicBlock>());
        }
        for (BasicBlock block : allBlocks) {
            BasicBlock idom = immediateDominators.get(block);
            if (idom != null) {
                dominatorTree.get(idom).add(block);
            }
        }
    }

    private void computeDominanceFrontiers(List<BasicBlock> allBlocks) {
        dominanceFrontiers = new LinkedHashMap<>();
        for (BasicBlock block : allBlocks) {
            dominanceFrontiers.put(block, new LinkedHashSet<BasicBlock>());
        }
        for (BasicBlock block : allBlocks) {
            if (block.getPredecessors().size() < 2) {
                continue;
            }
            for (BasicBlock pred : block.getPredecessors()) {
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
                Set<BasicBlock> frontierBlocks = dominanceFrontiers.get(block);
                if (frontierBlocks == null) {
                    continue;
                }
                for (BasicBlock frontier : frontierBlocks) {
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

    private void initializeImplicitValues() {
        int limit = Math.max(chunk.getMaxStackSize(), chunk.getNumParams());
        for (int register = 0; register < limit; register++) {
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
            SsaInstructionSummary summary = SsaInstructionSummarizer.summarize(chunk, pc);
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
