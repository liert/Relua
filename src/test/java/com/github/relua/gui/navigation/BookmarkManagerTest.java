package com.github.relua.gui.navigation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class BookmarkManagerTest {

    private final BookmarkManager manager = BookmarkManager.getInstance();
    private final File fileA = new File("testA.lua");
    private final File fileB = new File("testB.lua");

    @BeforeEach
    public void setUp() {
        manager.clearAll();
    }

    @Test
    @DisplayName("测试切换普通行书签 (Toggle Bookmark)")
    public void testToggleBookmark() {
        assertNull(manager.getBookmark(fileA, 15));

        // 第一次切换：添加书签
        boolean added = manager.toggleBookmark(fileA, 15, "local a = 1");
        assertTrue(added);
        Bookmark b = manager.getBookmark(fileA, 15);
        assertNotNull(b);
        assertEquals(15, b.getLine());
        assertEquals("local a = 1", b.getPreviewText());
        assertNull(b.getMnemonic());

        // 第二次切换：移除书签
        boolean removed = manager.toggleBookmark(fileA, 15, "local a = 1");
        assertFalse(removed);
        assertNull(manager.getBookmark(fileA, 15));
    }

    @Test
    @DisplayName("测试设置与快速查询助记符书签 1~9")
    public void testMnemonicBookmark() {
        manager.setMnemonicBookmark(fileA, 25, 1, "function foo()");
        Bookmark b1 = manager.getBookmarkByMnemonic(1);
        assertNotNull(b1);
        assertEquals(fileA, b1.getFile());
        assertEquals(25, b1.getLine());
        assertEquals(1, b1.getMnemonic());

        // 将助记符 1 移动到 fileB 的第 50 行，原位置的助记符应被接管
        manager.setMnemonicBookmark(fileB, 50, 1, "local bar = 2");
        Bookmark b1New = manager.getBookmarkByMnemonic(1);
        assertNotNull(b1New);
        assertEquals(fileB, b1New.getFile());
        assertEquals(50, b1New.getLine());
        assertNull(manager.getBookmark(fileA, 25));
    }

    @Test
    @DisplayName("测试书签循环跳转 (Next & Previous Bookmark)")
    public void testNextAndPreviousBookmark() {
        manager.toggleBookmark(fileA, 10, "line 10");
        manager.toggleBookmark(fileA, 30, "line 30");
        manager.toggleBookmark(fileB, 5, "fileB line 5");

        List<Bookmark> all = manager.getAllBookmarks();
        assertEquals(3, all.size());

        // 从 fileA 的第 10 行找下一个 -> 应为 fileA 的第 30 行
        Bookmark next1 = manager.getNextBookmark(fileA, 10);
        assertNotNull(next1);
        assertEquals(fileA, next1.getFile());
        assertEquals(30, next1.getLine());

        // 从 fileA 的第 30 行找下一个 -> 应循环跨文件或取下一个
        Bookmark next2 = manager.getNextBookmark(fileA, 30);
        assertNotNull(next2);

        // 从 fileA 的第 30 行找上一个 -> 应为 fileA 的第 10 行
        Bookmark prev1 = manager.getPreviousBookmark(fileA, 30);
        assertNotNull(prev1);
        assertEquals(fileA, prev1.getFile());
        assertEquals(10, prev1.getLine());
    }

    @Test
    @DisplayName("测试书签事件监听器机制")
    public void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        BookmarkManager.BookmarkListener listener = file -> notified.set(true);

        manager.addListener(listener);
        manager.toggleBookmark(fileA, 42, "code line 42");
        assertTrue(notified.get());

        manager.removeListener(listener);
    }
}
