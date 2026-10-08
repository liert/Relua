package com.github.relua.gui.views;

import com.github.relua.log.LogLevel;
import com.github.relua.log.LogOutput;
import com.github.relua.log.Logger;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * 日志视图类，统一展示 GUI 及反编译核心引擎的全部运行时日志输出
 */
public class LogView {
    private final TextArea logTextArea;
    private ComboBox<String> logLevelComboBox;
    private Button clearLogButton;
    private LogHandler julLogHandler;
    private Level currentJulLevel = Level.ALL;
    private LogLevel currentCoreLevel = LogLevel.DEBUG;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 核心反编译器自定义 Logger 管道输出适配器
     */
    private final LogOutput coreLogOutput = new LogOutput() {
        @Override
        public void output(LogLevel level, String fileName, int lineNumber, String message) {
            // 根据当前选择的日志级别进行过滤
            if (!level.isGreaterOrEqual(currentCoreLevel)) {
                return;
            }

            String timestamp;
            synchronized (dateFormat) {
                timestamp = dateFormat.format(new Date());
            }
            String formatted = String.format("[%s] [%s] [%s:%d] - %s%n",
                    timestamp, level.name(), fileName, lineNumber, message);

            // 在 JavaFX UI 线程中追加显示
            Platform.runLater(() -> {
                logTextArea.appendText(formatted);
                logTextArea.setScrollTop(Double.MAX_VALUE);
            });
        }

        @Override
        public void close() {
        }
    };

    /**
     * 构造函数
     */
    public LogView() {
        // 初始化日志文本区域
        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
        logTextArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");
        logTextArea.setMinHeight(50);

        // 初始化清空日志按钮
        clearLogButton = new Button("清空日志");
        clearLogButton.setOnAction(e -> clearLog());

        // 初始化 JDK JUL 日志处理器
        initializeJulHandler();

        // 注册到核心 Logger 日志管道，彻底打通日志断层
        Logger.addOutput(coreLogOutput);

        // 设置默认日志级别为 DEBUG
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
     * 初始化 JUL 日志处理器
     */
    private void initializeJulHandler() {
        julLogHandler = new LogHandler();
        julLogHandler.setFormatter(new SimpleFormatter() {
            @Override
            public synchronized String format(LogRecord record) {
                String level = record.getLevel().getName();
                String message = record.getMessage();
                String loggerName = record.getLoggerName();
                String timestamp;
                synchronized (dateFormat) {
                    timestamp = dateFormat.format(new Date(record.getMillis()));
                }

                return String.format("[%s] [%s] [%s] - %s%n", timestamp, level, loggerName, message);
            }
        });

        // 获取根日志记录器并添加处理器
        java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
        rootLogger.addHandler(julLogHandler);
        rootLogger.setLevel(Level.ALL);
    }

    /**
     * 处理日志级别变化，同步更新 JUL 与核心 Logger 的日志级别
     */
    private void handleLogLevelChange() {
        String selectedLevel = logLevelComboBox.getValue();
        Level julLevel;
        LogLevel coreLevel;

        switch (selectedLevel) {
            case "DEBUG":
                julLevel = Level.FINE;
                coreLevel = LogLevel.DEBUG;
                break;
            case "INFO":
                julLevel = Level.INFO;
                coreLevel = LogLevel.INFO;
                break;
            case "WARNING":
                julLevel = Level.WARNING;
                coreLevel = LogLevel.WARNING;
                break;
            case "ERROR":
            case "SEVERE":
                julLevel = Level.SEVERE;
                coreLevel = LogLevel.ERROR;
                break;
            default:
                julLevel = Level.ALL;
                coreLevel = LogLevel.DEBUG;
                break;
        }

        this.currentCoreLevel = coreLevel;
        Logger.setLogLevel(coreLevel);
        setLogLevel(julLevel);
    }

    /**
     * 设置日志级别
     * @param level 日志级别
     */
    public void setLogLevel(Level level) {
        this.currentJulLevel = level;
        if (julLogHandler != null) {
            julLogHandler.setLevel(level);
        }

        if (logLevelComboBox != null) {
            String displayLevel;
            if (level.equals(Level.FINE)) {
                displayLevel = "DEBUG";
            } else if (level.equals(Level.WARNING)) {
                displayLevel = "WARNING";
            } else if (level.equals(Level.SEVERE)) {
                displayLevel = "SEVERE";
            } else {
                displayLevel = level.getName();
            }
            logLevelComboBox.setValue(displayLevel);
        }
    }

    /**
     * 清空日志文本区
     */
    public void clearLog() {
        logTextArea.clear();
    }

    public TextArea getLogTextArea() {
        return logTextArea;
    }

    public ComboBox<String> getLogLevelComboBox() {
        return logLevelComboBox;
    }

    public Button getClearLogButton() {
        return clearLogButton;
    }

    /**
     * 自定义 JUL 日志处理器，用于将日志输出到 TextArea
     */
    private class LogHandler extends Handler {
        @Override
        public void publish(LogRecord record) {
            if (!isLoggable(record)) {
                return;
            }

            final String logMessage = getFormatter().format(record);

            // 在 JavaFX 应用线程中更新 UI
            Platform.runLater(() -> {
                logTextArea.appendText(logMessage);
                logTextArea.setScrollTop(Double.MAX_VALUE);
            });
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }
}