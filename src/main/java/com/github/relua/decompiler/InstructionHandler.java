package com.github.relua.decompiler;

import com.github.relua.model.Chunk;
import com.github.relua.model.Constant;
import com.github.relua.model.Instruction;
import com.github.relua.model.Instruction.Opcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指令处理器，负责处理指令并构建中间表示
 */
public class InstructionHandler {
    // 寄存器值类型枚举
    public enum RegisterValueType {
        UNKNOWN,
        FUNCTION,
        STRING,
        NUMBER,
        BOOLEAN,
        NIL,
        TABLE,
        GLOBAL_VAR
    }
    
    // 寄存器状态类，用于维护寄存器的值和类型信息
    public static class RegisterState {
        private String value;
        private RegisterValueType type;
        
        public RegisterState() {
            this.value = null;
            this.type = RegisterValueType.UNKNOWN;
        }
        
        public RegisterState(String value, RegisterValueType type) {
            this.value = value;
            this.type = type;
        }
        
        // getter和setter方法
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public RegisterValueType getType() { return type; }
        public void setType(RegisterValueType type) { this.type = type; }
        
        // 复制方法，用于分支状态管理
        public RegisterState copy() {
            return new RegisterState(this.value, this.type);
        }
    }
    
    // 基本块类，用于控制流分析
    public static class BasicBlock {
        private int startIndex;
        private int endIndex;
        private List<BasicBlock> successors;
        private List<BasicBlock> predecessors;
        private boolean isIfBlock = false;
        private boolean isLoopBlock = false;
        private boolean isElseBlock = false;
        private int conditionRegister = -1;
        private Map<Integer, RegisterState> registerStates; // 基本块入口处的寄存器状态
        
        public BasicBlock(int startIndex) {
            this.startIndex = startIndex;
            this.endIndex = startIndex;
            this.successors = new ArrayList<>();
            this.predecessors = new ArrayList<>();
            this.registerStates = new HashMap<>();
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
        public int getConditionRegister() { return conditionRegister; }
        public void setConditionRegister(int conditionRegister) { this.conditionRegister = conditionRegister; }
        public Map<Integer, RegisterState> getRegisterStates() { return registerStates; }
        
        public void addSuccessor(BasicBlock block) {
            if (block != null && !successors.contains(block)) {
                successors.add(block);
                block.predecessors.add(this);
            }
        }
        
        // 设置寄存器状态
        public void setRegisterState(int register, RegisterState state) {
            registerStates.put(register, state);
        }
        
        // 获取寄存器状态
        public RegisterState getRegisterState(int register) {
            return registerStates.getOrDefault(register, new RegisterState());
        }
    }
    
    private List<BasicBlock> basicBlocks;
    private Map<Integer, BasicBlock> instructionToBlockMap;
    private Map<Integer, RegisterState> currentRegisterStates; // 当前寄存器状态
    
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
        currentRegisterStates = new HashMap<>();
        
        // 构建基本块
        buildBasicBlocks(chunk);
        
        // 先处理指令，建立寄存器状态映射
        processInstructions(chunk);
        
        // 构建控制流图
        buildControlFlowGraph(chunk);
        
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
        
        BasicBlock currentBlock = new BasicBlock(0);
        basicBlocks.add(currentBlock);
        
