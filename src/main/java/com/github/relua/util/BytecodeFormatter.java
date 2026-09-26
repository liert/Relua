package com.github.relua.util;

import com.github.relua.model.Chunk;
import com.github.relua.model.Constant;
import com.github.relua.model.Instruction;
import com.github.relua.model.LocalVar;
import com.github.relua.model.LuacFile;
import com.github.relua.model.Opcode;
import java.util.List;

/**
 * 字节码格式化工具类
 */
public class BytecodeFormatter {
    /**
     * 格式化整个 Luac 文件的完整字节码反汇编（包含文件头、主函数及所有子函数）
     * 
     * @param luacFile Luac文件对象
     * @return 格式化后的完整字节码反汇编文本
     */
    public static String formatLuacFile(LuacFile luacFile) {
        if (luacFile == null) {
            return "; 未加载有效的 Luac 文件。\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("; ====================================================================\n");
        sb.append("; Relua 字节码反汇编\n");
        sb.append(String.format("; Lua 版本: %s\n", luacFile.getLuaVersion()));
        sb.append(String.format("; 字节序: %s\n", luacFile.isLittleEndian() ? "Little Endian (小端)" : "Big Endian (大端)"));
        sb.append(String.format("; 指令大小: %d 字节, 数字大小: %d 字节\n", 
                                luacFile.getInstructionSize(), luacFile.getLuaNumberSize()));
        sb.append(String.format("; 格式标识: %d, 整数标志: %d\n", luacFile.getFormat(), luacFile.getIntegralFlag()));
        sb.append("; ====================================================================\n\n");
        
        Chunk mainChunk = luacFile.getMainChunk();
        if (mainChunk != null) {
            formatChunkInternal(mainChunk, 0, true, sb);
        } else {
            sb.append("; 主代码块 (Main chunk) 为空。\n");
        }
        return sb.toString();
    }

    /**
     * 格式化单个代码块（Chunk）的字节码
     * 
     * @param chunk 代码块
     * @return 格式化后的字节码文本
     */
    public static String formatChunk(Chunk chunk) {
        return formatChunk(chunk, true);
    }

    /**
     * 格式化单个代码块（Chunk）的字节码
     * 
     * @param chunk 代码块
     * @param includeSubChunks 是否递归包含子函数
     * @return 格式化后的字节码文本
     */
    public static String formatChunk(Chunk chunk, boolean includeSubChunks) {
        if (chunk == null) {
            return "; 代码块 (Chunk) 为空。\n";
        }
        StringBuilder sb = new StringBuilder();
        formatChunkInternal(chunk, 0, includeSubChunks, sb);
        return sb.toString();
    }

    private static void formatChunkInternal(Chunk chunk, int depth, boolean includeSubChunks, StringBuilder sb) {
        String indent = "  ".repeat(depth);
        String funcName = chunk.getFunction();
        if (funcName == null || funcName.isEmpty()) {
            funcName = (depth == 0) ? "main" : "anonymous_func";
        }

        sb.append(indent).append("; --------------------------------------------------------------------\n");
        sb.append(indent).append(String.format("; 函数: %s (层级: %d)\n", funcName, depth));
        sb.append(indent).append(String.format("; 定义行号: %d 到 %d\n", chunk.getLineDefined(), chunk.getLastLineDefined()));
        sb.append(indent).append(String.format("; 参数数量: %d, 可变参数: %d, 最大栈大小: %d, Upvalues: %d\n",
                chunk.getNumParams(), chunk.getIsVararg(), chunk.getMaxStackSize(), chunk.getNup()));
        sb.append(indent).append("; --------------------------------------------------------------------\n");

        // 常量表
        List<Constant> constants = chunk.getConstants();
        if (constants != null && !constants.isEmpty()) {
            sb.append(indent).append(String.format("; 常量池 (共 %d 个):\n", constants.size()));
            for (int i = 0; i < constants.size(); i++) {
                Constant c = constants.get(i);
                sb.append(indent).append(String.format(";   .const K%-3d = %s\n", i, c != null ? c.toString() : "nil"));
            }
            sb.append("\n");
        }

        // 局部变量表
        List<LocalVar> localVars = chunk.getLocalVars();
        if (localVars != null && !localVars.isEmpty()) {
            sb.append(indent).append(String.format("; 局部变量 (共 %d 个):\n", localVars.size()));
            for (int i = 0; i < localVars.size(); i++) {
                LocalVar lv = localVars.get(i);
                sb.append(indent).append(String.format(";   .local [%d] %-16s (生命周期 pc: %d - %d)\n", 
                        i, lv.getName(), lv.getStartPC(), lv.getEndPC()));
            }
            sb.append("\n");
        }

        // 指令列表
        List<Instruction> instructions = chunk.getInstructions();
        if (instructions != null && !instructions.isEmpty()) {
            sb.append(indent).append(String.format("; 指令列表 (共 %d 条指令):\n", instructions.size()));
            for (int i = 0; i < instructions.size(); i++) {
                Instruction inst = instructions.get(i);
                String formattedInst = formatInstruction(chunk, inst, i);
                sb.append(indent).append(formattedInst).append("\n");
            }
        } else {
            sb.append(indent).append("; 无指令。\n");
        }

        sb.append("\n");

        // 递归输出子函数
        if (includeSubChunks) {
            List<Chunk> subChunks = chunk.getSubChunks();
            if (subChunks != null && !subChunks.isEmpty()) {
                sb.append(indent).append(String.format("; 子函数 (共 %d 个):\n\n", subChunks.size()));
                for (int i = 0; i < subChunks.size(); i++) {
                    formatChunkInternal(subChunks.get(i), depth + 1, true, sb);
                }
            }
        }
    }
    /**
     * 格式化单个指令为字节码字符串
     * 
     * @param chunk 代码块
     * @param instruction 指令
     * @param index 指令索引
     * @return 格式化后的字节码字符串，格式为："0 [-]: GETGLOBAL R0 K0        ; R0 := pcall"
     */
    public static String formatInstruction(Chunk chunk, Instruction instruction, int index) {
        StringBuilder sb = new StringBuilder();
        
        // 格式化指令基本信息：索引 [-]: 操作码 寄存器/常量
        sb.append(String.format("%4d: %s\t", index, instruction.getOpcode().name()));
        
        // 添加操作数信息
        appendOperands(instruction, sb);
        
        // 对齐格式
        while (sb.length() < 35) {
            sb.append(' ');
        }
        
        // 添加注释：寄存器操作说明
        sb.append("; ");
        generateRegisterOperation(chunk, instruction, index, sb);
        
        return sb.toString();
    }
    
    /**
     * 将RK值转换为对应的符号表达（如 K0, R1 等）
     * 
     * @param rk 寄存器或常量编码
     * @return 符号表达字符串
     */
    private static String rkToSymbolString(int rk) {
        if (rk >= 256) {
            return "K" + (rk - 256);
        }
        return "R" + rk;
    }

    /**
     * 获取关系或测试指令在满足条件时的实际跳转目标地址
     *
     * @param chunk 代码块
     * @param currentIndex 关系或测试指令的索引
     * @return 绝对跳转目标指令的索引
     */
    private static int getJumpTargetForRelation(Chunk chunk, int currentIndex) {
        if (chunk != null && chunk.getInstructions() != null) {
            List<Instruction> insts = chunk.getInstructions();
            int nextIndex = currentIndex + 1;
            if (nextIndex < insts.size()) {
                Instruction nextInst = insts.get(nextIndex);
                if (nextInst.getOpcode() == Opcode.JMP) {
                    return nextIndex + 1 + nextInst.getSBx();
                }
            }
        }
        return currentIndex + 2; // 默认跳过下一条指令
    }
    
    /**
     * 添加指令操作数
     * @param instruction 指令
     * @param sb 字符串构建器
     */
    private static void appendOperands(Instruction instruction, StringBuilder sb) {
        Opcode opcode = instruction.getOpcode();
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();
        int bx = instruction.getBx();
        int sbx = instruction.getSBx();
        
        switch (opcode) {
            case MOVE:
                sb.append(String.format("R%d R%d", a, b));
                break;
            case LOADK:
                sb.append(String.format("R%d K%d", a, bx));
                break;
            case LOADBOOL:
                sb.append(String.format("R%d %d %d", a, b, c));
                break;
            case LOADNIL:
                sb.append(String.format("R%d R%d", a, b));
                break;
            case GETGLOBAL:
                sb.append(String.format("R%d K%d", a, bx));
                break;
            case SETGLOBAL:
                sb.append(String.format("R%d K%d", a, bx));
                break;
            case GETUPVAL:
                sb.append(String.format("R%d %d", a, b));
                break;
            case SETUPVAL:
                sb.append(String.format("R%d %d", a, b));
                break;
            case GETTABLE:
                sb.append(String.format("R%d R%d %s", a, b, rkToSymbolString(c)));
                break;
            case SETTABLE:
                sb.append(String.format("R%d %s %s", a, rkToSymbolString(b), rkToSymbolString(c)));
                break;
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
            case POW:
                sb.append(String.format("R%d %s %s", a, rkToSymbolString(b), rkToSymbolString(c)));
                break;
            case UNM:
            case NOT:
            case LEN:
                sb.append(String.format("R%d R%d", a, b));
                break;
            case CONCAT:
                sb.append(String.format("R%d R%d R%d", a, b, c));
                break;
            case JMP:
                sb.append(String.format("%d", sbx));
                break;
            case EQ:
            case LT:
            case LE:
                sb.append(String.format("%d %s %s", a, rkToSymbolString(b), rkToSymbolString(c)));
                break;
            case TEST:
                sb.append(String.format("R%d %d", a, c));
                break;
            case TESTSET:
                sb.append(String.format("R%d R%d %d", a, b, c));
                break;
            case CALL:
            case TAILCALL:
                sb.append(String.format("R%d %d %d", a, b, c));
                break;
            case RETURN:
                sb.append(String.format("R%d %d", a, b));
                break;
            case FORLOOP:
            case FORPREP:
                sb.append(String.format("R%d %d", a, sbx));
                break;
            case TFORLOOP:
                sb.append(String.format("R%d %d %d", a, b, c));
                break;
            case SETLIST:
                sb.append(String.format("R%d %d %d", a, b, c));
                break;
            case CLOSE:
                sb.append(String.format("R%d", a));
                break;
            case CLOSURE:
                sb.append(String.format("R%d %d", a, bx));
                break;
            case VARARG:
                sb.append(String.format("R%d %d", a, b));
                break;
            case NEWTABLE:
                sb.append(String.format("R%d %d %d", a, b, c));
                break;
            case SELF:
                sb.append(String.format("R%d R%d %s", a, b, rkToSymbolString(c)));
                break;
            default:
                sb.append(instruction.toString());
                break;
        }
    }
    
    /**
     * 生成寄存器操作格式的指令输出
     * @param chunk 代码块
     * @param instruction 指令
     * @param index 指令PC值
     * @param sb 字符串构建器
     */
    private static void generateRegisterOperation(Chunk chunk, Instruction instruction, int index, StringBuilder sb) {
        Opcode opcode = instruction.getOpcode();
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();
        int bx = instruction.getBx();
        int sbx = instruction.getSBx();
        
        switch (opcode) {
            case MOVE:
                sb.append(String.format("R%d := R%d", a, b));
                break;
            case LOADK:
                Constant constant = chunk.getConstant(bx);
                if (constant != null) {
                    sb.append(String.format("R%d := %s", a, constant.toString()));
                } else {
                    sb.append(String.format("R%d := nil -- Unknown constant index %d", a, bx));
                }
                break;
            case LOADBOOL:
                if (c != 0) {
                    sb.append(String.format("R%d := %b; goto %d", a, b != 0, index + 2));
                } else {
                    sb.append(String.format("R%d := %b", a, b != 0));
                }
                break;
            case LOADNIL:
                for (int i = a; i <= b; i++) {
                    if (i > a) sb.append("; ");
                    sb.append(String.format("R%d := nil", i));
                }
                break;
            case GETGLOBAL:
                constant = chunk.getConstant(bx);
                if (constant != null) {
                    sb.append(String.format("R%d := %s", a, constant.getValue()));
                } else {
                    sb.append(String.format("R%d := nil -- Unknown global index %d", a, bx));
                }
                break;
            case SETGLOBAL:
                constant = chunk.getConstant(bx);
                if (constant != null) {
                    sb.append(String.format("%s := R%d", constant.getValue(), a));
                } else {
                    sb.append(String.format("-- Unknown global index %d := R%d", bx, a));
                }
                break;
            case GETUPVAL:
                sb.append(String.format("R%d := UpValue[%d]", a, b));
                break;
            case SETUPVAL:
                sb.append(String.format("UpValue[%d] := R%d", b, a));
                break;
            case GETTABLE:
                sb.append(String.format("R%d := %s[%s]", a, rkToString(chunk, b), rkToString(chunk, c)));
                break;
            case SETTABLE:
                sb.append(String.format("%s[%s] := %s", rkToString(chunk, a), rkToString(chunk, b), rkToString(chunk, c)));
                break;
            case ADD:
                sb.append(String.format("R%d := %s + %s", a, rkToString(chunk, b), rkToString(chunk, c)));
                break;
            case SUB:
                sb.append(String.format("R%d := %s - %s", a, rkToString(chunk, b), rkToString(chunk, c)));
                break;
            case MUL:
                sb.append(String.format("R%d := %s * %s", a, rkToString(chunk, b), rkToString(chunk, c)));
                break;
            case DIV:
                sb.append(String.format("R%d := %s / %s", a, rkToString(chunk, b), rkToString(chunk, c)));
                break;
            case MOD:
                sb.append(String.format("R%d := %s %% %s", a, rkToString(chunk, b), rkToString(chunk, c)));
                break;
            case POW:
                sb.append(String.format("R%d := %s ^ %s", a, rkToString(chunk, b), rkToString(chunk, c)));
                break;
            case UNM:
                sb.append(String.format("R%d := -R%d", a, b));
                break;
            case NOT:
                sb.append(String.format("R%d := not R%d", a, b));
                break;
            case LEN:
                sb.append(String.format("R%d := #R%d", a, b));
                break;
            case CONCAT:
                sb.append(String.format("R%d := R%d .. R%d", a, b, c));
                break;
            case JMP:
                sb.append(String.format("goto %d", index + 1 + sbx));
                break;
            case EQ:
                sb.append(String.format("if %s %s %s then goto %d", rkToString(chunk, b), (a == 0 ? "~=" : "=="), rkToString(chunk, c), getJumpTargetForRelation(chunk, index)));
                break;
            case LT:
                sb.append(String.format("if %s %s %s then goto %d", rkToString(chunk, b), (a == 0 ? ">=" : "<"), rkToString(chunk, c), getJumpTargetForRelation(chunk, index)));
                break;
            case LE:
                sb.append(String.format("if %s %s %s then goto %d", rkToString(chunk, b), (a == 0 ? ">" : "<="), rkToString(chunk, c), getJumpTargetForRelation(chunk, index)));
                break;
            case TEST:
                sb.append(String.format("if %sR%d then goto %d", (c == 0 ? "not " : ""), a, getJumpTargetForRelation(chunk, index)));
                break;
            case TESTSET:
                sb.append(String.format("if %sR%d then R%d := R%d; goto %d", (c == 0 ? "not " : ""), b, a, b, getJumpTargetForRelation(chunk, index)));
                break;
            case CALL:
                if (c == 0) {
                    sb.append(String.format("R%d... := ", a));
                } else if (c > 1) {
                    for (int i = a; i <= a + c - 2; i++) {
                        if (i > a) sb.append(", ");
                        sb.append(String.format("R%d", i));
                    }
                    sb.append(" := ");
                }
                sb.append(String.format("R%d(", a));
                if (b == 0) {
                    sb.append(String.format("R%d...", a + 1));
                } else if (b > 1) {
                    for (int i = a + 1; i <= a + b - 1; i++) {
                        if (i > a + 1) sb.append(", ");
                        sb.append(String.format("R%d", i));
                    }
                }
                sb.append(")");
                break;
            case TAILCALL:
                sb.append(String.format("return R%d(", a));
                if (b == 0) {
                    sb.append(String.format("R%d...", a + 1));
                } else if (b > 1) {
                    for (int i = a + 1; i <= a + b - 1; i++) {
                        if (i > a + 1) sb.append(", ");
                        sb.append(String.format("R%d", i));
                    }
                }
                sb.append(")");
                break;
            case RETURN:
                if (b == 0) {
                    sb.append(String.format("return R%d...", a));
                } else if (b == 1) {
                    sb.append("return");
                } else {
                    sb.append("return ");
                    for (int i = a; i <= a + b - 2; i++) {
                        if (i > a) sb.append(", ");
                        sb.append(String.format("R%d", i));
                    }
                }
                break;
            case FORLOOP:
                sb.append(String.format("R%d += R%d; if R%d <?= R%d then { R%d := R%d; goto %d }", a, a + 2, a, a + 1, a + 3, a, index + 1 + sbx));
                break;
            case FORPREP:
                sb.append(String.format("R%d -= R%d; goto %d", a, a + 2, index + 1 + sbx));
                break;
            case TFORLOOP:
                sb.append(String.format("R%d, ..., R%d := R%d(R%d, R%d); if R%d ~= nil then { R%d := R%d; goto %d }", 
                    a + 3, a + 2 + c, a, a + 1, a + 2, a + 3, a + 2, a + 3, getJumpTargetForRelation(chunk, index)));
                break;
            case SETLIST:
                int realC = c;
                if (c == 0 && chunk != null && chunk.getInstructions() != null && index + 1 < chunk.getInstructions().size()) {
                    realC = chunk.getInstructions().get(index + 1).getCode();
                }
                if (b == 0) {
                    sb.append(String.format("R%d[(%d-1)*50+i] := R%d+i...", a, realC, a));
                } else {
                    sb.append(String.format("R%d[(%d-1)*50+i] := R%d+i, 1 <= i <= %d", a, realC, a, b));
                }
                break;
            case CLOSE:
                sb.append(String.format("close all variables in the stack up to (>=) R%d", a));
                break;
            case CLOSURE:
                sb.append(String.format("R%d := closure(%d)", a, bx));
                break;
            case VARARG:
                if (b == 0) {
                    sb.append(String.format("R%d... := vararg()", a));
                } else if (b == 1) {
                    sb.append("vararg()");
                } else {
                    for (int i = a; i <= a + b - 2; i++) {
                        if (i > a) sb.append(", ");
                        sb.append(String.format("R%d", i));
                    }
                    sb.append(" := vararg()");
                }
                break;
            case NEWTABLE:
                sb.append(String.format("R%d := {} (size = %d,%d)", a, b, c));
                break;
            case SELF:
                sb.append(String.format("R%d := R%d; R%d := R%d[%s]", a + 1, b, a, b, rkToString(chunk, c)));
                break;
            default:
                sb.append(instruction.toString());
                break;
        }
    }

    private static String rkToString(Chunk chunk, int rk) {
        if (rk >= 256) {
            Constant constant = chunk.getConstant(rk - 256);
            if (constant != null) {
                return constant.toString();
            }
            return "K" + (rk - 256);
        }
        return "R" + rk;
    }
}
