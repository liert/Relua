package com.github.relua.decompiler;

import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制流图生成器，用于从Lua字节码生成控制流图
 */
public class CFGGenerator {
    private List<BasicBlock> basicBlocks;
    private Map<Integer, BasicBlock> instructionToBlockMap;
    private CodeGeneratorContext codeGenContext; // 代码生成上下文

    /**
     * 构造函数
     * 
     * @param codeGenContext 代码生成上下文
     */
    public CFGGenerator(CodeGeneratorContext codeGenContext) {
        this.codeGenContext = codeGenContext;
    }

    /**
     * 生成控制流图
     * 
     * @param chunk 代码块
     */
    public void generateCFG(Chunk chunk) {
        if (chunk == null) {
            return;
        }

        // 初始化数据结构
        basicBlocks = new ArrayList<>();
        instructionToBlockMap = new HashMap<>();

        // 构建基本块
        buildBasicBlocks(chunk);

        // 分析控制流
        analyzeControlFlow(chunk);

        // 构建控制流图
        buildControlFlowGraph(chunk);
    }

    /**
     * 构建基本块
     * 
     * @param chunk 代码块
     */
    private void buildBasicBlocks(Chunk chunk) {
        List<Instruction> instructions = chunk.getInstructions();

        // 首先标记所有跳转目标指令
        boolean[] isJumpTarget = new boolean[instructions.size()];
        
        // 标记跳转目标指令
        for (int i = 0; i < instructions.size(); i++) {
            Instruction inst = instructions.get(i);
            Opcode opcode = inst.getOpcode();

            // 检查所有可能产生跳转的指令
            if (opcode == Opcode.JMP) {
                int jumpTarget = i + 1 + inst.getSBx();
                if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                    isJumpTarget[jumpTarget] = true;
                }
                codeGenContext.addLabelPC(jumpTarget);
            } else if (opcode == Opcode.EQ || opcode == Opcode.LT || opcode == Opcode.LE) {
                if (inst.getC() != 0) {
                    int jumpTarget = i + 1 + inst.getC();
                    if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                        isJumpTarget[jumpTarget] = true;
                    }
                }
            } else if (opcode == Opcode.FORLOOP) {
                // FORLOOP指令：pc += sBx
                int jumpTarget = inst.getSBx() == 0 ? inst.getNumericForPrepTarget(instructions, i) : i + 1 + inst.getSBx();
                if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                    isJumpTarget[jumpTarget] = true;
                }
                codeGenContext.addLabelPC(jumpTarget);
            } else if (opcode == Opcode.FORPREP) {
                // FORPREP指令：pc += sBx
                int jumpTarget = inst.getSBx() == 0 ? inst.getNumericForLoopTarget(instructions, i) : i + 1 + inst.getSBx();
                if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                    isJumpTarget[jumpTarget] = true;
                }
                codeGenContext.addLabelPC(jumpTarget);
            } else if (opcode == Opcode.TFORLOOP) {
                // TFORLOOP指令：如果条件满足，pc += sBx
                int jumpTarget = i + 1 + inst.getSBx();
                if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                    isJumpTarget[jumpTarget] = true;
                }
                // TFORLOOP 的跳转目标也需要注册为 label PC，
                // 使得内层 for-in 的 backward JMP 能找到对应的 LabelStatement
                codeGenContext.addLabelPC(jumpTarget);
                // 注册 TFORLOOP 区域 PC，防止被 if-body 吞没
                codeGenContext.addTforRegionPC(i);        // TFORLOOP 本身
                codeGenContext.addTforRegionPC(jumpTarget); // 跳转目标 label
                if (i + 1 < instructions.size()) {
                    codeGenContext.addTforRegionPC(i + 1); // TFORLOOP 后面的 backward JMP
                }
            }
        }
        
        // 创建基本块
        BasicBlock currentBlock = new BasicBlock(0);
        basicBlocks.add(currentBlock);
        
        if (instructions.isEmpty()) {
            // 如果指令列表为空，设置当前块的结束索引为-1
            currentBlock.setEndIndex(-1);
            return;
        }
        
        // 构建基本块
        for (int i = 0; i < instructions.size(); i++) {
            if (instructionToBlockMap.containsKey(i)) {
                currentBlock = instructionToBlockMap.get(i);
            } else {
                // 如果当前指令是跳转目标，创建新的基本块
                if (isJumpTarget[i] && i > 0) {
                    currentBlock = new BasicBlock(i);
                    basicBlocks.add(currentBlock);
                }
                instructionToBlockMap.put(i, currentBlock);
            }
            currentBlock.setEndIndex(i);

            Opcode opcode = instructions.get(i).getOpcode();

            // 检查是否是基本块结束指令
            if (isBlockEndInstruction(opcode)) {
                if (i + 1 < instructions.size()) {
                    currentBlock = new BasicBlock(i + 1);
                    basicBlocks.add(currentBlock);
                    instructionToBlockMap.put(i + 1, currentBlock);
                }
            }
        }
    }

    /**
     * 检查是否是基本块结束指令
     * 
     * @param opcode 操作码
     * @return 是否是基本块结束指令
     */
    private boolean isBlockEndInstruction(Opcode opcode) {
        return opcode == Opcode.JMP || opcode == Opcode.RETURN ||
                opcode == Opcode.FORLOOP || opcode == Opcode.FORPREP ||
                opcode == Opcode.TFORLOOP ||
                opcode == Opcode.EQ || opcode == Opcode.LT || opcode == Opcode.LE;
    }

    /**
     * 构建控制流图
     * 
     * @param chunk 代码块
     */
    private void buildControlFlowGraph(Chunk chunk) {
        List<Instruction> instructions = chunk.getInstructions();

        for (int i = 0; i < instructions.size(); i++) {
            Instruction inst = instructions.get(i);
            BasicBlock currentBlock = instructionToBlockMap.get(i);

            Opcode opcode = inst.getOpcode();

            if (opcode == Opcode.JMP) {
                int target = i + 1 + inst.getSBx();
                addEdge(currentBlock, target);
            } else if (opcode == Opcode.TEST || opcode == Opcode.TESTSET) {
                // TEST always followed by JMP in if/else patterns
                if (i + 1 < instructions.size() && instructions.get(i + 1).getOpcode() == Opcode.JMP) {
                    handleTestInstruction(i, inst, instructions, currentBlock);
                    continue;
                }

                // fallback：普通顺序
                addEdge(currentBlock, i + 1);
            } else if (opcode == Opcode.EQ || opcode == Opcode.LT || opcode == Opcode.LE) {
                Instruction jmp = instructions.get(i + 1);
                int target = i + 1 + jmp.getSBx(); // 跳转目标

                // true → 跳
                addEdge(currentBlock, target);

                // false → 顺序执行
                addEdge(currentBlock, i + 2);

                continue;
            } else if (opcode == Opcode.FORLOOP) {
                // 处理FORLOOP指令
                int jumpTarget = inst.getSBx() == 0 ? inst.getNumericForPrepTarget(instructions, i) : i + 1 + inst.getSBx();
                BasicBlock targetBlock = instructionToBlockMap.get(jumpTarget);
                if (targetBlock != null) {
                    currentBlock.addSuccessor(targetBlock);
                }

                // FORLOOP 结束条件不满足时会流向下一条指令，所以需要添加下一条指令作为后继
                if (i + 1 < instructions.size()) {
                    BasicBlock nextBlock = instructionToBlockMap.get(i + 1);
                    if (nextBlock != null && nextBlock != currentBlock) {
                        currentBlock.addSuccessor(nextBlock);
                    }
                }
            } else if (opcode == Opcode.FORPREP) {
                // 处理FORPREP指令
                int jumpTarget = inst.getSBx() == 0 ? inst.getNumericForLoopTarget(instructions, i) : i + 1 + inst.getSBx();
                BasicBlock targetBlock = instructionToBlockMap.get(jumpTarget);
                if (targetBlock != null) {
                    currentBlock.addSuccessor(targetBlock);
                }

                // FORPREP指令执行后总是跳转到循环头，所以不需要添加下一条指令作为后继
            } else if (opcode == Opcode.TFORLOOP) {
                // 处理TFORLOOP指令
                int jumpTarget = i + 1 + inst.getSBx();
                BasicBlock targetBlock = instructionToBlockMap.get(jumpTarget);
                if (targetBlock != null) {
                    currentBlock.addSuccessor(targetBlock);
                }

                // TFORLOOP指令执行后，如果条件满足则跳转到循环头，否则继续执行下一条指令
                if (i + 1 < instructions.size()) {
                    BasicBlock nextBlock = instructionToBlockMap.get(i + 1);
                    if (nextBlock != null && nextBlock != currentBlock) {
                        currentBlock.addSuccessor(nextBlock);
                    }
                }
            } else if (opcode != Opcode.RETURN) {
                // 其他非返回指令，添加下一条指令作为后继
                if (i + 1 < instructions.size()) {
                    BasicBlock nextBlock = instructionToBlockMap.get(i + 1);
                    if (nextBlock != null && nextBlock != currentBlock) {
                        currentBlock.addSuccessor(nextBlock);
                    }
                }
            }
        }
    }

    private void handleTestInstruction(int i, Instruction inst, List<Instruction> instructions,
            BasicBlock currentBlock) {
        Instruction jmp = instructions.get(i + 1); // guaranteed JMP in TEST+JMP pattern

        int jmpTarget = i + 1 + jmp.getSBx();

        // TEST true → PC + 2 (进入 then)
        int thenTarget = i + 2;

        // TEST false → JMP target (进入 else)
        int elseTarget = jmpTarget;

        addEdge(currentBlock, thenTarget);
        addEdge(currentBlock, elseTarget);
    }

    private void addEdge(BasicBlock from, int instIndex) {
        BasicBlock to = instructionToBlockMap.get(instIndex);
        if (to != null) {
            from.addSuccessor(to);
        }
    }

    /**
     * 分析控制流，识别if-else和循环结构
     * 
     * @param chunk 代码块
     */
    private void analyzeControlFlow(Chunk chunk) {
        List<Instruction> instructions = chunk.getInstructions();

        // 首先标记所有包含条件指令的块
        for (BasicBlock block : basicBlocks) {
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
    }

    /**
     * 获取基本块列表
     * 
     * @return 基本块列表
     */
    public List<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    /**
     * 获取指令到基本块的映射
     * 
     * @return 指令到基本块的映射
     */
    public Map<Integer, BasicBlock> getInstructionToBlockMap() {
        return instructionToBlockMap;
    }

    /**
     * 根据指令索引获取基本块
     * 
     * @param instructionIndex 指令索引
     * @return 基本块
     */
    public BasicBlock getBlockByInstructionIndex(int instructionIndex) {
        return instructionToBlockMap.get(instructionIndex);
    }

    /**
     * 获取基本块的最后一条指令
     * 
     * @param block 基本块
     * @param chunk 代码块
     * @return 最后一条指令
     */
    public Instruction getLastInstruction(BasicBlock block, Chunk chunk) {
        if (block == null) {
            return null;
        }
        List<Instruction> instructions = chunk.getInstructions();
        int endIndex = block.getEndIndex();
        if (endIndex >= 0 && endIndex < instructions.size()) {
            return instructions.get(endIndex);
        }
        return null;
    }

    /**
     * 获取基本块的第一条指令
     * 
     * @param block 基本块
     * @param chunk 代码块
     * @return 第一条指令
     */
    public Instruction getFirstInstruction(BasicBlock block, Chunk chunk) {
        if (block == null) {
            return null;
        }
        List<Instruction> instructions = chunk.getInstructions();
        int startIndex = block.getStartIndex();
        if (startIndex >= 0 && startIndex < instructions.size()) {
            return instructions.get(startIndex);
        }
        return null;
    }

    /**
     * 获取基本块的下一个基本块（按索引顺序）
     * 
     * @param block 基本块
     * @return 下一个基本块
     */
    public BasicBlock getNextBlock(BasicBlock block) {
        if (block == null) {
            return null;
        }
        int currentIndex = basicBlocks.indexOf(block);
        if (currentIndex >= 0 && currentIndex < basicBlocks.size() - 1) {
            return basicBlocks.get(currentIndex + 1);
        }
        return null;
    }

    /**
     * 根据起始索引获取基本块
     * 
     * @param startIndex 起始索引
     * @return 基本块
     */
    public BasicBlock getBlockByStartIndex(int startIndex) {
        for (BasicBlock block : basicBlocks) {
            if (block.getStartIndex() == startIndex) {
                return block;
            }
        }
        return null;
    }
}
