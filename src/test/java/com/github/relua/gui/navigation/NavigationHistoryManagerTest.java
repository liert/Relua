package com.github.relua.gui.navigation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class NavigationHistoryManagerTest {

    private final NavigationHistoryManager manager = NavigationHistoryManager.getInstance();
    private final File fileA = new File("testA.lua");
    private final File fileB = new File("testB.lua");

    @BeforeEach
    public void setUp() {
        manager.clear();
    }

    @Test
    @DisplayName("测试导航历史记录与临近行合并优化")
    public void testRecordLocationAndProximityMerge() {
        assertFalse(manager.canNavigateBack());
        assertFalse(manager.canNavigateForward());

        // 记录初始位置 (fileA, line 10)
        manager.recordLocation(fileA, 10, 0);
        assertEquals(0, manager.getBackStackSize());

        // 临近微调 (line 11, line 12) 不应压入新栈帧
        manager.recordLocation(fileA, 11, 2);
        manager.recordLocation(fileA, 12, 5);
        assertEquals(0, manager.getBackStackSize());

        // 显著跳转 (line 60) 应压入之前的栈帧
        manager.recordLocation(fileA, 60, 0);
        assertTrue(manager.canNavigateBack());
        assertEquals(1, manager.getBackStackSize());
    }

    @Test
    @DisplayName("测试向后与向前导航 (Back & Forward)")
    public void testNavigateBackAndForward() {
        manager.recordLocation(fileA, 10, 0);
        manager.recordLocation(fileA, 50, 0);
        manager.recordLocation(fileB, 20, 0);

        // 当前位于 (fileB, 20)，后退栈应该有 2 步
        assertEquals(2, manager.getBackStackSize());
        assertFalse(manager.canNavigateForward());

        // 执行后退 -> 应回到 (fileA, 50)
        NavigationLocation back1 = manager.navigateBack(new NavigationLocation(fileB, 20, 0));
        assertNotNull(back1);
        assertEquals(fileA, back1.getFile());
        assertEquals(50, back1.getLine());
        assertTrue(manager.canNavigateForward());

        // 再次后退 -> 应回到 (fileA, 10)
        NavigationLocation back2 = manager.navigateBack(back1);
        assertNotNull(back2);
        assertEquals(fileA, back2.getFile());
        assertEquals(10, back2.getLine());
        assertFalse(manager.canNavigateBack());

        // 执行前进 -> 应回到 (fileA, 50)
        NavigationLocation fwd1 = manager.navigateForward(back2);
        assertNotNull(fwd1);
        assertEquals(fileA, fwd1.getFile());
        assertEquals(50, fwd1.getLine());

        // 再次前进 -> 应回到 (fileB, 20)
        NavigationLocation fwd2 = manager.navigateForward(fwd1);
        assertNotNull(fwd2);
        assertEquals(fileB, fwd2.getFile());
        assertEquals(20, fwd2.getLine());
        assertFalse(manager.canNavigateForward());
    }

    @Test
    @DisplayName("测试导航中产生新跳转会清空前进栈")
    public void testBranchingNavigationClearsForwardStack() {
        manager.recordLocation(fileA, 10, 0);
        manager.recordLocation(fileA, 50, 0);

        // 后退到 10
        manager.navigateBack(new NavigationLocation(fileA, 50, 0));
        assertTrue(manager.canNavigateForward());

        // 在位置 10 产生新的跳转到 100，前进栈应当被清空
        manager.recordLocation(fileA, 100, 0);
        assertFalse(manager.canNavigateForward());
    }
}
