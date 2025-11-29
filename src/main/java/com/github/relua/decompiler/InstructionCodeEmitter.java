package com.github.relua.decompiler;

import com.github.relua.manager.RegisterManager;
import com.github.relua.model.Chunk;
import com.github.relua.model.Constant;
import com.github.relua.model.Instruction;
import com.github.relua.model.Instruction.Opcode;
import com.github.relua.model.Register;
import com.github.relua.model.ValueType;
import com.github.relua.model.Register.RegisterEntity;

/**
 * 指令代码生成器，负责生成单个指令的Lua代码
 */
public class InstructionCodeEmitter {
    /**
     * 生成单个指令的代码
     * 
     * @param registerManager 寄存器管理器
     * @param chunk           代码块
     * @param instruction     指令
     * @param index           指令索引
     * @param context         代码生成上下文
     * @param handler         指令处理器
     */
    public void emitInstruction(RegisterManager registerManager, Chunk chunk, Instruction instruction, int index, CodeGeneratorContext context, InstructionHandler handler) {
        generateInstruction(registerManager, chunk, instruction, index, context, handler);
    }

    /**
     * 生成单个指令的代码
     * 
     * @param registerManager 寄存器管理器
     * @param chunk           代码块
     * @param instruction     指令
     * @param index           指令索引
     * @param context         代码生成上下文
     * @param handler         指令处理器
     */
    private void generateInstruction(RegisterManager registerManager, Chunk chunk, Instruction instruction, int index, CodeGeneratorContext context, InstructionHandler handler) {
        Opcode opcode = instruction.getOpcode();
        Register currentRegister = registerManager.getCurrentRegister();

        System.out.println("\n--- 处理指令 " + index + " --- ");
        System.out.println("指令: " + opcode.name() + ", A=" + instruction.getA() + ", B=" + instruction.getB() + ", C="
                + instruction.getC() + ", Bx=" + instruction.getBx() + ", sBx=" + instruction.getSBx());
        System.out.println("当前寄存器状态: " + currentRegister);

        String code = generateInstructionCode(registerManager, chunk, instruction, index, handler);
        System.out.println("生成的指令代码: '" + code + "'");

        if (opcode == Opcode.JMP) {
            // 处理JMP指令，不生成代码，但更新控制流状态
            int sbx = instruction.getSBx();
            int jumpTarget = index + 1 + sbx;
            System.out.println("JMP指令，跳转目标: " + jumpTarget);

            // JMP指令不直接生成else，else生成由基本块分析处理
            return;
        }

        if (!code.isEmpty()) {
            // 对于if指令，直接使用上下文添加if语句，以便更新控制流栈
            if (code.startsWith("if ") && code.endsWith(" then")) {
                String condition = code.substring(3, code.length() - 5);
                System.out.println("添加if语句，条件: '" + condition + "'");
                context.addIfStatement(condition);
            } else {
                System.out.println("添加代码行: '" + code + "'");
                context.addCodeLine(code);
            }
        }
    }

