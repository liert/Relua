package com.github.relua.log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 核心日志类，负责管理日志配置、输出目标和日志记录的核心逻辑
 */
public class Logger {
    // 默认日志配置
    private static LogConfig config = new LogConfig();
    
    // 日志输出目标列表
    private static List<LogOutput> outputs = new ArrayList<>();
    
    // 初始化标志
    private static boolean initialized = false;
    
    /**
     * 初始化日志系统
     * @param logConfig 日志配置
     */
    public static synchronized void init(LogConfig logConfig) {
        if (initialized) {
            // 关闭现有输出目标
            closeOutputs();
        }
        
        // 更新配置
        config = logConfig;
        
        // 初始化输出目标
        initOutputs();
        
        initialized = true;
    }
    
    /**
     * 初始化输出目标
     */
    private static void initOutputs() {
        outputs.clear();
        
        // 添加控制台输出
        if (config.isConsoleOutput()) {
            outputs.add(new ConsoleOutput(config.getLogFormat()));
        }
        
        // 添加文件输出
        if (config.isFileOutput()) {
            try {
                outputs.add(new FileOutput(
                        config.getLogFilePath(),
                        config.getLogFormat(),
                        config.getMaxFileSize(),
                        config.getMaxBackupFiles()
                ));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 关闭所有输出目标
     */
    private static void closeOutputs() {
        for (LogOutput output : outputs) {
            output.close();
        }
        outputs.clear();
    }
    
    /**
     * 获取调用者的文件名和行号
     * @return 包含文件名和行号的数组，索引0为文件名，索引1为行号
     */
    private static String[] getCallerInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        
        // 找到调用Logger的方法
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            if (element.getClassName().equals(Logger.class.getName())) {
                // 下一个元素是调用Logger的方法
                if (i + 1 < stackTrace.length) {
                    StackTraceElement caller = stackTrace[i + 1];
                    return new String[] {
                            caller.getFileName(),
                            String.valueOf(caller.getLineNumber())
                    };
                }
            }
        }
        
        return new String[] {"Unknown", "0"};
    }
    
    /**
     * 记录日志
     * @param level 日志级别
     * @param message 日志消息
     */
    private static void log(LogLevel level, String message) {
        // 检查是否初始化
        if (!initialized) {
            init(new LogConfig());
        }
        
        // 检查日志级别
        if (!level.isGreaterOrEqual(config.getLogLevel())) {
            return;
        }
        
        // 获取调用者信息
        String[] callerInfo = getCallerInfo();
        String fileName = callerInfo[0];
        int lineNumber = Integer.parseInt(callerInfo[1]);
        
        // 输出日志到所有目标
        for (LogOutput output : outputs) {
            output.output(level, fileName, lineNumber, message);
        }
    }
    
    /**
     * 记录DEBUG级别日志
     * @param message 日志消息
     */
    public static void debug(String message) {
        log(LogLevel.DEBUG, message);
    }
    
    /**
     * 记录INFO级别日志
     * @param message 日志消息
     */
    public static void info(String message) {
        log(LogLevel.INFO, message);
    }
    
    /**
     * 记录WARNING级别日志
     * @param message 日志消息
     */
    public static void warning(String message) {
        log(LogLevel.WARNING, message);
    }
    
    /**
     * 记录ERROR级别日志
     * @param message 日志消息
     */
    public static void error(String message) {
        log(LogLevel.ERROR, message);
    }
    
    /**
     * 记录CRITICAL级别日志
     * @param message 日志消息
     */
    public static void critical(String message) {
        log(LogLevel.CRITICAL, message);
    }
    
    /**
     * 记录DEBUG级别日志，带异常信息
     * @param message 日志消息
     * @param throwable 异常
     */
    public static void debug(String message, Throwable throwable) {
        log(LogLevel.DEBUG, message + "\n" + getStackTrace(throwable));
    }
    
    /**
     * 记录INFO级别日志，带异常信息
     * @param message 日志消息
     * @param throwable 异常
     */
    public static void info(String message, Throwable throwable) {
        log(LogLevel.INFO, message + "\n" + getStackTrace(throwable));
    }
    
    /**
     * 记录WARNING级别日志，带异常信息
     * @param message 日志消息
     * @param throwable 异常
     */
    public static void warning(String message, Throwable throwable) {
        log(LogLevel.WARNING, message + "\n" + getStackTrace(throwable));
    }
    
    /**
     * 记录ERROR级别日志，带异常信息
     * @param message 日志消息
     * @param throwable 异常
     */
    public static void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message + "\n" + getStackTrace(throwable));
    }
    
    /**
     * 记录CRITICAL级别日志，带异常信息
     * @param message 日志消息
     * @param throwable 异常
     */
    public static void critical(String message, Throwable throwable) {
        log(LogLevel.CRITICAL, message + "\n" + getStackTrace(throwable));
    }
    
    /**
     * 获取异常堆栈信息
     * @param throwable 异常
     * @return 堆栈信息字符串
     */
    private static String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("    at ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * 获取当前日志配置
     * @return 日志配置
     */
    public static LogConfig getConfig() {
        return config;
    }
    
    /**
     * 设置日志级别
     * @param level 日志级别
     */
    public static void setLogLevel(LogLevel level) {
        config.setLogLevel(level);
    }
    
    /**
     * 关闭日志系统，释放资源
     */
    public static synchronized void close() {
        if (initialized) {
            closeOutputs();
            initialized = false;
        }
    }
}