        for (int i = 0; i < instructions.size(); i++) {
            instructionToBlockMap.put(i, currentBlock);
            currentBlock.setEndIndex(i);
            
            Opcode opcode = instructions.get(i).getOpcode();
            
            // 检查是否是基本块结束指令
            if (isBlockEndInstruction(opcode)) {
                // 如果是跳转指令，下一条指令是新的基本块
                if (i + 1 < instructions.size()) {
                    currentBlock = new BasicBlock(i + 1);
                    basicBlocks.add(currentBlock);
                }
            } else if (isJumpTargetInstruction(instructions, i)) {
                // 如果当前指令是跳转目标，创建新的基本块
                currentBlock = new BasicBlock(i);
                basicBlocks.add(currentBlock);
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
               opcode == Opcode.CALL || opcode == Opcode.TAILCALL ||
               opcode == Opcode.FORLOOP || opcode == Opcode.FORPREP ||
               opcode == Opcode.TFORLOOP;
    }
    
    /**
     * 检查是否是跳转目标指令
     * @param instructions 指令列表
     * @param index 当前指令索引
     * @return 是否是跳转目标
     */
    private boolean isJumpTargetInstruction(List<Instruction> instructions, int index) {
        if (index == 0) {
            return true; // 第一条指令总是跳转目标
        }
        
        // 检查前面的指令是否有跳转到当前指令的
        for (int i = 0; i < index; i++) {
            Instruction inst = instructions.get(i);
            Opcode opcode = inst.getOpcode();
            
            if (opcode == Opcode.JMP) {
                int jumpTarget = i + 1 + inst.getSBx();
                if (jumpTarget == index) {
                    return true;
                }
            } else if (opcode == Opcode.TEST || opcode == Opcode.TESTSET) {
                if (inst.getC() != 0) {
                    int jumpTarget = i + 1 + inst.getC();
                    if (jumpTarget == index) {
                        return true;
                    }
                }
            } else if (opcode == Opcode.EQ || opcode == Opcode.LT || opcode == Opcode.LE) {
                if (inst.getC() != 0) {
                    int jumpTarget = i + 1 + inst.getC();
                    if (jumpTarget == index) {
                        return true;
                    }
                }
            }
        }
        
        return false;
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
                
                if (inst.getC() != 0) {
                    // 有跳转
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
            } else if (opcode != Opcode.RETURN) {
                // 其他非返回指令，添加下一条指令作为后继
                if (i + 1 < instructions.size()) {
                    BasicBlock nextBlock = instructionToBlockMap.get(i + 1);
                    if (nextBlock != null) {
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
     * 分析控制流，识别if-else和循环结构
     * @param chunk 代码块
     */
    private void analyzeControlFlow(Chunk chunk) {
        for (BasicBlock block : basicBlocks) {
            // 识别if块
            if (block.getSuccessors().size() == 2) {
                block.setIfBlock(true);
                
                // 识别else块
                BasicBlock trueBlock = block.getSuccessors().get(0);
                BasicBlock falseBlock = block.getSuccessors().get(1);
                
                // 简单的启发式：如果falseBlock的后继包含trueBlock的后继，那么falseBlock可能是else块
                if (trueBlock.getSuccessors().size() == 1 && falseBlock.getSuccessors().size() == 1) {
                    BasicBlock trueSucc = trueBlock.getSuccessors().get(0);
                    BasicBlock falseSucc = falseBlock.getSuccessors().get(0);
                    if (trueSucc == falseSucc) {
                        falseBlock.setElseBlock(true);
                    }
                }
            }
            
            // 识别循环块
            for (BasicBlock successor : block.getSuccessors()) {
                if (successor.getStartIndex() < block.getStartIndex()) {
                    block.setLoopBlock(true);
                    successor.setLoopBlock(true);
                    break;
                }
            }
        }
    }
    
    /**
     * 处理指令列表，为每个基本块维护独立的寄存器状态
     * @param chunk 代码块
     */
    private void processInstructions(Chunk chunk) {
        if (basicBlocks.isEmpty()) {
            return;
        }
        
        // 初始化第一个基本块的寄存器状态
        BasicBlock firstBlock = basicBlocks.get(0);
        firstBlock.getRegisterStates().clear();
        
        // 遍历所有基本块，处理每个块的指令并维护寄存器状态
        for (BasicBlock block : basicBlocks) {
            // 复制前驱块的寄存器状态作为当前块的初始状态
            copyPredecessorRegisterStates(block);
            
            // 处理当前块的所有指令
            for (int i = block.getStartIndex(); i <= block.getEndIndex(); i++) {
                if (i < chunk.getInstructions().size()) {
                    Instruction instruction = chunk.getInstructions().get(i);
                    processInstruction(chunk, instruction, i);
                    
                    // 将当前寄存器状态保存到指令映射中，供代码生成使用
                    instructionToBlockMap.get(i).getRegisterStates().putAll(new HashMap<>(currentRegisterStates));
                }
            }
            
            // 将当前块的最终寄存器状态保存，供后继块使用
            block.getRegisterStates().putAll(new HashMap<>(currentRegisterStates));
        }
    }
    
    /**
     * 复制前驱块的寄存器状态作为当前块的初始状态
     * @param block 当前基本块
     */
    private void copyPredecessorRegisterStates(BasicBlock block) {
        // 清空当前寄存器状态
        currentRegisterStates.clear();
        
        // 如果没有前驱块，使用空状态
        if (block.getPredecessors().isEmpty()) {
            return;
        }
        
        // 复制第一个前驱块的寄存器状态
        BasicBlock firstPredecessor = block.getPredecessors().get(0);
        for (Map.Entry<Integer, RegisterState> entry : firstPredecessor.getRegisterStates().entrySet()) {
            currentRegisterStates.put(entry.getKey(), entry.getValue().copy());
        }
        
        // 如果有多个前驱块，需要合并寄存器状态（简单处理：只保留公共状态）
        if (block.getPredecessors().size() > 1) {
            Map<Integer, RegisterState> commonStates = new HashMap<>(currentRegisterStates);
            
            for (int i = 1; i < block.getPredecessors().size(); i++) {
                BasicBlock predecessor = block.getPredecessors().get(i);
                Map<Integer, RegisterState> predStates = predecessor.getRegisterStates();
                
                // 只保留在所有前驱块中都存在且状态相同的寄存器
                commonStates.entrySet().removeIf(entry -> {
                    int reg = entry.getKey();
                    RegisterState state = entry.getValue();
                    return !predStates.containsKey(reg) || 
                           !predStates.get(reg).getValue().equals(state.getValue()) ||
                           predStates.get(reg).getType() != state.getType();
                });
            }
            
            currentRegisterStates = commonStates;
        }
    }
    
    /**
     * 处理单个指令
     * @param chunk 代码块
     * @param instruction 指令
     * @param index 指令索引
     */
    private void processInstruction(Chunk chunk, Instruction instruction, int index) {
        Opcode opcode = instruction.getOpcode();
        
        // 根据操作码处理不同类型的指令
        switch (opcode) {
            case MOVE:          // 寄存器间数据移动
                processMoveInstruction(chunk, instruction);
                break;
            case LOADK:         // 加载常量到寄存器
                processLoadKInstruction(chunk, instruction);
                break;
            case LOADBOOL:      // 加载布尔值到寄存器
                processLoadBoolInstruction(chunk, instruction);
                break;
            case LOADNIL:       // 加载nil到寄存器
                processLoadNilInstruction(chunk, instruction);
                break;
            case GETGLOBAL:     // 获取全局变量
                processGetGlobalInstruction(chunk, instruction);
                break;
            case SETGLOBAL:     // 设置全局变量
                processSetGlobalInstruction(chunk, instruction);
                break;
            case GETTABLE:      // 获取表元素
                processGetTableInstruction(chunk, instruction);
                break;
            case SETTABLE:      // 设置表元素
                processSetTableInstruction(chunk, instruction);
                break;
            case ADD:           // 加法
            case SUB:           // 减法
            case MUL:           // 乘法
            case DIV:           // 除法
            case MOD:           // 取模
            case POW:           // 幂运算
                processArithmeticInstruction(chunk, instruction);
                break;
            case UNM:           // 取反
            case NOT:           // 逻辑非
            case LEN:           // 长度
                processUnaryInstruction(chunk, instruction);
                break;
            case CONCAT:        // 字符串连接
                processConcatInstruction(chunk, instruction);
                break;
            case JMP:           // 跳转
                processJumpInstruction(chunk, instruction);
                break;
            case EQ:            // 等于
            case LT:            // 小于
            case LE:            // 小于等于
                processCompareInstruction(chunk, instruction);
                break;
            case TEST:          // 测试
            case TESTSET:       // 测试并设置
                processTestInstruction(chunk, instruction);
                break;
            case CALL:          // 函数调用
            case TAILCALL:      // 尾调用
                processCallInstruction(chunk, instruction);
                break;
            case RETURN:        // 返回
                processReturnInstruction(chunk, instruction);
                break;
            case FORLOOP:       // for循环
            case FORPREP:       // for循环准备
            case TFORLOOP:      // 泛型for循环
                processLoopInstruction(chunk, instruction);
                break;
            case SETLIST:       // 设置表列表
                processSetListInstruction(chunk, instruction);
                break;
            case CLOSE:         // 关闭upvalue
                processCloseInstruction(chunk, instruction);
                break;
            case CLOSURE:       // 创建闭包
                processClosureInstruction(chunk, instruction);
                break;
            case VARARG:        // 可变参数
                processVarargInstruction(chunk, instruction);
                break;
            case GETUPVAL:      // 获取upvalue
            case SETUPVAL:      // 设置upvalue
                processUpvalInstruction(chunk, instruction);
                break;
            default:
                // 未知指令，暂时忽略
                break;
        }
    }
    
    // 以下是各种指令的处理方法
    
    private void processMoveInstruction(Chunk chunk, Instruction instruction) {
        // 记录寄存器间的移动，用于变量跟踪
        int a = instruction.getA();
        int b = instruction.getB();
        
        // 如果源寄存器已经有状态信息，复制到目标寄存器
        if (currentRegisterStates.containsKey(b)) {
            RegisterState sourceState = currentRegisterStates.get(b);
            currentRegisterStates.put(a, sourceState.copy());
        }
    }
    
    private void processLoadKInstruction(Chunk chunk, Instruction instruction) {
        // 加载常量到寄存器，记录寄存器状态
        int a = instruction.getA();
        int bx = instruction.getBx();
        
        if (bx < chunk.getConstants().size()) {
            Constant constant = chunk.getConstants().get(bx);
            String value = constant.getValue().toString();
            RegisterValueType type = RegisterValueType.UNKNOWN;
            
            // 确定常量类型
            if (constant.getType() == Constant.Type.STRING) {
                type = RegisterValueType.STRING;
                // 去除引号
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
            } else if (constant.getType() == Constant.Type.NUMBER) {
                type = RegisterValueType.NUMBER;
            } else if (constant.getType() == Constant.Type.BOOLEAN) {
                type = RegisterValueType.BOOLEAN;
            } else if (constant.getType() == Constant.Type.NIL) {
                type = RegisterValueType.NIL;
            }
            
            currentRegisterStates.put(a, new RegisterState(value, type));
        }
    }
    
    private void processLoadBoolInstruction(Chunk chunk, Instruction instruction) {
        // 加载布尔值到寄存器
        int a = instruction.getA();
        boolean boolValue = instruction.getB() != 0;
        currentRegisterStates.put(a, new RegisterState(String.valueOf(boolValue), RegisterValueType.BOOLEAN));
    }
    
    private void processLoadNilInstruction(Chunk chunk, Instruction instruction) {
        // 加载nil到寄存器
        int a = instruction.getA();
        int b = instruction.getB();
        for (int i = a; i <= b; i++) {
            currentRegisterStates.put(i, new RegisterState("nil", RegisterValueType.NIL));
        }
    }
    
    private void processGetGlobalInstruction(Chunk chunk, Instruction instruction) {
        // 获取全局变量，记录寄存器状态
        int a = instruction.getA();
        int bx = instruction.getBx();
        
        if (bx < chunk.getConstants().size()) {
            String varName = chunk.getConstants().get(bx).getValue().toString();
            if (varName.startsWith("\"") && varName.endsWith("\"")) {
                varName = varName.substring(1, varName.length() - 1);
            }
            currentRegisterStates.put(a, new RegisterState(varName, RegisterValueType.GLOBAL_VAR));
        }
    }
    
    private void processSetGlobalInstruction(Chunk chunk, Instruction instruction) {
        // 设置全局变量
    }
    
    private void processGetTableInstruction(Chunk chunk, Instruction instruction) {
        // 获取表元素
        int a = instruction.getA();
        // 简单处理：记录为表访问
        currentRegisterStates.put(a, new RegisterState("table_access", RegisterValueType.TABLE));
    }
    
    private void processSetTableInstruction(Chunk chunk, Instruction instruction) {
        // 设置表元素
    }
    
    private void processArithmeticInstruction(Chunk chunk, Instruction instruction) {
        // 处理算术指令
        int a = instruction.getA();
        // 简单处理：记录为数字类型
        currentRegisterStates.put(a, new RegisterState("arithmetic_result", RegisterValueType.NUMBER));
    }
    
    private void processUnaryInstruction(Chunk chunk, Instruction instruction) {
        // 处理一元操作指令
        int a = instruction.getA();
        // 简单处理：保持原类型
        if (currentRegisterStates.containsKey(instruction.getB())) {
            RegisterState sourceState = currentRegisterStates.get(instruction.getB());
            currentRegisterStates.put(a, sourceState.copy());
        }
    }
    
    private void processConcatInstruction(Chunk chunk, Instruction instruction) {
        // 处理CONCAT指令
        int a = instruction.getA();
        // 简单处理：记录为字符串类型
        currentRegisterStates.put(a, new RegisterState("concat_result", RegisterValueType.STRING));
    }
    
    private void processJumpInstruction(Chunk chunk, Instruction instruction) {
        // 处理跳转指令
    }
    
    private void processCompareInstruction(Chunk chunk, Instruction instruction) {
        // 处理比较指令
    }
    
    private void processTestInstruction(Chunk chunk, Instruction instruction) {
        // 处理测试指令
    }
    
    private void processCallInstruction(Chunk chunk, Instruction instruction) {
        // 处理函数调用指令
        int a = instruction.getA();
        // 简单处理：记录为函数调用结果
        currentRegisterStates.put(a, new RegisterState("call_result", RegisterValueType.UNKNOWN));
    }
    
    private void processReturnInstruction(Chunk chunk, Instruction instruction) {
        // 处理返回指令
    }
    
    private void processLoopInstruction(Chunk chunk, Instruction instruction) {
        // 处理循环指令
    }
    
    private void processSetListInstruction(Chunk chunk, Instruction instruction) {
        // 处理SETLIST指令
    }
    
    private void processCloseInstruction(Chunk chunk, Instruction instruction) {
        // 处理CLOSE指令
    }
    
    private void processClosureInstruction(Chunk chunk, Instruction instruction) {
        // 处理CLOSURE指令
        int a = instruction.getA();
        // 记录为函数类型
        currentRegisterStates.put(a, new RegisterState("closure", RegisterValueType.FUNCTION));
    }
    
    private void processVarargInstruction(Chunk chunk, Instruction instruction) {
        // 处理VARARG指令
    }
    
    private void processUpvalInstruction(Chunk chunk, Instruction instruction) {
        // 处理upvalue指令
    }
    
    // getter方法，供代码生成器使用
    public List<BasicBlock> getBasicBlocks() { return basicBlocks; }
    public Map<Integer, BasicBlock> getInstructionToBlockMap() { return instructionToBlockMap; }
    public Map<Integer, RegisterState> getCurrentRegisterStates() { return currentRegisterStates; }
    
    /**
     * 根据指令索引获取寄存器状态
     * @param instructionIndex 指令索引
     * @return 该指令对应的寄存器状态
     */
    public Map<Integer, RegisterState> getRegisterStatesByInstructionIndex(int instructionIndex) {
        BasicBlock block = instructionToBlockMap.get(instructionIndex);
        if (block != null) {
            return new HashMap<>(block.getRegisterStates());
        }
        return new HashMap<>();
    }
    
    // 获取寄存器名，优先使用已知的变量名或值，否则使用R+寄存器号
    public String getRegisterName(int register) {
        if (currentRegisterStates.containsKey(register)) {
            RegisterState state = currentRegisterStates.get(register);
            if (state.getType() == RegisterValueType.GLOBAL_VAR || 
                state.getType() == RegisterValueType.FUNCTION) {
                return state.getValue();
            }
        }
        return "R" + register;
    }
}