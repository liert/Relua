package com.github.relua.decompiler;

import com.github.relua.ast.AstNode;
import com.github.relua.ast.Block;
import com.github.relua.ast.Statement;
import com.github.relua.ast.AstPrinter;
import com.github.relua.model.Chunk;
import com.github.relua.model.CodeLine;
import com.github.relua.model.Register;
import com.github.relua.model.CodeLine.CodeType;
import com.github.relua.model.UpValue;

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

    // // 控制流类型枚举
    public enum ControlFlowType {
        IF, // if语句
        ELSE, // else语句
        LOOP // 循环语句
    }

    // 控制流元素类
    public static class ControlFlowElement {
        private ControlFlowType type; // 控制流类型
        private int startLine; // 起始行号

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

    private Chunk chunk; // 当前正在处理的代码块
    private Block astBlock; // 存储生成的AST节点
    private List<CodeLine> codeLines; // 生成的代码行列表（保留用于兼容）
    private int currentIndent; // 当前缩进级别
    private Stack<ControlFlowElement> controlFlowStack; // 控制流栈，用于跟踪if-else嵌套结构
    private Set<Integer> labelPCs = new HashSet<>(); // 标签指令的PC值集合
    private Set<Integer> tforRegionPCs = new HashSet<>(); // TFORLOOP 相关区域的PC值（TFORLOOP、backward JMP、target label）
    private int thenIndex = 0;
    private Map<Integer, BasicBlock> thenBlocks = new HashMap<>(); // 基本块映射
    private Register register = new Register(); // 初始寄存器状态
    private List<UpValue> upvalues = new ArrayList<>(); // 上值存储，key为上值索引，value为上值对象
    
    // 用于跟踪pending的SELF指令，用于方法调用绑定
    private Map<Integer, PendingSelf> pendingSelfCalls = new HashMap<>();
    
    /**
     * 存储pending的SELF指令信息
     */
    public static class PendingSelf {
        private final int baseRegister; // 基础寄存器B
        private final String methodName; // 方法名RK(C)
        
        public PendingSelf(int baseRegister, String methodName) {
            this.baseRegister = baseRegister;
            this.methodName = methodName;
        }
        
        public int getBaseRegister() {
            return baseRegister;
        }
        
        public String getMethodName() {
            return methodName;
        }
    }

    public CodeGeneratorContext() {
        this.codeLines = new ArrayList<>();
        this.astBlock = new Block(null);
        this.currentIndent = 0;
        this.controlFlowStack = new Stack<>();
    }

    /**
     * 构造函数
     */
    public CodeGeneratorContext(Chunk chunk) {
        this.chunk = chunk;
        this.codeLines = new ArrayList<>();
        this.astBlock = new Block(null);
        this.currentIndent = 0;
        this.controlFlowStack = new Stack<>();
    }

    /**
     * 构造函数
     */
    public CodeGeneratorContext(Chunk chunk, Register register) {
        this.chunk = chunk;
        this.register = register;
        this.codeLines = new ArrayList<>();
        this.astBlock = new Block(null);
        this.currentIndent = 0;
        this.controlFlowStack = new Stack<>();
    }

    /**
     * 设置当前正在处理的代码块
     * 
     * @param chunk 代码块
     */
    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    /**
     * 获取当前正在处理的代码块
     * 
     * @return 代码块
     */
    public Chunk getChunk() {
        return chunk;
    }

    /**
     * 获取当前寄存器状态
     * 
     * @return 寄存器状态
     */
    public Register getRegister() {
        return register;
    }

    /**
     * 添加普通代码行
     * 
     * @param content 代码内容
     */
    public void addCodeLine(String content) {
        codeLines.add(new CodeLine(content, currentIndent, CodeType.NORMAL));
    }

    /**
     * 添加指定类型的代码行
     * 
     * @param content 代码内容
     * @param type    代码类型
     */
    public void addCodeLine(String content, CodeType type) {
        codeLines.add(new CodeLine(content, currentIndent, type));
    }

    /**
     * 添加if语句
     * 
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
     * 
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
     * 
     * @return 当前缩进级别
     */
    public int getCurrentIndent() {
        return currentIndent;
    }

    /**
     * 设置当前缩进级别
     * 
     * @param currentIndent 当前缩进级别
     */
    public void setCurrentIndent(int currentIndent) {
        this.currentIndent = Math.max(0, currentIndent);
    }

    /**
     * 获取代码行列表
     * 
     * @return 代码行列表
     */
    public List<CodeLine> getCodeLines() {
        return codeLines;
    }

    /**
     * 获取控制流栈
     * 
     * @return 控制流栈
     */
    public Stack<ControlFlowElement> getControlFlowStack() {
        return controlFlowStack;
    }

    /**
     * 添加标签指令的PC值
     * 
     * @param pc 标签指令的PC值
     */
    public void addLabelPC(int pc) {
        labelPCs.add(pc);
    }

    /**
     * 检查PC值是否为标签指令
     * 
     * @param pc PC值
     * @return 如果是标签指令则返回true，否则返回false
     */
    public boolean isLabelPC(int pc) {
        return labelPCs.contains(pc);
    }

    /**
     * 添加 TFORLOOP 区域相关的 PC 值
     * 包括 TFORLOOP 本身、其后的 backward JMP、以及跳转目标 label
     * 
     * @param pc 需要保护的 PC 值
     */
    public void addTforRegionPC(int pc) {
        tforRegionPCs.add(pc);
    }

    /**
     * 检查 PC 是否属于 TFORLOOP 区域（需要防止被 if-body 吞没）
     * 
     * @param pc PC值
     * @return 如果是 TFORLOOP 区域则返回 true
     */
    public boolean isTforRegionPC(int pc) {
        return tforRegionPCs.contains(pc);
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
     * 
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
     * 添加上值
     * 
     * @param index 上值索引
     * @param upvalue 上值对象
     */
    public void addUpvalue(int index, UpValue upvalue) {
        while (this.upvalues.size() <= index) {
            this.upvalues.add(null);
        }
        this.upvalues.set(index, upvalue);
    }

    // /**
    //  * 添加上值
    //  * 
    //  * @param index 上值索引
    //  * @param name 上值名称
    //  * @param value 上值的值
    //  * @param type 上值的类型
    //  * @param fromType 上值的来源类型
    //  */
    // public void addUpvalue(String index, String name, Object value, com.github.relua.model.ValueType type, com.github.relua.model.FromType fromType) {
    //     Upvalue upvalue = new Upvalue(index, name, value, type, fromType);
    //     this.upvalues.put(index, upvalue);
    // }

    /**
     * 获取上值
     * 
     * @param index 上值索引
     * @return 上值对象，如果不存在则返回null
     */
    public UpValue getUpvalue(int index) {
        if (index >= 0 && index < this.upvalues.size()) {
            return this.upvalues.get(index);
        }
        return null;
    }

    public List<UpValue> getUpvalues() {
        return this.upvalues;
    }

    /**
     * 移除上值
     * 
     * @param index 上值索引
     * @return 被移除的上值对象，如果不存在则返回null
     */
    public UpValue removeUpvalue(int index) {
        if (index >= 0 && index < this.upvalues.size()) {
            return this.upvalues.remove(index);
        }
        return null;
    }

    /**
     * 检查上值是否存在
     * 
     * @param index 上值索引
     * @return 如果存在则返回true，否则返回false
     */
    public boolean hasUpvalue(int index) {
        return index >= 0 && index < this.upvalues.size() && this.upvalues.get(index) != null;
    }

    /**
     * 获取所有上值
     * 
     * @return 上值映射，key为上值索引，value为上值对象
     */
    // public Li<String, Upvalue> getAllUpvalues() {
    //     return this.upvalues;
    // }

    /**
     * 添加pending的SELF指令
     * 
     * @param registerA 寄存器A
     * @param baseRegister 基础寄存器B
     * @param methodName 方法名RK(C)
     */
    public void addPendingSelf(int registerA, int baseRegister, String methodName) {
        this.pendingSelfCalls.put(registerA, new PendingSelf(baseRegister, methodName));
    }
    
    /**
     * 获取pending的SELF指令
     * 
     * @param registerA 寄存器A
     * @return PendingSelf对象，如果不存在则返回null
     */
    public PendingSelf getPendingSelf(int registerA) {
        return this.pendingSelfCalls.get(registerA);
    }
    
    /**
     * 移除pending的SELF指令
     * 
     * @param registerA 寄存器A
     * @return 被移除的PendingSelf对象，如果不存在则返回null
     */
    public PendingSelf removePendingSelf(int registerA) {
        return this.pendingSelfCalls.remove(registerA);
    }
    
    /**
     * 清除所有pending的SELF指令
     */
    public void clearPendingSelfCalls() {
        this.pendingSelfCalls.clear();
    }

    /**
     * 添加AST语句到代码块
     * 
     * @param statement AST语句节点
     */
    public void addStatement(Statement statement) {
        this.astBlock.statements.add(statement);
    }
    
    /**
     * 获取AST代码块
     * 
     * @return AST代码块
     */
    public Block getAstBlock() {
        return this.astBlock;
    }
    
    /**
     * 设置AST代码块
     * 
     * @param astBlock AST代码块
     */
    public void setAstBlock(Block astBlock) {
        this.astBlock = astBlock;
    }
    
    /**
     * 生成最终的Lua代码字符串
     * 
     * @return 生成的Lua代码
     */
    public String generateCode() {
        // 使用AST打印机生成代码
        AstPrinter printer = new AstPrinter();
        return astBlock.accept(printer);
    }

    /**
     * 关闭所有未结束的控制流结构
     */
    public void closeAllControlFlow() {
        while (!controlFlowStack.isEmpty()) {
            addEndStatement();
        }
    }

    private Set<String> declaredVariables = new HashSet<>();

    public Set<String> getDeclaredVariables() {
        return declaredVariables;
    }

    public void setDeclaredVariables(Set<String> declaredVariables) {
        this.declaredVariables = declaredVariables;
    }
}