package com.github.relua.gui.views;

import com.github.relua.gui.theme.ThemeManager;
import com.github.relua.gui.preferences.AppSettings;
import com.github.relua.gui.preferences.EditorAppearance;
import com.github.relua.gui.preferences.SettingsManager;
import com.github.relua.log.LogLevel;
import com.github.relua.log.LogOutput;
import com.github.relua.log.Logger;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;

/**
 * 日志视图类，采用与主代码区域相同的 RichTextFX 高性能虚拟化渲染引擎，
 * 彻底消除 JavaFX 原生 TextArea 及其内嵌 ScrollPane 带来的双层视口裁剪与亚像素模糊，
 * 保证与反编译源码/字节码区域拥有完全相同的极致清晰锐利文字呈现，
 * 并支持随系统/用户界面主题热切换自动更新文本填充色与语法高亮。
 */
public class LogView {
    private final CodeArea logCodeArea;
    private final VirtualizedScrollPane<CodeArea> scrollPane;
    private ComboBox<String> logLevelComboBox;
    private Button clearLogButton;
    private LogHandler julLogHandler;
    private Level currentJulLevel = Level.ALL;
    private LogLevel currentCoreLevel = LogLevel.DEBUG;
    private final SettingsManager settingsManager;
    private final Consumer<AppSettings> settingsListener;

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

