package com.github.relua.gui.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * 最近打开文件与工程历史管理器，基于平台首选项 Preferences 提供轻量安全的跨会话持久化
 */
public class RecentHistoryManager {
    private static final Logger LOGGER = Logger.getLogger(RecentHistoryManager.class.getName());

    public static final int MAX_RECENT_ITEMS = 10;
    private static final String PREF_KEY_RECENT_PATHS = "recent_paths_v1";
    private static final String PATH_SEPARATOR = "\n";

    private static final RecentHistoryManager INSTANCE = new RecentHistoryManager();

    private final Preferences preferences;
    private final List<String> recentPaths = new ArrayList<>();

    private RecentHistoryManager() {
        Preferences prefs = null;
        try {
            prefs = Preferences.userNodeForPackage(RecentHistoryManager.class);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "初始化 Preferences 失败，将仅使用内存记录: " + e.getMessage(), e);
        }
        this.preferences = prefs;
        loadFromPreferences();
    }

    public static RecentHistoryManager getInstance() {
        return INSTANCE;
    }

    /**
     * 从首选项加载历史路径列表
     */
    private synchronized void loadFromPreferences() {
        recentPaths.clear();
        if (preferences == null) {
            return;
        }
        try {
            String raw = preferences.get(PREF_KEY_RECENT_PATHS, "");
            if (raw != null && !raw.trim().isEmpty()) {
                String[] items = raw.split(PATH_SEPARATOR);
                for (String item : items) {
                    String trimmed = item.trim();
                    if (!trimmed.isEmpty() && !recentPaths.contains(trimmed)) {
                        recentPaths.add(trimmed);
                    }
                    if (recentPaths.size() >= MAX_RECENT_ITEMS) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("加载最近历史记录失败: " + e.getMessage());
        }
    }

    /**
     * 将当前历史记录序列化并保存到首选项
     */
    private synchronized void saveToPreferences() {
        if (preferences == null) {
            return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < recentPaths.size(); i++) {
                if (i > 0) {
                    sb.append(PATH_SEPARATOR);
                }
                sb.append(recentPaths.get(i));
            }
            preferences.put(PREF_KEY_RECENT_PATHS, sb.toString());
            preferences.flush();
        } catch (Exception e) {
            LOGGER.warning("保存最近历史记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前所有最近打开的有效路径列表（不可变副本）
     * @return 路径字符串列表，按最近访问时间倒序排列
     */
    public synchronized List<String> getRecentPaths() {
        return Collections.unmodifiableList(new ArrayList<>(recentPaths));
    }

    /**
     * 添加一条最近打开的路径，自动置顶、去重并限制最大数量
     * @param path 文件或文件夹路径
     */
    public synchronized void addRecentPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return;
        }
        String normalized = new File(path.trim()).getAbsolutePath();
        // 移除旧有出现项并提升至首位
        recentPaths.remove(normalized);
        recentPaths.add(0, normalized);

        // 超出限制截断
        while (recentPaths.size() > MAX_RECENT_ITEMS) {
            recentPaths.remove(recentPaths.size() - 1);
        }

        saveToPreferences();
    }

    /**
     * 移除指定的路径（例如当用户尝试打开已不存在的文件时触发清理）
     * @param path 要移除的路径
     */
    public synchronized void removeRecentPath(String path) {
        if (path == null) {
            return;
        }
        String normalized = new File(path.trim()).getAbsolutePath();
        if (recentPaths.remove(normalized)) {
            saveToPreferences();
        }
    }

    /**
     * 清空全部最近打开历史
     */
    public synchronized void clearRecentPaths() {
        recentPaths.clear();
        saveToPreferences();
    }
}
