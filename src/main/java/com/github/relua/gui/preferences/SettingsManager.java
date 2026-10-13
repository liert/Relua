package com.github.relua.gui.preferences;

import com.github.relua.gui.theme.ThemeManager;
import com.github.relua.gui.theme.ThemeManager.Theme;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** 首选项唯一状态源；用户配置持久化到系统 Preferences，不写入源目录。 */
public final class SettingsManager {
    private static final Logger LOGGER = Logger.getLogger(SettingsManager.class.getName());
    private static final SettingsManager INSTANCE = createDefault();
    private final Preferences preferences;
    private volatile AppSettings settings;
    private final List<WeakReference<Consumer<AppSettings>>> listeners = new ArrayList<>();

    public SettingsManager(Preferences preferences) {
        this(preferences, AppSettings.defaults());
    }

    private SettingsManager(Preferences preferences, AppSettings defaults) {
        this.preferences = preferences;
        settings = load(defaults);
    }

    private static SettingsManager createDefault() {
        try {
            AppSettings defaults = AppSettings.defaults();
            String legacyTheme = Preferences.userNodeForPackage(ThemeManager.class).get("relua_ui_theme", "DARK");
            try { defaults = defaults.withTheme(Theme.valueOf(legacyTheme)); } catch (IllegalArgumentException ignored) {}
            return new SettingsManager(Preferences.userNodeForPackage(SettingsManager.class), defaults);
        } catch (Exception error) {
            LOGGER.warning("无法读取用户设置，将使用默认设置: " + error.getMessage());
            return new SettingsManager(null);
        }
    }

    public static SettingsManager getInstance() { return INSTANCE; }
    public AppSettings getSettings() { return settings; }

    private AppSettings load(AppSettings defaults) {
        if (preferences == null) return defaults;
        String family = preferences.get("font_family", defaults.getFontFamily());
        if (family.trim().isEmpty() || family.length() > 200 || family.contains("\n") || family.contains("\r")) {
            family = defaults.getFontFamily();
        }
        int size = preferences.getInt("font_size", defaults.getFontSize());
        if (size < AppSettings.MIN_FONT_SIZE || size > AppSettings.MAX_FONT_SIZE) size = defaults.getFontSize();
        Theme theme = defaults.getTheme();
        try { theme = Theme.valueOf(preferences.get("theme", theme.name())); } catch (IllegalArgumentException ignored) {}
        String level = preferences.get("log_level", defaults.getLogLevel());
        if (!AppSettings.LOG_LEVELS.contains(level)) level = defaults.getLogLevel();
        return new AppSettings(family, size, preferences.getBoolean("wrap_text", defaults.isWrapText()),
                preferences.getBoolean("show_line_numbers", defaults.isShowLineNumbers()),
                preferences.getBoolean("default_split_view", defaults.isDefaultSplitView()), theme, level);
    }

    public synchronized void apply(AppSettings value) throws BackingStoreException {
        if (value == null) throw new IllegalArgumentException("首选项不能为空");
        if (value.equals(settings)) return;
        if (preferences != null) {
            try {
                store(value);
                preferences.flush();
            } catch (BackingStoreException error) {
                store(settings);
                try { preferences.flush(); } catch (BackingStoreException rollbackError) { error.addSuppressed(rollbackError); }
                throw error;
            }
        }
        settings = value;
        listeners.removeIf(reference -> reference.get() == null);
        for (WeakReference<Consumer<AppSettings>> reference : new ArrayList<>(listeners)) {
            Consumer<AppSettings> listener = reference.get();
            if (listener != null) {
                try { listener.accept(value); }
                catch (RuntimeException error) { LOGGER.warning("更新首选项视图失败: " + error.getMessage()); }
            }
        }
    }

    private void store(AppSettings value) {
        preferences.put("font_family", value.getFontFamily());
        preferences.putInt("font_size", value.getFontSize());
        preferences.putBoolean("wrap_text", value.isWrapText());
        preferences.putBoolean("show_line_numbers", value.isShowLineNumbers());
        preferences.putBoolean("default_split_view", value.isDefaultSplitView());
        preferences.put("theme", value.getTheme().name());
        preferences.put("log_level", value.getLogLevel());
    }

    /** 调用方持有监听器，已关闭的视图不因设置监听而被保留。 */
    public synchronized void addListener(Consumer<AppSettings> listener) {
        listeners.add(new WeakReference<>(listener));
    }
}
