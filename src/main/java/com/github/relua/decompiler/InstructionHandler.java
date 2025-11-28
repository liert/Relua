package com.github.relua.decompiler;

import com.github.relua.model.Chunk;
import com.github.relua.model.Constant;
import com.github.relua.model.Instruction;
import com.github.relua.model.Instruction.Opcode;
import com.github.relua.model.Register;
import com.github.relua.model.Register.RegisterEntity;
import com.github.relua.model.ValueType;

import java.util.ArrayList;
import java.util.Collections;
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
    
    private List<BasicBlock> basicBlocks;
    private Map<Integer, BasicBlock> instructionToBlockMap;
    private Register[] inStates; // 每条指令执行前的寄存器状态
    private Register[] outStates; // 每条指令执行后的寄存器状态
    
    /**
     * 处理代码块的指令
     * @param chunk 代码块
     */
    public void processChunk(Chunk chunk) {
        if (chunk == null) {
            return;
        }
        
        // 初始化数据结构
        basicBlocks = new ArrayList<>();
        instructionToBlockMap = new HashMap<>();
        
        // 构建基本块
        buildBasicBlocks(chunk);
        
        // 构建控制流图
        buildControlFlowGraph(chunk);
        
        // 初始化指令级寄存器状态
        int numInstructions = chunk.getInstructions().size();
        inStates = new Register[numInstructions];
        outStates = new Register[numInstructions];
        
        // 初始化所有状态为新的Register对象
        for (int i = 0; i < numInstructions; i++) {
            inStates[i] = new Register();
            outStates[i] = new Register();
        }
        
        // 执行迭代数据流分析
        iterativeDataFlowAnalysis(chunk);
        
        // 分析控制流
        analyzeControlFlow(chunk);
        
        // 递归处理子代码块
        for (Chunk subChunk : chunk.getSubChunks()) {
            processChunk(subChunk);
        }
    }
    
    /**
     * 构建基本块
     * @param chunk 代码块
     */
    private void buildBasicBlocks(Chunk chunk) {
        List<Instruction> instructions = chunk.getInstructions();
        if (instructions.isEmpty()) {
            return;
        }
        
        // 首先标记所有跳转目标指令
        boolean[] isJumpTarget = new boolean[instructions.size()];
        for (int i = 0; i < instructions.size(); i++) {
            Instruction inst = instructions.get(i);
            Opcode opcode = inst.getOpcode();
            
            // 检查所有可能产生跳转的指令
            if (opcode == Opcode.JMP) {
                int jumpTarget = i + 1 + inst.getSBx();
                if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                    isJumpTarget[jumpTarget] = true;
                }
            } else if (opcode == Opcode.TEST || opcode == Opcode.TESTSET) {
                // TEST/TESTSET指令本身不直接产生跳转，跳转由后续的JMP指令执行
                // 所以这里不需要标记跳转目标
            } else if (opcode == Opcode.EQ || opcode == Opcode.LT || opcode == Opcode.LE) {
                if (inst.getC() != 0) {
                    int jumpTarget = i + 1 + inst.getC();
                    if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                        isJumpTarget[jumpTarget] = true;
                    }
                }
            } else if (opcode == Opcode.FORLOOP) {
                // FORLOOP指令：pc += sBx
                int jumpTarget = i + 1 + inst.getSBx();
                if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                    isJumpTarget[jumpTarget] = true;
                }
            } else if (opcode == Opcode.FORPREP) {
                // FORPREP指令：pc += sBx
                int jumpTarget = i + 1 + inst.getSBx();
                if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                    isJumpTarget[jumpTarget] = true;
                }
            } else if (opcode == Opcode.TFORLOOP) {
                // TFORLOOP指令：如果条件满足，pc += sBx
                int jumpTarget = i + 1 + inst.getSBx();
                if (jumpTarget >= 0 && jumpTarget < instructions.size()) {
                    isJumpTarget[jumpTarget] = true;
                }
            }
        }
        
        // 创建基本块
        BasicBlock currentBlock = new BasicBlock(0);
        basicBlocks.add(currentBlock);
        
        for (int i = 0; i < instructions.size(); i++) {
            // 如果当前指令是跳转目标，创建新的基本块
            if (isJumpTarget[i] && i > 0) {
                currentBlock = new BasicBlock(i);
                basicBlocks.add(currentBlock);
                System.out.println("跳转目标指令: " + i);
            }
            
            Opcode opcode = instructions.get(i).getOpcode();
            
            // 处理TEST/TESTSET + JMP组合的特殊情况
            // 1. TEST/TESTSET单独成块
            if ((opcode == Opcode.TEST || opcode == Opcode.TESTSET) && currentBlock.getStartIndex() != i) {
                // 结束当前块，创建新块包含当前TEST/TESTSET指令
                currentBlock = new BasicBlock(i);
                basicBlocks.add(currentBlock);
                System.out.println("TEST指令单独块: " + i);
            }
            
            instructionToBlockMap.put(i, currentBlock);
            currentBlock.setEndIndex(i);
            
            // 2. 如果当前指令是TEST/TESTSET，且下一条指令是JMP，TEST/TESTSET单独成块
            if ((opcode == Opcode.TEST || opcode == Opcode.TESTSET) && i + 1 < instructions.size()) {
                Opcode nextOpcode = instructions.get(i + 1).getOpcode();
                if (nextOpcode == Opcode.JMP) {
                    // TEST/TESTSET单独成块，JMP将在下一次循环中处理
                    currentBlock = new BasicBlock(i + 1);
                    basicBlocks.add(currentBlock);
                    System.out.println("JMP指令单独块: " + (i + 1));
                    // 跳过下一条指令的处理，因为已经创建了块
                    i++;
                    instructionToBlockMap.put(i, currentBlock);
                    currentBlock.setEndIndex(i);
                    // JMP指令结束当前块
                    if (i + 1 < instructions.size()) {
                        currentBlock = new BasicBlock(i + 1);
                        basicBlocks.add(currentBlock);
                    }
                    continue;
                }
            }
            
            // 对于其他条件指令，确保它单独成为一个块
            // 如果当前块不是空的且当前指令是条件指令，先结束当前块
            if ((opcode == Opcode.EQ || opcode == Opcode.LT || opcode == Opcode.LE) && 
                currentBlock.getStartIndex() != i) {
                // 结束当前块，创建新块包含当前条件指令
                currentBlock = new BasicBlock(i);
                basicBlocks.add(currentBlock);
                System.out.println("条件指令单独块: " + i);
            }
            
            // 检查是否是基本块结束指令
            if (isBlockEndInstruction(opcode)) {
                // 跳过TEST/TESTSET指令的处理，因为它们已经单独成块
                if (opcode == Opcode.TEST || opcode == Opcode.TESTSET) {
                    continue;
                }
                
                if (i + 1 < instructions.size()) {
                    currentBlock = new BasicBlock(i + 1);
                    basicBlocks.add(currentBlock);
                }
                if (opcode == Opcode.RETURN) {
                    System.out.println("RETURN指令: " + i);
                }
            }
        }
    }
    
    /**
     * 检查是否是基本块结束指令
     * @param opcode 操作码
     * @return 是否是基本块结束指令
     */
    private boolean isBlockEndInstruction(Opcode opcode) {
        return opcode == Opcode.JMP || opcode == Opcode.RETURN || 
               opcode == Opcode.FORLOOP || opcode == Opcode.FORPREP ||
               opcode == Opcode.TFORLOOP ||
               opcode == Opcode.EQ || opcode == Opcode.LT || opcode == Opcode.LE ||
               opcode == Opcode.TEST || opcode == Opcode.TESTSET;
    }
    
    /**
     * 构建控制流图
     * @param chunk 代码块
     */
    private void buildControlFlowGraph(Chunk chunk) {
        List<Instruction> instructions = chunk.getInstructions();
        
        for (int i = 0; i < instructions.size(); i++) {
            Instruction inst = instructions.get(i);
            BasicBlock currentBlock = instructionToBlockMap.get(i);
            
            Opcode opcode = inst.getOpcode();
            
            if (opcode == Opcode.JMP) {
                // 处理跳转指令
                int jumpTarget = i + 1 + inst.getSBx();
                BasicBlock targetBlock = instructionToBlockMap.get(jumpTarget);
                if (targetBlock != null) {
                    currentBlock.addSuccessor(targetBlock);
                }
                
                // 如果不是无条件跳转，还需要添加下一条指令作为后继
                if (!isUnconditionalJump(inst)) {
                    if (i + 1 < instructions.size()) {
                        BasicBlock nextBlock = instructionToBlockMap.get(i + 1);
                        if (nextBlock != null) {
                            currentBlock.addSuccessor(nextBlock);
                        }
                    }
                }
            } else if (opcode == Opcode.TEST || opcode == Opcode.TESTSET) {
                // 处理测试指令
                int a = inst.getA();
                currentBlock.setConditionRegister(a);
                
                // 检查下一条指令是否是JMP
                if (i + 1 < instructions.size()) {
                    Instruction nextInst = instructions.get(i + 1);
                    if (nextInst.getOpcode() == Opcode.JMP) {
                        // 这是TEST+JMP组合，Lua编译器生成if-else的典型模式
                        // TEST指令根据条件跳转：如果条件为true，执行then分支；否则执行else分支
                        
                        // 1. 添加then分支作为后继（下一条指令的下一条）
                        if (i + 2 < instructions.size()) {
                            BasicBlock thenBlock = instructionToBlockMap.get(i + 2);
                            if (thenBlock != null) {
                                currentBlock.addSuccessor(thenBlock);
                            }
                        }
                        
                        // 2. 添加else分支作为后继（JMP的跳转目标）
                        int elseTarget = i + 2 + nextInst.getSBx();
                        BasicBlock elseBlock = instructionToBlockMap.get(elseTarget);
                        if (elseBlock != null) {
                            currentBlock.addSuccessor(elseBlock);
                        }
                    } else {
                        // 普通TEST指令，添加下一条指令作为后继
                        BasicBlock nextBlock = instructionToBlockMap.get(i + 1);
                        if (nextBlock != null) {
                            currentBlock.addSuccessor(nextBlock);
                        }
                    }
                }
            } else if (opcode == Opcode.EQ || opcode == Opcode.LT || opcode == Opcode.LE) {
                // 处理比较指令
                if (inst.getC() != 0) {
                    int jumpTarget = i + 1 + inst.getC();
                    BasicBlock targetBlock = instructionToBlockMap.get(jumpTarget);
                    if (targetBlock != null) {
                        currentBlock.addSuccessor(targetBlock);
                    }
                }
                
                // 总是添加下一条指令作为后继
                if (i + 1 < instructions.size()) {
                    BasicBlock nextBlock = instructionToBlockMap.get(i + 1);
                    if (nextBlock != null) {
                        currentBlock.addSuccessor(nextBlock);
                    }
                }
            } else if (opcode == Opcode.FORLOOP) {
                // 处理FORLOOP指令
                int jumpTarget = i + 1 + inst.getSBx();
                BasicBlock targetBlock = instructionToBlockMap.get(jumpTarget);
                if (targetBlock != null) {
                    currentBlock.addSuccessor(targetBlock);
                }
                
                // FORLOOP指令执行后总是跳转到循环头，所以不需要添加下一条指令作为后继
            } else if (opcode == Opcode.FORPREP) {
                // 处理FORPREP指令
                int jumpTarget = i + 1 + inst.getSBx();
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
                    if (nextBlock != null) {
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
    
    /**
     * 检查是否是无条件跳转
     * @param inst 指令
     * @return 是否是无条件跳转
     */
    private boolean isUnconditionalJump(Instruction inst) {
        // JMP指令如果没有设置条件位，则是无条件跳转
        return inst.getOpcode() == Opcode.JMP && inst.getA() == 0;
    }
    
    /**
     * 执行迭代数据流分析
     * @param chunk 代码块
     */
    private void iterativeDataFlowAnalysis(Chunk chunk) {
        boolean changed;
        List<Instruction> instructions = chunk.getInstructions();
        int numInstructions = instructions.size();
        
        // 初始化所有指令的输入和输出状态
        for (int i = 0; i < numInstructions; i++) {
            inStates[i] = new Register();
            outStates[i] = new Register();
        }
        
        // 初始化第一个指令的输入状态，设置pcall、dofile、_G、module等全局变量
        if (numInstructions > 0) {
            Register initialState = new Register();
            initialState.setRegisterEntity(0, "pcall", ValueType.GLOBAL);
            initialState.setRegisterEntity(1, "dofile", ValueType.GLOBAL);
            initialState.setRegisterEntity(2, "_G", ValueType.GLOBAL);
            initialState.setRegisterEntity(3, "module", ValueType.GLOBAL);
            inStates[0] = initialState;
        }
        
        do {
            changed = false;
            
            // 遍历所有基本块
            for (BasicBlock block : basicBlocks) {
                // 合并前驱块的输出状态作为当前块的输入状态
                Register mergedInput = mergePredecessors(block);
                
                // 如果输入状态没有变化，跳过
                if (mergedInput.equals(block.getInputState())) {
                    continue;
                }
                
                // 更新块的输入状态
                block.setInputState(mergedInput);
                
                // 模拟执行块内的指令，更新指令级寄存器状态
                Register currentState = new Register(mergedInput);
                for (int i = block.getStartIndex(); i <= block.getEndIndex(); i++) {
                    if (i < numInstructions) {
                        // 更新指令i的输入状态
                        if (!currentState.equals(inStates[i])) {
                            inStates[i] = new Register(currentState);
                            changed = true;
                        }
                        
                        // 处理指令，更新当前状态
                        processInstruction(chunk, instructions.get(i), i, currentState);
                        
                        // 更新指令i的输出状态
                        if (!currentState.equals(outStates[i])) {
                            outStates[i] = new Register(currentState);
                            changed = true;
                        }
                    }
                }
                
                // 更新块的输出状态
                if (!currentState.equals(block.getOutputState())) {
                    block.setOutputState(currentState);
                    changed = true;
                }
            }
        } while (changed);
    }
    
    /**
     * 合并前驱块的输出状态
     * @param block 当前块
     * @return 合并后的寄存器状态
     */
    public Register mergePredecessors(BasicBlock block) {
        Register merged = new Register();
        
        // 如果没有前驱，返回空状态
        if (block.getPredecessors().isEmpty()) {
            return merged;
        }
        
        // 获取第一个前驱的输出状态作为初始值
        BasicBlock firstPredecessor = block.getPredecessors().get(0);
        merged = new Register(firstPredecessor.getOutputState());
        
        // 合并其他前驱的输出状态
        for (int i = 1; i < block.getPredecessors().size(); i++) {
            BasicBlock predecessor = block.getPredecessors().get(i);
            merged = mergeRegisterStates(merged, predecessor.getOutputState());
        }
        
        return merged;
    }
    
    /**
     * 合并两个寄存器状态（PHI合并）
     * @param state1 第一个状态
     * @param state2 第二个状态
     * @return 合并后的状态
     */
    private Register mergeRegisterStates(Register state1, Register state2) {
        Register merged = new Register(state1);
        
        // 遍历state2的所有寄存器实体
        for (Map.Entry<Integer, RegisterEntity> entry : state2.getAllRegisterEntities().entrySet()) {
            int index = entry.getKey();
            RegisterEntity entity2 = entry.getValue();
            RegisterEntity entity1 = merged.getRegisterEntity(index);
            
            // 如果两个寄存器实体类型相同且值相同，保持不变
            if (entity1.getType() == entity2.getType() && 
                entity1.getValue().equals(entity2.getValue())) {
                continue;
            }
            
            // 否则，标记为UNKNOWN类型，等待后续指令进一步确定
            merged.setRegisterEntity(index, "R" + index, ValueType.UNKNOWN);
        }
        
        return merged;
    }
    
    /**
     * 处理单个指令
     * @param chunk 代码块
     * @param instruction 指令
     * @param index 指令索引
     * @param currentState 当前寄存器状态
     */
    private void processInstruction(Chunk chunk, Instruction instruction, int index, Register currentState) {
        Opcode opcode = instruction.getOpcode();
        
        // 根据操作码处理不同类型的指令
        switch (opcode) {
            case MOVE:          // 寄存器间数据移动
                processMoveInstruction(chunk, instruction, currentState);
                break;
            case LOADK:         // 加载常量到寄存器
                processLoadKInstruction(chunk, instruction, currentState);
                break;
            case LOADBOOL:      // 加载布尔值到寄存器
                processLoadBoolInstruction(chunk, instruction, currentState);
                break;
            case LOADNIL:       // 加载nil到寄存器
                processLoadNilInstruction(chunk, instruction, currentState);
                break;
            case GETGLOBAL:     // 获取全局变量
                processGetGlobalInstruction(chunk, instruction, currentState);
                break;
            case SETGLOBAL:     // 设置全局变量
                processSetGlobalInstruction(chunk, instruction, currentState);
                break;
            case GETTABLE:      // 获取表元素
                processGetTableInstruction(chunk, instruction, currentState);
                break;
            case SETTABLE:      // 设置表元素
                processSetTableInstruction(chunk, instruction, currentState);
                break;
            case ADD:           // 加法
            case SUB:           // 减法
            case MUL:           // 乘法
            case DIV:           // 除法
            case MOD:           // 取模
            case POW:           // 幂运算
                processArithmeticInstruction(chunk, instruction, currentState);
                break;
            case UNM:           // 取反
            case NOT:           // 逻辑非
            case LEN:           // 长度
                processUnaryInstruction(chunk, instruction, currentState);
                break;
            case CONCAT:        // 字符串连接
                processConcatInstruction(chunk, instruction, currentState);
                break;
            case JMP:           // 跳转
                // 跳转指令不改变寄存器状态
                break;
            case EQ:            // 等于
            case LT:            // 小于
            case LE:            // 小于等于
                // 比较指令不改变寄存器状态
                break;
            case TEST:          // 测试
            case TESTSET:       // 测试并设置
                processTestInstruction(chunk, instruction, currentState);
                break;
            case CALL:          // 函数调用
            case TAILCALL:      // 尾调用
                processCallInstruction(chunk, instruction, currentState);
                break;
            case RETURN:        // 返回
                // 返回指令不改变寄存器状态
                break;
            case FORLOOP:       // for循环
            case FORPREP:       // for循环准备
            case TFORLOOP:      // 泛型for循环
                processLoopInstruction(chunk, instruction, currentState);
                break;
            case SETLIST:       // 设置表列表
                processSetListInstruction(chunk, instruction, currentState);
                break;
            case CLOSE:         // 关闭upvalue
                processCloseInstruction(chunk, instruction, currentState);
                break;
            case CLOSURE:       // 创建闭包
                processClosureInstruction(chunk, instruction, currentState);
                break;
            case VARARG:        // 可变参数
                processVarargInstruction(chunk, instruction, currentState);
                break;
            case GETUPVAL:      // 获取upvalue
            case SETUPVAL:      // 设置upvalue
                processUpvalInstruction(chunk, instruction, currentState);
                break;
            default:
                // 未知指令，暂时忽略
                break;
        }
    }
    
    // 以下是各种指令的处理方法
    
    private void processMoveInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 记录寄存器间的移动，用于变量跟踪
        int a = instruction.getA();
        int b = instruction.getB();
        
        // 获取源寄存器的实体
        RegisterEntity srcEntity = currentState.getRegisterEntity(b);
        
        // 复制源寄存器的完整状态到目标寄存器
        // 确保复制所有属性，包括值和类型
        currentState.setRegisterEntity(a, srcEntity.getValue(), srcEntity.getType());
        
        // 调试信息
        System.out.println(String.format("MOVE: R%d = R%d (%s, %s)", a, b, srcEntity.getValue(), srcEntity.getType()));
    }
    
    private void processLoadKInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 加载常量到寄存器，记录寄存器状态
        int a = instruction.getA();
        int bx = instruction.getBx();
        
        if (bx < chunk.getConstants().size()) {
            Constant constant = chunk.getConstants().get(bx);
            Object value = constant.getValue();
            ValueType type = constant.getType();
            
            // 处理字符串类型，去除引号
            if (type == ValueType.STRING) {
                String strValue = value.toString();
                if (strValue.startsWith("\"") && strValue.endsWith("\"")) {
                    value = strValue.substring(1, strValue.length() - 1);
                }
            }
            
            currentState.setRegisterEntity(a, value, type);
        }
    }
    
    private void processLoadBoolInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 加载布尔值到寄存器
        int a = instruction.getA();
        boolean boolValue = instruction.getB() != 0;
        currentState.setRegisterEntity(a, boolValue, ValueType.BOOLEAN);
    }
    
    private void processLoadNilInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 加载nil到寄存器
        int a = instruction.getA();
        int b = instruction.getB();
        for (int i = a; i <= b; i++) {
            currentState.setRegisterEntity(i, "nil", ValueType.NIL);
        }
    }
    
    private void processGetGlobalInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 获取全局变量，记录寄存器状态
        int a = instruction.getA();
        int bx = instruction.getBx();
        
        if (bx < chunk.getConstants().size()) {
            String varName = chunk.getConstants().get(bx).getValue().toString();
            if (varName.startsWith("\"") && varName.endsWith("\"")) {
                varName = varName.substring(1, varName.length() - 1);
            }
            currentState.setRegisterEntity(a, varName, ValueType.GLOBAL);
        }
    }
    
    private void processSetGlobalInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 设置全局变量
        int a = instruction.getA();
        int bx = instruction.getBx();
        
        if (bx < chunk.getConstants().size()) {
            String varName = chunk.getConstants().get(bx).getValue().toString();
            if (varName.startsWith("\"") && varName.endsWith("\"")) {
                varName = varName.substring(1, varName.length() - 1);
            }
            // 设置全局变量时，将寄存器标记为全局变量
            currentState.setRegisterEntity(a, varName, ValueType.GLOBAL);
        }
    }
    
    private void processGetTableInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 获取表元素
        int a = instruction.getA();
        // 简单处理：记录为表访问
        currentState.setRegisterEntity(a, "table_access", ValueType.TABLE);
    }
    
    private void processSetTableInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 设置表元素
        // 不修改寄存器状态
    }
    
    private void processArithmeticInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理算术指令
        int a = instruction.getA();
        // 简单处理：记录为数字类型
        currentState.setRegisterEntity(a, "arithmetic_result", ValueType.NUMBER);
    }
    
    private void processUnaryInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理一元操作指令
        int a = instruction.getA();
        int b = instruction.getB();
        // 简单处理：保持原类型
        currentState.move(a, b);
    }
    
    private void processConcatInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理CONCAT指令
        int a = instruction.getA();
        // 简单处理：记录为字符串类型
        currentState.setRegisterEntity(a, "concat_result", ValueType.STRING);
    }
    
    private void processTestInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理测试指令
        // 不修改寄存器状态
    }
    
    private void processCallInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理函数调用指令
        int a = instruction.getA();
        int b = instruction.getB();
        
        // 获取函数实体
        RegisterEntity funcEntity = currentState.getRegisterEntity(a);
        
        // 函数调用结果的类型暂时设为 UNKNOWN
        // 对于pcall等特殊函数，保留其名称
        if (funcEntity.getType() == ValueType.FUNCTION || funcEntity.getType() == ValueType.GLOBAL) {
            String funcName = funcEntity.getValue().toString();
            // 对于pcall等特殊函数，保留其名称作为结果
            if (funcName.equals("pcall")) {
                // pcall返回两个值：success和result，这里简化处理
                currentState.setRegisterEntity(a, true, ValueType.BOOLEAN);
            } else if (funcName.equals("dofile")) {
                // dofile函数返回执行结果
                currentState.setRegisterEntity(a, "dofile_result", ValueType.UNKNOWN);
            } else if (funcName.equals("module")) {
                // module函数不返回值，或者返回模块本身
                currentState.setRegisterEntity(a, funcName, ValueType.MODULE);
            } else {
                // 其他函数调用，保留函数名
                currentState.setRegisterEntity(a, funcName, ValueType.UNKNOWN);
            }
        } else {
            // 否则，使用默认的 "call_result"
            currentState.setRegisterEntity(a, "call_result", ValueType.UNKNOWN);
        }
    }
    
    private void processReturnInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理返回指令
        // 不修改寄存器状态
    }
    
    private void processLoopInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理循环指令
        // 不修改寄存器状态
    }
    
    private void processSetListInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理SETLIST指令
        // 不修改寄存器状态
    }
    
    private void processCloseInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理CLOSE指令
        // 不修改寄存器状态
    }
    
    private void processClosureInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理CLOSURE指令
        int a = instruction.getA();
        // 记录为函数类型
        currentState.setRegisterEntity(a, "closure", ValueType.FUNCTION);
    }
    
    private void processVarargInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理VARARG指令
        // 不修改寄存器状态
    }
    
    private void processUpvalInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理upvalue指令
        // 不修改寄存器状态
    }
    
    /**
     * 分析控制流，识别if-else和循环结构
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
        
        // 使用新的if-else识别算法
        for (BasicBlock block : basicBlocks) {
            // 检测if-else结构
            IfElsePattern pattern = detectIfElse(block, chunk);
            if (pattern != null) {
                // 标记为if块
                pattern.testBlock.setIfBlock(true);
                
                // 标记else块
                pattern.elseBlock.setElseBlock(true);
                
                // 打印识别到的if-else结构
                System.out.println("识别到if-else结构:");
                System.out.println("  testBlock: " + pattern.testBlock.getStartIndex() + "-" + pattern.testBlock.getEndIndex());
                System.out.println("  thenBlock: " + pattern.thenBlock.getStartIndex() + "-" + pattern.thenBlock.getEndIndex());
                System.out.println("  elseBlock: " + pattern.elseBlock.getStartIndex() + "-" + pattern.elseBlock.getEndIndex());
                System.out.println("  endBlock: " + pattern.endBlock.getStartIndex() + "-" + pattern.endBlock.getEndIndex());
            }
        }
        
        // 使用新的循环检测算法
        if (!basicBlocks.isEmpty()) {
            // 计算支配关系
            BasicBlock entry = basicBlocks.get(0); // 假设第一个基本块是入口
            Map<BasicBlock, Set<BasicBlock>> dom = computeDominators(basicBlocks, entry);
            
            // 查找出口基本块
            BasicBlock exit = findExitBlock(basicBlocks);
            System.out.println("出口基本块: " + exit.getStartIndex() + "-" + exit.getEndIndex());
            
            // 计算后支配关系
            Map<BasicBlock, Set<BasicBlock>> postDom = computePostDominators(basicBlocks, exit);
            
            // 检测SESE区域
            List<SESERegion> regions = detectSESERegions(basicBlocks, dom, postDom, chunk);
            
            // 打印检测到的SESE区域
            System.out.println("检测到的SESE区域数量: " + regions.size());
            for (int i = 0; i < regions.size(); i++) {
                SESERegion region = regions.get(i);
                System.out.println("SESE区域 " + (i + 1) + ":");
                System.out.println("  类型: " + region.getType());
                System.out.println("  入口: " + region.getEntry().getStartIndex() + "-" + region.getEntry().getEndIndex());
                System.out.println("  出口: " + region.getExit().getStartIndex() + "-" + region.getExit().getEndIndex());
                System.out.println("  块数量: " + region.getBlocks().size());
                System.out.print("  块范围: ");
                for (BasicBlock regionBlock : region.getBlocks()) {
                    System.out.print(regionBlock.getStartIndex() + "-" + regionBlock.getEndIndex() + " ");
                }
                System.out.println();
            }
            
            // 折叠SESE区域
            List<SESERegion> collapsedRegions = collapseRegions(regions, dom, postDom);
            
            // 打印折叠后的SESE区域
            System.out.println("折叠后的SESE区域数量: " + collapsedRegions.size());
            for (int i = 0; i < collapsedRegions.size(); i++) {
                SESERegion region = collapsedRegions.get(i);
                System.out.println("折叠后的SESE区域 " + (i + 1) + ":");
                System.out.println("  类型: " + region.getType());
                System.out.println("  入口: " + region.getEntry().getStartIndex() + "-" + region.getEntry().getEndIndex());
                System.out.println("  出口: " + region.getExit().getStartIndex() + "-" + region.getExit().getEndIndex());
                System.out.println("  块数量: " + region.getBlocks().size());
                System.out.print("  块范围: ");
                for (BasicBlock regionBlock : region.getBlocks()) {
                    System.out.print(regionBlock.getStartIndex() + "-" + regionBlock.getEndIndex() + " ");
                }
                System.out.println();
            }
            
            // 生成AST节点
            ASTNode ast = generateAST(collapsedRegions, chunk);
            System.out.println("AST生成完成，根节点类型: " + ast.getType());
            System.out.println("AST子节点数量: " + ast.getChildren().size());
            
            // 查找回边
            List<BasicBlock[]> backEdges = findBackEdges(basicBlocks, dom);
            
            // 构建并标记自然循环
        Set<BasicBlock> allLoopBlocks = new HashSet<>();
        for (BasicBlock[] backEdge : backEdges) {
            BasicBlock u = backEdge[0];
            BasicBlock v = backEdge[1];
            
            // 构建自然循环
            Set<BasicBlock> loop = buildNaturalLoop(u, v);
            
            // 只有当循环包含至少两个块时，才标记为循环块
            if (loop.size() >= 2) {
                // 标记循环块
                for (BasicBlock loopBlock : loop) {
                    loopBlock.setLoopBlock(true);
                    allLoopBlocks.add(loopBlock);
                }
                
                // 打印识别到的循环
                System.out.println("识别到循环:");
                System.out.println("  循环头: " + v.getStartIndex() + "-" + v.getEndIndex());
                System.out.println("  循环块数量: " + loop.size());
                System.out.print("  循环块范围: ");
                for (BasicBlock loopBlock : loop) {
                    System.out.print(loopBlock.getStartIndex() + "-" + loopBlock.getEndIndex() + " ");
                }
                System.out.println();
            }
        }
        
        // 清除非循环块的循环标记
        for (BasicBlock block : basicBlocks) {
            if (!allLoopBlocks.contains(block)) {
                block.setLoopBlock(false);
            }
        }
        }
    }
    
    // getter方法，供代码生成器使用
    public List<BasicBlock> getBasicBlocks() { return basicBlocks; }
    public Map<Integer, BasicBlock> getInstructionToBlockMap() { return instructionToBlockMap; }
    
    /**
     * 获取基本块的最后一条指令
     */
    public Instruction getLastInstruction(BasicBlock block, Chunk chunk) {
        if (block == null) return null;
        List<Instruction> instructions = chunk.getInstructions();
        int endIndex = block.getEndIndex();
        if (endIndex >= 0 && endIndex < instructions.size()) {
            return instructions.get(endIndex);
        }
        return null;
    }
    
    /**
     * 获取基本块的第一条指令
     */
    public Instruction getFirstInstruction(BasicBlock block, Chunk chunk) {
        if (block == null) return null;
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
    public BasicBlock getNextBlock(BasicBlock block) {
        if (block == null) return null;
        int currentIndex = basicBlocks.indexOf(block);
        if (currentIndex >= 0 && currentIndex < basicBlocks.size() - 1) {
            return basicBlocks.get(currentIndex + 1);
        }
        return null;
    }
    
    /**
     * 根据起始索引获取基本块
     */
    public BasicBlock getBlockByStartIndex(int startIndex) {
        for (BasicBlock block : basicBlocks) {
            if (block.getStartIndex() == startIndex) {
                return block;
            }
        }
        return null;
    }
    
    /**
     * 检测if-else结构
     */
    public IfElsePattern detectIfElse(BasicBlock testBlock, Chunk chunk) {
        Instruction last = getLastInstruction(testBlock, chunk);
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
        
        // 条件块应该有两个后继：then分支和else分支
        List<BasicBlock> successors = testBlock.getSuccessors();
        if (successors.size() != 2) {
            return null;
        }
        
        // 确定thenBlock和elseBlock
        BasicBlock thenBlock = successors.get(0);
        BasicBlock elseBlock = successors.get(1);
        
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
     * 根据指令索引获取寄存器状态
     * @param instructionIndex 指令索引
     * @return 该指令对应的寄存器
     */
    public Register getRegisterByInstructionIndex(int instructionIndex) {
        if (instructionIndex >= 0 && instructionIndex < inStates.length) {
            return inStates[instructionIndex];
        }
        return new Register();
    }
    
    /**
     * 获取寄存器名，优先使用已知的变量名或值，否则使用R+寄存器号
     * @param register 寄存器号
     * @param instructionIndex 指令索引，用于获取正确的寄存器状态
     * @return 寄存器名或变量名
     */
    public String getRegisterName(int register, int instructionIndex) {
        Register registerStates = getRegisterByInstructionIndex(instructionIndex);
        RegisterEntity entity = registerStates.getRegisterEntity(register);
        
        // 对于全局变量、函数、字符串等类型，返回实际值
        if (entity.getType() == ValueType.GLOBAL || 
            entity.getType() == ValueType.FUNCTION ||
            entity.getType() == ValueType.STRING ||
            entity.getType() == ValueType.BOOLEAN ||
            entity.getType() == ValueType.NUMBER) {
            Object value = entity.getValue();
            if (value != null) {
                if (entity.getType() == ValueType.STRING) {
                    // 字符串需要添加引号
                    return String.format("\"%s\"", value.toString());
                }
                return value.toString();
            }
        }
        
        // 对于表访问，返回表访问表达式
        if (entity.getType() == ValueType.TABLE) {
            return entity.getValue().toString();
        }
        
        // 对于其他类型，返回寄存器名
        return "R" + register;
    }
    
    /**
     * 获取寄存器名，使用当前寄存器状态
     * @param register 寄存器号
     * @return 寄存器名或变量名
     */
    public String getRegisterName(int register) {
        return getRegisterName(register, 0);
    }
    
    /**
     * 计算所有基本块的支配关系
     * @param blocks 基本块列表
     * @param entry 入口基本块
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
     * @param dom 支配关系映射
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
     * @param blocks 基本块列表
     * @param dom 支配关系映射
     * @return 回边列表，每个回边是一个包含两个基本块的数组 [u, v]，其中 u->v 是回边
     */
    public List<BasicBlock[]> findBackEdges(List<BasicBlock> blocks, Map<BasicBlock, Set<BasicBlock>> dom) {
        List<BasicBlock[]> backEdges = new ArrayList<>();
        
        for (BasicBlock u : blocks) {
            for (BasicBlock v : u.getSuccessors()) {
                // 如果 v 支配 u，并且 u != v，则 u->v 是回边
                // 排除自环，只处理真正的循环回边
                if (u != v && dom.get(v).contains(u)) {
                    backEdges.add(new BasicBlock[]{u, v});
                }
            }
        }
        
        return backEdges;
    }
    
    /**
     * 构建自然循环
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
     * @param blocks 基本块列表
     * @param exit 出口基本块
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
     * @param postDom 后支配关系映射
     * @param successors 后继基本块列表（在反转图中是前驱）
     * @return 后继后支配者的交集
     */
    private Set<BasicBlock> intersectPostDominators(Map<BasicBlock, Set<BasicBlock>> postDom, List<BasicBlock> successors) {
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
     * @param blocks 基本块列表
     * @param dom 支配关系映射
     * @param postDom 后支配关系映射
     * @param chunk 代码块，用于指令分析
     * @return SESE区域列表
     */
    public List<SESERegion> detectSESERegions(List<BasicBlock> blocks, Map<BasicBlock, Set<BasicBlock>> dom, Map<BasicBlock, Set<BasicBlock>> postDom, Chunk chunk) {
        List<SESERegion> regions = new ArrayList<>();
        
        // 首先检测简单的if-then-else结构
        for (BasicBlock block : blocks) {
            // 检测if-else结构
            IfElsePattern pattern = detectIfElse(block, chunk);
            if (pattern != null) {
                // 检查是否形成SESE区域
                Set<BasicBlock> regionBlocks = new HashSet<>();
                regionBlocks.add(pattern.testBlock);
                regionBlocks.add(pattern.thenBlock);
                regionBlocks.add(pattern.elseBlock);
                regionBlocks.add(pattern.endBlock);
                
                // 检查是否满足SESE条件
                if (isSESE(regionBlocks, pattern.testBlock, pattern.endBlock, dom, postDom)) {
                    SESERegion region = new SESERegion(pattern.testBlock, pattern.endBlock, regionBlocks, SESERegion.RegionType.IF_THEN_ELSE);
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
     * @param blocks 基本块列表
     * @param regions 区域列表，用于添加检测到的区域
     * @param dom 支配关系映射
     * @param postDom 后支配关系映射
     */
    private void detectSequenceRegions(List<BasicBlock> blocks, List<SESERegion> regions, Map<BasicBlock, Set<BasicBlock>> dom, Map<BasicBlock, Set<BasicBlock>> postDom) {
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
     * @param blocks 基本块集合
     * @param entry 入口基本块
     * @param exit 出口基本块
     * @param dom 支配关系映射
     * @param postDom 后支配关系映射
     * @return 是否形成SESE区域
     */
    private boolean isSESE(Set<BasicBlock> blocks, BasicBlock entry, BasicBlock exit, Map<BasicBlock, Set<BasicBlock>> dom, Map<BasicBlock, Set<BasicBlock>> postDom) {
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
     * @param regions 原始SESE区域列表
     * @param dom 支配关系映射
     * @param postDom 后支配关系映射
     * @return 折叠后的SESE区域列表
     */
    public List<SESERegion> collapseRegions(List<SESERegion> regions, Map<BasicBlock, Set<BasicBlock>> dom, Map<BasicBlock, Set<BasicBlock>> postDom) {
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
                    SESERegion singleRegion = new SESERegion(block, block, singleBlockSet, SESERegion.RegionType.SIMPLE_BLOCK);
                    collapsedRegions.add(singleRegion);
                    processedBlocks.add(block);
                }
            }
        }
        
        return collapsedRegions;
    }
    

    
    /**
     * 从SESE区域生成AST节点
     * @param regions 折叠后的SESE区域列表
     * @param chunk 代码块
     * @return 生成的AST根节点
     */
    public ASTNode generateAST(List<SESERegion> regions, Chunk chunk) {
        System.out.println("   - 创建AST根节点，类型: PROGRAM");
        ASTNode root = new ASTNode(ASTNode.NodeType.PROGRAM);
        ASTNode block = new ASTNode(ASTNode.NodeType.BLOCK);
        root.addChild(block);
        System.out.println("   - 添加根块节点，类型: BLOCK");
        
        // 按入口块的起始索引排序区域
        System.out.println("   - 按入口块起始索引排序SESE区域");
        regions.sort((r1, r2) -> Integer.compare(r1.getEntry().getStartIndex(), r2.getEntry().getStartIndex()));
        
        // 为每个区域生成AST节点
        System.out.println("   - 为每个SESE区域生成AST节点:");
        for (int i = 0; i < regions.size(); i++) {
            SESERegion region = regions.get(i);
            System.out.println("     区域 " + (i+1) + ": 类型=" + region.getType() + ", 入口=" + region.getEntry().getStartIndex() + "-" + region.getEntry().getEndIndex() + ", 出口=" + region.getExit().getStartIndex() + "-" + region.getExit().getEndIndex() + ", 块数量=" + region.getBlocks().size());
            ASTNode regionNode = generateRegionAST(region, chunk);
            System.out.println("     生成区域AST节点，类型: " + regionNode.getType());
            block.addChild(regionNode);
        }
        
        // 如果没有SESE区域，直接添加所有基本块
        if (regions.isEmpty()) {
            System.out.println("   - 没有SESE区域，直接添加所有基本块:");
            for (int i = 0; i < basicBlocks.size(); i++) {
                BasicBlock basicBlock = basicBlocks.get(i);
                System.out.println("     基本块 " + i + ": 范围=" + basicBlock.getStartIndex() + "-" + basicBlock.getEndIndex());
                ASTNode basicBlockNode = generateBasicBlockAST(basicBlock, chunk);
                System.out.println("     生成基本块AST节点，子节点数量: " + basicBlockNode.getChildren().size());
                block.addChild(basicBlockNode);
            }
        }
        
        return root;
    }
    
    /**
     * 从代码块生成AST节点
     * @param chunk 代码块
     * @return 生成的AST根节点
     */
    public ASTNode generateASTFromChunk(Chunk chunk) {
        System.out.println("\n=== 开始生成AST ===");
        
        // 计算支配关系
        System.out.println("1. 计算支配关系...");
        BasicBlock entry = basicBlocks.get(0); // 假设第一个基本块是入口
        Map<BasicBlock, Set<BasicBlock>> dom = computeDominators(basicBlocks, entry);
        System.out.println("   支配关系计算完成，基本块数量: " + basicBlocks.size());
        
        // 查找出口基本块
        System.out.println("2. 查找出口基本块...");
        BasicBlock exit = findExitBlock(basicBlocks);
        System.out.println("   出口基本块: " + exit.getStartIndex() + "-" + exit.getEndIndex());
        
        // 计算后支配关系
        System.out.println("3. 计算后支配关系...");
        Map<BasicBlock, Set<BasicBlock>> postDom = computePostDominators(basicBlocks, exit);
        System.out.println("   后支配关系计算完成");
        
        // 检测SESE区域
        System.out.println("4. 检测SESE区域...");
        List<SESERegion> regions = detectSESERegions(basicBlocks, dom, postDom, chunk);
        System.out.println("   检测到SESE区域数量: " + regions.size());
        
        // 折叠SESE区域
        System.out.println("5. 折叠SESE区域...");
        List<SESERegion> collapsedRegions = collapseRegions(regions, dom, postDom);
        System.out.println("   折叠后SESE区域数量: " + collapsedRegions.size());
        
        // 生成AST
        System.out.println("6. 生成AST节点...");
        ASTNode ast = generateAST(collapsedRegions, chunk);
        System.out.println("   AST生成完成，根节点类型: " + ast.getType());
        System.out.println("   AST子节点数量: " + ast.getChildren().size());
        
        System.out.println("=== AST生成完成 ===\n");
        return ast;
    }
    
    /**
     * 为单个SESE区域生成AST节点
     * @param region SESE区域
     * @param chunk 代码块
     * @return 生成的AST节点
     */
    private ASTNode generateRegionAST(SESERegion region, Chunk chunk) {
        System.out.println("     - 生成区域AST，类型: " + region.getType());
        
        ASTNode regionNode;
        switch (region.getType()) {
            case IF_THEN:
                System.out.println("     - 生成IF_THEN AST节点");
                regionNode = generateIfThenAST(region, chunk);
                break;
            case IF_THEN_ELSE:
                System.out.println("     - 生成IF_THEN_ELSE AST节点");
                regionNode = generateIfThenElseAST(region, chunk);
                break;
            case WHILE_LOOP:
                System.out.println("     - 生成WHILE_LOOP AST节点");
                regionNode = generateWhileLoopAST(region, chunk);
                break;
            case SEQUENCE:
                System.out.println("     - 生成SEQUENCE AST节点");
                regionNode = generateSequenceAST(region, chunk);
                break;
            case SIMPLE_BLOCK:
                System.out.println("     - 生成SIMPLE_BLOCK AST节点");
                regionNode = generateSimpleBlockAST(region, chunk);
                break;
            default:
                System.out.println("     - 生成默认BLOCK AST节点，类型: " + region.getType());
                // 对于其他类型的区域，生成默认的块节点
                regionNode = new ASTNode(ASTNode.NodeType.BLOCK);
                // 添加区域内的所有指令对应的AST节点
                for (BasicBlock block : region.getBlocks()) {
                    System.out.println("     - 添加基本块到默认BLOCK: " + block.getStartIndex() + "-" + block.getEndIndex());
                    ASTNode basicBlockNode = generateBasicBlockAST(block, chunk);
                    regionNode.addChild(basicBlockNode);
                }
                break;
        }
        
        System.out.println("     - 区域AST生成完成，类型: " + regionNode.getType() + ", 子节点数量: " + regionNode.getChildren().size());
        return regionNode;
    }
    
    /**
     * 为if-then结构生成AST节点
     * @param region SESE区域
     * @param chunk 代码块
     * @return 生成的AST节点
     */
    private ASTNode generateIfThenAST(SESERegion region, Chunk chunk) {
        ASTNode ifNode = new ASTNode(ASTNode.NodeType.IF_STATEMENT);
        
        // 生成条件表达式节点
        ASTNode conditionNode = new ASTNode(ASTNode.NodeType.EXPRESSION);
        // 这里简化处理，实际应该从条件指令生成准确的条件表达式
        conditionNode.setValue("condition");
        ifNode.addChild(conditionNode);
        
        // 生成then块节点
        ASTNode thenBlockNode = new ASTNode(ASTNode.NodeType.BLOCK);
        // 添加then块内的所有指令对应的AST节点
        for (BasicBlock block : region.getBlocks()) {
            if (block != region.getEntry()) { // 跳过条件块
                ASTNode basicBlockNode = generateBasicBlockAST(block, chunk);
                thenBlockNode.addChild(basicBlockNode);
            }
        }
        ifNode.addChild(thenBlockNode);
        
        return ifNode;
    }
    
    /**
     * 为简单块结构生成AST节点
     * @param region SESE区域
     * @param chunk 代码块
     * @return 生成的AST节点
     */
    private ASTNode generateSimpleBlockAST(SESERegion region, Chunk chunk) {
        ASTNode blockNode = new ASTNode(ASTNode.NodeType.BLOCK);
        // 添加区域内的所有指令对应的AST节点
        for (BasicBlock block : region.getBlocks()) {
            ASTNode basicBlockNode = generateBasicBlockAST(block, chunk);
            blockNode.addChild(basicBlockNode);
        }
        return blockNode;
    }
    
    /**
     * 为if-then-else结构生成AST节点
     * @param region SESE区域
     * @param chunk 代码块
     * @return 生成的AST节点
     */
    private ASTNode generateIfThenElseAST(SESERegion region, Chunk chunk) {
        ASTNode ifNode = new ASTNode(ASTNode.NodeType.IF_STATEMENT);
        
        // 生成条件表达式节点
        ASTNode conditionNode = new ASTNode(ASTNode.NodeType.EXPRESSION);
        // 这里简化处理，实际应该从条件指令生成准确的条件表达式
        conditionNode.setValue("condition");
        ifNode.addChild(conditionNode);
        
        // 生成then块节点
        ASTNode thenBlockNode = new ASTNode(ASTNode.NodeType.BLOCK);
        // 生成else块节点
        ASTNode elseBlockNode = new ASTNode(ASTNode.NodeType.ELSE_CLAUSE);
        ASTNode elseBodyNode = new ASTNode(ASTNode.NodeType.BLOCK);
        elseBlockNode.addChild(elseBodyNode);
        
        // 区分then块和else块
        boolean isThenBlock = true;
        for (BasicBlock block : region.getBlocks()) {
            if (block == region.getEntry()) {
                // 跳过条件块
                continue;
            }
            
            ASTNode basicBlockNode = generateBasicBlockAST(block, chunk);
            if (isThenBlock) {
                thenBlockNode.addChild(basicBlockNode);
                // 如果块有两个后继，说明是then块结束
                if (block.getSuccessors().size() == 2) {
                    isThenBlock = false;
                }
            } else {
                elseBodyNode.addChild(basicBlockNode);
            }
        }
        
        ifNode.addChild(thenBlockNode);
        ifNode.addChild(elseBlockNode);
        
        return ifNode;
    }
    
    /**
     * 为while循环结构生成AST节点
     * @param region SESE区域
     * @param chunk 代码块
     * @return 生成的AST节点
     */
    private ASTNode generateWhileLoopAST(SESERegion region, Chunk chunk) {
        ASTNode whileNode = new ASTNode(ASTNode.NodeType.WHILE_LOOP);
        
        // 生成条件表达式节点
        ASTNode conditionNode = new ASTNode(ASTNode.NodeType.EXPRESSION);
        // 这里简化处理，实际应该从条件指令生成准确的条件表达式
        conditionNode.setValue("condition");
        whileNode.addChild(conditionNode);
        
        // 生成循环体节点
        ASTNode bodyNode = new ASTNode(ASTNode.NodeType.BLOCK);
        // 添加循环体内的所有指令对应的AST节点
        for (BasicBlock block : region.getBlocks()) {
            if (block != region.getEntry()) { // 跳过条件块
                ASTNode basicBlockNode = generateBasicBlockAST(block, chunk);
                bodyNode.addChild(basicBlockNode);
            }
        }
        whileNode.addChild(bodyNode);
        
        return whileNode;
    }
    
    /**
     * 为顺序结构生成AST节点
     * @param region SESE区域
     * @param chunk 代码块
     * @return 生成的AST节点
     */
    private ASTNode generateSequenceAST(SESERegion region, Chunk chunk) {
        ASTNode sequenceNode = new ASTNode(ASTNode.NodeType.SEQUENCE);
        
        // 按顺序添加区域内的所有块对应的AST节点
        for (BasicBlock block : region.getBlocks()) {
            ASTNode blockNode = generateBasicBlockAST(block, chunk);
            sequenceNode.addChild(blockNode);
        }
        
        return sequenceNode;
    }
    
    /**
     * 为简单基本块生成AST节点
     * @param block 基本块
     * @param chunk 代码块
     * @return 生成的AST节点
     */
    private ASTNode generateBasicBlockAST(BasicBlock block, Chunk chunk) {
        System.out.println("       - 生成基本块AST，范围: " + block.getStartIndex() + "-" + block.getEndIndex());
        ASTNode blockNode = new ASTNode(ASTNode.NodeType.BLOCK);
        
        // 创建指令到AST的转换器
        InstructionToASTConverter converter = new InstructionToASTConverter(chunk, this);
        
        List<Instruction> instructions = chunk.getInstructions();
        // 添加块内的所有指令对应的AST节点
        System.out.println("       - 遍历基本块内的指令:");
        for (int i = block.getStartIndex(); i <= block.getEndIndex(); i++) {
            if (i < instructions.size()) {
                Instruction instruction = instructions.get(i);
                System.out.println("         指令 " + i + ": " + instruction.getOpcode().name() + ", A=" + instruction.getA() + ", B=" + instruction.getB() + ", C=" + instruction.getC());
                // 使用转换器将指令转换为AST节点
                ASTNode instructionNode = converter.convertInstructionToAST(instruction, i);
                if (instructionNode != null) {
                    System.out.println("         生成指令AST节点，类型: " + instructionNode.getType() + ", 值: " + instructionNode.getValue());
                    blockNode.addChild(instructionNode);
                } else {
                    System.out.println("         未生成指令AST节点");
                }
            }
        }
        
        System.out.println("       - 基本块AST生成完成，子节点数量: " + blockNode.getChildren().size());
        return blockNode;
    }
    
    /**
     * 为单个指令生成AST节点
     * @param instruction 指令
     * @param index 指令索引
     * @param chunk 代码块
     * @return 生成的AST节点，或null如果不需要生成节点
     */
    private ASTNode generateInstructionAST(Instruction instruction, int index, Chunk chunk) {
        Opcode opcode = instruction.getOpcode();
        
        switch (opcode) {
            case MOVE:
                // 寄存器间数据移动，通常不生成单独的AST节点
                return null;
            case LOADK:
                // 加载常量，生成赋值节点
                ASTNode loadKNode = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
                // 这里简化处理，实际应该生成准确的赋值表达式
                loadKNode.setValue("loadk");
                return loadKNode;
            case LOADBOOL:
                // 加载布尔值，生成赋值节点
                ASTNode loadBoolNode = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
                loadBoolNode.setValue("loadbool");
                return loadBoolNode;
            case LOADNIL:
                // 加载nil，生成赋值节点
                ASTNode loadNilNode = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
                loadNilNode.setValue("loadnil");
                return loadNilNode;
            case CALL:
                // 函数调用，生成函数调用节点
                ASTNode callNode = new ASTNode(ASTNode.NodeType.FUNCTION_CALL);
                callNode.setValue("call");
                return callNode;
            case RETURN:
                // 返回语句，生成返回节点
                ASTNode returnNode = new ASTNode(ASTNode.NodeType.RETURN_STATEMENT);
                returnNode.setValue("return");
                return returnNode;
            case JMP:
                // 跳转指令，不生成AST节点
                return null;
            case TEST:
            case TESTSET:
            case EQ:
            case LT:
            case LE:
                // 条件指令，通常在区域级别处理，不生成单独的AST节点
                return null;
            default:
                // 其他指令，生成默认节点
                ASTNode defaultNode = new ASTNode(ASTNode.NodeType.EXPRESSION);
                defaultNode.setValue(opcode.name());
                return defaultNode;
        }
    }
}