    /**
     * 生成指令代码
     * 
     * @param registerManager 寄存器管理器
     * @param chunk           代码块
     * @param instruction     指令
     * @param index           指令索引
     * @param handler         指令处理器
     * @return 生成的指令代码
     */
    private String generateInstructionCode(RegisterManager registerManager, Chunk chunk, Instruction instruction, int index, InstructionHandler handler) {
        Opcode opcode = instruction.getOpcode();
        Register currentRegister = registerManager.getCurrentRegister();
        String result = "";

        switch (opcode) {
            case MOVE:
                // 寄存器间数据移动
                result = generateMoveCode(currentRegister, chunk, instruction, index);
                break;
            case LOADK:
                // 加载常量到寄存器
                result = generateLoadKCode(currentRegister, chunk, instruction, index);
                break;
            case LOADBOOL:
                // 加载布尔值到寄存器
                result = generateLoadBoolCode(currentRegister, chunk, instruction, index);
                break;
            case LOADNIL:
                // 加载nil到寄存器
                result = generateLoadNilCode(currentRegister, chunk, instruction, index);
                break;
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
            case POW:
                // 算术运算
                result = generateArithmeticCode(chunk, instruction, index, handler);
                break;
            case UNM:
            case NOT:
            case LEN:
                // 一元运算
                result = generateUnaryCode(chunk, instruction, index, handler);
                break;
            case GETGLOBAL:
                // 获取全局变量
                result = generateGetGlobalCode(currentRegister, chunk, instruction, index);
                break;
            case SETGLOBAL:
                // 设置全局变量
                result = generateSetGlobalCode(currentRegister, chunk, instruction, index);
                break;
            case GETTABLE:
                // 获取表元素
                result = generateGetTableCode(currentRegister, chunk, instruction, index);
                break;
            case SETTABLE:
                // 设置表元素
                result = generateSetTableCode(chunk, instruction, index, handler);
                break;
            case SELF:
                result = generateSelfCode(currentRegister, chunk, instruction, index);
                break;
            case CALL:
                // 函数调用
                result = generateCallCode(currentRegister, chunk, instruction, index);
                break;
            case RETURN:
                // 返回
                result = generateReturnCode(currentRegister, chunk, instruction, index, handler);
                break;
            case CONCAT:
                // 字符串连接
                result = generateConcatCode(chunk, instruction, index, handler);
                break;
            case JMP:
                // 跳转指令，根据上下文决定是否生成代码
                result = generateJmpCode(currentRegister, chunk, instruction, index);
                break;
            case TEST:
            case TESTSET:
            case EQ:
            case LT:
            case LE:
                // 生成if条件
                result = generateIfCode(currentRegister, chunk, instruction, index, handler);
                break;
            default:
                // 其他指令，暂时输出指令信息
                result = String.format("-- %s: A=%d, B=%d, C=%d, Bx=%d, sBx=%d",
                        opcode.name(),
                        instruction.getA(),
                        instruction.getB(),
                        instruction.getC(),
                        instruction.getBx(),
                        instruction.getSBx());
                break;
        }
        return result;
    }

    /**
     * 生成寄存器间数据移动指令代码
     * 
     * @param register    寄存器
     * @param chunk       代码块
     * @param instruction 指令
     * @param index       指令索引
     * @return 生成的指令代码
     */
    private String generateMoveCode(Register register, Chunk chunk, Instruction instruction, int index) {
        // OP_MOVE A B R(A) := R(B)
        int a = instruction.getA();
        int b = instruction.getB();
        RegisterEntity desRegisterEntity = register.getRegisterEntity(a);
        RegisterEntity srcRegisterEntity = register.getRegisterEntity(b);
        register.move(desRegisterEntity, srcRegisterEntity);
        return "";
    }

    /**
     * 生成加载常量指令代码
     * 
     * @param register    寄存器
     * @param chunk       代码块
     * @param instruction 指令
     * @param index       指令索引
     * @return 生成的指令代码
     */
    private String generateLoadKCode(Register register, Chunk chunk, Instruction instruction, int index) {
        // OP_LOADK A Bx R(A) := Kst(Bx)
        int a = instruction.getA();
        int bx = instruction.getBx();
        Constant constant = chunk.getConstant(bx);
        RegisterEntity registerEntity = register.getRegisterEntity(a);
        registerEntity.setType(constant.getType());
        registerEntity.setValue(constant.getValue().toString());
        return "";
    }

    /**
     * 生成加载布尔值指令代码
     * 
     * @param register    寄存器
     * @param chunk       代码块
     * @param instruction 指令
     * @param index       指令索引
     * @return 生成的指令代码
     */
    private String generateLoadBoolCode(Register register, Chunk chunk, Instruction instruction, int index) {
        // OP_LOADBOOL A B C R(A) := (Bool)B; if (C) pc++
        int a = instruction.getA();
        boolean boolValue = instruction.getB() != 0;
        return "";
    }

    /**
     * 生成加载nil指令代码
     * 
     * @param register    寄存器
     * @param chunk       代码块
     * @param instruction 指令
     * @param index       指令索引
     * @return 生成的指令代码
     */
    private String generateLoadNilCode(Register register, Chunk chunk, Instruction instruction, int index) {
        // OP_LOADNIL A B R(A) := ... := R(B) := nil
        int a = instruction.getA();
        int b = instruction.getB();
        for (int i = a; i <= a + b; i++) {
            RegisterEntity registerEntity = register.getRegisterEntity(i);
            registerEntity.setType(ValueType.NIL);
            registerEntity.setValue("nil");
        }
        return "";
    }

