package com.github.relua.model;

import java.util.ArrayList;
import java.util.List;

public class Proto {
    private String function; // 函数名
    private int source; // 函数定义的源文件索引
    private int lineDefined; // 函数定义的起始行号
    private int lastLineDefined; // 函数定义的结束行号
    private int numParams; // 固定参数数量
    private int isVararg; // 是否是可变参数
    private int maxStackSize; // 最大栈大小
    private int nups; // upvalues数量

    private List<Instruction> instructions = new ArrayList<>(); // 指令列表
    private List<Constant> constants = new ArrayList<>(); // 常量表
    private List<Proto> protos = new ArrayList<>(); // 嵌套函数
    private List<LocalVar> localVars = new ArrayList<>(); // 局部变量表
    private List<Integer> lineNumbers = new ArrayList<>(); // 行号表
}
