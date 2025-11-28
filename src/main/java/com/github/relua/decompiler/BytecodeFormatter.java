package com.github.relua.decompiler;

import com.github.relua.model.Chunk;
import com.github.relua.model.Constant;
import com.github.relua.model.Instruction;

/**
 * 字节码格式化工具类
 */
public class BytecodeFormatter {
    
    /**
     * 格式化单个指令为字节码字符串
     * @param chunk 代码块
     * @param instruction 指令
     * @param index 指令索引
     * @return 格式化后的字节码字符串，格式为："0 [-]: GETGLOBAL R0 K0        ; R0 := pcall"
     */
    public static String formatInstruction(Chunk chunk, Instruction instruction, int index) {
        StringBuilder sb = new StringBuilder();
        
        // 格式化指令基本信息：索引 [-]: 操作码 寄存器/常量
        sb.append(String.format("%4d: %s ", index, instruction.getOpcode().name()));
        
        // 添加操作数信息
        appendOperands(instruction, sb);
        
        // 对齐格式
        while (sb.length() < 30) {
            sb.append(' ');
        }
        
        // 添加注释：寄存器操作说明
        sb.append("; ");
        generateRegisterOperation(chunk, instruction, sb);
        
        return sb.toString();
    }
    
    /**
     * 添加指令操作数
     * @param instruction 指令
     * @param sb 字符串构建器
     */
    private static void appendOperands(Instruction instruction, StringBuilder sb) {
        Instruction.Opcode opcode = instruction.getOpcode();
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
                sb.append(String.format("R%d R%d R%d", a, b, c));
                break;
            case SETTABLE:
                sb.append(String.format("R%d R%d R%d", a, b, c));
                break;
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
            case POW:
                sb.append(String.format("R%d R%d R%d", a, b, c));
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
                sb.append(String.format("R%d R%d %d", b, c, sbx));
                break;
            case TEST:
                sb.append(String.format("R%d %d %d", a, c, sbx));
                break;
            case TESTSET:
                sb.append(String.format("R%d R%d %d %d", a, c, b, sbx));
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
                sb.append(String.format("R%d", a));
                break;
            case SELF:
                sb.append(String.format("R%d R%d R%d", a, b, c));
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
     * @param sb 字符串构建器
     */
    private static void generateRegisterOperation(Chunk chunk, Instruction instruction, StringBuilder sb) {
        Instruction.Opcode opcode = instruction.getOpcode();
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
                sb.append(String.format("R%d := %b", a, b != 0));
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
                sb.append(String.format("R%d := R%d[R%d]", a, b, c));
                break;
            case SETTABLE:
                sb.append(String.format("R%d[R%d] := R%d", a, b, c));
                break;
            case ADD:
                sb.append(String.format("R%d := R%d + R%d", a, b, c));
                break;
            case SUB:
                sb.append(String.format("R%d := R%d - R%d", a, b, c));
                break;
            case MUL:
                sb.append(String.format("R%d := R%d * R%d", a, b, c));
                break;
            case DIV:
                sb.append(String.format("R%d := R%d / R%d", a, b, c));
                break;
            case MOD:
                sb.append(String.format("R%d := R%d %% R%d", a, b, c));
                break;
            case POW:
                sb.append(String.format("R%d := R%d ^ R%d", a, b, c));
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
                sb.append(String.format("goto %d", sbx));
                break;
            case EQ:
                sb.append(String.format("if R%d == R%d then goto %d", b, c, sbx));
                break;
            case LT:
                sb.append(String.format("if R%d < R%d then goto %d", b, c, sbx));
                break;
            case LE:
                sb.append(String.format("if R%d <= R%d then goto %d", b, c, sbx));
                break;
            case TEST:
                sb.append(String.format("if R%d %s then goto %d", a, (c == 0 ? "== false" : "== true"), sbx));
                break;
            case TESTSET:
                sb.append(String.format("if R%d %s then R%d := R%d; goto %d", c, (b == 0 ? "== false" : "== true"), a, c, sbx));
                break;
            case CALL:
                sb.append(String.format("R%d := R%d(...) -- b=%d, c=%d", a, a, b, c));
                break;
            case TAILCALL:
                sb.append(String.format("tailcall R%d(...) -- b=%d, c=%d", a, b, c));
                break;
            case RETURN:
                if (b == 0) {
                    sb.append("return");
                } else if (b == 1) {
                    sb.append(String.format("return R%d", a));
                } else {
                    sb.append("return ");
                    for (int i = a; i < a + b; i++) {
                        if (i > a) sb.append(", ");
                        sb.append(String.format("R%d", i));
                    }
                }
                break;
            case FORLOOP:
                sb.append(String.format("forloop R%d, R%d, R%d, R%d -- sBx=%d", a, a+1, a+2, a+3, sbx));
                break;
            case FORPREP:
                sb.append(String.format("forprep R%d, R%d, R%d, R%d -- sBx=%d", a, a+1, a+2, a+3, sbx));
                break;
            case TFORLOOP:
                sb.append(String.format("tforloop R%d, R%d -- b=%d, c=%d", a, a+1, b, c));
                break;
            case SETLIST:
                sb.append(String.format("setlist R%d, R%d -- b=%d, c=%d", a, b, b, c));
                break;
            case CLOSE:
                sb.append(String.format("close R%d", a));
                break;
            case CLOSURE:
                sb.append(String.format("R%d := closure(%d)", a, bx));
                break;
            case VARARG:
                sb.append(String.format("R%d := vararg() -- b=%d", a, b));
                break;
            case NEWTABLE:
                sb.append(String.format("R%d := {}", a));
                break;
            case SELF:
                sb.append(String.format("R%d, R%d := R%d, R%d", a, a+1, b, c));
                break;
            default:
                sb.append(instruction.toString());
                break;
        }
    }
}