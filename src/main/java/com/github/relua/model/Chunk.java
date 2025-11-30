package com.github.relua.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Lua代码块模型
 */
public class Chunk {
    private String function;       // 函数名
    private int source;            // 函数定义的源文件索引
    private int lineDefined;       // 函数定义的起始行号
    private int lastLineDefined;   // 函数定义的结束行号
    private int numParams;         // 固定参数数量
    private int isVararg;          // 是否是可变参数
    private int maxStackSize;      // 最大栈大小
    private int nup;               // upvalues数量
    
    private List<Instruction> instructions;  // 指令列表
    private List<Constant> constants;        // 常量表
    private List<Chunk> subChunks;           // 子代码块（嵌套函数）
    private List<LocalVar> localVars;        // 局部变量表
    private List<Integer> lineNumbers;       // 行号表

    /**
     * 局部变量模型
     */
    public static class LocalVar {
        private String name;       // 变量名
        private int startPC;       // 变量生效的起始指令位置
        private int endPC;         // 变量生效的结束指令位置

        /**
         * 构造函数
         * @param name 变量名
         * @param startPC 起始指令位置
         * @param endPC 结束指令位置
         */
        public LocalVar(String name, int startPC, int endPC) {
            this.name = name;
            this.startPC = startPC;
            this.endPC = endPC;
        }

        /**
         * 获取变量名
         * @return 变量名
         */
        public String getName() {
            return name;
        }

        /**
         * 获取起始指令位置
         * @return 起始指令位置
         */
        public int getStartPC() {
            return startPC;
        }

        /**
         * 获取结束指令位置
         * @return 结束指令位置
         */
        public int getEndPC() {
            return endPC;
        }
    }

    /**
     * 构造函数
     */
    public Chunk() {
        this.instructions = new ArrayList<>();
        this.constants = new ArrayList<>();
        this.subChunks = new ArrayList<>();
        this.localVars = new ArrayList<>();
        this.lineNumbers = new ArrayList<>();
    }

    /**
     * 获取函数名
     * @return 函数名
     */
    public String getFunction() {
        return function;
    }

    /**
     * 设置函数名
     * @param function 函数名
     */
    public void setFunction(String function) {
        this.function = function;
    }


     /**
     * 获取函数定义的源文件索引
     * @return 源文件索引
     */
    public int getSource() {
        return source;
    }

    /**
     * 设置函数定义的源文件索引
     * @param source 源文件索引
     */
    public void setSource(int source) {
        this.source = source;
    }

    /**
     * 获取函数定义的起始行号
     * @return 起始行号
     */
    public int getLineDefined() {
        return lineDefined;
    }

    /**
     * 设置函数定义的起始行号
     * @param lineDefined 起始行号
     */
    public void setLineDefined(int lineDefined) {
        this.lineDefined = lineDefined;
    }

    /**
     * 获取函数定义的结束行号
     * @return 结束行号
     */
    public int getLastLineDefined() {
        return lastLineDefined;
    }

    /**
     * 设置函数定义的结束行号
     * @param lastLineDefined 结束行号
     */
    public void setLastLineDefined(int lastLineDefined) {
        this.lastLineDefined = lastLineDefined;
    }

    /**
     * 获取固定参数数量
     * @return 固定参数数量
     */
    public int getNumParams() {
        return numParams;
    }

    /**
     * 设置固定参数数量
     * @param numParams 固定参数数量
     */
    public void setNumParams(int numParams) {
        this.numParams = numParams;
    }

    /**
     * 获取是否是可变参数
     * @return 是否是可变参数
     */
    public int getIsVararg() {
        return isVararg;
    }

    /**
     * 设置是否是可变参数
     * @param isVararg 是否是可变参数
     */
    public void setIsVararg(int isVararg) {
        this.isVararg = isVararg;
    }

    /**
     * 获取最大栈大小
     * @return 最大栈大小
     */
    public int getMaxStackSize() {
        return maxStackSize;
    }

    /**
     * 设置最大栈大小
     * @param maxStackSize 最大栈大小
     */
    public void setMaxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
    }

    /**
     * 获取upvalues数量
     * @return upvalues数量
     */
    public int getNup() {
        return nup;
    }

    /**
     * 设置upvalues数量
     * @param nup upvalues数量
     */
    public void setNup(int nup) {
        this.nup = nup;
    }

    /**
     * 获取指令列表
     * @return 指令列表
     */
    public List<Instruction> getInstructions() {
        return instructions;
    }

    /**
     * 添加指令
     * @param instruction 指令
     */
    public void addInstruction(Instruction instruction) {
        this.instructions.add(instruction);
    }

    /**
     * 获取常量表
     * @return 常量表
     */
    public List<Constant> getConstants() {
        return constants;
    }

    /**
     * 添加常量
     * @param constant 常量
     */
    public void addConstant(Constant constant) {
        this.constants.add(constant);
    }

    /**
     * 获取子代码块列表
     * @return 子代码块列表
     */
    public List<Chunk> getSubChunks() {
        return subChunks;
    }

    /**
     * 添加子代码块
     * @param subChunk 子代码块
     */
    public void addSubChunk(Chunk subChunk) {
        this.subChunks.add(subChunk);
    }

    /**
     * 获取局部变量表
     * @return 局部变量表
     */
    public List<LocalVar> getLocalVars() {
        return localVars;
    }

    /**
     * 添加局部变量
     * @param localVar 局部变量
     */
    public void addLocalVar(LocalVar localVar) {
        this.localVars.add(localVar);
    }

    /**
     * 获取行号表
     * @return 行号表
     */
    public List<Integer> getLineNumbers() {
        return lineNumbers;
    }

    /**
     * 添加行号
     * @param lineNumber 行号
     */
    public void addLineNumber(int lineNumber) {
        this.lineNumbers.add(lineNumber);
    }

    /**
     * 获取指定索引的常量
     * @param index 常量索引
     * @return 常量
     */
    public Constant getConstant(int index) {
        if (index >= 0 && index < constants.size()) {
            return constants.get(index);
        }
        return new Constant(ValueType.UNKNOWN, "RX" + index);
    }

    /**
     * 获取指定索引的子代码块
     * @param index 子代码块索引
     * @return 子代码块
     */
    public Chunk getSubChunk(int index) {
        if (index >= 0 && index < subChunks.size()) {
            return subChunks.get(index);
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("Chunk[lineDefined=%d, lastLineDefined=%d, numParams=%d, isVararg=%d, maxStackSize=%d, instructions=%d, constants=%d, subChunks=%d, localVars=%d]",
                lineDefined, lastLineDefined, numParams, isVararg, maxStackSize,
                instructions.size(), constants.size(), subChunks.size(), localVars.size());
    }
}