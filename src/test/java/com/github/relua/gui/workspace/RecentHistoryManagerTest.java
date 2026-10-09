package com.github.relua.gui.workspace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 最近打开历史记录管理器单元测试
 */
public class RecentHistoryManagerTest {

    private RecentHistoryManager manager;

    @BeforeEach
    public void setUp() {
        manager = RecentHistoryManager.getInstance();
        manager.clearRecentPaths();
    }

    @AfterEach
    public void tearDown() {
        manager.clearRecentPaths();
    }

    @Test
    @DisplayName("验证最近路径添加、置顶与去重规则")
    public void testAddAndDeduplicate() {
        File fileA = new File("scripts/a.luac").getAbsoluteFile();
        File fileB = new File("scripts/b.luac").getAbsoluteFile();
        File fileC = new File("scripts/c.luac").getAbsoluteFile();

        manager.addRecentPath(fileA.getAbsolutePath());
        manager.addRecentPath(fileB.getAbsolutePath());
        manager.addRecentPath(fileC.getAbsolutePath());

        List<String> list = manager.getRecentPaths();
        assertEquals(3, list.size());
        assertEquals(fileC.getAbsolutePath(), list.get(0));
        assertEquals(fileB.getAbsolutePath(), list.get(1));
        assertEquals(fileA.getAbsolutePath(), list.get(2));

        // 重新访问 A，应当将 A 提升至最前面且总数保持为 3
        manager.addRecentPath(fileA.getAbsolutePath());
        list = manager.getRecentPaths();
        assertEquals(3, list.size());
        assertEquals(fileA.getAbsolutePath(), list.get(0));
        assertEquals(fileC.getAbsolutePath(), list.get(1));
        assertEquals(fileB.getAbsolutePath(), list.get(2));
    }

    @Test
    @DisplayName("验证最大历史条数限制为 10 条")
    public void testMaxItemsLimit() {
        for (int i = 1; i <= 15; i++) {
            File f = new File("dummy/path_" + i + ".lua").getAbsoluteFile();
            manager.addRecentPath(f.getAbsolutePath());
        }

        List<String> list = manager.getRecentPaths();
        assertEquals(RecentHistoryManager.MAX_RECENT_ITEMS, list.size());
        // 最新的应该是 path_15
        File latest = new File("dummy/path_15.lua").getAbsoluteFile();
        assertEquals(latest.getAbsolutePath(), list.get(0));
    }

    @Test
    @DisplayName("验证路径删除与清空")
    public void testRemoveAndClear() {
        File fileA = new File("scripts/a.luac").getAbsoluteFile();
        File fileB = new File("scripts/b.luac").getAbsoluteFile();

        manager.addRecentPath(fileA.getAbsolutePath());
        manager.addRecentPath(fileB.getAbsolutePath());
        assertEquals(2, manager.getRecentPaths().size());

        manager.removeRecentPath(fileA.getAbsolutePath());
        assertEquals(1, manager.getRecentPaths().size());
        assertEquals(fileB.getAbsolutePath(), manager.getRecentPaths().get(0));

        manager.clearRecentPaths();
        assertTrue(manager.getRecentPaths().isEmpty());
    }
}
