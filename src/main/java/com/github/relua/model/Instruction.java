package com.github.relua.model;

import com.github.relua.model.Instruction.Opcode;

/**
 * Lua指令模型
 */
public class Instruction {
    private static final int SIZE_C = 9;
    private static final int SIZE_B = 9;
    private static final int SIZE_Bx = (SIZE_C + SIZE_B);
    private static final int SIZE_A = 8;

    private static final int SIZE_OP = 6;

    private static final int POS_OP = 0;
    private static final int POS_A = (POS_OP + SIZE_OP);
    private static final int POS_C = (POS_A + SIZE_A);
    private static final int POS_B = (POS_C + SIZE_C);
    private static final int POS_Bx = POS_C;

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

    private long code; // 原始指令码
    private Opcode opcode; // 操作码
    private int a; // 操作数A
    private int b; // 操作数B
    private int c; // 操作数C
    private int bx; // 操作数Bx
    private int sbx; // 操作数sBx
    private boolean processed = false; // 指令是否已处理

    /**
     * 构造函数
     * 
     * @param code 原始指令码
     */
    public Instruction(int code) {
        // long ucode =
        this.code = code & 0xFFFFFFFFL;
        int opcodeValue = (int) (code & 0x3F);
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
                a = (int) (ucode >> 6) & 0xFF;
                b = (int) (ucode >> 23) & 0x1FF;
                c = 0;
                break;
            case LOADK:
            case GETGLOBAL:
            case SETGLOBAL:
            case CLOSURE:
                a = (int) (ucode >> 6) & 0xFF;
                bx = (int) (ucode >> 14);
                break;
            case LOADBOOL:
            case TEST:
            case TESTSET:
                // System.out.println("Code => " + ucode);
                a = getArgA((int) ucode);
                b = getArgB((int) ucode);
                c = (int) (ucode >> 14) & 0xFF;
                break;
            
            case SETTABLE:
                a = (int) (ucode >> 6) & 0xFF;
                b = (int) (ucode >> 23) & 0xFF;
                c = (int) (ucode >> 14) & 0xFF;
                break;
            case GETTABLE:
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
                a = (int) (ucode >> 6) & 0xFF;
                b = (int) (ucode >> 23) & 0x1FF;
                c = (int) (ucode >> 14) & 0xFF;
                break;
            case EQ:
            case LT:
            case LE:
                a = 0;
                b = (int) (ucode >> 23) & 0x1FF;
                c = (int) (ucode >> 15) & 0x1FF;
                break;
            case JMP:
                a = 0;
                sbx = (int) (ucode >> 14) - 131071;
                break;
            default:
                // 处理未知操作码
                a = (int) (ucode >> 6) & 0xFF;
                b = (int) (ucode >> 23) & 0x1FF;
                c = (int) (ucode >> 15) & 0x1FF;
                bx = (int) (ucode >> 14);
                sbx = (int) (ucode >> 14) - 1073741824;
                break;
        }
    }

    /**
     * 获取原始指令码
     * 
     * @return 原始指令码
     */
    public int getCode() {
        return (int) code;
    }

    /**
     * 获取操作码
     * 
     * @return 操作码
     */
    public Opcode getOpcode() {
        return opcode;
    }

    /**
     * 获取操作数A
     * 
     * @return 操作数A
     */
    public int getA() {
        return a;
    }

    /**
     * 获取操作数B
     * 
     * @return 操作数B
     */
    public int getB() {
        return b;
    }

    /**
     * 获取操作数C
     * 
     * @return 操作数C
     */
    public int getC() {
        return c;
    }

    /**
     * 获取操作数Bx
     * 
     * @return 操作数Bx
     */
    public int getBx() {
        return bx;
    }

    /**
     * 获取操作数sBx
     * 
     * @return 操作数sBx
     */
    public int getSBx() {
        return sbx;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void markProcessed() {
        this.processed = true;
    }

    private int mask(int n, int p) {
        return (~((~0) << n)) << p;
    }

    private int getArgA(int code) {
        return (int) (code >> POS_A) & mask(SIZE_A, 0);
    }

    private int getArgB(int code) {
        return (int) (code >> POS_B) & mask(SIZE_B, 0);
    }

    private int getArgC(int code) {
        return (int) (code >> POS_C) & mask(SIZE_C, 0);
    }

    private int getArgBx(int code) {
        return (int) (code >> POS_Bx) & mask(SIZE_Bx, 0);
    }

    private int getArgSBx(int code) {
        return (int) (getArgBx(code) - (((1 << SIZE_Bx) - 1) >> 1));
    }

    @Override
    public String toString() {
        return String.format("%s A=%d B=%d C=%d Bx=%d sBx=%d",
                opcode.name(), a, b, c, bx, sbx);
    }
}