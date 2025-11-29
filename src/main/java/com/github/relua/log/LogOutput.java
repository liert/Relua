package com.github.relua.log;

/**
 * 日志输出接口，定义日志输出的统一接口
 */
public interface LogOutput {
    /**
     * 输出日志
     * @param level 日志级别
     * @param fileName 文件名
     * @param lineNumber 行号
     * @param message 日志消息
     */
    void output(LogLevel level, String fileName, int lineNumber, String message);
    
    /**
     * 关闭输出资源
     */
    void close();
}