    /**
     * 生成GETGLOBAL指令代码
     * 
     * @param register    寄存器
     * @param chunk       代码块
     * @param instruction 指令
     * @param index       指令索引
     * @return 生成的指令代码
     */
    private String generateGetGlobalCode(Register register, Chunk chunk, Instruction instruction, int index) {
        // OP_GETGLOBAL A Bx R(A) := Gbl[Kst(Bx)]
        int a = instruction.getA();
        int bx = instruction.getBx();
        Constant constant = chunk.getConstant(bx);
        RegisterEntity registerEntity = register.getRegisterEntity(a);
        registerEntity.setType(ValueType.GLOBAL);
        registerEntity.setValue(constant.getValue().toString());
        return "";
    }

    /**
     * 生成SELF指令代码
     * 
     * @param register    寄存器
     * @param chunk       代码块
     * @param instruction 指令
     * @param index       指令索引
     * @return 生成的指令代码
     */
    private String generateSelfCode(Register register, Chunk chunk, Instruction instruction, int index) {
        // OP_SELF A B C R(A+1) := R(B); R(A) := R(B)[RK(C)]
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();
        Constant constant = chunk.getConstant(c);
        register.move(a + 1, b);
        RegisterEntity RA = register.getRegisterEntity(a);
        RegisterEntity RB = register.getRegisterEntity(b);
        String result = String.format("%s[%s]", getArgumentValue(RB, chunk, b, index), constant.toString());
        RA.setType(ValueType.FUNCTION);
        RA.setValue(result);
        return "";
    }

    /**
     * 生成函数调用代码
     * 
     * @param register         寄存器
     * @param chunk            代码块
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的指令代码
     */
    private String generateCallCode(Register register, Chunk chunk, Instruction instruction, int instructionIndex) {
        // OP_CALL A B C R(A), ... ,R(A+C-2) := R(A)(R(A+1), ... ,R(A+B-1))
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();

        // 使用RegisterEntity的getValue方法获取函数名
        String funcName = register.getRegisterEntity(a).getValue().toString();

        // 生成函数调用 func(arg1, arg2, ...)
        StringBuilder funcCall = new StringBuilder();
        funcCall.append(String.format("%s(", funcName));

        // 生成参数
        if (b > 1) {
            for (int i = a + 1; i < a + b; i++) {
                if (i > a + 1)
                    funcCall.append(", ");
                funcCall.append(register.getRegisterEntity(i).getValue().toString());
            }
        }
        funcCall.append(")");

        if (c == 0 || c == 1) {
            return funcCall.toString();
        }

        StringBuilder args = new StringBuilder();

        // 生成返回值
        if (c >= 2) {
            for (int i = a; i < a + c - 1; i++) {
                if (i == a + c - 2) {
                    args.append(String.format("%s", register.getRegisterEntity(i).getName()));
                } else {
                    args.append(String.format("%s, ", register.getRegisterEntity(i).getName()));
                }
            }
        }

        return String.format("%s = %s", args, funcCall.toString());
    }

    /**
     * 生成全局变量赋值代码
     * 
     * @param register         当前寄存器
     * @param chunk            代码块
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的指令代码
     */
    private String generateSetGlobalCode(Register register, Chunk chunk, Instruction instruction, int instructionIndex) {
        // OP_SETGLOBAL A Bx Gbl[Kst(Bx)] := R(A)
        int a = instruction.getA();
        int bx = instruction.getBx();
        Constant constant = chunk.getConstant(bx);
        if (constant == null) {
            return String.format("-- Unknown global variable index %d = %s", bx,
                    getArgumentValue(register.getRegisterEntity(a), chunk, a, instructionIndex));
        } else {
            return String.format("%s = %s",
                    constant.getValue(),
                    getArgumentValue(register.getRegisterEntity(a), chunk, a, instructionIndex));
        }
    }

    /**
     * 获取参数值，根据寄存器状态返回合适的表达式
     * 
     * @param registerEntity   寄存器实体
     * @param chunk            代码块
     * @param register         寄存器号
     * @param instructionIndex 指令索引
     * @return 参数值表达式
     */
    private String getArgumentValue(RegisterEntity registerEntity, Chunk chunk, int register, int instructionIndex) {
        System.out.println(registerEntity);
        switch (registerEntity.getType()) {
            case STRING:
                return "\"" + registerEntity.getValue() + "\"";
            case NUMBER:
            case BOOLEAN:
            case NIL:
                return registerEntity.getValue().toString();
            case GLOBAL:
            case FUNCTION:
                return registerEntity.getValue().toString();
            default:
                return getRegisterName(register, instructionIndex);
        }
    }

