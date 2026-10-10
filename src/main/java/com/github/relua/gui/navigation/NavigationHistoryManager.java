package com.github.relua.gui.navigation;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * 光标跳转与浏览历史管理器 (Navigate Back: Alt+Left / Forward: Alt+Right)
 */
public class NavigationHistoryManager {

    private static final int MAX_HISTORY_SIZE = 100;
    private static final NavigationHistoryManager INSTANCE = new NavigationHistoryManager();

    private final Deque<NavigationLocation> backStack = new ArrayDeque<>();
    private final Deque<NavigationLocation> forwardStack = new ArrayDeque<>();
    private NavigationLocation currentLocation;
    private boolean isNavigating = false;

    public static NavigationHistoryManager getInstance() {
        return INSTANCE;
    }

    private NavigationHistoryManager() {
    }

    /**
     * 记录当前光标位置变化
     * 如果在短时间内同一文件内行变化很小 (<= 3 行)，则合并或更新当前位置，避免历史记录臃肿
     * @param file 目标文件
     * @param line 目标行号 (1-based)
     * @param column 目标列号 (0-based)
     */
    public synchronized void recordLocation(File file, int line, int column) {
        if (isNavigating || file == null) {
            return;
        }

        NavigationLocation newLoc = new NavigationLocation(file, line, column);

        if (currentLocation == null) {
            currentLocation = newLoc;
            return;
        }

        // 如果在同一个文件的临近行 (<= 3行)，更新当前位置但不压入新栈帧
        if (Objects.equals(currentLocation.getFile(), file) && Math.abs(currentLocation.getLine() - line) <= 3) {
            currentLocation = newLoc;
            return;
        }

        // 产生实质性跳转：将前一个位置压入后退栈
        backStack.push(currentLocation);
        if (backStack.size() > MAX_HISTORY_SIZE) {
            backStack.removeLast();
        }
        currentLocation = newLoc;
        // 用户主动导航时清空前进栈
        forwardStack.clear();
    }

    public synchronized boolean canNavigateBack() {
        return !backStack.isEmpty();
    }

    public synchronized boolean canNavigateForward() {
        return !forwardStack.isEmpty();
    }

    /**
     * 向后导航 (Back, Alt+Left)
     * @param currentActualLoc 当前光标实际位置（若有）
     * @return 目标跳转位置，若无法后退则返回 null
     */
    public synchronized NavigationLocation navigateBack(NavigationLocation currentActualLoc) {
        if (backStack.isEmpty()) {
            return null;
        }

        isNavigating = true;
        try {
            NavigationLocation target = backStack.pop();
            NavigationLocation from = (currentActualLoc != null) ? currentActualLoc : currentLocation;
            if (from != null) {
                forwardStack.push(from);
                if (forwardStack.size() > MAX_HISTORY_SIZE) {
                    forwardStack.removeLast();
                }
            }
            currentLocation = target;
            return target;
        } finally {
            isNavigating = false;
        }
    }

    /**
     * 向前导航 (Forward, Alt+Right)
     * @param currentActualLoc 当前光标实际位置（若有）
     * @return 目标跳转位置，若无法前进则返回 null
     */
    public synchronized NavigationLocation navigateForward(NavigationLocation currentActualLoc) {
        if (forwardStack.isEmpty()) {
            return null;
        }

        isNavigating = true;
        try {
            NavigationLocation target = forwardStack.pop();
            NavigationLocation from = (currentActualLoc != null) ? currentActualLoc : currentLocation;
            if (from != null) {
                backStack.push(from);
                if (backStack.size() > MAX_HISTORY_SIZE) {
                    backStack.removeLast();
                }
            }
            currentLocation = target;
            return target;
        } finally {
            isNavigating = false;
        }
    }

    public synchronized NavigationLocation getCurrentLocation() {
        return currentLocation;
    }

    public synchronized int getBackStackSize() {
        return backStack.size();
    }

    public synchronized int getForwardStackSize() {
        return forwardStack.size();
    }

    public synchronized void clear() {
        backStack.clear();
        forwardStack.clear();
        currentLocation = null;
        isNavigating = false;
    }
}
