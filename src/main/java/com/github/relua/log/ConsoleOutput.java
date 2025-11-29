package com.github.relua.log;

import java.io.PrintStream;

/**
 * 控制台输出实现类，负责将日志输出到控制台
 */
public class ConsoleOutput implements LogOutput {
    // 日志格式化器
    private final LogFormatter formatter;
    
    // 输出流
    private final PrintStream out;
    
    /**
     * 构造函数
     * @param logFormat 日志格式
     */
    public ConsoleOutput(String logFormat) {
        this.formatter = new LogFormatter(logFormat);
        this.out = System.out;
    }
    
    /**
     * 输出日志
     * @param level 日志级别
     * @param fileName 文件名
     * @param lineNumber 行号
     * @param message 日志消息
     */
    @Override
    public void output(LogLevel level, String fileName, int lineNumber, String message) {
        String formatted = formatter.format(level, fileName, lineNumber, message);
        
        synchronized (this) {
            // 根据日志级别选择输出流
            if (level == LogLevel.ERROR || level == LogLevel.CRITICAL) {
                System.err.print(formatted);
            } else {
                out.print(formatted);
            }
        }
    }
    
    /**
     * 关闭输出资源
     */
    @Override
    public void close() {
        // 控制台输出不需要关闭
    }
}