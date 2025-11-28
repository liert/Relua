package com.github.relua.decompiler;

import com.github.relua.model.Chunk;
import com.github.relua.model.Constant;
import com.github.relua.model.Instruction;
import com.github.relua.model.Instruction.Opcode;
import com.github.relua.model.Register;
import com.github.relua.model.Register.RegisterEntity;
import com.github.relua.model.ValueType;

/**
 * 指令到AST节点的转换器
 * 负责将Lua指令转换为对应的AST节点
 */
public class InstructionToASTConverter {
    private Chunk chunk;
    private InstructionHandler instructionHandler;
    
    /**
     * 构造函数
     * @param chunk 代码块
     * @param instructionHandler 指令处理器
     */
    public InstructionToASTConverter(Chunk chunk, InstructionHandler instructionHandler) {
        this.chunk = chunk;
        this.instructionHandler = instructionHandler;
    }
    
    /**
     * 将指令转换为AST节点
     * @param instruction 指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    public ASTNode convertInstructionToAST(Instruction instruction, int instructionIndex) {
        Opcode opcode = instruction.getOpcode();
        
        switch (opcode) {
            case MOVE:
                return convertMoveInstruction(instruction, instructionIndex);
            case LOADK:
                return convertLoadKInstruction(instruction, instructionIndex);
            case LOADBOOL:
                return convertLoadBoolInstruction(instruction, instructionIndex);
            case LOADNIL:
                return convertLoadNilInstruction(instruction, instructionIndex);
            case GETGLOBAL:
                return convertGetGlobalInstruction(instruction, instructionIndex);
            case SETGLOBAL:
                return convertSetGlobalInstruction(instruction, instructionIndex);
            case GETTABLE:
                return convertGetTableInstruction(instruction, instructionIndex);
            case SETTABLE:
                return convertSetTableInstruction(instruction, instructionIndex);
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
            case POW:
                return convertArithmeticInstruction(instruction, instructionIndex);
            case UNM:
            case NOT:
            case LEN:
                return convertUnaryInstruction(instruction, instructionIndex);
            case CONCAT:
                return convertConcatInstruction(instruction, instructionIndex);
            case CALL:
            case TAILCALL:
                return convertCallInstruction(instruction, instructionIndex);
            case RETURN:
                return convertReturnInstruction(instruction, instructionIndex);
            case EQ:
            case LT:
            case LE:
                return convertComparisonInstruction(instruction, instructionIndex);
            case TEST:
            case TESTSET:
                return convertTestInstruction(instruction, instructionIndex);
            default:
                // 对于其他指令，生成一个默认的表达式节点
                ASTNode defaultNode = new ASTNode(ASTNode.NodeType.EXPRESSION);
                defaultNode.setValue(opcode.toString());
                return defaultNode;
        }
    }
    
    /**
     * 转换MOVE指令
     * @param instruction 指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private ASTNode convertMoveInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int b = instruction.getB();
        
        // 获取寄存器状态
        Register registerState = instructionHandler.getRegisterByInstructionIndex(instructionIndex);
        RegisterEntity sourceEntity = registerState.getRegisterEntity(b);
        
        // 如果源寄存器是常量或全局变量，直接使用其值，否则返回null表示不生成单独的AST节点
        if (sourceEntity.getType() == ValueType.STRING || sourceEntity.getType() == ValueType.NUMBER || sourceEntity.getType() == ValueType.BOOLEAN || sourceEntity.getType() == ValueType.NIL || sourceEntity.getType() == ValueType.GLOBAL) {
            // MOVE指令：R(a) := R(b)，其中R(b)是常量或全局变量
            ASTNode assignmentNode = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
            
            // 目标变量
            ASTNode targetNode = new ASTNode(ASTNode.NodeType.VARIABLE);
            targetNode.setValue("R" + a);
            assignmentNode.addChild(targetNode);
            
            // 源值
            ASTNode sourceNode;
            if (sourceEntity.getType() == ValueType.STRING || sourceEntity.getType() == ValueType.NUMBER || sourceEntity.getType() == ValueType.BOOLEAN || sourceEntity.getType() == ValueType.NIL) {
                // 常量值
                sourceNode = new ASTNode(ASTNode.NodeType.CONSTANT);
                sourceNode.setValue(sourceEntity.getValue());
            } else {
                // 全局变量
                sourceNode = new ASTNode(ASTNode.NodeType.VARIABLE);
                sourceNode.setValue(sourceEntity.getValue().toString());
            }
            assignmentNode.addChild(sourceNode);
            
            return assignmentNode;
        } else {
            // 如果是寄存器之间的移动，不生成单独的AST节点，因为这通常是编译器优化的结果
            return null;
        }
    }
    
    /**
     * 转换LOADK指令
     * @param instruction 指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private ASTNode convertLoadKInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int bx = instruction.getBx();
        
        // LOADK指令：R(a) := Kst(bx)
        ASTNode assignmentNode = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
        
        // 目标变量
        ASTNode targetNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        targetNode.setValue("R" + a);
        assignmentNode.addChild(targetNode);
        
        // 常量值
        if (bx < chunk.getConstants().size()) {
            Constant constant = chunk.getConstants().get(bx);
            ASTNode constantNode = new ASTNode(ASTNode.NodeType.CONSTANT);
            constantNode.setValue(constant.getValue());
            assignmentNode.addChild(constantNode);
        }
        
        return assignmentNode;
    }
    
    /**
     * 转换LOADBOOL指令
     * @param instruction 指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private ASTNode convertLoadBoolInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        boolean boolValue = instruction.getB() != 0;
        
        // LOADBOOL指令：R(a) := (boolValue)
        ASTNode assignmentNode = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
        
        // 目标变量
        ASTNode targetNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        targetNode.setValue("R" + a);
        assignmentNode.addChild(targetNode);
        
        // 布尔值
        ASTNode boolNode = new ASTNode(ASTNode.NodeType.CONSTANT);
        boolNode.setValue(boolValue);
        assignmentNode.addChild(boolNode);
        
        return assignmentNode;
    }
    
    /**
     * 转换LOADNIL指令
     * @param instruction 指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private ASTNode convertLoadNilInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int b = instruction.getB();
        
        // LOADNIL指令：R(a) := nil, R(a+1) := nil, ..., R(b) := nil
        ASTNode sequenceNode = new ASTNode(ASTNode.NodeType.SEQUENCE);
        
        for (int i = a; i <= b; i++) {
            ASTNode assignmentNode = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
            
            // 目标变量
            ASTNode targetNode = new ASTNode(ASTNode.NodeType.VARIABLE);
            targetNode.setValue("R" + i);
            assignmentNode.addChild(targetNode);
            
            // nil值
            ASTNode nilNode = new ASTNode(ASTNode.NodeType.CONSTANT);
            nilNode.setValue("nil");
            assignmentNode.addChild(nilNode);
            
            sequenceNode.addChild(assignmentNode);
        }
        
        return sequenceNode;
    }
    
    /**
     * 转换GETGLOBAL指令
     * @param instruction 指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private ASTNode convertGetGlobalInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int bx = instruction.getBx();
        
        // GETGLOBAL指令：R(a) := Gbl[Kst(bx)]
        ASTNode assignmentNode = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
        
        // 目标变量 - 使用寄存器的实际名称（如果有）
        Register registerState = instructionHandler.getRegisterByInstructionIndex(instructionIndex);
        RegisterEntity targetEntity = registerState.getRegisterEntity(a);
        
        ASTNode targetNode;
        if (targetEntity.getType() == ValueType.GLOBAL) {
            // 如果寄存器有实际的全局变量名，使用它
            targetNode = new ASTNode(ASTNode.NodeType.VARIABLE);
            targetNode.setValue(targetEntity.getValue().toString());
        } else {
            // 否则使用R+索引格式
            targetNode = new ASTNode(ASTNode.NodeType.VARIABLE);
            targetNode.setValue("R" + a);
        }
        assignmentNode.addChild(targetNode);
        
        // 全局变量
        if (bx < chunk.getConstants().size()) {
            String varName = chunk.getConstants().get(bx).getValue().toString();
            if (varName.startsWith("\"") && varName.endsWith("\"")) {
                varName = varName.substring(1, varName.length() - 1);
            }
            ASTNode globalNode = new ASTNode(ASTNode.NodeType.VARIABLE);
            globalNode.setValue(varName);
            assignmentNode.addChild(globalNode);
        }
        
        return assignmentNode;
    }
    
    /**
     * 转换SETGLOBAL指令
     * @param instruction 指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private ASTNode convertSetGlobalInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int bx = instruction.getBx();
        
        // SETGLOBAL指令：Gbl[Kst(bx)] := R(a)
        ASTNode assignmentNode = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
        
        // 目标全局变量
        if (bx < chunk.getConstants().size()) {
            String varName = chunk.getConstants().get(bx).getValue().toString();
            if (varName.startsWith("\"") && varName.endsWith("\"")) {
                varName = varName.substring(1, varName.length() - 1);
            }
            ASTNode globalNode = new ASTNode(ASTNode.NodeType.VARIABLE);
            globalNode.setValue(varName);
            assignmentNode.addChild(globalNode);
        }
        
        // 源变量 - 使用寄存器的实际值
        Register registerState = instructionHandler.getRegisterByInstructionIndex(instructionIndex);
        RegisterEntity sourceEntity = registerState.getRegisterEntity(a);
        
        ASTNode sourceNode;
        if (sourceEntity.getType() == ValueType.STRING) {
            // 字符串常量
            sourceNode = new ASTNode(ASTNode.NodeType.CONSTANT);
            sourceNode.setValue(sourceEntity.getValue());
        } else if (sourceEntity.getType() == ValueType.NUMBER || sourceEntity.getType() == ValueType.BOOLEAN || sourceEntity.getType() == ValueType.NIL) {
            // 数值、布尔值或nil常量
            sourceNode = new ASTNode(ASTNode.NodeType.CONSTANT);
            sourceNode.setValue(sourceEntity.getValue());
        } else if (sourceEntity.getType() == ValueType.GLOBAL) {
            // 全局变量
            sourceNode = new ASTNode(ASTNode.NodeType.VARIABLE);
            sourceNode.setValue(sourceEntity.getValue().toString());
        } else {
            // 其他类型，检查是否是_G访问
            String valueStr = sourceEntity.getValue().toString();
            if (valueStr.startsWith("_G[")) {
                // 提取_G["var"]中的变量名
                int start = valueStr.indexOf("[") + 2;
                int end = valueStr.lastIndexOf("]") - 1;
                if (start < end) {
                    String varName = valueStr.substring(start, end);
                    sourceNode = new ASTNode(ASTNode.NodeType.VARIABLE);
                    sourceNode.setValue(varName);
                } else {
                    // 无法解析，使用原始值
                    sourceNode = new ASTNode(ASTNode.NodeType.VARIABLE);
                    sourceNode.setValue(valueStr);
                }
            } else {
                // 使用变量名
                sourceNode = new ASTNode(ASTNode.NodeType.VARIABLE);
                sourceNode.setValue(valueStr);
            }
        }
        assignmentNode.addChild(sourceNode);
        
        return assignmentNode;
    }
    
    /**
     * 转换GETTABLE指令
     * @param instruction 指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private ASTNode convertGetTableInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();
        
        // GETTABLE指令：R(a) := R(b)[R(c)]
        ASTNode assignmentNode = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
        
        // 目标变量
        ASTNode targetNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        targetNode.setValue("R" + a);
        assignmentNode.addChild(targetNode);
        
        // 表访问表达式
        ASTNode tableAccessNode = new ASTNode(ASTNode.NodeType.EXPRESSION);
        tableAccessNode.setValue("table_access");
        
        // 表变量
        ASTNode tableNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        tableNode.setValue("R" + b);
        tableAccessNode.addChild(tableNode);
        
        // 索引变量
        ASTNode indexNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        indexNode.setValue("R" + c);
        tableAccessNode.addChild(indexNode);
        
        assignmentNode.addChild(tableAccessNode);
        
        return assignmentNode;
    }
    
    /**
     * 转换SETTABLE指令
     * @param instruction 指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private ASTNode convertSetTableInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();
        
        // SETTABLE指令：R(a)[R(b)] := R(c)
        ASTNode assignmentNode = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
        
        // 表访问表达式（作为目标）
        ASTNode tableAccessNode = new ASTNode(ASTNode.NodeType.EXPRESSION);
        tableAccessNode.setValue("table_access");
        
        // 表变量
        ASTNode tableNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        tableNode.setValue("R" + a);
        tableAccessNode.addChild(tableNode);
        
        // 索引变量
        ASTNode indexNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        indexNode.setValue("R" + b);
        tableAccessNode.addChild(indexNode);
        
        assignmentNode.addChild(tableAccessNode);
        
        // 源变量
        ASTNode sourceNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        sourceNode.setValue("R" + c);
        assignmentNode.addChild(sourceNode);
        
        return assignmentNode;
    }
    
    /**
     * 转换算术指令
     * @param instruction 指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private ASTNode convertArithmeticInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();
        Opcode opcode = instruction.getOpcode();
        
        // 算术指令：R(a) := R(b) op R(c)
        ASTNode assignmentNode = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
        
        // 目标变量
        ASTNode targetNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        targetNode.setValue("R" + a);
        assignmentNode.addChild(targetNode);
        
        // 二元操作
        ASTNode binaryOpNode = new ASTNode(ASTNode.NodeType.BINARY_OP);
        binaryOpNode.setValue(opcode.toString());
        
        // 左操作数
        ASTNode leftNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        leftNode.setValue("R" + b);
        binaryOpNode.addChild(leftNode);
        
        // 右操作数
        ASTNode rightNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        rightNode.setValue("R" + c);
        binaryOpNode.addChild(rightNode);
        
        assignmentNode.addChild(binaryOpNode);
        
        return assignmentNode;
    }
    
    /**
     * 转换一元指令
     * @param instruction 指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private ASTNode convertUnaryInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int b = instruction.getB();
        Opcode opcode = instruction.getOpcode();
        
        // 一元指令：R(a) := op R(b)
        ASTNode assignmentNode = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
        
        // 目标变量
        ASTNode targetNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        targetNode.setValue("R" + a);
        assignmentNode.addChild(targetNode);
        
        // 一元操作
        ASTNode unaryOpNode = new ASTNode(ASTNode.NodeType.UNARY_OP);
        unaryOpNode.setValue(opcode.toString());
        
        // 操作数
        ASTNode operandNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        operandNode.setValue("R" + b);
        unaryOpNode.addChild(operandNode);
        
        assignmentNode.addChild(unaryOpNode);
        
        return assignmentNode;
    }
    
    /**
     * 转换CONCAT指令
     * @param instruction 指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private ASTNode convertConcatInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();
        
        // CONCAT指令：R(a) := R(b) .. R(b+1) .. ... .. R(c)
        ASTNode assignmentNode = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
        
        // 目标变量
        ASTNode targetNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        targetNode.setValue("R" + a);
        assignmentNode.addChild(targetNode);
        
        // 字符串连接表达式
        ASTNode concatNode = new ASTNode(ASTNode.NodeType.BINARY_OP);
        concatNode.setValue("CONCAT");
        
        // 简化处理：只显示连接操作
        ASTNode leftNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        leftNode.setValue("R" + b);
        concatNode.addChild(leftNode);
        
        ASTNode rightNode = new ASTNode(ASTNode.NodeType.EXPRESSION);
        rightNode.setValue("...");
        concatNode.addChild(rightNode);
        
        assignmentNode.addChild(concatNode);
        
        return assignmentNode;
    }
    
    /**
     * 转换CALL指令
     * @param instruction 指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private ASTNode convertCallInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int b = instruction.getB();
        
        // CALL指令：R(a), R(a+1), ..., R(a+b-2) := R(a)(R(a+1), ..., R(a+b-1))
        ASTNode callNode = new ASTNode(ASTNode.NodeType.FUNCTION_CALL);
        
        // 获取函数寄存器的实际值
        Register registerState = instructionHandler.getRegisterByInstructionIndex(instructionIndex);
        RegisterEntity funcEntity = registerState.getRegisterEntity(a);
        
        // 函数变量
        ASTNode funcNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        String funcName = funcEntity.getValue().toString();
        funcNode.setValue(funcName);
        callNode.addChild(funcNode);
        
        // 处理特殊函数调用
        if (funcName.equals("module") && b > 1) {
            // module函数调用，生成module("name")格式
            int argRegister = a + 1;
            RegisterEntity argEntity = registerState.getRegisterEntity(argRegister);
            if (argEntity.getType() == ValueType.STRING) {
                ASTNode argNode = new ASTNode(ASTNode.NodeType.CONSTANT);
                argNode.setValue(argEntity.getValue());
                callNode.addChild(argNode);
            }
        } else if (funcName.equals("pcall") && b > 1) {
            // pcall函数调用，生成pcall(dofile, "/path")格式
            int dofileRegister = a + 1;
            int pathRegister = a + 2;
            
            RegisterEntity dofileEntity = registerState.getRegisterEntity(dofileRegister);
            RegisterEntity pathEntity = registerState.getRegisterEntity(pathRegister);
            
            if (dofileEntity.getType() == ValueType.GLOBAL && dofileEntity.getValue().equals("dofile")) {
                ASTNode dofileNode = new ASTNode(ASTNode.NodeType.VARIABLE);
                dofileNode.setValue("dofile");
                callNode.addChild(dofileNode);
                
                if (pathEntity.getType() == ValueType.STRING) {
                    ASTNode pathNode = new ASTNode(ASTNode.NodeType.CONSTANT);
                    pathNode.setValue(pathEntity.getValue());
                    callNode.addChild(pathNode);
                }
            }
        } else if (b > 1) {
            // 其他函数调用，处理实际参数
            for (int i = 1; i < b; i++) {
                int argRegister = a + i;
                RegisterEntity argEntity = registerState.getRegisterEntity(argRegister);
                
                ASTNode argNode;
                if (argEntity.getType() == ValueType.STRING || argEntity.getType() == ValueType.NUMBER || argEntity.getType() == ValueType.BOOLEAN || argEntity.getType() == ValueType.NIL) {
                    // 常量值
                    argNode = new ASTNode(ASTNode.NodeType.CONSTANT);
                    argNode.setValue(argEntity.getValue());
                } else {
                    // 变量
                    argNode = new ASTNode(ASTNode.NodeType.VARIABLE);
                    argNode.setValue(argEntity.getValue().toString());
                }
                callNode.addChild(argNode);
            }
        }
        
        return callNode;
    }
    
    /**
     * 转换RETURN指令
     * @param instruction 指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private ASTNode convertReturnInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int b = instruction.getB();
        
        // RETURN指令：return R(a), R(a+1), ..., R(a+b-2)
        ASTNode returnNode = new ASTNode(ASTNode.NodeType.RETURN_STATEMENT);
        
        // 返回值（简化处理）
        if (b > 1) {
            ASTNode retValueNode = new ASTNode(ASTNode.NodeType.VARIABLE);
            retValueNode.setValue("R" + a);
            returnNode.addChild(retValueNode);
            
            if (b > 2) {
                ASTNode moreValuesNode = new ASTNode(ASTNode.NodeType.EXPRESSION);
                moreValuesNode.setValue("...");
                returnNode.addChild(moreValuesNode);
            }
        }
        
        return returnNode;
    }
    
    /**
     * 转换比较指令
     * @param instruction 指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private ASTNode convertComparisonInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();
        Opcode opcode = instruction.getOpcode();
        
        // 比较指令：if (R(a) op R(b)) then pc += c
        ASTNode conditionNode = new ASTNode(ASTNode.NodeType.EXPRESSION);
        conditionNode.setValue("condition");
        
        // 二元比较操作
        ASTNode binaryOpNode = new ASTNode(ASTNode.NodeType.BINARY_OP);
        binaryOpNode.setValue(opcode.toString());
        
        // 左操作数
        ASTNode leftNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        leftNode.setValue("R" + a);
        binaryOpNode.addChild(leftNode);
        
        // 右操作数
        ASTNode rightNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        rightNode.setValue("R" + b);
        binaryOpNode.addChild(rightNode);
        
        conditionNode.addChild(binaryOpNode);
        
        return conditionNode;
    }
    
    /**
     * 转换TEST指令
     * @param instruction 指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private ASTNode convertTestInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int c = instruction.getC();
        Opcode opcode = instruction.getOpcode();
        
        // TEST指令：if not (R(a) <=> 0) then pc += c
        // TESTSET指令：if not (R(b) <=> 0) then pc += c else R(a) := R(b)
        ASTNode conditionNode = new ASTNode(ASTNode.NodeType.EXPRESSION);
        conditionNode.setValue("condition");
        
        // 测试操作
        ASTNode testNode = new ASTNode(ASTNode.NodeType.UNARY_OP);
        testNode.setValue("TEST");
        
        // 操作数
        ASTNode operandNode = new ASTNode(ASTNode.NodeType.VARIABLE);
        operandNode.setValue("R" + a);
        testNode.addChild(operandNode);
        
        conditionNode.addChild(testNode);
        
        return conditionNode;
    }
}