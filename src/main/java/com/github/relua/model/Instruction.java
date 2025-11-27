package com.github.relua.model;

/**
 * Lua指令模型
 */
public class Instruction {
    // Lua 5.1/5.2 操作码定义
    public enum Opcode {
        // 加载和存储指令
        MOVE, LOADK, LOADBOOL, LOADNIL, GETUPVAL, GETGLOBAL, GETTABLE,
        SETGLOBAL, SETUPVAL, SETTABLE, NEWTABLE, SELF,
        // 算术指令
        ADD, SUB, MUL, DIV, MOD, POW, UNM, NOT, LEN,
        // 比较和逻辑指令
        CONCAT, JMP, EQ, LT, LE,
        // 表操作指令
        TEST, TESTSET, CALL, TAILCALL, RETURN, FORLOOP, FORPREP,
        // 函数定义和调用指令
        TFORLOOP, SETLIST, CLOSE, CLOSURE, VARARG,
        // 未知操作码
        UNKNOWN
    }

    private long code;          // 原始指令码
    private Opcode opcode;     // 操作码
    private int a;             // 操作数A
    private int b;             // 操作数B
    private int c;             // 操作数C
    private int bx;            // 操作数Bx
    private int sbx;           // 操作数sBx

    /**
     * 构造函数
     * @param code 原始指令码
     */
    public Instruction(int code) {
        // long ucode = 
        this.code = code & 0xFFFFFFFFL;
        int opcodeValue = (int)(code & 0x3F);
        try {
            this.opcode = Opcode.values()[opcodeValue];
        } catch (ArrayIndexOutOfBoundsException e) {
            // 处理未知操作码
            this.opcode = Opcode.UNKNOWN;
        }
        decodeOperands();
    }

    /**
     * 解析操作数
     */
    private void decodeOperands() {
        long ucode = this.code;
        Opcode op = this.opcode;

        switch (op) {
            case MOVE:
            case LOADNIL:
            case GETUPVAL:
            case SETUPVAL:
            case UNM:
            case NOT:
            case LEN:
            case RETURN:
            case TAILCALL:
            case VARARG:
            case FORLOOP:
            case FORPREP:
                a = (int)(ucode >> 6) & 0xFF;
                b = (int)(ucode >> 23) & 0x1FF;
                c = 0;
                break;
            case LOADK:
            case GETGLOBAL:
            case SETGLOBAL:
            case CLOSURE:
                a = (int)(ucode >> 6) & 0xFF;
                bx = (int)(ucode >> 14);
                break;
            case LOADBOOL:
            case TEST:
            case TESTSET:
                a = (int)(ucode >> 6) & 0xFF;
                b = (int)(ucode >> 23) & 0x1FF;
                c = (int)(ucode >> 15) & 0x1FF;
                break;
            case GETTABLE:
            case SETTABLE:
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
            case POW:
            case CONCAT:
            case CALL:
            case TFORLOOP:
            case SETLIST:
            case CLOSE:
            case NEWTABLE:
            case SELF:
                a = (int)(ucode >> 6) & 0xFF;
                b = (int)(ucode >> 23) & 0x1FF;
                c = (int)(ucode >> 14) & 0xFF;
                break;
            case EQ:
            case LT:
            case LE:
                a = 0;
                b = (int)(ucode >> 23) & 0x1FF;
                c = (int)(ucode >> 15) & 0x1FF;
                break;
            case JMP:
                a = 0;
                System.out.println(ucode);
                sbx = (int)(ucode >> 14) - 131071;
                break;
            default:
                // 处理未知操作码
                a = (int)(ucode >> 6) & 0xFF;
                b = (int)(ucode >> 23) & 0x1FF;
                c = (int)(ucode >> 15) & 0x1FF;
                bx = (int)(ucode >> 14);
                sbx = (int)(ucode >> 14) - 1073741824;
                break;
        }
    }

    /**
     * 获取原始指令码
     * @return 原始指令码
     */
    public int getCode() {
        return (int)code;
    }

    /**
     * 获取操作码
     * @return 操作码
     */
    public Opcode getOpcode() {
        return opcode;
    }

    /**
     * 获取操作数A
     * @return 操作数A
     */
    public int getA() {
        return a;
    }

    /**
     * 获取操作数B
     * @return 操作数B
     */
    public int getB() {
        return b;
    }

    /**
     * 获取操作数C
     * @return 操作数C
     */
    public int getC() {
        return c;
    }

    /**
     * 获取操作数Bx
     * @return 操作数Bx
     */
    public int getBx() {
        return bx;
    }

    /**
     * 获取操作数sBx
     * @return 操作数sBx
     */
    public int getSBx() {
        return sbx;
    }

    @Override
    public String toString() {
        return String.format("%s A=%d B=%d C=%d Bx=%d sBx=%d",
                opcode.name(), a, b, c, bx, sbx);
    }
}