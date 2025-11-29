package com.github.relua.decompiler;

import com.github.relua.model.CodeLine;
import com.github.relua.model.CodeLine.CodeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * 代码生成上下文，用于管理代码生成过程
 */
public class CodeGeneratorContext {
    private static final String INDENT_STRING = "    "; // 4个空格缩进
    
    // 控制流类型枚举
    public enum ControlFlowType {
        IF,          // if语句
        ELSE,        // else语句
        LOOP         // 循环语句
    }
    
    // 控制流元素类
    public static class ControlFlowElement {
        private ControlFlowType type;   // 控制流类型
        private int startLine;          // 起始行号
        
        public ControlFlowElement(ControlFlowType type, int startLine) {
            this.type = type;
            this.startLine = startLine;
        }
        
        public ControlFlowType getType() {
            return type;
        }
        
        public int getStartLine() {
            return startLine;
        }
    }
    
    private List<CodeLine> codeLines;           // 生成的代码行列表
    private int currentIndent;                  // 当前缩进级别
    private Stack<ControlFlowElement> controlFlowStack; // 控制流栈，用于跟踪if-else嵌套结构
    private Set<Integer> labelPCs = new HashSet<>(); // 标签指令的PC值集合
    private int thenIndex = 0;
    private Map<Integer, BasicBlock> thenBlocks = new HashMap<>(); // 基本块映射
    
    /**
     * 构造函数
     */
    public CodeGeneratorContext() {
        this.codeLines = new ArrayList<>();
        this.currentIndent = 0;
        this.controlFlowStack = new Stack<>();
    }

    /**
     * 添加普通代码行
     * @param content 代码内容
     */
    public void addCodeLine(String content) {
        codeLines.add(new CodeLine(content, currentIndent, CodeType.NORMAL));
    }
    
    /**
     * 添加指定类型的代码行
     * @param content 代码内容
     * @param type 代码类型
     */
    public void addCodeLine(String content, CodeType type) {
        codeLines.add(new CodeLine(content, currentIndent, type));
    }
    
    /**
     * 添加if语句
     * @param condition 条件表达式
     */
    public void addIfStatement(String condition) {
        String content = String.format("if %s then", condition);
        codeLines.add(new CodeLine(content, currentIndent, CodeType.IF));
        controlFlowStack.push(new ControlFlowElement(ControlFlowType.IF, codeLines.size() - 1));
        currentIndent++;
    }
    
    /**
     * 添加else语句
     */
    public void addElseStatement() {
        currentIndent--;
        codeLines.add(new CodeLine("else", currentIndent, CodeType.ELSE));
        controlFlowStack.push(new ControlFlowElement(ControlFlowType.ELSE, codeLines.size() - 1));
        currentIndent++;
    }
    
    /**
     * 添加end语句
     */
    public void addEndStatement() {
        currentIndent--;
        codeLines.add(new CodeLine("end", currentIndent, CodeType.END));
        if (!controlFlowStack.isEmpty()) {
            controlFlowStack.pop();
        }
    }
    
    /**
     * 添加注释行
     * @param comment 注释内容
     */
    public void addComment(String comment) {
        codeLines.add(new CodeLine("-- " + comment, currentIndent, CodeType.COMMENT));
    }
    
    /**
     * 添加空行
     */
    public void addEmptyLine() {
        codeLines.add(new CodeLine("", 0, CodeType.EMPTY));
    }
    
    /**
     * 增加缩进级别
     */
    public void increaseIndent() {
        currentIndent++;
    }
    
    /**
     * 减少缩进级别
     */
    public void decreaseIndent() {
        if (currentIndent > 0) {
            currentIndent--;
        }
    }
    
    /**
     * 获取当前缩进级别
     * @return 当前缩进级别
     */
    public int getCurrentIndent() {
        return currentIndent;
    }
    
    /**
     * 设置当前缩进级别
     * @param currentIndent 当前缩进级别
     */
    public void setCurrentIndent(int currentIndent) {
        this.currentIndent = Math.max(0, currentIndent);
    }
    
    /**
     * 获取代码行列表
     * @return 代码行列表
     */
    public List<CodeLine> getCodeLines() {
        return codeLines;
    }
    
    /**
     * 获取控制流栈
     * @return 控制流栈
     */
    public Stack<ControlFlowElement> getControlFlowStack() {
        return controlFlowStack;
    }

    /**
     * 添加标签指令的PC值
     * @param pc 标签指令的PC值
     */
    public void addLabelPC(int pc) {
        labelPCs.add(pc);
    }
    
    /**
     * 检查PC值是否为标签指令
     * @param pc PC值
     * @return 如果是标签指令则返回true，否则返回false
     */
    public boolean isLabelPC(int pc) {
        return labelPCs.contains(pc);
    }

    /**
     * 添加then块
     * 
     * @param block then块
     */
    public void addThenBlock(BasicBlock block) {
        thenBlocks.put(thenIndex++, block);
    }
    
    /**
     * 获取then块
     * 
     * @param index then块索引
     * @return then块
     */
    public BasicBlock getThenBlock(int index) {
        return thenBlocks.get(index);
    }

    /**
     * 获取最后一个then块
     * @return 最后一个then块
     */
    public BasicBlock getLastThenBlock() {
        // 确保thenIndex大于0，避免空指针异常
        if (thenIndex <= 0) {
            return null;
        }
        return getThenBlock(thenIndex - 1);
    }
    
    /**
     * 生成最终的Lua代码字符串
     * @return 生成的Lua代码
     */
    public String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (CodeLine line : codeLines) {
            sb.append(line.toIndentedString(INDENT_STRING)).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * 关闭所有未结束的控制流结构
     */
    public void closeAllControlFlow() {
        while (!controlFlowStack.isEmpty()) {
            addEndStatement();
        }
    }
}