    /**
     * 生成跳转指令代码
     * 
     * @param register         寄存器
     * @param chunk            代码块
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的指令代码
     */
    private String generateJmpCode(Register register, Chunk chunk, Instruction instruction, int instructionIndex) {
        // OP_JMP sBx pc+=sBx
        int sbx = instruction.getSBx();
        int jumpTarget = instructionIndex + 1 + sbx;

        System.out.println("sbx = " + sbx + ", jumpTarget = " + jumpTarget);

        if (register.jump) {
            // 处理if语句后的第一个跳转，记录跳转目标
            register.jump = false;
            // 保存跳转目标，用于后续处理
            if (register.ifDepth > 0) {
                register.jumpTargets[register.ifDepth - 1] = jumpTarget;
            }
            return "";
        }
        return "";
    }

    /**
     * 生成if块代码
     * 
     * @param register         寄存器
     * @param chunk            代码块
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @param handler          指令处理器
     * @return 生成的指令代码
     */
    private String generateIfCode(Register register, Chunk chunk, Instruction instruction, int instructionIndex, InstructionHandler handler) {
        // 使用InstructionHandler的寄存器状态，而不是传入的register参数
        Register currentRegister = handler.getRegisterByInstructionIndex(instructionIndex);

        int a = instruction.getA();
        int c = instruction.getC();
        Opcode opcode = instruction.getOpcode();
        String condition;

        if (opcode == Opcode.TEST || opcode == Opcode.TESTSET) {
            // TEST指令：if not (R(A) <=> C) then pc += sBx
            String regName = handler.getRegisterName(a, instructionIndex);

            // 根据C值生成正确的条件表达式
            if (c == 0) {
                // C=0: if R(A) then
                condition = regName;
            } else {
                // C=1: if not R(A) then
                condition = String.format("not %s", regName);
            }
        } else if (opcode == Opcode.EQ) {
            // EQ指令：if R(B) ~= RK(C) then pc += sBx
            // 生成的条件应该是 R(B) == RK(C)
            String regB = handler.getRegisterName(instruction.getB(), instructionIndex);
            String regC = handler.getRegisterName(instruction.getC(), instructionIndex);
            condition = String.format("%s == %s", regB, regC);
        } else if (opcode == Opcode.LT) {
            // LT指令：if R(B) >= RK(C) then pc += sBx
            // 生成的条件应该是 R(B) < RK(C)
            String regB = handler.getRegisterName(instruction.getB(), instructionIndex);
            String regC = handler.getRegisterName(instruction.getC(), instructionIndex);
            condition = String.format("%s < %s", regB, regC);
        } else if (opcode == Opcode.LE) {
            // LE指令：if R(B) > RK(C) then pc += sBx
            // 生成的条件应该是 R(B) <= RK(C)
            String regB = handler.getRegisterName(instruction.getB(), instructionIndex);
            String regC = handler.getRegisterName(instruction.getC(), instructionIndex);
            condition = String.format("%s <= %s", regB, regC);
        } else {
            // 默认情况，使用寄存器值
            condition = handler.getRegisterName(a, instructionIndex);
        }

        // 设置jump标志，以便后续JMP指令处理
        register.jump = true;

        // 生成if语句
        register.ifDepth++;
        String jumpCode = String.format("if %s then", condition);
        return jumpCode;
    }

    /**
     * 生成返回代码
     * 
     * @param register         寄存器
     * @param chunk            代码块
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @param handler          指令处理器
     * @return 生成的指令代码
     */
    private String generateReturnCode(Register register, Chunk chunk, Instruction instruction, int instructionIndex, InstructionHandler handler) {
        int a = instruction.getA();
        int b = instruction.getB();

        StringBuilder sb = new StringBuilder();

        // 生成返回语句
        sb.append("return");

        if (b > 0) {
            sb.append(" ");
            for (int i = a; i < a + b; i++) {
                if (i > a)
                    sb.append(", ");
                sb.append(getRegisterName(i, instructionIndex, handler));
            }
        }
        return sb.toString();
    }

    /**
     * 生成表访问代码
     * 
     * @param register         寄存器
     * @param chunk            代码块
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @return 生成的指令代码
     */
    private String generateGetTableCode(Register register, Chunk chunk, Instruction instruction, int instructionIndex) {
        // OP_GETTABLE A B C R(A) := R(B)[RK(C)]
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();
        Constant constant = chunk.getConstant(c);

        RegisterEntity desRegisterEntity = register.getRegisterEntity(a);

        if (constant == null) {
            return String.format("-- OP_GETTABLE Unknown table index %d = %s", c,
                    getRegisterName(c, instructionIndex));
        } else {
            String result = String.format("%s[%s]",
                    getArgumentValue(register.getRegisterEntity(b), chunk, b, instructionIndex),
                    constant.getValue());
            desRegisterEntity.setValue(result);
            return "";
        }
    }

