package com.github.relua.model;

/**
 * 表示生成的Lua代码行
 */
public class CodeLine {
    // 代码类型枚举
    public enum CodeType {
        NORMAL,      // 普通代码行
        IF,          // if语句
        ELSE,        // else语句
        ELSE_IF,     // elseif语句
        END,         // end语句
        LOOP,        // 循环语句
        COMMENT,     // 注释行
        EMPTY        // 空行
    }
    
    private String content;      // 代码内容
    private int indentLevel;     // 缩进级别
    private CodeType type;       // 代码类型
    
    /**
     * 构造函数
     * @param content 代码内容
     * @param indentLevel 缩进级别
     * @param type 代码类型
     */
    public CodeLine(String content, int indentLevel, CodeType type) {
        this.content = content;
        this.indentLevel = indentLevel;
        this.type = type;
    }
    
    /**
     * 构造普通代码行
     * @param content 代码内容
     * @param indentLevel 缩进级别
     */
    public CodeLine(String content, int indentLevel) {
        this(content, indentLevel, CodeType.NORMAL);
    }
    
    /**
     * 获取代码内容
     * @return 代码内容
     */
    public String getContent() {
        return content;
    }
    
    /**
     * 设置代码内容
     * @param content 代码内容
     */
    public void setContent(String content) {
        this.content = content;
    }
    
    /**
     * 获取缩进级别
     * @return 缩进级别
     */
    public int getIndentLevel() {
        return indentLevel;
    }
    
    /**
     * 设置缩进级别
     * @param indentLevel 缩进级别
     */
    public void setIndentLevel(int indentLevel) {
        this.indentLevel = indentLevel;
    }
    
    /**
     * 获取代码类型
     * @return 代码类型
     */
    public CodeType getType() {
        return type;
    }
    
    /**
     * 设置代码类型
     * @param type 代码类型
     */
    public void setType(CodeType type) {
        this.type = type;
    }
    
    /**
     * 生成带缩进的完整代码行
     * @param indentString 缩进字符串（如"    "）
     * @return 带缩进的完整代码行
     */
    public String toIndentedString(String indentString) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            sb.append(indentString);
        }
        sb.append(content);
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("[%d] %s", indentLevel, content);
    }
}