            appendLogSafely(formatted);
        }

        @Override
        public void close() {
        }
    };

    /**
     * 构造函数
     */
    public LogView() {
        this(SettingsManager.getInstance());
    }

    public LogView(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
        settingsListener = settings -> {
            if (Platform.isFxApplicationThread()) applySettings(settings);
            else Platform.runLater(() -> applySettings(settings));
        };
        // 初始化日志代码文本区域（采用与主代码区域一致的 RichTextFX 引擎，像素级清晰）
        logCodeArea = new CodeArea();
        logCodeArea.setEditable(false);
        logCodeArea.setWrapText(false); // 等宽日志保持原样排版，避免动态断行带来的字符重排与模糊
        logCodeArea.setCursor(Cursor.TEXT);
        logCodeArea.getStyleClass().add("log-code-area");
        logCodeArea.setMinHeight(50);

        scrollPane = new VirtualizedScrollPane<>(logCodeArea);

        // 初始化清空日志按钮
        clearLogButton = new Button("清空日志");
        clearLogButton.setOnAction(e -> clearLog());

        // 初始化 JDK JUL 日志处理器
        initializeJulHandler();

        // 注册到核心 Logger 日志管道，彻底打通日志断层
        Logger.addOutput(coreLogOutput);

        // 设置默认日志级别为 DEBUG
        setLogLevel(Level.FINE);

        // 初始化并监听主题变更，确保字体前景色与背景色随主题无缝热适配
        applyThemeStyles();
        ThemeManager.getInstance().addListener(newTheme -> Platform.runLater(this::applyThemeStyles));
        applySettings(settingsManager.getSettings());
        settingsManager.addListener(settingsListener);
    }

    /**
     * 应用当前主题的样式表到日志代码区域
     */
    private void applyThemeStyles() {
        try {
            logCodeArea.getStylesheets().clear();
            URL url = getClass().getResource("/css/" + ThemeManager.getInstance().getCurrentTheme().getCssFileName());
            if (url != null) {
                logCodeArea.getStylesheets().add(url.toExternalForm());
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 设置日志级别组合框
     * @param logLevelComboBox 日志级别组合框
     */
    public void setLogLevelComboBox(ComboBox<String> logLevelComboBox) {
        this.logLevelComboBox = logLevelComboBox;
        logLevelComboBox.getItems().addAll("ALL", "DEBUG", "INFO", "WARNING", "ERROR", "SEVERE");
        logLevelComboBox.setValue(settingsManager.getSettings().getLogLevel());
        logLevelComboBox.setOnAction(e -> handleLogLevelChange());
        handleLogLevelChange();
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
        String savedLevel = "SEVERE".equals(selectedLevel) ? "ERROR" : selectedLevel;
        if (!settingsManager.getSettings().getLogLevel().equals(savedLevel)) {
            try {
                settingsManager.apply(settingsManager.getSettings().withLogLevel(savedLevel));
            } catch (BackingStoreException error) {
                java.util.logging.Logger.getLogger(LogView.class.getName()).warning("无法保存日志级别: " + error.getMessage());
            }
        }
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
                displayLevel = "ERROR";
            } else {
                displayLevel = level.getName();
            }
            logLevelComboBox.setValue(displayLevel);
        }
    }

    private void applySettings(AppSettings settings) {
        logCodeArea.setStyle(EditorAppearance.fontStyle(settings));
        if (logLevelComboBox != null) {
            if (!settings.getLogLevel().equals(logLevelComboBox.getValue())) logLevelComboBox.setValue(settings.getLogLevel());
            handleLogLevelChange();
        } else {
            Level level = "ALL".equals(settings.getLogLevel()) ? Level.ALL
                    : "DEBUG".equals(settings.getLogLevel()) ? Level.FINE
                    : "WARNING".equals(settings.getLogLevel()) ? Level.WARNING
                    : "ERROR".equals(settings.getLogLevel()) ? Level.SEVERE : Level.INFO;
            currentCoreLevel = com.github.relua.log.LogLevel.valueOf("ALL".equals(settings.getLogLevel()) ? "DEBUG" : settings.getLogLevel());
            setLogLevel(level);
        }
    }

    /**
     * 清空日志文本区
     */
    public void clearLog() {
        if (Platform.isFxApplicationThread()) {
            logCodeArea.clear();
        } else {
            Platform.runLater(logCodeArea::clear);
        }
    }

    /**
     * 获取日志视图根节点（包含虚拟化滚动的 RichTextFX 视图）
     * @return 视图节点
     */
    public Node getView() {
        return scrollPane;
    }

    /**
     * 获取 RichTextFX CodeArea 组件
     * @return CodeArea 实例
     */
    public CodeArea getLogCodeArea() {
        return logCodeArea;
    }

    /**
     * 兼容旧接口，返回 null
     */
    @Deprecated
    public TextArea getLogTextArea() {
        return null;
    }

    public ComboBox<String> getLogLevelComboBox() {
        return logLevelComboBox;
    }

    public Button getClearLogButton() {
        return clearLogButton;
    }

    /**
     * 安全追加日志文本并自动滚动至底部，同时针对不同级别应用主题感知的高对比高亮
     * @param text 要追加的文本
     */
    private void appendLogSafely(String text) {
        Platform.runLater(() -> {
            int paragraphCount = logCodeArea.getParagraphs().size();
            // 防止长日志消耗过多内存，保留最多约 10000 行
            if (paragraphCount > 10000) {
                int trimLength = 0;
                for (int i = 0; i < 2000 && i < paragraphCount; i++) {
                    trimLength += logCodeArea.getParagraph(i).length() + 1;
                }
                logCodeArea.deleteText(0, trimLength);
            }
            int start = logCodeArea.getLength();
            logCodeArea.appendText(text);
            int end = logCodeArea.getLength();

            // 识别日志级别并赋予与主题深度契合的色彩样式
            String levelClass = "log-default";
            if (text.contains("[INFO]")) {
                levelClass = "log-info";
            } else if (text.contains("[WARNING]")) {
                levelClass = "log-warning";
            } else if (text.contains("[ERROR]") || text.contains("[SEVERE]")) {
                levelClass = "log-error";
            } else if (text.contains("[DEBUG]")) {
                levelClass = "log-debug";
            }

            try {
                logCodeArea.setStyle(start, end, Collections.singleton(levelClass));
            } catch (Throwable ignored) {
            }

            logCodeArea.moveTo(end);
            logCodeArea.requestFollowCaret();
        });
    }

    /**
     * 自定义 JUL 日志处理器，用于将日志输出到 CodeArea
     */
    private class LogHandler extends Handler {
        @Override
        public void publish(LogRecord record) {
            if (!isLoggable(record)) {
                return;
            }

            final String logMessage = getFormatter().format(record);
            appendLogSafely(logMessage);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }
}