    /**
     * 生成表赋值代码
     * 
     * @param chunk            代码块
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @param handler          指令处理器
     * @return 生成的指令代码
     */
    private String generateSetTableCode(Chunk chunk, Instruction instruction, int instructionIndex, InstructionHandler handler) {
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();

        return String.format("%s[%s] = %s",
                getRegisterName(a, instructionIndex, handler),
                getRegisterName(b, instructionIndex, handler),
                getRegisterName(c, instructionIndex, handler));
    }

    /**
     * 生成字符串连接代码
     * 
     * @param chunk            代码块
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @param handler          指令处理器
     * @return 生成的指令代码
     */
    private String generateConcatCode(Chunk chunk, Instruction instruction, int instructionIndex, InstructionHandler handler) {
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();

        StringBuilder sb = new StringBuilder(String.format("%s = ", getRegisterName(a, instructionIndex, handler)));
        for (int i = b; i <= c; i++) {
            if (i > b)
                sb.append(" .. ");
            sb.append(getRegisterName(i, instructionIndex, handler));
        }
        return sb.toString();
    }

    /**
     * 生成算术运算代码
     * 
     * @param chunk            代码块
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @param handler          指令处理器
     * @return 生成的指令代码
     */
    private String generateArithmeticCode(Chunk chunk, Instruction instruction, int instructionIndex, InstructionHandler handler) {
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();
        String op = getArithmeticOperator(instruction.getOpcode());

        return String.format("%s = %s %s %s",
                getRegisterName(a, instructionIndex, handler),
                getRegisterName(b, instructionIndex, handler),
                op,
                getRegisterName(c, instructionIndex, handler));
    }

    /**
     * 生成一元运算代码
     * 
     * @param chunk            代码块
     * @param instruction      指令
     * @param instructionIndex 指令索引
     * @param handler          指令处理器
     * @return 生成的指令代码
     */
    private String generateUnaryCode(Chunk chunk, Instruction instruction, int instructionIndex, InstructionHandler handler) {
        int a = instruction.getA();
        int b = instruction.getB();
        String op = getUnaryOperator(instruction.getOpcode());

        return String.format("%s = %s%s",
                getRegisterName(a, instructionIndex, handler),
                op,
                getRegisterName(b, instructionIndex, handler));
    }

    /**
     * 获取算术运算符
     * 
     * @param opcode 操作码
     * @return 运算符字符串
     */
    private String getArithmeticOperator(Opcode opcode) {
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
                return "?";
        }
    }

    /**
     * 获取一元运算符
     * 
     * @param opcode 操作码
     * @return 运算符字符串
     */
    private String getUnaryOperator(Opcode opcode) {
        switch (opcode) {
            case UNM:
                return "-";
            case NOT:
                return "not ";
            case LEN:
                return "#";
            default:
                return "?";
        }
    }

    /**
     * 获取寄存器名，如果有已知值则使用值，否则使用R+寄存器号
     * 
     * @param register         寄存器号
     * @param instructionIndex 指令索引
     * @param handler          指令处理器
     * @return 寄存器名或变量名
     */
    private String getRegisterName(int register, int instructionIndex, InstructionHandler handler) {
        // 从InstructionHandler获取寄存器对象
        Register registerObj = handler.getRegisterByInstructionIndex(instructionIndex);
        return getRegisterName(register, registerObj);
    }

    /**
     * 获取寄存器名，如果有已知值则使用值，否则使用R+寄存器号
     * 
     * @param register    寄存器号
     * @param registerObj 寄存器对象
     * @return 寄存器名或变量名
     */
    private String getRegisterName(int register, Register registerObj) {
        // 使用RegisterEntity的getName方法获取寄存器名
        return registerObj.getRegisterEntity(register).getName();
    }

    /**
     * 兼容旧方法签名，从InstructionHandler获取寄存器对象
     * 
     * @param register         寄存器号
     * @param instructionIndex 指令索引
     * @return 寄存器名或变量名
     */
    private String getRegisterName(int register, int instructionIndex) {
        // 这个方法需要根据实际情况实现，这里暂时返回寄存器号
        return "R" + register;
    }
}
