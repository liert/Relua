package com.github.relua.decompiler.cfg;

import java.util.List;

import com.github.relua.decompiler.BasicBlock;
import com.github.relua.decompiler.DecompilerPipeline;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;

public class ControlFlowGraphBuilder {
    private final DecompilerPipeline pipeline;

    public ControlFlowGraphBuilder(DecompilerPipeline pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * 构建控制流图
     * 
     * @param chunk 代码块
     */
    public void build(Chunk chunk) {
        List<Instruction> instructions = chunk.getInstructions();

        for (int i = 0; i < instructions.size(); i++) {
            Instruction inst = instructions.get(i);
            BasicBlock currentBlock = pipeline.getBasicBlock(chunk.getFunction(), i);

            Opcode opcode = inst.getOpcode();

            if (opcode == Opcode.JMP) {
                int target = i + 1 + inst.getSBx();
                addEdge(chunk, currentBlock, target);
            } else if (opcode == Opcode.TEST || opcode == Opcode.TESTSET) {
                // TEST always followed by JMP in if/else patterns
                if (i + 1 < instructions.size() && instructions.get(i + 1).getOpcode() == Opcode.JMP) {
                    handleTestInstruction(chunk, i, inst, instructions, currentBlock);
                    continue;
                }

                // fallback：普通顺序
                addEdge(chunk, currentBlock, i + 1);
            } else if (opcode == Opcode.EQ || opcode == Opcode.LT || opcode == Opcode.LE) {
                // 比较指令后面总是紧跟 JMP
                if (i + 1 < instructions.size() && instructions.get(i + 1).getOpcode() == Opcode.JMP) {
                    handleComparisonInstruction(chunk, i, inst, instructions, currentBlock);
                    continue;
                }

                // fallback：普通顺序
                addEdge(chunk, currentBlock, i + 1);
            } else if (opcode == Opcode.FORLOOP) {
                // 处理FORLOOP指令
                int jumpTarget = inst.getSBx() == 0 ? inst.getNumericForPrepTarget(instructions, i) : i + 1 + inst.getSBx();
                BasicBlock targetBlock = pipeline.getBasicBlock(chunk.getFunction(), jumpTarget);
                if (targetBlock != null) {
                    currentBlock.addSuccessor(targetBlock);
                }

                // FORLOOP 结束条件不满足时会流向下一条指令，所以需要添加下一条指令作为后继
                if (i + 1 < instructions.size()) {
                    BasicBlock nextBlock = pipeline.getBasicBlock(chunk.getFunction(), i + 1);
                    if (nextBlock != null) {
                        currentBlock.addSuccessor(nextBlock);
                    }
                }
            } else if (opcode == Opcode.FORPREP) {
                // 处理FORPREP指令
                int jumpTarget = inst.getSBx() == 0 ? inst.getNumericForLoopTarget(instructions, i) : i + 1 + inst.getSBx();
                BasicBlock targetBlock = pipeline.getBasicBlock(chunk.getFunction(), jumpTarget);
                if (targetBlock != null) {
                    currentBlock.addSuccessor(targetBlock);
                }

                // FORPREP指令执行后总是跳转到循环头，所以不需要添加下一条指令作为后继
            } else if (opcode == Opcode.TFORLOOP) {
                // 处理TFORLOOP指令
                int jumpTarget = i + 1 + inst.getSBx();
                BasicBlock targetBlock = pipeline.getBasicBlock(chunk.getFunction(), jumpTarget);
                if (targetBlock != null) {
                    currentBlock.addSuccessor(targetBlock);
                }

                // TFORLOOP指令执行后，如果条件满足则跳转到循环头，否则继续执行下一条指令
                if (i + 1 < instructions.size()) {
                    BasicBlock nextBlock = pipeline.getBasicBlock(chunk.getFunction(), i + 1);
                    if (nextBlock != null) {
                        currentBlock.addSuccessor(nextBlock);
                    }
                }
            } else if (opcode == Opcode.LOADBOOL) {
                if (inst.getC() != 0) {
                    int target = i + 2;
                    addEdge(chunk, currentBlock, target);
                } else {
                    if (i + 1 < instructions.size()) {
                        BasicBlock nextBlock = pipeline.getBasicBlock(chunk.getFunction(), i + 1);
                        if (nextBlock != null && nextBlock != currentBlock) {
                            currentBlock.addSuccessor(nextBlock);
                        }
                    }
                }
            } else if (opcode != Opcode.RETURN && opcode != Opcode.TAILCALL) {
                // 其他非返回/尾调用指令，添加下一条指令作为后继
                if (i + 1 < instructions.size()) {
                    BasicBlock nextBlock = pipeline.getBasicBlock(chunk.getFunction(), i + 1);
                    if (nextBlock != null && nextBlock != currentBlock) {
                        currentBlock.addSuccessor(nextBlock);
                    }
                }
            }
        }
    }

    private void handleTestInstruction(Chunk chunk, int i, Instruction inst, List<Instruction> instructions,
            BasicBlock currentBlock) {
        Instruction jmp = instructions.get(i + 1); // guaranteed JMP in TEST+JMP pattern

        int jmpTarget = i + 2 + jmp.getSBx(); // (i + 1) + 1 + jmp.getSBx()

        // TEST true → PC + 2 (进入 then)
        int thenTarget = i + 2;

        // TEST false → JMP target (进入 else)
        int elseTarget = jmpTarget;

        addEdge(chunk, currentBlock, thenTarget);
        addEdge(chunk, currentBlock, elseTarget);
    }

    private void handleComparisonInstruction(Chunk chunk, int i, Instruction inst, List<Instruction> instructions,
            BasicBlock currentBlock) {
        Instruction jmp = instructions.get(i + 1); // guaranteed JMP in EQ/LT/LE+JMP pattern

        int jmpTarget = i + 2 + jmp.getSBx(); // (i + 1) + 1 + jmp.getSBx()

        // 比较满足/不满足条件时：
        // 一条分支是不满足/跳过 JMP，落入 i + 2
        int thenTarget = i + 2;

        // 另一条分支是满足/执行 JMP，跳转到 jmpTarget
        int elseTarget = jmpTarget;

        addEdge(chunk, currentBlock, thenTarget);
        addEdge(chunk, currentBlock, elseTarget);
    }

    private void addEdge(Chunk chunk, BasicBlock from, int instIndex) {
        if (from == null) {
            return;
        }
        BasicBlock to = pipeline.getBasicBlock(chunk.getFunction(), instIndex);
        if (to != null) {
            from.addSuccessor(to);
        }
    }
}
