package com.github.relua.log;

/**
 * 日志配置类，用于管理日志的各种配置选项
 */
public class LogConfig {
    // 默认日志级别
    private LogLevel logLevel = LogLevel.INFO;
    
    // 默认日志格式
    private String logFormat = "[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%p] [%F:%L] - %m%n";
    
    // 是否输出到控制台
    private boolean consoleOutput = true;
    
    // 是否输出到文件
    private boolean fileOutput = false;
    
    // 日志文件路径
    private String logFilePath = "logs/app.log";
    
    // 最大文件大小（字节），默认10MB
    private long maxFileSize = 10 * 1024 * 1024;
    
    // 最大备份文件数，默认5个
    private int maxBackupFiles = 5;
    
    /**
     * 获取日志级别
     * @return 日志级别
     */
    public LogLevel getLogLevel() {
        return logLevel;
    }
    
    /**
     * 设置日志级别
     * @param logLevel 日志级别
     */
    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }
    
    /**
     * 获取日志格式
     * @return 日志格式
     */
    public String getLogFormat() {
        return logFormat;
    }
    
    /**
     * 设置日志格式
     * @param logFormat 日志格式
     */
    public void setLogFormat(String logFormat) {
        this.logFormat = logFormat;
    }
    
    /**
     * 是否输出到控制台
     * @return true表示输出到控制台，false表示不输出
     */
    public boolean isConsoleOutput() {
        return consoleOutput;
    }
    
    /**
     * 设置是否输出到控制台
     * @param consoleOutput true表示输出到控制台，false表示不输出
     */
    public void setConsoleOutput(boolean consoleOutput) {
        this.consoleOutput = consoleOutput;
    }
    
    /**
     * 是否输出到文件
     * @return true表示输出到文件，false表示不输出
     */
    public boolean isFileOutput() {
        return fileOutput;
    }
    
    /**
     * 设置是否输出到文件
     * @param fileOutput true表示输出到文件，false表示不输出
     */
    public void setFileOutput(boolean fileOutput) {
        this.fileOutput = fileOutput;
    }
    
    /**
     * 获取日志文件路径
     * @return 日志文件路径
     */
    public String getLogFilePath() {
        return logFilePath;
    }
    
    /**
     * 设置日志文件路径
     * @param logFilePath 日志文件路径
     */
    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
    }
    
    /**
     * 获取最大文件大小
     * @return 最大文件大小（字节）
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    /**
     * 设置最大文件大小
     * @param maxFileSize 最大文件大小（字节）
     */
    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
    
    /**
     * 获取最大备份文件数
     * @return 最大备份文件数
     */
    public int getMaxBackupFiles() {
        return maxBackupFiles;
    }
    
    /**
     * 设置最大备份文件数
     * @param maxBackupFiles 最大备份文件数
     */
    public void setMaxBackupFiles(int maxBackupFiles) {
        this.maxBackupFiles = maxBackupFiles;
    }
}