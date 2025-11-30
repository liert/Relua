package com.github.relua.gui.views;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.logging.*;

/**
 * 日志视图类，用于显示日志输出
 */
public class LogView {
    private TextArea logTextArea;
    private ComboBox<String> logLevelComboBox;
    private Button clearLogButton;
    private LogHandler logHandler;
    private Level currentLevel = Level.ALL;
    
    /**
     * 构造函数
     */
    public LogView() {
        // 初始化日志文本区域
        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
        logTextArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");
        
        // 初始化清空日志按钮
        clearLogButton = new Button("清空日志");
        clearLogButton.setOnAction(e -> clearLog());
        
        // 初始化日志处理器
        initializeLogHandler();
        
        // 设置默认日志级别为DEBUG
        setLogLevel(Level.FINE);
    }
    
    /**
     * 设置日志级别组合框
     * @param logLevelComboBox 日志级别组合框
     */
    public void setLogLevelComboBox(ComboBox<String> logLevelComboBox) {
        this.logLevelComboBox = logLevelComboBox;
        logLevelComboBox.getItems().addAll("ALL", "DEBUG", "INFO", "WARNING", "ERROR", "SEVERE");
        logLevelComboBox.setValue("ALL");
        logLevelComboBox.setOnAction(e -> handleLogLevelChange());
    }
    
    /**
     * 设置清空日志按钮
     * @param clearLogButton 清空日志按钮
     */
    public void setClearLogButton(Button clearLogButton) {
        this.clearLogButton = clearLogButton;
        clearLogButton.setOnAction(e -> clearLog());
    }
    
    /**
     * 初始化日志处理器
     */
    private void initializeLogHandler() {
        logHandler = new LogHandler();
        logHandler.setFormatter(new SimpleFormatter() {
            @Override
            public synchronized String format(LogRecord record) {
                String level = record.getLevel().getName();
                String message = record.getMessage();
                String loggerName = record.getLoggerName();
                String timestamp = new java.util.Date(record.getMillis()).toString();
                
                return String.format("[%s] %s %s - %s%n", timestamp, level, loggerName, message);
            }
        });
        
        // 获取根日志记录器并添加处理器
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(logHandler);
        rootLogger.setLevel(Level.ALL);
    }
    
    /**
     * 处理日志级别变化
     */
    private void handleLogLevelChange() {
        String selectedLevel = logLevelComboBox.getValue();
        Level level;
        
        // 将显示级别转换为Java Logging级别
        switch (selectedLevel) {
            case "DEBUG":
                level = Level.FINE;
                break;
            case "INFO":
                level = Level.INFO;
                break;
            case "WARNING":
                level = Level.WARNING;
                break;
            case "ERROR":
                level = Level.SEVERE;
                break;
            case "SEVERE":
                level = Level.SEVERE;
                break;
            default:
                level = Level.ALL;
        }
        
        setLogLevel(level);
    }
    
    /**
     * 设置日志级别
     * @param level 日志级别
     */
    public void setLogLevel(Level level) {
        currentLevel = level;
        logHandler.setLevel(level);
        
        // 只有当logLevelComboBox不为null时才设置值
        if (logLevelComboBox != null) {
            // 将Java Logging级别转换为显示级别
            String displayLevel;
            switch (level.getName()) {
                case "FINE":
                    displayLevel = "DEBUG";
                    break;
                case "WARNING":
                    displayLevel = "WARNING";
                    break;
                case "SEVERE":
                    displayLevel = "SEVERE";
                    break;
                default:
                    displayLevel = level.getName();
            }
            logLevelComboBox.setValue(displayLevel);
        }
    }
    
    /**
     * 清空日志
     */
    public void clearLog() {
        logTextArea.clear();
    }
    
    /**
     * 获取TextArea控件
     * @return TextArea控件
     */
    public TextArea getLogTextArea() {
        return logTextArea;
    }
    
    /**
     * 获取日志级别组合框
     * @return 日志级别组合框
     */
    public ComboBox<String> getLogLevelComboBox() {
        return logLevelComboBox;
    }
    
    /**
     * 获取清空日志按钮
     * @return 清空日志按钮
     */
    public Button getClearLogButton() {
        return clearLogButton;
    }
    
    /**
     * 自定义日志处理器，用于将日志输出到TextArea
     */
    private class LogHandler extends Handler {
        @Override
        public void publish(LogRecord record) {
            if (!isLoggable(record)) {
                return;
            }
            
            final String logMessage = getFormatter().format(record);
            
            // 在JavaFX应用线程中更新UI
            Platform.runLater(() -> {
                logTextArea.appendText(logMessage);
                // 自动滚动到最新日志
                logTextArea.setScrollTop(Double.MAX_VALUE);
            });
        }
        
        @Override
        public void flush() {
            // 无需实现
        }
        
        @Override
        public void close() throws SecurityException {
            // 无需实现
        }
    }
}