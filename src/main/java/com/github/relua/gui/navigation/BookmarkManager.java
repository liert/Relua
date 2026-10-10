package com.github.relua.gui.navigation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 全局代码行书签与助记符书签管理器 (Bookmarks: Ctrl+F11 / Ctrl+1~9)
 */
public class BookmarkManager {

    public interface BookmarkListener {
        void onBookmarksChanged(File file);
    }

    private static final BookmarkManager INSTANCE = new BookmarkManager();

    // Map: File -> (Line -> Bookmark)
    private final Map<File, Map<Integer, Bookmark>> fileBookmarks = new HashMap<>();
    private final List<BookmarkListener> listeners = new CopyOnWriteArrayList<>();

    public static BookmarkManager getInstance() {
        return INSTANCE;
    }

    private BookmarkManager() {
    }

    public void addListener(BookmarkListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(BookmarkListener listener) {
        listeners.remove(listener);
    }

    private void notifyChanged(File file) {
        for (BookmarkListener listener : listeners) {
            listener.onBookmarksChanged(file);
        }
    }

    /**
     * 切换当前行的普通书签 (Toggle Bookmark, Ctrl+F11)
     * @param file 目标文件
     * @param line 目标行号 (1-based)
     * @param previewText 代码行预览文本
     * @return true 表示新增书签，false 表示移除书签
     */
    public synchronized boolean toggleBookmark(File file, int line, String previewText) {
        if (file == null || line <= 0) {
            return false;
        }
        Map<Integer, Bookmark> lineMap = fileBookmarks.computeIfAbsent(file, k -> new HashMap<>());
        boolean added;
        if (lineMap.containsKey(line)) {
            lineMap.remove(line);
            if (lineMap.isEmpty()) {
                fileBookmarks.remove(file);
            }
            added = false;
        } else {
            lineMap.put(line, new Bookmark(file, line, null, previewText));
            added = true;
        }
        notifyChanged(file);
        return added;
    }

    /**
     * 设置或切换助记符数字书签 1~9 (Ctrl+Shift+1~9 或 Ctrl+F11)
     * @param file 目标文件
     * @param line 目标行号 (1-based)
     * @param mnemonic 助记符 1~9
     * @param previewText 代码行预览文本
     */
    public synchronized void setMnemonicBookmark(File file, int line, int mnemonic, String previewText) {
        if (file == null || line <= 0 || mnemonic < 1 || mnemonic > 9) {
            return;
        }

        // 先移除其他位置已存在的相同 mnemonic 书签
        File oldFile = null;
        for (Map.Entry<File, Map<Integer, Bookmark>> entry : fileBookmarks.entrySet()) {
            for (Map.Entry<Integer, Bookmark> bEntry : new HashMap<>(entry.getValue()).entrySet()) {
                if (Objects.equals(bEntry.getValue().getMnemonic(), mnemonic)) {
                    entry.getValue().remove(bEntry.getKey());
                    oldFile = entry.getKey();
                }
            }
        }

        Map<Integer, Bookmark> lineMap = fileBookmarks.computeIfAbsent(file, k -> new HashMap<>());
        // 如果当前行已有书签且助记符相同，则取消书签
        Bookmark existing = lineMap.get(line);
        if (existing != null && Objects.equals(existing.getMnemonic(), mnemonic)) {
            lineMap.remove(line);
            if (lineMap.isEmpty()) {
                fileBookmarks.remove(file);
            }
        } else {
            lineMap.put(line, new Bookmark(file, line, mnemonic, previewText));
        }

        if (oldFile != null && !oldFile.equals(file)) {
            notifyChanged(oldFile);
        }
        notifyChanged(file);
    }

    public synchronized Bookmark getBookmark(File file, int line) {
        if (file == null || line <= 0) {
            return null;
        }
        Map<Integer, Bookmark> lineMap = fileBookmarks.get(file);
        return lineMap != null ? lineMap.get(line) : null;
    }

    public synchronized Bookmark getBookmarkByMnemonic(int mnemonic) {
        if (mnemonic < 1 || mnemonic > 9) {
            return null;
        }
        for (Map<Integer, Bookmark> lineMap : fileBookmarks.values()) {
            for (Bookmark b : lineMap.values()) {
                if (Objects.equals(b.getMnemonic(), mnemonic)) {
                    return b;
                }
            }
        }
        return null;
    }

    public synchronized List<Bookmark> getBookmarksForFile(File file) {
        if (file == null) {
            return Collections.emptyList();
        }
        Map<Integer, Bookmark> lineMap = fileBookmarks.get(file);
        if (lineMap == null || lineMap.isEmpty()) {
            return Collections.emptyList();
        }
        List<Bookmark> list = new ArrayList<>(lineMap.values());
        list.sort(Comparator.comparingInt(Bookmark::getLine));
        return list;
    }

    public synchronized List<Bookmark> getAllBookmarks() {
        List<Bookmark> list = new ArrayList<>();
        for (Map<Integer, Bookmark> lineMap : fileBookmarks.values()) {
            list.addAll(lineMap.values());
        }
        list.sort(Comparator.comparing((Bookmark b) -> b.getFile().getName())
                .thenComparingInt(Bookmark::getLine));
        return list;
    }

    public synchronized Bookmark getNextBookmark(File currentFile, int currentLine) {
        List<Bookmark> all = getAllBookmarks();
        if (all.isEmpty()) {
            return null;
        }
        // 先在当前文件内向后寻找
        for (Bookmark b : all) {
            if (Objects.equals(b.getFile(), currentFile) && b.getLine() > currentLine) {
                return b;
            }
        }
        // 如果当前文件没有后续的，取跨文件列表中的下一个或循环回第一个
        boolean foundCurrent = false;
        for (Bookmark b : all) {
            if (foundCurrent) {
                return b;
            }
            if (Objects.equals(b.getFile(), currentFile) && b.getLine() == currentLine) {
                foundCurrent = true;
            }
        }
        return all.get(0);
    }

    public synchronized Bookmark getPreviousBookmark(File currentFile, int currentLine) {
        List<Bookmark> all = getAllBookmarks();
        if (all.isEmpty()) {
            return null;
        }
        // 在当前文件内向前寻找
        for (int i = all.size() - 1; i >= 0; i--) {
            Bookmark b = all.get(i);
            if (Objects.equals(b.getFile(), currentFile) && b.getLine() < currentLine) {
                return b;
            }
        }
        // 跨文件列表向前或循环回最后一个
        boolean foundCurrent = false;
        for (int i = all.size() - 1; i >= 0; i--) {
            Bookmark b = all.get(i);
            if (foundCurrent) {
                return b;
            }
            if (Objects.equals(b.getFile(), currentFile) && b.getLine() == currentLine) {
                foundCurrent = true;
            }
        }
        return all.get(all.size() - 1);
    }

    public synchronized void clearAll() {
        fileBookmarks.clear();
        notifyChanged(null);
    }
}
