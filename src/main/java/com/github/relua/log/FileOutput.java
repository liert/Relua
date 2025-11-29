package com.github.relua.log;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * 文件输出实现类，负责将日志输出到文件，并支持日志轮转
 */
public class FileOutput implements LogOutput {
    // 日志格式化器
    private final LogFormatter formatter;
    
    // 日志文件路径
    private final Path logFilePath;
    
    // 最大文件大小（字节）
    private final long maxFileSize;
    
    // 最大备份文件数
    private final int maxBackupFiles;
    
    // 文件输出流
    private FileOutputStream fileOutputStream;
    
    // 缓冲输出流
    private BufferedWriter bufferedWriter;
    
    /**
     * 构造函数
     * @param logFilePath 日志文件路径
     * @param logFormat 日志格式
     * @param maxFileSize 最大文件大小（字节）
     * @param maxBackupFiles 最大备份文件数
     * @throws IOException IO异常
     */
    public FileOutput(String logFilePath, String logFormat, long maxFileSize, int maxBackupFiles) throws IOException {
        this.formatter = new LogFormatter(logFormat);
        this.logFilePath = Paths.get(logFilePath);
        this.maxFileSize = maxFileSize;
        this.maxBackupFiles = maxBackupFiles;
        
        // 初始化文件输出流
        initFileOutputStream();
    }
    
    /**
     * 初始化文件输出流
     * @throws IOException IO异常
     */
    private void initFileOutputStream() throws IOException {
        // 创建日志文件目录
        Path parentDir = logFilePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        
        // 创建文件输出流
        fileOutputStream = new FileOutputStream(logFilePath.toFile(), true);
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream, "UTF-8"));
    }
    
    /**
     * 执行日志文件轮转
     * @throws IOException IO异常
     */
    private synchronized void rotateLogs() throws IOException {
        // 关闭当前文件输出流
        close();
        
        // 轮转备份文件
        for (int i = maxBackupFiles - 1; i >= 1; i--) {
            Path srcPath = logFilePath.resolveSibling(logFilePath.getFileName() + "." + i);
            Path destPath = logFilePath.resolveSibling(logFilePath.getFileName() + "." + (i + 1));
            
            if (Files.exists(srcPath)) {
                if (Files.exists(destPath)) {
                    Files.delete(destPath);
                }
                Files.move(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        
        // 将当前日志文件重命名为第一个备份文件
        Path backupPath = logFilePath.resolveSibling(logFilePath.getFileName() + ".1");
        if (Files.exists(backupPath)) {
            Files.delete(backupPath);
        }
        Files.move(logFilePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        
        // 重新初始化文件输出流
        initFileOutputStream();
    }
    
    /**
     * 检查并执行日志轮转
     * @throws IOException IO异常
     */
    private void checkAndRotate() throws IOException {
        if (logFilePath.toFile().length() >= maxFileSize) {
            rotateLogs();
        }
    }
    
    /**
     * 输出日志
     * @param level 日志级别
     * @param fileName 文件名
     * @param lineNumber 行号
     * @param message 日志消息
     */
    @Override
    public synchronized void output(LogLevel level, String fileName, int lineNumber, String message) {
        try {
            // 检查是否需要轮转
            checkAndRotate();
            
            // 格式化并写入日志
            String formatted = formatter.format(level, fileName, lineNumber, message);
            bufferedWriter.write(formatted);
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 关闭输出资源
     */
    @Override
    public synchronized void close() {
        if (bufferedWriter != null) {
            try {
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bufferedWriter = null;
        }
        
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileOutputStream = null;
        }
    }
}