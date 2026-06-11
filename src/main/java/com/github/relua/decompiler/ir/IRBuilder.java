package com.github.relua.decompiler.ir;

import com.github.relua.decompiler.CodeGeneratorContext;
import com.github.relua.decompiler.DecompilerPipeline;
import com.github.relua.ast.Expression;
import com.github.relua.ast.SourcePos;
import com.github.relua.ast.TableConstructor;
import com.github.relua.log.Logger;
import com.github.relua.model.Chunk;
import com.github.relua.model.Constant;
import com.github.relua.model.FromType;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;
import com.github.relua.model.Register;
import com.github.relua.model.Register.RegisterEntity;
import com.github.relua.model.UpValue;
import com.github.relua.util.BytecodeFormatter;
import com.github.relua.util.TransformUtils;
import com.github.relua.model.ValueType;

public class IRBuilder {
    private final DecompilerPipeline pipeline;

    public IRBuilder(DecompilerPipeline pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * 处理单个指令
     * 
     * @param chunk        代码块
     * @param instruction  指令
     * @param index        指令索引
     * @param currentState 当前寄存器状态
     * @return 下一条指令的索引
     */
    public int processInstruction(Chunk chunk, Instruction instruction, int index, Register currentState) {
        Logger.debug(BytecodeFormatter.formatInstruction(chunk, instruction, index));
        Opcode opcode = instruction.getOpcode();

        // 根据操作码处理不同类型的指令
        switch (opcode) {
            case MOVE: // 寄存器间数据移动
                processMoveInstruction(chunk, instruction, currentState);
                break;
            case LOADK: // 加载常量到寄存器
                processLoadKInstruction(chunk, instruction, currentState);
                break;
            case LOADBOOL: // 加载布尔值到寄存器
                processLoadBoolInstruction(chunk, instruction, currentState);
                break;
            case LOADNIL: // 加载nil到寄存器
                processLoadNilInstruction(chunk, instruction, currentState);
                break;
            case GETGLOBAL: // 获取全局变量
                processGetGlobalInstruction(chunk, instruction, currentState);
                break;
            case SETGLOBAL: // 设置全局变量
                processSetGlobalInstruction(chunk, instruction, currentState);
                break;
            case GETTABLE: // 获取表元素
                processGetTableInstruction(chunk, instruction, currentState);
                break;
            case SETTABLE: // 设置表元素
                processSetTableInstruction(chunk, instruction, currentState);
                break;
            case NEWTABLE: // 新建表
                processNewTableInstruction(chunk, instruction, currentState);
                break;
            case SELF: // 调用方法
                processSelfInstruction(chunk, instruction, currentState);
                break;
            case ADD: // 加法
            case SUB: // 减法
            case MUL: // 乘法
            case DIV: // 除法
            case MOD: // 取模
            case POW: // 幂运算
                processArithmeticInstruction(chunk, instruction, currentState);
                break;
            case UNM: // 取反
            case NOT: // 逻辑非
            case LEN: // 长度
                processUnaryInstruction(chunk, instruction, currentState);
                break;
            case CONCAT: // 字符串连接
                processConcatInstruction(chunk, instruction, currentState);
                break;
            case JMP: // 跳转
                // 跳转指令不改变寄存器状态
                break;
            case EQ: // 等于
            case LT: // 小于
            case LE: // 小于等于
                // 比较指令不改变寄存器状态
                break;
            case TEST: // 测试
            case TESTSET: // 测试并设置
                processTestInstruction(chunk, instruction, currentState);
                break;
            case CALL: // 函数调用
            case TAILCALL: // 尾调用
                processCallInstruction(chunk, instruction, currentState);
                break;
            case RETURN: // 返回
                // 返回指令不改变寄存器状态
                break;
            case FORLOOP: // for循环
            case FORPREP: // for循环准备
            case TFORLOOP: // 泛型for循环
                processLoopInstruction(chunk, instruction, currentState);
                break;
            case SETLIST: // 设置表列表
                processSetListInstruction(chunk, instruction, currentState);
                break;
            case CLOSE: // 关闭upvalue
                processCloseInstruction(chunk, instruction, currentState);
                break;
            case CLOSURE: // 创建闭包
                return processClosureInstruction(chunk, instruction, currentState, index);
            case VARARG: // 可变参数
                processVarargInstruction(chunk, instruction, currentState);
                break;
            case GETUPVAL: // 获取upvalue
                processGetUpvalInstruction(chunk, instruction, currentState);
                break;
            case SETUPVAL: // 设置upvalue
                processSetUpvalInstruction(chunk, instruction, currentState);
                break;
            default:
                break;
        }

        // Logger.debug(currentState.toString());
        return index + 1;
    }

    // 以下是各种指令的处理方法

    private void processMoveInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 记录寄存器间的移动，用于变量跟踪
        int a = instruction.getA();
        int b = instruction.getB();

        // 获取源寄存器的实体
        RegisterEntity srcEntity = currentState.getRegisterEntity(b);

        // 对于 TABLE 类型（无论是否为空），目标寄存器记录为对源寄存器的命名引用
        // 这样 CALL 参数展开时能显示正确的寄存器名（如 R0, R3 而非 {} 或 R5）
        if (srcEntity.getType() == ValueType.TABLE) {
            currentState.setRegisterEntity(a, "R" + b, ValueType.TABLE, FromType.GLOBAL);
        } else if (srcEntity.getType() == ValueType.UNKNOWN && srcEntity.getValue() == null) {
            // 当源是算术结果（UNKNOWN/null）时，目标记录为对源的命名引用
            currentState.setRegisterEntity(a, "R" + b, ValueType.UNKNOWN, FromType.REGISTER);
        } else if (srcEntity.getType() == ValueType.NIL) {
            // 源是 nil：不传播 NIL 类型，避免后续 MOVE 链将 literal nil 传递到
            // 方法调用位置（如 R7:match(...) 变成 nil:match(...)）。
            // 将目标标记为 UNKNOWN，让转换器输出寄存器名而非 nil 常量。
            currentState.setRegisterEntity(a, "R" + a, ValueType.UNKNOWN, FromType.REGISTER);
        } else {
            // 复制源寄存器的完整状态到目标寄存器
            currentState.setRegisterEntity(a, srcEntity.getValue(), srcEntity.getType(), srcEntity.getFromType());
        }
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

            currentState.setRegisterEntity(a, value, type, FromType.CONSTANT);
        }
    }

    private void processLoadBoolInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 加载布尔值到寄存器
        // OP_LOADBOOL A B C R(A) := (Bool)B; if (C) pc++
        int a = instruction.getA();
        boolean boolValue = instruction.getB() != 0;
        currentState.setRegisterEntity(a, boolValue, ValueType.BOOLEAN, FromType.CONSTANT);
    }

    private void processLoadNilInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 加载nil到寄存器
        int a = instruction.getA();
        int b = instruction.getB();
        for (int i = a; i <= b; i++) {
            currentState.setRegisterEntity(i, "nil", ValueType.NIL, FromType.NIL);
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
            currentState.setRegisterEntity(a, varName, ValueType.GLOBAL, FromType.GLOBAL);
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
        // OP_GETTABLE A B C R(A) := R(B)[RK(C)]
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();

        RegisterEntity RB = currentState.getRegisterEntity(b);

        String rx = TransformUtils.transformRX(chunk, currentState, c);
        String value;
        if (rx.startsWith("\"") && rx.endsWith("\"") && rx.length() >= 2) {
            String key = rx.substring(1, rx.length() - 1);
            if (key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                value = String.format("%s.%s", TransformUtils.transformRegister(RB), key);
            } else {
                value = String.format("%s[%s]", TransformUtils.transformRegister(RB), rx);
            }
        } else {
            value = String.format("%s[%s]", TransformUtils.transformRegister(RB), rx);
        }
        currentState.setRegisterEntity(a, value, ValueType.TABLE, FromType.GLOBAL);
    }

    private void processSetTableInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // OP_SETTABLE A B C R(A)[RK(B)] := RK(C)
        // 没有更新寄存器实体，直接记录为表访问
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();

        // RegisterEntity RA = currentState.getRegisterEntity(a);
        // RegisterEntity RB = currentState.getRegisterEntity(b);
        // String cValue = "RK" + c;
        // if (c < chunk.getConstants().size()) {
        // cValue = chunk.getConstants().get(c).getValue().toString();
        // }
        // if (RB.getFromType() == FromType.GLOBAL) {
        // String value = String.format("%s[\"%s\"] = %s", RB.getValue(), cValue,
        // cValue);
        // currentState.setRegisterEntity(a, value, ValueType.TABLE, FromType.GLOBAL);
        // } else {
        // // 简单处理：记录为表访问
        // currentState.setRegisterEntity(a, "table_access", ValueType.TABLE);
        // }
    }

    private void processNewTableInstruction(Chunk chunk, Instruction instruction, Register register) {
        // 新建表
        // OP_NEWTABLE A B C R(A) := {} (size = B, C)
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();

        register.setRegisterEntity(a, new TableConstructor(new java.util.ArrayList<>(), new SourcePos(instruction.getPc(), -1)),
                ValueType.TABLE, FromType.CONSTANT);
    }

    private void processSelfInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // OP_SELF A B C R(A+1) := R(B); R(A) := R(B)[RK(C)]
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();

        currentState.move(a + 1, b);

        RegisterEntity RB = currentState.getRegisterEntity(b);
        String RCValue = null;
        if (c < 256) {
            RCValue = TransformUtils.transformRegister(currentState.getRegisterEntity(c));
        } else {
            RCValue = chunk.getConstant(c - 256).getValue().toString();
        }

        if (RB.getFromType() == FromType.GLOBAL) {
            String value = String.format("%s:%s", RB.getValue(), RCValue);
            currentState.setRegisterEntity(a, value, ValueType.TABLE, FromType.GLOBAL);
        } else {
            // 简单处理：记录为表访问
            currentState.setRegisterEntity(a, RB.getName(), ValueType.TABLE, FromType.REGISTER);
        }
    }

    private void processArithmeticInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理算术指令 R(A) := RK(B) op RK(C)
        int a = instruction.getA();
        // 将结果标记为 REGISTER 类型，名称为 null，后续读取时会输出 R+a 形式
        currentState.setRegisterEntity(a, null, ValueType.UNKNOWN, FromType.REGISTER);
    }

    private void processUnaryInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理一元操作指令
        int a = instruction.getA();
        int b = instruction.getB();
        // 简单处理：保持原类型
        currentState.move(a, b);
    }

    private void processConcatInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // OP_CONCAT A B C R(A) := R(B).. ... ..R(C)
        int a = instruction.getA();
        // 简单处理：记录为字符串类型，但值设为 null，代表一个未知的临时字符串变量
        currentState.setRegisterEntity(a, null, ValueType.STRING, FromType.REGISTER);
    }

    private void processTestInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理测试指令
        // 不修改寄存器状态
    }

    private void processCallInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // OP_TAILCALL A B C return R(A)(R(A+1), ... ,R(A+B-1))
        if (instruction.getOpcode() == Opcode.TAILCALL) {
            return;
        }

        // OP_CALL A B C R(A), ... ,R(A+C-2) := R(A)(R(A+1), ... ,R(A+B-1))
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();

        // 获取函数实体
        RegisterEntity RA = currentState.getRegisterEntity(a);

        // 更新返回值寄存器状态
        // 情况1：C == 1 → 没有返回值
        if (c == 1) {
            // 函数调用没有返回值，不需要更新寄存器
            return;
        }

        // 情况2：C > 1 → 固定返回值个数
        if (c > 1) {
            // 返回值写入 R(A) 到 R(A+C-2)
            for (int i = 0; i < c - 1; i++) {
                int registerIndex = a + i;
                // 更新寄存器状态，将返回值标记为UNKNOWN类型
                String RAValue = (RA != null && RA.getValue() != null) ? RA.getValue().toString() : "";
                // Logger.debug(String.format("调用函数 %s %s", RAValue,
                // RAValue.equals("require")));
                if ("require".equals(RAValue)) {
                    RegisterEntity argsEntity = currentState.getRegisterEntity(a + 1);
                    if (argsEntity != null && argsEntity.getValue() != null) {
                        RAValue = argsEntity.getValue().toString().replace(".", "_");
                    } else {
                        RAValue = "R" + registerIndex;
                    }
                    currentState.setRegisterEntity(registerIndex, RAValue, ValueType.OBJECT, FromType.GLOBAL);
                    // Logger.debug(String.format("require 调用，返回值 %s 写入寄存器 R%d", RAValue,
                    // registerIndex));
                    continue;
                } else {
                    RAValue = "R" + registerIndex;
                }
                currentState.setRegisterEntity(registerIndex, RAValue, ValueType.UNKNOWN, FromType.REGISTER);
            }
        }

        // 情况3：C == 0 → 多返回值（VARARG 返回）
        if (c == 0) {
            // 多返回值，只更新第一个返回值寄存器
            // 实际的多返回值处理会在指令转换为AST时处理
            currentState.setRegisterEntity(a, "R" + a, ValueType.UNKNOWN, FromType.REGISTER);
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
        int a = instruction.getA();
        int b = instruction.getB();
        RegisterEntity tableEntity = currentState.getRegisterEntity(a);
        if (!(tableEntity.getValue() instanceof TableConstructor)) {
            return;
        }

        TableConstructor table = (TableConstructor) tableEntity.getValue();
        for (int i = 1; i <= b; i++) {
            RegisterEntity valueEntity = currentState.getRegisterEntity(a + i);
            Expression value = TransformUtils.transformToAstNode(valueEntity, instruction.getPc());
            table.addArrayField(value);
        }
    }

    private void processCloseInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理CLOSE指令
        // 不修改寄存器状态
    }

    private int processClosureInstruction(Chunk chunk, Instruction instruction, Register currentState,
            int instructionIndex) {
        // OP_CLOSURE A Bx R(A) := closure(KPROTO[Bx], R(A), ... ,R(A+n))
        int a = instruction.getA();
        int bx = instruction.getBx();

        String targetChunk = chunk.getFunction() + "_" + bx;
        // Logger.debug(String.format("创建闭包 %s 写入寄存器 R%d", targetChunk, a));

        // 记录为函数类型
        currentState.setRegisterEntity(a, targetChunk, ValueType.FUNCTION, FromType.GLOBAL);

        CodeGeneratorContext currentContext = pipeline.getContext();
        CodeGeneratorContext context = pipeline.getContext(targetChunk);
        // Logger.debug(String.format("闭包 %s 上值数量 %d", targetChunk, context.getChunk().getNup()));
        // Chunk targetChunkObj = context.getChunk();
        // int nextInstructionIndex = instructionIndex + 1;
        for (int i = 0; i < context.getChunk().getNup(); i++) {
            instructionIndex = instructionIndex + 1;
            Instruction nextInstruction = chunk.getInstruction(instructionIndex);
            if (nextInstruction.getOpcode() == Opcode.MOVE) {
                RegisterEntity RB = currentState.getRegisterEntity(nextInstruction.getB());
                // RegisterEntity entity = currentState.getRegisterEntity(upvalueIndex);
                context.addUpvalue(i, new UpValue(i, RB.getName(), RB.getValue(), RB.getType(), RB.getFromType()));
                // Logger.debug(String.format("%s: 上值 %s 写入寄存器 R%d", targetChunk, RB.getName(), a + i));
            } else if (nextInstruction.getOpcode() == Opcode.GETUPVAL) {
                UpValue upvalue = currentContext.getUpvalue(nextInstruction.getB());
                context.addUpvalue(i, upvalue);
                // Logger.debug(String.format("%s: 上值 %s 写入寄存器 R%d", targetChunk, upvalue.getName(), a + i));
            } else {
                Logger.error(String.format("%s: 未知上值指令 %s", chunk.getFunction(), nextInstruction));
            }
        }

        // context.addUpvalue(bx, null, context, null, null);
        return instructionIndex + 1;
    }

    private void processVarargInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // 处理VARARG指令
        // 不修改寄存器状态
    }

    private void processGetUpvalInstruction(Chunk chunk, Instruction instruction, Register register) {
        // OP_GETUPVAL A B R(A) := UpValue[B]
        int a = instruction.getA();
        int b = instruction.getB();
        RegisterEntity RA = register.getRegisterEntity(a);

        // 从CodeGeneratorContext中获取上值
        CodeGeneratorContext context = pipeline.getContext();
        UpValue upvalue = context.getUpvalue(b);

        if (upvalue != null) {
            // 更新寄存器状态
            RA.setValue(upvalue.getValue());
            RA.setType(upvalue.getType());
            RA.setFromType(upvalue.getFromType());
        } else {
            // 如果上值不存在，使用默认值
            RA.setValue("upvalue_" + b);
            RA.setType(ValueType.OBJECT);
            RA.setFromType(FromType.CONSTANT);
        }
    }

    private void processSetUpvalInstruction(Chunk chunk, Instruction instruction, Register currentState) {
        // OP_SETUPVAL A B UpValue[B] := R(A)
        int a = instruction.getA();
        int b = instruction.getB();

        // 获取A寄存器的值
        RegisterEntity registerEntity = currentState.getRegisterEntity(a);

        // 将寄存器的当前状态写入上值，使后续 GETUPVAL 能读到最新值
        CodeGeneratorContext context = pipeline.getContext();
        UpValue upvalue = context.getUpvalue(b);
        String name = (upvalue != null) ? upvalue.getName() : ("upvalue_" + b);
        context.addUpvalue(b, new UpValue(b, name,
                registerEntity.getValue(), registerEntity.getType(), registerEntity.getFromType()));
    }
}

