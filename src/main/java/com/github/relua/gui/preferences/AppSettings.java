package com.github.relua.gui.preferences;

import com.github.relua.gui.theme.ThemeManager.Theme;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** 已校验的程序首选项，独立于文件分析和工作区数据。 */
public final class AppSettings {
    public static final int MIN_FONT_SIZE = 8;
    public static final int MAX_FONT_SIZE = 48;
    public static final List<String> LOG_LEVELS = Collections.unmodifiableList(
            Arrays.asList("ALL", "DEBUG", "INFO", "WARNING", "ERROR"));
    private final String fontFamily;
    private final int fontSize;
    private final boolean wrapText;
    private final boolean showLineNumbers;
    private final boolean defaultSplitView;
    private final Theme theme;
    private final String logLevel;

    public AppSettings(String fontFamily, int fontSize, boolean wrapText, boolean showLineNumbers,
                       boolean defaultSplitView, Theme theme, String logLevel) {
        if (fontFamily == null || fontFamily.trim().isEmpty() || fontFamily.length() > 200
                || fontFamily.contains("\n") || fontFamily.contains("\r")) {
            throw new IllegalArgumentException("请选择有效的字体");
        }
        if (fontSize < MIN_FONT_SIZE || fontSize > MAX_FONT_SIZE) {
            throw new IllegalArgumentException("字号应在 8–48 px 之间");
        }
        if (theme == null || !LOG_LEVELS.contains(logLevel)) throw new IllegalArgumentException("无效的首选项");
        this.fontFamily = fontFamily.trim();
        this.fontSize = fontSize;
        this.wrapText = wrapText;
        this.showLineNumbers = showLineNumbers;
        this.defaultSplitView = defaultSplitView;
        this.theme = theme;
        this.logLevel = logLevel;
    }

    public static AppSettings defaults() {
        return new AppSettings("Consolas", 14, false, true, false, Theme.DARK, "INFO");
    }

    public String getFontFamily() { return fontFamily; }
    public int getFontSize() { return fontSize; }
    public boolean isWrapText() { return wrapText; }
    public boolean isShowLineNumbers() { return showLineNumbers; }
    public boolean isDefaultSplitView() { return defaultSplitView; }
    public Theme getTheme() { return theme; }
    public String getLogLevel() { return logLevel; }

    public AppSettings withTheme(Theme value) {
        return new AppSettings(fontFamily, fontSize, wrapText, showLineNumbers, defaultSplitView, value, logLevel);
    }

    public AppSettings withLogLevel(String value) {
        return new AppSettings(fontFamily, fontSize, wrapText, showLineNumbers, defaultSplitView, theme, value);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AppSettings)) return false;
        AppSettings settings = (AppSettings) other;
        return fontFamily.equals(settings.fontFamily) && fontSize == settings.fontSize
                && wrapText == settings.wrapText && showLineNumbers == settings.showLineNumbers
                && defaultSplitView == settings.defaultSplitView && theme == settings.theme
                && logLevel.equals(settings.logLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fontFamily, fontSize, wrapText, showLineNumbers, defaultSplitView, theme, logLevel);
    }
}
