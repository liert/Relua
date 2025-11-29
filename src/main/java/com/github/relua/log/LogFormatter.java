package com.github.relua.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志格式化器，用于将日志事件格式化为指定的字符串格式
 */
public class LogFormatter {
    // 日期时间占位符正则表达式
    private static final Pattern DATE_PATTERN = Pattern.compile("%d\\{([^}]+)\\}");
    
    // 日志格式
    private final String logFormat;
    
    // 日期格式化器
    private SimpleDateFormat dateFormat;
    
    // 默认日期格式
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    
    /**
     * 构造函数
     * @param logFormat 日志格式
     */
    public LogFormatter(String logFormat) {
        this.logFormat = logFormat;
        initDateFormat();
    }
    
    /**
     * 初始化日期格式化器
     */
    private void initDateFormat() {
        Matcher matcher = DATE_PATTERN.matcher(logFormat);
        if (matcher.find()) {
            String datePattern = matcher.group(1);
            dateFormat = new SimpleDateFormat(datePattern);
        } else {
            dateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
        }
    }
    
    /**
     * 格式化日志信息
     * @param level 日志级别
     * @param fileName 文件名
     * @param lineNumber 行号
     * @param message 日志消息
     * @return 格式化后的日志字符串
     */
    public String format(LogLevel level, String fileName, int lineNumber, String message) {
        String formatted = logFormat;
        
        // 替换日期时间
        formatted = DATE_PATTERN.matcher(formatted).replaceAll(match -> {
            return dateFormat.format(new Date());
        });
        
        // 替换日志级别
        formatted = formatted.replace("%p", level.name());
        
        // 替换文件名
        formatted = formatted.replace("%F", fileName);
        
        // 替换行号
        formatted = formatted.replace("%L", String.valueOf(lineNumber));
        
        // 替换日志消息
        formatted = formatted.replace("%m", message);
        
        // 替换换行符
        formatted = formatted.replace("%n", System.lineSeparator());
        
        return formatted;
    }
}