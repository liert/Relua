package com.github.relua.gui.theme;

import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * 界面现代主题管理器：支持深色专业极客风格与现代浅色风格无缝热切换与偏好持久化
 */
public class ThemeManager {

    /**
     * 主题类型枚举
     */
    public enum Theme {
        DARK("theme-dark.css", "现代暗色 (Modern Dark)"),
        LIGHT("theme-light.css", "现代浅色 (Modern Light)");

        private final String cssFileName;
        private final String displayName;

        Theme(String cssFileName, String displayName) {
            this.cssFileName = cssFileName;
            this.displayName = displayName;
        }

        public String getCssFileName() {
            return cssFileName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final String PREF_THEME_KEY = "relua_ui_theme";
    private static final ThemeManager INSTANCE = new ThemeManager();

    private Theme currentTheme = Theme.DARK; // 默认深色专业极客风格
    private final List<ThemeChangeListener> listeners = new ArrayList<>();

    /**
     * 主题切换事件监听器
     */
    public interface ThemeChangeListener {
        void onThemeChanged(Theme newTheme);
    }

    private ThemeManager() {
        // 读取持久化偏好
        try {
            Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
            String saved = prefs.get(PREF_THEME_KEY, Theme.DARK.name());
            this.currentTheme = Theme.valueOf(saved);
        } catch (Exception e) {
            this.currentTheme = Theme.DARK;
        }
    }

    public static ThemeManager getInstance() {
        return INSTANCE;
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public boolean isDark() {
        return currentTheme == Theme.DARK;
    }

    /**
     * 切换主题并应用到指定 Scene
     * @param theme 目标主题
     * @param scene 当前场景
     */
    public void setTheme(Theme theme, Scene scene) {
        if (theme == null) return;
        this.currentTheme = theme;
        try {
            Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
            prefs.put(PREF_THEME_KEY, theme.name());
        } catch (Exception ignored) {
        }

        if (scene != null) {
            applyTheme(scene);
        }

        for (ThemeChangeListener listener : new ArrayList<>(listeners)) {
            try {
                listener.onThemeChanged(theme);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 将当前主题应用到指定 Scene
     * @param scene 当前场景
     */
    public void applyTheme(Scene scene) {
        if (scene == null) return;
        List<String> stylesheets = scene.getStylesheets();
        
        // 移除所有已知主题文件
        for (Theme t : Theme.values()) {
            URL url = getClass().getResource("/css/" + t.getCssFileName());
            if (url != null) {
                stylesheets.remove(url.toExternalForm());
            }
        }
        
        // 添加当前主题样式
        URL currentUrl = getClass().getResource("/css/" + currentTheme.getCssFileName());
        if (currentUrl != null) {
            String form = currentUrl.toExternalForm();
            if (!stylesheets.contains(form)) {
                stylesheets.add(form);
            }
        }
    }

    /**
     * 将当前主题应用到指定 Dialog
     * @param dialog 对话框
     */
    public void applyTheme(Dialog<?> dialog) {
        if (dialog == null) return;
        applyTheme(dialog.getDialogPane());
    }

    /**
     * 将当前主题应用到指定 DialogPane
     * @param dialogPane 对话框面板
     */
    public void applyTheme(DialogPane dialogPane) {
        if (dialogPane == null) return;
        List<String> stylesheets = dialogPane.getStylesheets();

        for (Theme t : Theme.values()) {
            URL url = getClass().getResource("/css/" + t.getCssFileName());
            if (url != null) {
                stylesheets.remove(url.toExternalForm());
            }
        }

        URL currentUrl = getClass().getResource("/css/" + currentTheme.getCssFileName());
        if (currentUrl != null) {
            String form = currentUrl.toExternalForm();
            if (!stylesheets.contains(form)) {
                stylesheets.add(form);
            }
        }
    }

    /**
     * 添加主题监听器
     * @param listener 监听器
     */
    public void addListener(ThemeChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * 移除主题监听器
     * @param listener 监听器
     */
    public void removeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }
}
