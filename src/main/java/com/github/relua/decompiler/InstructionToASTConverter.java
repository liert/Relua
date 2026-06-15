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
     * 清除残留的 pendingTest 状态。
     * 在 buildBlock 入口调用，防止外层条件指令的 pendingTest 泄漏到内层块。
     */
    public void clearPendingTest() {
        this.pendingTest = null;
    }

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

    public PendingIf tryConvertConditionalJump(List<Instruction> instructions, int instructionIndex) {
        if (instructionIndex < 0 || instructionIndex + 1 >= instructions.size()) {
            return null;
        }

        Instruction condition = instructions.get(instructionIndex);
        Instruction jump = instructions.get(instructionIndex + 1);
        if (!isComparisonInstruction(condition) || jump.getOpcode() != Opcode.JMP) {
            return null;
        }

        convertInstructionToAST(condition, instructionIndex);
        Object result = convertInstructionToAST(jump, instructionIndex + 1);
        return result instanceof PendingIf ? (PendingIf) result : null;
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
            case SETUPVAL:
                return convertSetUpvalInstruction(instruction, instructionIndex);
            case CLOSE:
            case SETLIST:
            case TFORLOOP:
            case VARARG:
                return null;
            case FORPREP: {
                int loopIdx = -1;
                int a = instruction.getA();
                for (int j = instructionIndex + 1; j < chunk.getInstructions().size(); j++) {
                    Instruction target = chunk.getInstructions().get(j);
                    if (target.getOpcode() == Opcode.FORLOOP && target.getA() == a) {
                        loopIdx = j;
                        break;
                    }
                }
                int jumpTarget = loopIdx != -1 ? loopIdx : instructionIndex + 1;
                return new GotoStatement("L" + jumpTarget, new SourcePos(instructionIndex, -1));
            }
            case FORLOOP: {
                int prepIdx = -1;
                int a = instruction.getA();
                for (int j = instructionIndex - 1; j >= 0; j--) {
                    Instruction target = chunk.getInstructions().get(j);
                    if (target.getOpcode() == Opcode.FORPREP && target.getA() == a) {
                        prepIdx = j;
                        break;
                    }
                }
                int jumpTarget = prepIdx != -1 ? prepIdx + 1 : instructionIndex + 1;
                return new GotoStatement("L" + jumpTarget, new SourcePos(instructionIndex, -1));
            }
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

        // 目标变量
        Expression target = new Name("R" + a, new SourcePos(instructionIndex, -1));

        // 源值
        Expression source;
        if (sourceEntity.getType() == ValueType.STRING && sourceEntity.getFromType() == FromType.CONSTANT && sourceEntity.getValue() != null && !sourceEntity.getName().equals(sourceEntity.getValue().toString())) {
            source = new StringConst(sourceEntity.getValue().toString(), new SourcePos(instructionIndex, -1));
        } else if (sourceEntity.getType() == ValueType.NUMBER && sourceEntity.getValue() != null) {
            Object value = sourceEntity.getValue();
            Double numberValue;
            if (value instanceof Double) {
                numberValue = (Double) value;
            } else if (value instanceof String) {
                try {
                    numberValue = Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    numberValue = 0.0;
                }
            } else {
                numberValue = 0.0;
            }
            source = new NumberConst(numberValue, new SourcePos(instructionIndex, -1));
        } else if (sourceEntity.getType() == ValueType.BOOLEAN && sourceEntity.getValue() != null) {
            source = new BooleanConst((Boolean) sourceEntity.getValue(), new SourcePos(instructionIndex, -1));
        } else if (sourceEntity.getType() == ValueType.NIL) {
            source = new NilConst(new SourcePos(instructionIndex, -1));
        } else if (sourceEntity.getType() == ValueType.GLOBAL && sourceEntity.getValue() != null) {
            source = new Name(sourceEntity.getValue().toString(), new SourcePos(instructionIndex, -1));
        } else {
            source = new Name("R" + b, new SourcePos(instructionIndex, -1));
        }

        List<Expression> left = new ArrayList<>();
        left.add(target);
        List<Expression> right = new ArrayList<>();
        right.add(source);
        return new Assign(left, right, new SourcePos(instructionIndex, -1));
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

        return new Assign(left, source, pos);
    }

    /**
     * 转换LOADBOOL指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private Object convertLoadBoolInstruction(Instruction instruction, int instructionIndex) {
        // OP_LOADBOOL A B C R(A) := (Bool)B; if (C) pc++
        int a = instruction.getA();
        boolean boolValue = instruction.getB() != 0;
        int c = instruction.getC();

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
        
        Assign assign = new Assign(left, right, new SourcePos(instructionIndex, -1));
        
        if (c != 0) {
            int jumpTarget = instructionIndex + 2;
            Block block = new Block(new SourcePos(instructionIndex, -1));
            block.statements.add(assign);
            block.statements.add(new GotoStatement("L" + jumpTarget, new SourcePos(instructionIndex, -1)));
            return block;
        }
        
        return assign;
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
            // 如果这个 nil 只是紧随其后的 CALL/TAILCALL 的实参准备，不生成独立赋值。
            // 例如：
            //   MOVE     R7 R3
            //   LOADNIL  R8 R8
            //   LOADBOOL R9 0 0
            //   LOADBOOL R10 1 0
            //   CALL     R6 5 1
            // 应恢复为：
            //   R6(R7, nil, false, true)
            // 而不是额外输出：
            //   R8 = nil
            if (InstructionFlowAnalyzer.isRegisterConsumedByFollowingCallArgument(chunk.getInstructions(), i, instructionIndex)) {
                continue;
            }
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

        if (block.statements.isEmpty()) {
            return null;
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
        Object constVal = chunk.getConstants().get(bx).getValue();
        String name = constVal != null ? constVal.toString() : "";
        if (name.length() >= 2 && name.startsWith("\"") && name.endsWith("\"")) {
            name = name.substring(1, name.length() - 1);
        }
        if (name.matches("^R\\d+$")) {
            name = "global_" + name;
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

        Expression source = TransformUtils.transformToAstNode(sourceEntity, instructionIndex);

        // 目标全局变量
        String name = "RK" + bx;
        if (bx < chunk.getConstants().size()) {
            Object cv = chunk.getConstants().get(bx).getValue();
            name = cv != null ? cv.toString() : "RK" + bx;
        }
        if (name.matches("^R\\d+$")) {
            name = "global_" + name;
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
        if (RB.getValue() instanceof Expression) {
            table = (Expression) RB.getValue();
        } else if ((RB.getType() == ValueType.GLOBAL || RB.getFromType() == FromType.GLOBAL) && RB.getValue() != null) {
            table = new Name(RB.getValue().toString(), pos);
        }

        Expression index = rkExpression(registerState, c, pos);
        Expression tableAccess = new IndexExpr(table, index, pos);

        List<Expression> right = new ArrayList<>();
        right.add(tableAccess);
        if (RB.getFromType() == FromType.GLOBAL) {
            return null;
        }
        List<Expression> left = new ArrayList<>();
        left.add(new Name("R" + a, pos));
        return new Assign(left, right, pos);
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

        if (isArrayTableInitialization(instructionIndex, a)) {
            return null;
        }

        SourcePos pos = new SourcePos(instructionIndex, -1);

        // NEWTABLE 的目标变量始终用寄存器名 R+a，避免使用指令执行前残留的旧值
        String targetName = "R" + a;
        Expression tableExpression = new TableConstructor(new ArrayList<>(), pos);
        List<Expression> left = new ArrayList<>();
        left.add(new Name(targetName, pos));
        List<Expression> right = new ArrayList<>();
        right.add(tableExpression);
        return new Assign(left, right, pos);
    }

    private boolean isArrayTableInitialization(int instructionIndex, int tableRegister) {
        List<Instruction> instructions = chunk.getInstructions();
        for (int pc = instructionIndex + 1; pc < instructions.size(); pc++) {
            Instruction next = instructions.get(pc);
            if (next.getOpcode() == Opcode.SETLIST && next.getA() == tableRegister) {
                return true;
            }
            if (next.getOpcode() != Opcode.LOADK && next.getOpcode() != Opcode.MOVE) {
                return false;
            }
        }
        return false;
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
        if (methodName != null && methodName.length() >= 2 && methodName.startsWith("\"") && methodName.endsWith("\"")) {
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
            if (value == null) {
                return new NilConst(pos);
            } else if (value instanceof Double) {
                // 数字常量：整数用 long 表示，浮点用 double
                return new NumberConst((Double) value, pos);
            } else if (value instanceof Boolean) {
                return new BooleanConst((Boolean) value, pos);
            } else {
                // 字符串常量
                return new StringConst(value.toString(), pos);
            }
        }
        RegisterEntity entity = register.getRegisterEntity(rk);
        if (entity != null) {
            if (entity.getFromType() == FromType.CONSTANT && entity.getValue() instanceof String
                    && !entity.getName().equals(entity.getValue().toString())) {
                return new StringConst(entity.getValue().toString(), pos);
            }
        }
        return TransformUtils.transformToAstNode(entity, pos.pc);
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

        Register registerState = pipeline.getRegisterByInstructionIndex(instructionIndex);
        SourcePos pos = new SourcePos(instructionIndex, -1);

        // 算术指令：R(a) := RK(b) op RK(c)
        // 目标变量
        Expression target = new Name("R" + a, pos);

        // 左操作数：解析真实的寄存器/常量名称
        Expression left = rkExpression(registerState, b, pos);

        // 右操作数：解析真实的寄存器/常量名称
        Expression right = rkExpression(registerState, c, pos);

        // 二元操作
        String opStr = arithmeticOperator(opcode);
        Expression binaryOp = new BinaryOp(opStr, left, right, pos);

        List<Expression> leftList = new ArrayList<>();
        leftList.add(target);
        List<Expression> rightList = new ArrayList<>();
        rightList.add(binaryOp);
        return new Assign(leftList, rightList, pos);
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
        Register registerState = pipeline.getRegisterByInstructionIndex(instructionIndex);

        // 清除目标寄存器的pending SELF指令
        pipeline.getContext().removePendingSelf(a);

        // 一元指令：R(a) := op R(b)
        // 目标变量
        Expression target = new Name("R" + a, new SourcePos(instructionIndex, -1));

        // 操作数：通过寄存器状态解析真实名称（如参数名、全局变量名等）
        RegisterEntity operandEntity = registerState.getRegisterEntity(b);
        Expression operand = TransformUtils.transformToAstNode(operandEntity, instructionIndex);

        // 一元操作
        String opStr = unaryOperator(opcode, registerState.getRegisterEntity(b));
        Expression unaryOp = new UnaryOp(opStr, operand, new SourcePos(instructionIndex, -1));

        List<Expression> leftList = new ArrayList<>();
        leftList.add(target);
        List<Expression> rightList = new ArrayList<>();
        rightList.add(unaryOp);
        return new Assign(leftList, rightList, new SourcePos(instructionIndex, -1));
    }

    private String arithmeticOperator(Opcode opcode) {
        switch (opcode) {
            case ADD:
                return "+";
            case SUB:
                return "-";
            case MUL:
                return "*";
            case DIV:
                return "/";
            case MOD:
                return "%";
            case POW:
                return "^";
            default:
                return opcode.toString().toLowerCase();
        }
    }

    private String unaryOperator(Opcode opcode, RegisterEntity operand) {
        if (opcode == Opcode.NOT) {
            return "not";
        }
        if (opcode == Opcode.UNM) {
            return "-";
        }
        if (opcode == Opcode.LEN) {
            return operand.getType() == ValueType.NUMBER ? "-" : "#";
        }
        return opcode.toString().toLowerCase();
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
            if (currentEntity.getType() == ValueType.STRING
                    && currentEntity.getFromType() == FromType.CONSTANT
                    && currentEntity.getValue() != null
                    && !currentEntity.getName().equals(currentEntity.getValue().toString())) {
                // 字符串常量
                currentExpr = new StringConst(currentEntity.getValue().toString(), new SourcePos(instructionIndex, -1));
            } else if (currentEntity.getType() == ValueType.NUMBER
                    && currentEntity.getValue() != null) {
                // 数值常量，安全类型转换
                Object numVal = currentEntity.getValue();
                double dVal;
                if (numVal instanceof Double) {
                    dVal = (Double) numVal;
                } else if (numVal instanceof Integer) {
                    dVal = ((Integer) numVal).doubleValue();
                } else {
                    try { dVal = Double.parseDouble(numVal.toString()); }
                    catch (NumberFormatException e) { dVal = 0.0; }
                }
                currentExpr = new NumberConst(dVal, new SourcePos(instructionIndex, -1));
            } else if (currentEntity.getValue() != null
                    && !currentEntity.getValue().toString().equals("nil")) {
                // 有具体值（全局变量名、函数名、或寄存器引用如 "R4"）
                currentExpr = new Name(currentEntity.getValue().toString(), new SourcePos(instructionIndex, -1));
            } else {
                // UNKNOWN 类型或值为 nil/占位符，使用寄存器名避免输出 "nil"
                currentExpr = new Name("R" + i, new SourcePos(instructionIndex, -1));
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

    private String getExpressionName(Expression expr) {
        if (expr instanceof Name) {
            return ((Name) expr).name;
        } else if (expr instanceof MemberExpr) {
            String tbl = getExpressionName(((MemberExpr) expr).table);
            if (tbl != null) {
                return tbl + "_" + ((MemberExpr) expr).member;
            }
        }
        return null;
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
            // 如果 base 寄存器与 target 寄存器 a 相同，则 base 已经被 SELF 指令覆盖，其原始值备份在 a + 1 中
            int baseReg = pendingSelf.getBaseRegister();
            if (baseReg == a) {
                baseReg = a + 1;
            }
            Expression obj = resolveExpressionFromRegister(baseReg, instructionIndex, registerState);
            
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

        // 判断是否是 xxx.new() / xxx:new() 调用
        boolean isNewCall = false;
        String objName = "";
        if (func instanceof MemberExpr) {
            MemberExpr memberExpr = (MemberExpr) func;
            if ("new".equals(memberExpr.member)) {
                isNewCall = true;
                String tblName = getExpressionName(memberExpr.table);
                if (tblName != null) {
                    objName = tblName + "Obj";
                }
            }
        } else if (func instanceof Name) {
            Name nameExpr = (Name) func;
            String name = nameExpr.name;
            if (name.endsWith(".new") || name.endsWith(":new")) {
                isNewCall = true;
                String tblName = name.substring(0, name.length() - 4);
                objName = tblName.replace(".", "_").replace(":", "_") + "Obj";
            }
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
                if (i == 0 && isNewCall && !objName.isEmpty()) {
                    returnName.name = objName;
                } else {
                    RegisterEntity RA = registerState.getRegisterEntity(a);
                    String RAValue = (RA != null && RA.getValue() != null) ? RA.getValue().toString() : "";
                    if ("require".equals(RAValue)) {
                        RegisterEntity argsEntity = registerState.getRegisterEntity(a + 1);
                        if (argsEntity != null && argsEntity.getValue() != null) {
                            returnName.name = argsEntity.getValue().toString().replace(".", "_");
                        }
                    }
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
            String targetNameVal = "R" + a;
            if (isNewCall && !objName.isEmpty()) {
                targetNameVal = objName;
            }
            Name returnName = new Name(targetNameVal, pos);
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
        if (entity.getValue() instanceof Expression) {
            if (entity.getValue() instanceof TableConstructor
                    && ((TableConstructor) entity.getValue()).isEmpty()) {
                return new Name("R" + registerIndex, new SourcePos(instructionIndex, -1));
            }
            return (Expression) entity.getValue();
        }
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
                    if (entity.getValue() == null) {
                        return new Name("R" + registerIndex, new SourcePos(instructionIndex, -1));
                    }
                    String valStr = entity.getValue().toString();
                    SourcePos pos = new SourcePos(instructionIndex, -1);
                    if (valStr.contains(".")) {
                        String[] parts = valStr.split("\\.");
                        Expression current = new Name(parts[0], pos);
                        for (int i = 1; i < parts.length; i++) {
                            current = new MemberExpr(current, parts[i], pos);
                        }
                        return current;
                    }
                    return new Name(valStr, pos);
                case STRING:
                    // 只有当实体是常量（例如 FromType == CONSTANT）且其 value 不等于寄存器自身名称时才包装为 StringConst 
                    if (entity.getFromType() == FromType.CONSTANT && entity.getValue() != null 
                            && !entity.getName().equals(entity.getValue().toString())) {
                        return new StringConst(entity.getValue().toString(), new SourcePos(instructionIndex, -1));
                    }
                    if (entity.getValue() != null) {
                        return new Name(entity.getValue().toString(), new SourcePos(instructionIndex, -1));
                    }
                    return new Name("R" + registerIndex, new SourcePos(instructionIndex, -1));
                case NUMBER:
                    // 数值常量
                    if (entity.getValue() instanceof Double) {
                        return new NumberConst((Double) entity.getValue(), new SourcePos(instructionIndex, -1));
                    } else if (entity.getValue() instanceof Integer) {
                        return new NumberConst(((Integer) entity.getValue()).doubleValue(), new SourcePos(instructionIndex, -1));
                    } else {
                        if (entity.getValue() == null) {
                            return new Name("R" + registerIndex, new SourcePos(instructionIndex, -1));
                        }
                        return new Name(entity.getValue().toString(), new SourcePos(instructionIndex, -1));
                    }
                case BOOLEAN:
                    // 布尔值常量
                    if (entity.getValue() instanceof Boolean) {
                        return new BooleanConst((Boolean) entity.getValue(), new SourcePos(instructionIndex, -1));
                    } else {
                        if (entity.getValue() == null) {
                            return new Name("R" + registerIndex, new SourcePos(instructionIndex, -1));
                        }
                        return new Name(entity.getValue().toString(), new SourcePos(instructionIndex, -1));
                    }
                case NIL:
                    // nil值
                    return new NilConst(new SourcePos(instructionIndex, -1));
                default:
                    // 未知类型，如果值不为 null 且不是 "nil"，使用值名，否则使用寄存器名作为占位符
                    if (entity.getValue() != null && !entity.getValue().equals("nil")) {
                        return new Name(entity.getValue().toString(), new SourcePos(instructionIndex, -1));
                    }
                    return new Name("R" + registerIndex, new SourcePos(instructionIndex, -1));
            }
        } catch (Exception e) {
            // 处理异常，如果值不为空则返回值的名称，否则返回寄存器名作为占位符
            // System.out.println("异常寄存器: " + entity);
            if (entity != null && entity.getValue() != null) {
                return new Name(entity.getValue().toString(), new SourcePos(instructionIndex, -1));
            }
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
        } else if (b == 0) {
            // b = 0表示传递了前一个多返回值函数或vararg调用的所有返回值
            int top = a + 1;
            for (int i = instructionIndex - 1; i >= 0; i--) {
                Instruction prev = chunk.getInstruction(i);
                if (prev.getOpcode() == Opcode.CALL && prev.getC() == 0) {
                    top = prev.getA();
                    break;
                }
                if (prev.getOpcode() == Opcode.VARARG && prev.getB() == 0) {
                    top = prev.getA();
                    break;
                }
            }
            for (int i = a + 1; i <= top; i++) {
                args.add(resolveExpressionFromRegister(i, instructionIndex, registerState));
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
        Register registerState = pipeline.getRegisterByInstructionIndex(instructionIndex);

        // 返回值
        if (b > 1) {
            for (int i = 0; i < b - 1; i++) {
                values.add(resolveExpressionFromRegister(a + i, instructionIndex, registerState));
            }
        } else if (b == 0) {
            int top = a;
            for (int i = instructionIndex - 1; i >= 0; i--) {
                Instruction prev = chunk.getInstruction(i);
                if (prev.getOpcode() == Opcode.CALL && prev.getC() == 0) {
                    top = prev.getA();
                    break;
                }
                if (prev.getOpcode() == Opcode.VARARG && prev.getB() == 0) {
                    top = prev.getA();
                    break;
                }
            }
            for (int i = a; i <= top; i++) {
                values.add(resolveExpressionFromRegister(i, instructionIndex, registerState));
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
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();
        Opcode opcode = instruction.getOpcode();
        SourcePos pos = new SourcePos(instructionIndex, -1);
        Register registerState = pipeline.getRegisterByInstructionIndex(instructionIndex);

        Expression left = rkExpression(registerState, b, pos);
        Expression right = rkExpression(registerState, c, pos);
        String opStr = comparisonOperator(opcode, a);
        Expression condition = new BinaryOp(opStr, left, right, pos);

        pendingTest = new PendingTest(condition, true, instructionIndex, PendingTest.TestType.TEST, pos);
        return null;
    }

    private String comparisonOperator(Opcode opcode, int a) {
        if (opcode == Opcode.EQ) {
            return a == 0 ? "==" : "~=";
        }
        if (opcode == Opcode.LT) {
            return a == 0 ? "<" : ">=";
        }
        if (opcode == Opcode.LE) {
            return a == 0 ? "<=" : ">";
        }
        return opcode.toString().toLowerCase();
    }

    private boolean isComparisonInstruction(Instruction instruction) {
        Opcode opcode = instruction.getOpcode();
        return opcode == Opcode.EQ || opcode == Opcode.LT || opcode == Opcode.LE;
    }

    /**
     * 转换TEST指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点（返回null，不输出AST节点）
     */
    private AstNode convertTestInstruction(Instruction instruction, int instructionIndex) {
        // OP_TEST A C : if not (R(A) <=> C) then pc++
        // Lua 编译器对 if x 使用 codenot 后 TEST R 0 (C=0),
        // 对 if not x 使用 TEST R 1 (C=1)。
        // 因此反编译器条件映射为:
        //   C=0 → "R[A]" (对应源码 if x)
        //   C=1 → "not R[A]" (对应源码 if not x)
        int a = instruction.getA();
        int c = instruction.getC();

        Register registerState = pipeline.getRegisterByInstructionIndex(instructionIndex);
        RegisterEntity RA = registerState.getRegisterEntity(a);
        // 如果测试的目标寄存器当前保存的是非 false 且非 nil 的已知常量，则该测试不产生控制流分叉条件
        boolean isConstantAlwaysTrue = false;
        if (RA != null && RA.getFromType() == FromType.CONSTANT && RA.getValue() != null) {
            Object val = RA.getValue();
            if (!(val instanceof Boolean && !(Boolean) val) && !"nil".equals(val.toString())) {
                isConstantAlwaysTrue = true;
            }
        }

        if (isConstantAlwaysTrue) {
            return null;
        }

        // 操作数
        Name operand = new Name("R" + a, new SourcePos(instructionIndex, -1));
        if (RA.getFromType() == FromType.GLOBAL && RA.getValue() != null) {
            operand.name = RA.getValue().toString();
        }
        Expression condition;

        // 根据c的值构建条件表达式
        if (c == 0) {
            condition = operand;
        } else {
            condition = new UnaryOp("not", operand, new SourcePos(instructionIndex, -1));
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
        // OP_TESTSET A B C : if (R(B) <=> C) then R(A) := R(B) else pc++
        // Lua 编译器约定:
        //   C=0 → 源码条件为 "not x" (TESTSET 用于 and/or 短路)
        //   C=1 → 源码条件为 "x"
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();

        Register registerState = pipeline.getRegisterByInstructionIndex(instructionIndex);
        RegisterEntity RA = registerState.getRegisterEntity(a);
        RegisterEntity RB = registerState.getRegisterEntity(b);

        if (chunk.getFunction() != null && (chunk.getFunction().contains("sane") || chunk.getFunction().contains("main_2") || chunk.getFunction().contains("main_26"))) {
            Logger.debug("[DEBUG-TESTSET] PC=" + instructionIndex + ", a=" + a + ", b=" + b + ", c=" + c + ", RA=" + RA + ", RB=" + RB);
        }

        // 操作数 根据c的值构建条件表达式
        Expression condition = TransformUtils.transformToAstNode(RB, instructionIndex);
        if (c == 1) {
            condition = new UnaryOp("not", condition, new SourcePos(instructionIndex, -1));
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
            // 向后跳转（循环条件）不生成 PendingIf，避免 buildBlock 范围回退导致无限递归
            if (jmpTarget <= instructionIndex) {
                pendingTest = null;
                return null;
            }

            // 使用之前记录的TEST信息生成PendingIf
            Expression condition = pendingTest.condition;

            // ----------- 计算 then 块范围 -----------
            int thenStart = instructionIndex + 1;
            int thenEnd = jmpTarget - 1;
            BasicBlock currentThenBlock = new BasicBlock(thenStart);
            BasicBlock block = pipeline.getBlockByStartIndex(chunk.getFunction(), thenStart);
            if (block != null && chunk.getInstructions().get(block.getEndIndex() - 1).getOpcode() != Opcode.TEST &&
                    chunk.getInstructions().get(block.getEndIndex()).getOpcode() == Opcode.JMP) {
                Instruction jmpInst = chunk.getInstructions().get(block.getEndIndex());
                int endTarget = block.getEndIndex() + 1 + jmpInst.getSBx();
                if (endTarget >= jmpTarget) {
                    thenEnd = block.getEndIndex();
                }
            }
            // 获取上一个then块的结束索引
            int originalThenEnd = thenEnd;
            BasicBlock lastThenBlock = pipeline.getContext().getLastThenBlock();
            if (lastThenBlock != null && lastThenBlock.getEndIndex() > thenStart
                    && lastThenBlock.getEndIndex() < thenEnd) {
                thenEnd = lastThenBlock.getEndIndex();
            }
            currentThenBlock.setEndIndex(thenEnd);

            pipeline.getContext().addThenBlock(currentThenBlock);

            // ----------- 判断是否有 else 块 -----------
            Integer elseStart = null;
            Integer elseEnd = null;

            // then-block 最后一条可能是 JMP → 说明有 else
            if (thenEnd >= 0 && thenEnd < chunk.getInstructions().size()) {
                Instruction lastThen = chunk.getInstructions().get(thenEnd);
                if (lastThen.getOpcode() == Opcode.JMP) {
                    int endTarget = (thenEnd + 1) + lastThen.getSBx();
                    if (endTarget - 1 >= jmpTarget) {
                        elseStart = jmpTarget;
                        elseEnd = endTarget - 1;
                    }
                }
            }

            // 如果 lastThenBlock 缩减了 thenEnd，被排除的区间内可能存在前向 JMP，
            // 该 JMP 跳过了 else 体（即 then 块末尾的 return/break 后紧跟的 JMP 跳过 else）。
            // 扫描被排除的指令，找到这样的 JMP 来恢复 else 块。
            if (elseStart == null && thenEnd < originalThenEnd) {
                List<Instruction> instrs = chunk.getInstructions();
                for (int scanPc = thenEnd + 1; scanPc <= originalThenEnd && scanPc < instrs.size(); scanPc++) {
                    Instruction scanInst = instrs.get(scanPc);
                    if (scanInst.getOpcode() == Opcode.JMP && scanInst.getA() == 0) {
                        int scanTarget = scanPc + 1 + scanInst.getSBx();
                        if (scanTarget > scanPc + 1) {  // forward jump
                            elseStart = jmpTarget;
                            elseEnd = scanTarget - 1;
                            break;
                        }
                    }
                }
            }

            // 精化elseEnd：检查候选else范围中是否有来自外层作用域的跳转目标。
            if (elseStart != null && elseEnd != null) {
                int refinedEnd = refineElseEndByOuterJumpTargets(chunk, pendingTest.pc, jmpTarget, elseEnd);
                if (refinedEnd < elseEnd) {
                    elseEnd = refinedEnd;
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
        
        // 生成函数名：直接使用子 Chunk 对应的闭包函数标识符
        String funcName = chunk.getFunction() + "_" + bx;
        
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
        // OP_GETUPVAL A B R(A) := UpValue[B]
        int a = instruction.getA();
        int b = instruction.getB();
        
        // 清除目标寄存器的pending SELF指令
        pipeline.getContext().removePendingSelf(a);
        
        // 从上下文获取上值信息
        CodeGeneratorContext context = pipeline.getContext();
        UpValue upvalue = context.getUpvalue(b);
        
        if (chunk.getFunction() != null && (chunk.getFunction().contains("sane") || chunk.getFunction().contains("main_2") || chunk.getFunction().contains("main_26"))) {
            Logger.debug("[DEBUG-GETUPVAL] PC=" + instructionIndex + ", a=" + a + ", b=" + b + ", uv=" + upvalue + (upvalue != null ? (", uv.name=" + upvalue.getName() + ", uv.val=" + upvalue.getValue()) : ""));
        }
        
        SourcePos pos = new SourcePos(instructionIndex, -1);
        Expression right;
        if (upvalue != null && (upvalue.getFromType() == FromType.CONSTANT || upvalue.getFromType() == FromType.GLOBAL) && upvalue.getValue() != null) {
            Object val = upvalue.getValue();
            if (upvalue.getFromType() == FromType.GLOBAL) {
                String globalName = val.toString();
                if (globalName.contains(".")) {
                    String[] parts = globalName.split("\\.");
                    Expression current = new Name(parts[0], pos);
                    for (int i = 1; i < parts.length; i++) {
                        current = new MemberExpr(current, parts[i], pos);
                    }
                    right = current;
                } else {
                    right = new Name(globalName, pos);
                }
            } else { // CONSTANT
                if (val instanceof Boolean) {
                    right = new BooleanConst((Boolean) val, pos);
                } else if (val instanceof Number) {
                    right = new NumberConst(((Number) val).doubleValue(), pos);
                } else if ("nil".equals(val.toString())) {
                    right = new NilConst(pos);
                } else {
                    right = new StringConst(val.toString(), pos);
                }
            }
        } else {
            String upvalueName = (upvalue != null) ? upvalue.getName() : ("upvalue_" + b);
            right = new Name(upvalueName, pos);
        }
        
        return new Assign(new Name("R" + a, pos), right, pos);
    }

    /**
     * 转换SETUPVAL指令
     * 
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的AST节点
     */
    private AstNode convertSetUpvalInstruction(Instruction instruction, int instructionIndex) {
        // OP_SETUPVAL A B UpValue[B] := R(A)
        int a = instruction.getA();
        int b = instruction.getB();

        Register registerState = pipeline.getRegisterByInstructionIndex(instructionIndex);
        RegisterEntity RA = registerState.getRegisterEntity(a);

        CodeGeneratorContext context = pipeline.getContext();
        UpValue upvalue = context.getUpvalue(b);
        String upvalueName = (upvalue != null) ? upvalue.getName() : ("upvalue_" + b);

        SourcePos pos = new SourcePos(instructionIndex, -1);
        Expression left = new Name(upvalueName, pos);
        Expression right = TransformUtils.transformToAstNode(RA, instructionIndex);

        return new Assign(left, right, pos);
    }

    /**
     * 精化else块的结束位置，排除来自外层作用域的跳转目标之间的代码。
     * 当内层if的then块以JMP结尾时，该JMP的目标(endTarget)可能远大于
     * jmpTarget(当前if的else起始)。如果jmpTarget到endTarget之间存在
     * 来自外层if的跳转目标，说明外层else体的代码被错误地纳入了内层else。
     * 
     * @param chunk          代码块
     * @param testPc         TEST/EQ指令的PC（当前if的条件指令）
     * @param elseStart      候选else起始(即jmpTarget)
     * @param candidateEnd   候选else结束(endTarget - 1)
     * @return 精化后的else结束位置
     */
    private int refineElseEndByOuterJumpTargets(Chunk chunk, int testPc, int elseStart, int candidateEnd) {
        List<Instruction> instructions = chunk.getInstructions();

        // 收集在testPc之前（即外层）的JMP指令所指向的跳转目标
        java.util.Set<Integer> outerJumpTargets = new java.util.HashSet<>();
        for (int pc = 0; pc < testPc; pc++) {
            Instruction inst = instructions.get(pc);
            if (inst.getOpcode() == Opcode.JMP && inst.getA() == 0) {
                int target = pc + 1 + inst.getSBx();
                if (target > elseStart && target <= candidateEnd) {
                    outerJumpTargets.add(target);
                }
            }
        }

        // 找到最早的外层跳转目标
        int minTarget = Integer.MAX_VALUE;
        for (int target : outerJumpTargets) {
            if (target < minTarget) {
                minTarget = target;
            }
        }

        if (minTarget != Integer.MAX_VALUE) {
            return minTarget - 1;
        }
        return candidateEnd;
    }

    /**
     * 尝试在生成 ReturnStatement 时进行 peephole 优化，折叠前置寄存器赋值与常量返回。
     * 例如将:
     *   R4 = nil
     *   return nil
     * 折叠为:
     *   return nil
     *
     * @param block 当前 AST 块
     * @param ret 当前生成的返回语句
     * @param returnPC return 指令的 PC
     * @return 是否成功进行了优化
     */
    public boolean tryOptimizeAssignReturn(Block block, ReturnStatement ret, int returnPC) {
        return GenerationPeepholeOptimizer.tryOptimizeAssignReturn(block, ret, returnPC, chunk, pipeline.getContext());
    }
}
