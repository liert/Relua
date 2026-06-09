package com.github.relua.decompiler;

import com.github.relua.ast.*;
import com.github.relua.log.Logger;
import com.github.relua.model.Chunk;
import com.github.relua.model.Constant;
import com.github.relua.model.FromType;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;
import com.github.relua.model.Register;
import com.github.relua.model.Register.RegisterEntity;
import com.github.relua.model.UpValue;
import com.github.relua.model.ValueType;
import com.github.relua.util.TransformUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 指令到AST节点的转换器
 * 负责将Lua指令转换为对应的AST节点
 */
public class InstructionToASTConverter {
    private Chunk chunk;
    private DecompilerPipeline pipeline;
    private PendingTest pendingTest = null;

    /**
     * 构造函数
     * 
     * @param chunk              代码块
     * @param instructionHandler 指令处理器
     */
    public InstructionToASTConverter(Chunk chunk, DecompilerPipeline pipeline) {
        this.chunk = chunk;
        this.pipeline = pipeline;
    }

    /**
     * 将指令转换为AST节点或PendingIf对象
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点或PendingIf对象
     */
    public Object convertInstruction(Instruction instruction, int instructionIndex) {
        return convertInstructionToAST(instruction, instructionIndex);
    }

    /**
     * 将指令转换为AST节点或PendingIf对象（主转换方法）
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点或PendingIf对象
     */
    public Object convertInstructionToAST(Instruction instruction, int instructionIndex) {
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
            case NEWTABLE:
                return convertNewTableInstruction(instruction, instructionIndex);
            case SELF:
                return convertSelfInstruction(instruction, instructionIndex);
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
                return convertTestInstruction(instruction, instructionIndex);
            case TESTSET:
                return convertTestSetInstruction(instruction, instructionIndex);
            case JMP:
                return convertJmpInstruction(instruction, instructionIndex);
            case CLOSURE:
                return convertClosureInstruction(instruction, instructionIndex);
            case GETUPVAL:
                return convertGetUpvalInstruction(instruction, instructionIndex);
            default:
                // 对于其他指令，生成一个默认的表达式节点
                StringConst opcodeStr = new StringConst(opcode.toString(), new SourcePos(instructionIndex, -1));
                return new ExpressionStatement(opcodeStr, new SourcePos(instructionIndex, -1));
        }
    }

    /**
     * 转换MOVE指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private AstNode convertMoveInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int b = instruction.getB();

        // 清除目标寄存器的pending SELF指令
        pipeline.getContext().removePendingSelf(a);

        // 获取寄存器状态
        Register registerState = pipeline.getRegisterByInstructionIndex(instructionIndex);
        RegisterEntity sourceEntity = registerState.getRegisterEntity(b);

        // 如果源寄存器是常量或全局变量，直接使用其值，否则返回null表示不生成单独的AST节点
        if (sourceEntity.getType() == ValueType.STRING || sourceEntity.getType() == ValueType.NUMBER
                || sourceEntity.getType() == ValueType.BOOLEAN || sourceEntity.getType() == ValueType.NIL
                || sourceEntity.getType() == ValueType.GLOBAL) {
            // MOVE指令：R(a) := R(b)，其中R(b)是常量或全局变量
            // 目标变量
            Expression target = new Name("R" + a, new SourcePos(instructionIndex, -1));

            // 源值
            Expression source;
            if (sourceEntity.getType() == ValueType.STRING) {
                source = new StringConst(sourceEntity.getValue().toString(), new SourcePos(instructionIndex, -1));
            } else if (sourceEntity.getType() == ValueType.NUMBER) {
                Object value = sourceEntity.getValue();
                Double numberValue;
                if (value instanceof Double) {
                    numberValue = (Double) value;
                } else if (value instanceof String) {
                    // 尝试将字符串转换为Double
                    try {
                        numberValue = Double.parseDouble((String) value);
                    } catch (NumberFormatException e) {
                        // 如果转换失败，使用默认值0.0
                        numberValue = 0.0;
                    }
                } else {
                    // 其他类型，使用默认值0.0
                    numberValue = 0.0;
                }
                source = new NumberConst(numberValue, new SourcePos(instructionIndex, -1));
            } else if (sourceEntity.getType() == ValueType.BOOLEAN) {
                source = new BooleanConst((Boolean) sourceEntity.getValue(), new SourcePos(instructionIndex, -1));
            } else if (sourceEntity.getType() == ValueType.NIL) {
                source = new NilConst(new SourcePos(instructionIndex, -1));
            } else {
                // 全局变量
                source = new Name(sourceEntity.getValue().toString(), new SourcePos(instructionIndex, -1));
            }

            List<Expression> left = new ArrayList<>();
            left.add(target);
            List<Expression> right = new ArrayList<>();
            right.add(source);
            // return new Assign(left, right, new SourcePos(instructionIndex, -1));
            return null;
        } else {
            // 如果是寄存器之间的移动，不生成单独的AST节点，因为这通常是编译器优化的结果
            return null;
        }
    }

    /**
     * 转换LOADK指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private AstNode convertLoadKInstruction(Instruction instruction, int instructionIndex) {
        // OP_LOADK A Bx R(A) := Kst(Bx)
        int a = instruction.getA();
        int bx = instruction.getBx();

        // 清除目标寄存器的pending SELF指令
        pipeline.getContext().removePendingSelf(a);

        // 目标变量名
        List<String> names = new ArrayList<>();
        names.add("R" + a);

        // 常量值
        Expression source = null;
        if (bx < chunk.getConstants().size()) {
            Constant constant = chunk.getConstants().get(bx);
            Object value = constant.getValue();
            if (value instanceof String) {
                source = new StringConst((String) value, new SourcePos(instructionIndex, -1));
            } else if (value instanceof Double) {
                source = new NumberConst((Double) value, new SourcePos(instructionIndex, -1));
            } else if (value instanceof Boolean) {
                source = new BooleanConst((Boolean) value, new SourcePos(instructionIndex, -1));
            } else if (value == null) {
                source = new NilConst(new SourcePos(instructionIndex, -1));
            }
        }

        if (source == null) {
            source = new NilConst(new SourcePos(instructionIndex, -1));
        }

        SourcePos pos = new SourcePos(instructionIndex, -1);

        // 左侧目标: R(A)
        Expression left = new Name("R" + a, pos);

        // List<Expression> right = new ArrayList<>();
        // right.add(source);
        // return new LocalAssign(names, right, new SourcePos(instructionIndex, -1));
        // return new Assign(left, source, pos);
        return null;
    }

    /**
     * 转换LOADBOOL指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private AstNode convertLoadBoolInstruction(Instruction instruction, int instructionIndex) {
        // OP_LOADBOOL A B C R(A) := (Bool)B; if (C) pc++
        int a = instruction.getA();
        boolean boolValue = instruction.getB() != 0;

        // 清除目标寄存器的pending SELF指令
        pipeline.getContext().removePendingSelf(a);

        // 目标变量
        Expression target = new Name("R" + a, new SourcePos(instructionIndex, -1));

        // 布尔值
        Expression source = new BooleanConst(boolValue, new SourcePos(instructionIndex, -1));

        List<Expression> left = new ArrayList<>();
        left.add(target);
        List<Expression> right = new ArrayList<>();
        right.add(source);
        // return new Assign(left, right, new SourcePos(instructionIndex, -1));
        return null;
    }

    /**
     * 转换LOADNIL指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private AstNode convertLoadNilInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int b = instruction.getB();

        // 清除所有被修改寄存器的pending SELF指令
        for (int i = a; i <= b; i++) {
            pipeline.getContext().removePendingSelf(i);
        }

        // LOADNIL指令：R(a) := nil, R(a+1) := nil, ..., R(b) := nil
        Block block = new Block(new SourcePos(instructionIndex, -1));

        for (int i = a; i <= b; i++) {
            // 目标变量
            Expression target = new Name("R" + i, new SourcePos(instructionIndex, -1));

            // nil值
            Expression source = new NilConst(new SourcePos(instructionIndex, -1));

            List<Expression> left = new ArrayList<>();
            left.add(target);
            List<Expression> right = new ArrayList<>();
            right.add(source);
            block.statements.add(new Assign(left, right, new SourcePos(instructionIndex, -1)));
        }

        return block;
    }

    /**
     * 转换GETGLOBAL指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private AstNode convertGetGlobalInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int bx = instruction.getBx();

        // 清除目标寄存器的pending SELF指令
        pipeline.getContext().removePendingSelf(a);

        // 从常量表读取字符串
        String name = chunk.getConstants().get(bx).getValue().toString();
        if (name.startsWith("\"") && name.endsWith("\"")) {
            name = name.substring(1, name.length() - 1);
        }

        // System.out.println("AST GETGLOBAL: " + name);

        SourcePos pos = new SourcePos(instructionIndex, -1);

        // 左侧目标: R(A)
        Expression target = new Name("R" + a, pos);

        // 右侧变量名
        Expression globalName = new Name(name, pos);

        // 创建赋值语句 AST
        // return new Assign(target, globalName, pos);
        return null;
    }

    /**
     * 转换SETGLOBAL指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private AstNode convertSetGlobalInstruction(Instruction instruction, int instructionIndex) {
        // OP_SETGLOBAL A Bx Gbl[Kst(Bx)] := R(A)
        int a = instruction.getA();
        int bx = instruction.getBx();

        // 源变量 - 使用寄存器的实际值
        Register registerState = pipeline.getRegisterByInstructionIndex(instructionIndex);
        RegisterEntity sourceEntity = registerState.getRegisterEntity(a);

        Expression source = null;
        // if (sourceEntity.getType() == ValueType.STRING) {
        // // 字符串常量
        // source = new StringConst(sourceEntity.getValue().toString(), new
        // SourcePos(instructionIndex, -1));
        // } else if (sourceEntity.getType() == ValueType.NUMBER) {
        // // 数值常量
        // source = new NumberConst((Double) sourceEntity.getValue(), new
        // SourcePos(instructionIndex, -1));
        // } else if (sourceEntity.getType() == ValueType.BOOLEAN) {
        // // 布尔值常量
        // source = new BooleanConst((Boolean) sourceEntity.getValue(), new
        // SourcePos(instructionIndex, -1));
        // } else if (sourceEntity.getType() == ValueType.NIL) {
        // // nil值
        // source = new NilConst(new SourcePos(instructionIndex, -1));
        // } else
        if (sourceEntity.getFromType() == FromType.GLOBAL) {
            // 全局变量
            source = new Name(sourceEntity.getValue().toString(), new SourcePos(instructionIndex, -1));
        } else if (sourceEntity.getFromType() == FromType.CONSTANT) {
            source = new StringConst(sourceEntity.getValue().toString(), new SourcePos(instructionIndex, -1));
        } else {
            source = new Name("R" + a, new SourcePos(instructionIndex, -1));
        }

        // 目标全局变量
        String name = "RK" + bx;
        if (bx < chunk.getConstants().size()) {
            name = chunk.getConstants().get(bx).getValue().toString();
        }

        if (source != null) {
            List<String> left = new ArrayList<>();
            left.add(name);
            List<Expression> right = new ArrayList<>();
            right.add(source);
            return new GlobalAssign(left, right, new SourcePos(instructionIndex, -1));
        }

        return null;
    }

    /**
     * 转换GETTABLE指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private AstNode convertGetTableInstruction(Instruction instruction, int instructionIndex) {
        // OP_GETTABLE A B C R(A) := R(B)[RK(C)]
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();

        // 清除目标寄存器的pending SELF指令
        pipeline.getContext().removePendingSelf(a);

        // 目标变量名
        List<String> names = new ArrayList<>();
        names.add("R" + a);

        Register registerState = pipeline.getRegisterByInstructionIndex(instructionIndex);
        RegisterEntity RB = registerState.getRegisterEntity(b);

        SourcePos pos = new SourcePos(instructionIndex, -1);

        // 表访问表达式
        Expression table = new Name("R" + b, pos);
        if (RB.getType() == ValueType.GLOBAL) {
            table = new Name(RB.getValue().toString(), pos);
        }

        Expression index = rkExpression(registerState, c, pos);
        Expression tableAccess = new IndexExpr(table, index, pos);

        List<Expression> right = new ArrayList<>();
        right.add(tableAccess);
        if (RB.getFromType() == FromType.GLOBAL) {
            // return new Assign("R" + a, tableAccess, new SourcePos(instructionIndex, -1));
            return null;
        }
        return new LocalAssign(names, right, pos);
    }

    /**
     * 转换SETTABLE指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private AstNode convertSetTableInstruction(Instruction instruction, int instructionIndex) {
        // OP_SETTABLE A B C R(A)[RK(B)] := RK(C)
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();
        Register register = pipeline.getRegisterByInstructionIndex(instructionIndex);
        RegisterEntity RA = register.getRegisterEntity(a);

        SourcePos pos = new SourcePos(instructionIndex, -1);

        // 表访问表达式（作为目标）
        String tableName = TransformUtils.transformRegister(RA);
        if (tableName.equals("{}")) {
            tableName = RA.getName();
        }
        Expression table = new Name(tableName, pos);
        Expression index = rkExpression(register, b, pos);
        Expression tableAccess = new IndexExpr(table, index, pos);

        // 源变量
        Expression source = rkExpression(register, c, pos);

        // 赋值语句
        List<Expression> left = new ArrayList<>();
        left.add(tableAccess);
        List<Expression> right = new ArrayList<>();
        right.add(source);
        return new Assign(left, right, new SourcePos(instructionIndex, -1));
    }

    /**
     * 转换NEWTABLE指令
     * 
     * @param instruction
     * @param instructionIndex
     * @return
     */
    private AstNode convertNewTableInstruction(Instruction instruction, int instructionIndex) {
        // OP_NEWTABLE A B C R(A) := {} (size = B, C)
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();

        Register register = pipeline.getRegisterByInstructionIndex(instructionIndex);
        RegisterEntity RA = register.getRegisterEntity(a);

        SourcePos pos = new SourcePos(instructionIndex, -1);

        // 赋值语句
        List<Expression> left = new ArrayList<>();
        left.add(new Name(TransformUtils.transformRegister(RA), pos));
        List<Expression> right = new ArrayList<>();
        right.add(new Name("{}", pos));
        return new Assign(left, right, pos);
    }

    /**
     * 转换SELF指令
     * 
     * @param instruction
     * @param instructionIndex
     * @return
     */
    private AstNode convertSelfInstruction(Instruction instruction, int instructionIndex) {
        // OP_SELF A B C R(A+1) := R(B); R(A) := R(B)[RK(C)]
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();

        // 获取方法名
        String methodName = null;
        if (c < 256) {
            Register register = pipeline.getRegisterByInstructionIndex(instructionIndex);
            methodName = TransformUtils.transformRegister(register.getRegisterEntity(c));
        } else {
            methodName = chunk.getConstant(c - 256).getValue().toString();
        }
        
        // 处理字符串类型，去除引号
        if (methodName != null && methodName.startsWith("\"") && methodName.endsWith("\"")) {
            methodName = methodName.substring(1, methodName.length() - 1);
        }

        // 存储pending的SELF指令，不生成AST节点
        pipeline.getContext().addPendingSelf(a, b, methodName);
        
        // SELF指令不生成代码，返回null
        return null;
    }

    private Expression rkExpression(Register register, int rk, SourcePos pos) {
        if (rk >= 256) {
            Constant constant = chunk.getConstant(rk - 256);
            Object value = constant != null ? constant.getValue() : null;
            String stringValue = value != null ? value.toString() : "";
            return new StringConst(stringValue, pos);
        }
        return new Name(TransformUtils.transformRegister(register.getRegisterEntity(rk)), pos);
    }

    /**
     * 转换算术指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private AstNode convertArithmeticInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();
        Opcode opcode = instruction.getOpcode();

        // 清除目标寄存器的pending SELF指令
        pipeline.getContext().removePendingSelf(a);

        // 算术指令：R(a) := R(b) op R(c)
        // 目标变量
        Expression target = new Name("R" + a, new SourcePos(instructionIndex, -1));

        // 左操作数
        Expression left = new Name("R" + b, new SourcePos(instructionIndex, -1));

        // 右操作数
        Expression right = new Name("R" + c, new SourcePos(instructionIndex, -1));

        // 二元操作
        String opStr = opcode.toString().toLowerCase();
        Expression binaryOp = new BinaryOp(opStr, left, right, new SourcePos(instructionIndex, -1));

        List<Expression> leftList = new ArrayList<>();
        leftList.add(target);
        List<Expression> rightList = new ArrayList<>();
        rightList.add(binaryOp);
        return new Assign(leftList, rightList, new SourcePos(instructionIndex, -1));
    }

    /**
     * 转换一元指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private AstNode convertUnaryInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int b = instruction.getB();
        Opcode opcode = instruction.getOpcode();

        // 清除目标寄存器的pending SELF指令
        pipeline.getContext().removePendingSelf(a);

        // 一元指令：R(a) := op R(b)
        // 目标变量
        Expression target = new Name("R" + a, new SourcePos(instructionIndex, -1));

        // 操作数
        Expression operand = new Name("R" + b, new SourcePos(instructionIndex, -1));

        // 一元操作
        String opStr = opcode.toString().toLowerCase();
        Expression unaryOp = new UnaryOp(opStr, operand, new SourcePos(instructionIndex, -1));

        List<Expression> leftList = new ArrayList<>();
        leftList.add(target);
        List<Expression> rightList = new ArrayList<>();
        rightList.add(unaryOp);
        return new Assign(leftList, rightList, new SourcePos(instructionIndex, -1));
    }

    /**
     * 转换CONCAT指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private AstNode convertConcatInstruction(Instruction instruction, int instructionIndex) {
        // OP_CONCAT A B C R(A) := R(B).. ... ..R(C)
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();

        // 清除目标寄存器的pending SELF指令
        pipeline.getContext().removePendingSelf(a);

        Register register = pipeline.getRegisterByInstructionIndex(instructionIndex);

        // 目标变量
        Expression target = new Name("R" + a, new SourcePos(instructionIndex, -1));

        // 字符串连接表达式
        Expression concatOp = null;

        // 遍历从R(B)到R(C)的所有寄存器，连接它们的值
        for (int i = b; i <= c; i++) {
            RegisterEntity currentEntity = register.getRegisterEntity(i);
            Expression currentExpr;

            // 根据寄存器类型创建相应的表达式
            if (currentEntity.getType() == ValueType.STRING) {
                // 字符串常量
                currentExpr = new StringConst(currentEntity.getValue().toString(), new SourcePos(instructionIndex, -1));
            } else if (currentEntity.getType() == ValueType.NUMBER) {
                // 数值常量
                currentExpr = new NumberConst((Double) currentEntity.getValue(), new SourcePos(instructionIndex, -1));
            } else {
                // 其他类型，作为变量处理
                currentExpr = new Name(currentEntity.getValue().toString(), new SourcePos(instructionIndex, -1));
            }

            if (concatOp == null) {
                // 第一个寄存器，直接作为初始值
                concatOp = currentExpr;
            } else {
                // 后续寄存器，与之前的结果连接
                concatOp = new BinaryOp("..", concatOp, currentExpr, new SourcePos(instructionIndex, -1));
            }
        }

        // 创建赋值节点
        List<Expression> leftList = new ArrayList<>();
        leftList.add(target);
        List<Expression> rightList = new ArrayList<>();
        rightList.add(concatOp);

        return new Assign(leftList, rightList, new SourcePos(instructionIndex, -1));
    }

    /**
     * 转换CALL指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private AstNode convertCallInstruction(Instruction instruction, int instructionIndex) {
        // OP_CALL A B C R(A), ... ,R(A+C-2) := R(A)(R(A+1), ... ,R(A+B-1))
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();

        Register registerState = pipeline.getRegisterByInstructionIndex(instructionIndex);
        SourcePos pos = new SourcePos(instructionIndex, -1);

        // 检查是否有pending的SELF指令
        CodeGeneratorContext.PendingSelf pendingSelf = pipeline.getContext().getPendingSelf(a);
        Expression func;
        boolean isMethodCall = false;

        if (pendingSelf != null) {
            // 这是一个方法调用
            isMethodCall = true;
            
            // 解析对象表达式（base register）
            Expression obj = resolveExpressionFromRegister(pendingSelf.getBaseRegister(), instructionIndex, registerState);
            
            // 创建成员访问表达式作为函数调用的callee
            func = new MemberExpr(obj, pendingSelf.getMethodName(), pos);
            
            // 移除pending的SELF指令
            pipeline.getContext().removePendingSelf(a);
        } else {
            // 普通函数调用
            func = resolveExpressionFromRegister(a, instructionIndex, registerState);
        }

        // 参数列表
        List<Expression> args = resolveCallArguments(a, b, instructionIndex, registerState);

        // 创建返回值列表
        List<Expression> returns = new ArrayList<>();

        // 创建调用表达式
        FunctionCall call = new FunctionCall(func, args, isMethodCall, returns, pos);

        if (instruction.getOpcode() == Opcode.TAILCALL) {
            return new UnaryOp("return", call, pos);
        }

        // 情况 1：C == 1 → 没有返回值（语句形式）
        if (c == 1) {
            return new ExpressionStatement(call, pos);
        }

        // 情况 2：C > 1 → 固定返回值个数
        if (c > 1) {
            List<Expression> targets = new ArrayList<>();

            // 返回值写入 R(A) 到 R(A+C-2)
            for (int i = 0; i < c - 1; i++) {
                Name returnName = new Name("R" + (a + i), pos);

                RegisterEntity RA = registerState.getRegisterEntity(a);
                String RAValue = RA.getValue().toString();
                if (RAValue.equals("require")) {
                    RegisterEntity argsEntity = registerState.getRegisterEntity(a + 1);
                    returnName.name = argsEntity.getValue().toString().replace(".", "_");
                }

                targets.add(returnName);
            }

            return new Assign(targets, Collections.singletonList(call), pos);
        }

        // 情况 3：C == 0 → 多返回值（VARARG 返回）
        if (c == 0) {
            // 调用带有多返回值标志
            call.setMultiReturn(true);

            List<Expression> targets = new ArrayList<>();

            // 对于多返回值，我们只处理第一个返回值
            Name returnName = new Name("R" + a, pos);
            targets.add(returnName);
            returns.add(returnName);
            registerState.setRegisterEntity(a, call, ValueType.OBJECT, FromType.REGISTER);

            return new Assign(targets, Collections.singletonList(call), pos);
        }

        // fallback
        return new ExpressionStatement(call, pos);
    }

    /**
     * 从寄存器解析表达式
     * 
     * @param registerIndex    寄存器索引
     * @param instructionIndex 指令索引
     * @param registerState    当前寄存器状态
     * @return 解析后的表达式
     */
    private Expression resolveExpressionFromRegister(int registerIndex, int instructionIndex, Register registerState) {
        RegisterEntity entity = registerState.getRegisterEntity(registerIndex);
        String tableName = TransformUtils.transformRegister(entity);
        if (tableName.equals("{}")) {
            return new Name("R" + registerIndex, new SourcePos(instructionIndex, -1));
        }
        try {
            // 根据实体类型创建不同的表达式
            switch (entity.getType()) {
                case GLOBAL:
                case FUNCTION:
                case TABLE:
                case OBJECT:
                    // 全局变量、函数引用、表引用
                    return new Name(entity.getValue().toString(), new SourcePos(instructionIndex, -1));
                case STRING:
                    // 字符串常量
                    return new StringConst(entity.getValue().toString(), new SourcePos(instructionIndex, -1));
                case NUMBER:
                    // 数值常量
                    return new NumberConst((Double) entity.getValue(), new SourcePos(instructionIndex, -1));
                case BOOLEAN:
                    // 布尔值常量
                    return new BooleanConst((Boolean) entity.getValue(), new SourcePos(instructionIndex, -1));
                case NIL:
                    // nil值
                    return new NilConst(new SourcePos(instructionIndex, -1));
                default:
                    // 未知类型，使用寄存器名作为占位符
                    return new Name("R" + registerIndex, new SourcePos(instructionIndex, -1));
            }
        } catch (Exception e) {
            // 处理异常，返回寄存器名作为占位符
            System.out.println("异常寄存器: " + entity);
            return new Name("R" + registerIndex, new SourcePos(instructionIndex, -1));
        }

    }

    /**
     * 解析函数调用参数
     * 
     * @param a                寄存器A
     * @param b                参数数量+1
     * @param instructionIndex 指令索引
     * @param registerState    当前寄存器状态
     * @return 解析后的参数列表
     */
    private List<Expression> resolveCallArguments(int a, int b, int instructionIndex, Register registerState) {
        List<Expression> args = new ArrayList<>();

        // 处理参数：B=1表示无参数，B>1表示有B-1个参数
        if (b > 1) {
            for (int i = 1; i < b; i++) {
                int argRegister = a + i;
                Expression arg = resolveExpressionFromRegister(argRegister, instructionIndex, registerState);
                args.add(arg);
            }
        }

        return args;
    }

    /**
     * 转换RETURN指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private AstNode convertReturnInstruction(Instruction instruction, int instructionIndex) {
        int a = instruction.getA();
        int b = instruction.getB();

        // RETURN指令：return R(a), R(a+1), ..., R(a+b-2)
        List<Expression> values = new ArrayList<>();

        // 返回值
        if (b > 1) {
            values.add(new Name("R" + a, new SourcePos(instructionIndex, -1)));

            if (b > 2) {
                values.add(new Vararg(new SourcePos(instructionIndex, -1)));
            }
        }

        return new ReturnStatement(values, new SourcePos(instructionIndex, -1));
    }

    /**
     * 转换比较指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private AstNode convertComparisonInstruction(Instruction instruction, int instructionIndex) {
        // OP_JMP sBx pc+=sBx
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();
        Opcode opcode = instruction.getOpcode();

        // 比较指令：if (R(a) op R(b)) then pc += c

        // 左操作数
        Expression left = new Name("R" + a, new SourcePos(instructionIndex, -1));

        // 右操作数
        Expression right = new Name("R" + b, new SourcePos(instructionIndex, -1));

        // 二元比较操作
        String opStr = opcode.toString().toLowerCase();
        Expression binaryOp = new BinaryOp(opStr, left, right, new SourcePos(instructionIndex, -1));

        return binaryOp;
    }

    /**
     * 转换TEST指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点（返回null，不输出AST节点）
     */
    private AstNode convertTestInstruction(Instruction instruction, int instructionIndex) {
        // OP_TEST A C if not (R(A) <=> C) then pc++
        int a = instruction.getA();
        int c = instruction.getC();

        Register registerState = pipeline.getRegisterByInstructionIndex(instructionIndex);
        RegisterEntity RA = registerState.getRegisterEntity(a);

        // 操作数
        Name operand = new Name("R" + a, new SourcePos(instructionIndex, -1));
        if (RA.getFromType() == FromType.GLOBAL) {
            operand.name = RA.getValue().toString();
        }
        Expression condition;

        // 根据c的值构建条件表达式
        if (c == 0) {
            // TEST A 0: if not (R[A] <=> 0) then PC++
            // 相当于 if R[A] then ...
            condition = operand;
        } else {
            // TEST A 1: if not (R[A] <=> 1) then PC++
            // 相当于 if not R[A] then ...
            condition = new Name("not " + operand.name, new SourcePos(instructionIndex, -1));
        }

        // 记录TEST信息，供后续JMP指令使用
        pendingTest = new PendingTest(condition, c == 0, instructionIndex, PendingTest.TestType.TEST, new SourcePos(instructionIndex, -1));

        // 关键点：不输出AST节点，返回null
        return null;
    }

    /**
     * 转换TESTSET指令
     * 
     * @param instruction
     * @param instructionIndex
     * @return
     */
    private AstNode convertTestSetInstruction(Instruction instruction, int instructionIndex) {
        // OP_TESTSET A B C if (R(B) <=> C) then R(A) := R(B) else pc++
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();

        Register registerState = pipeline.getRegisterByInstructionIndex(instructionIndex);
        RegisterEntity RA = registerState.getRegisterEntity(a);
        RegisterEntity RB = registerState.getRegisterEntity(b);

        // 操作数 根据c的值构建条件表达式
        Expression condition = new Name(TransformUtils.transformRegister(RB), new SourcePos(instructionIndex, -1));
        if (c == 1) {
            condition = new Name("not " + TransformUtils.transformRegister(RB), new SourcePos(instructionIndex, -1));
        }

        Assign assign = new Assign(TransformUtils.transformToAstNode(RA, instructionIndex), TransformUtils.transformToAstNode(RB, instructionIndex), new SourcePos(instructionIndex, -1));
        // 记录TEST信息，供后续JMP指令使用
        pendingTest = new PendingTest(condition, c == 0, instructionIndex, PendingTest.TestType.TESTSET, new SourcePos(instructionIndex, -1));
        pendingTest.addAstNode(assign);

        return null;
    }

    /**
     * 转换JMP指令
     *
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点或PendingIf对象
     */
    private Object convertJmpInstruction(Instruction instruction, int instructionIndex) {

        int sBx = instruction.getSBx();
        int jmpTarget = instructionIndex + 1 + sBx;

        // ============= 识别 IF 结构：TEST + JMP =============
        if (pendingTest != null) {
            // 使用之前记录的TEST信息生成PendingIf
            Expression condition = pendingTest.condition;

            // ----------- 计算 then 块范围 -----------
            int thenStart = instructionIndex + 1;
            int thenEnd = jmpTarget - 1;
            BasicBlock currentThenBlock = new BasicBlock(thenStart);
            BasicBlock block = pipeline.getBlockByStartIndex(chunk.getFunction(), thenStart);
            if (block != null && chunk.getInstructions().get(block.getEndIndex() - 1).getOpcode() != Opcode.TEST &&
                    chunk.getInstructions().get(block.getEndIndex()).getOpcode() == Opcode.JMP) {
                thenEnd = block.getEndIndex();
            }
            // 获取上一个then块的结束索引
            BasicBlock lastThenBlock = pipeline.getContext().getLastThenBlock();
            if (lastThenBlock != null && lastThenBlock.getEndIndex() > thenStart
                    && lastThenBlock.getEndIndex() < thenEnd) {
                thenEnd = lastThenBlock.getEndIndex();
            }
            currentThenBlock.setEndIndex(thenEnd);

            pipeline.getContext().addThenBlock(currentThenBlock);

            // System.out.println("Then StartIndex = " + thenStart + ", Then EndIndex = " + thenEnd);

            // ----------- 判断是否有 else 块 -----------
            Integer elseStart = null;
            // BasicBlock elseBlock = instructionHandler.getBlockByStartIndex(elseStart);
            Integer elseEnd = null;

            // then-block 最后一条可能是 JMP → 说明有 else
            if (thenEnd >= 0 && thenEnd < chunk.getInstructions().size()) {
                Instruction lastThen = chunk.getInstructions().get(thenEnd);
                if (lastThen.getOpcode() == Opcode.JMP) {
                    int endTarget = (thenEnd + 1) + lastThen.getSBx();
                    if (endTarget - 1 > jmpTarget) {
                        elseStart = jmpTarget;
                        elseEnd = endTarget - 1;
                    }
                }
            }

            // 返回 PendingIf，让外层生成真正的 IfStatement
            PendingIf pendingIf = new PendingIf(
                    condition, pendingTest.flag, thenStart, thenEnd, elseStart, elseEnd,
                    pendingTest.type, new SourcePos(instructionIndex, -1), pendingTest.astNodes);

            // 清空pendingTest，避免重复处理
            pendingTest = null;

            return pendingIf;
        }

        // 普通无条件跳转
        return new GotoStatement("L" + jmpTarget, new SourcePos(instructionIndex, -1));
    }

    /**
     * 转换CLOSURE指令
     *
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private Object convertClosureInstruction(Instruction instruction, int instructionIndex) {
        // OP_CLOSURE A Bx R(A) := closure(KPROTO[Bx], R(A), ... ,R(A+n))
        int a = instruction.getA();
        int bx = instruction.getBx();
        
        // 清除目标寄存器的pending SELF指令
        pipeline.getContext().removePendingSelf(a);
        
        // 从寄存器状态获取函数名
        Register registerState = pipeline.getRegisterByInstructionIndex(instructionIndex);
        RegisterEntity entity = registerState.getRegisterEntity(a);
        
        // 生成函数名
        String funcName = entity.getValue().toString();
        
        // 创建函数引用表达式
        Name funcRef = new Name(funcName, new SourcePos(instructionIndex, -1));
        
        // 生成赋值语句，将函数引用赋值给寄存器
        Name target = new Name("R" + a, new SourcePos(instructionIndex, -1));
        List<Expression> targets = new ArrayList<>();
        targets.add(target);
        List<Expression> values = new ArrayList<>();
        values.add(funcRef);
        
        return new Assign(targets, values, new SourcePos(instructionIndex, -1));
    }

    /**
     * 转换GETUPVAL指令
     *
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private Object convertGetUpvalInstruction(Instruction instruction, int instructionIndex) {
        // OP_GETUPVAL A Bx R(A) := UpValue[Bx]
        int a = instruction.getA();
        int bx = instruction.getBx();
        
        // 清除目标寄存器的pending SELF指令
        pipeline.getContext().removePendingSelf(a);
        
        // 从上下文获取上值信息
        // CodeGeneratorContext context = pipeline.getContext();
        // Upvalue upvalue = context.getUpvalue(bx);
        
        // 生成赋值语句，将上值赋值给寄存器
        // Name target = new Name("R" + a, new SourcePos(instructionIndex, -1));
        // List<Expression> targets = new ArrayList<>();
        // targets.add(target);
        // List<Expression> values = new ArrayList<>();
        // values.add(new Name(upvalue.getName(), new SourcePos(instructionIndex, -1)));
        return null;
    }
    
}
