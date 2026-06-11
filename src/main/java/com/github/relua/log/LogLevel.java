package com.github.relua.log;

/**
 * 日志级别枚举类
 */
public enum LogLevel {
    DEBUG(0),
    INFO(1),
    WARNING(2),
    ERROR(3),
    CRITICAL(4);

    private final int level;

    /**
     * 构造函数
     * @param level 级别数值
     */
    LogLevel(int level) {
        this.level = level;
    }

    /**
     * 获取级别数值
     * @return 级别数值
     */
    public int getLevel() {
        return level;
    }

    /**
     * 比较当前级别是否大于等于指定级别
     * @param other 要比较的级别
     * @return 如果当前级别大于等于指定级别则返回true，否则返回false
     */
    public boolean isGreaterOrEqual(LogLevel other) {
        return this.level >= other.level;
    }

    /**
     * 根据字符串获取日志级别
     * @param levelStr 级别字符串
     * @return 对应的日志级别，如果没有匹配则返回INFO
     */
    public static LogLevel fromString(String levelStr) {
        if (levelStr == null) {
            return INFO;
        }
        
        switch (levelStr.toUpperCase()) {
            case "DEBUG":
                return DEBUG;
            case "INFO":
                return INFO;
            case "WARNING":
                return WARNING;
            case "ERROR":
                return ERROR;
            case "CRITICAL":
                return CRITICAL;
            default:
                return INFO;
        }
    }
}