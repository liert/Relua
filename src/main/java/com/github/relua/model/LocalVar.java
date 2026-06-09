package com.github.relua.model;

/**
 * 局部变量信息：名称 + 生存期 [startPC, endPC)
 */
public class LocalVar {
    private String name; // 变量名
    private int startPC; // 变量生效的起始指令位置
    private int endPC; // 变量生效的结束指令位置

    /**
     * 构造函数
     * 
     * @param name    变量名
     * @param startPC 起始指令位置
     * @param endPC   结束指令位置
     */
    public LocalVar(String name, int startPC, int endPC) {
        this.name = name;
        this.startPC = startPC;
        this.endPC = endPC;
    }

    /**
     * 获取变量名
     * 
     * @return 变量名
     */
    public String getName() {
        return name;
    }

    /**
     * 获取起始指令位置
     * 
     * @return 起始指令位置
     */
    public int getStartPC() {
        return startPC;
    }

    /**
     * 获取结束指令位置
     * 
     * @return 结束指令位置
     */
    public int getEndPC() {
        return endPC;
    }

    @Override
    public String toString() {
        return String.format("%s[%d:%d]", name, startPC, endPC);
    }
}
