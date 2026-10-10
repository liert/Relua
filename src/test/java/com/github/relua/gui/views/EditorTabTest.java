package com.github.relua.gui.views;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class EditorTabTest {

    @BeforeAll
    public static void initJFX() {
        try {
            Method startupMethod = Platform.class.getMethod("startup", Runnable.class);
            startupMethod.invoke(null, (Runnable) () -> {});
        } catch (Throwable ignored) {
        }
    }

    private void runOnFxThread(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] err = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                err[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timeout waiting for FX thread");
        if (err[0] != null) {
            throw new RuntimeException(err[0]);
        }
    }

    @Test
    @DisplayName("测试 EditorTab 三态视图切换 (LUA_SOURCE, SIDE_BY_SIDE, BYTECODE)")
    public void testViewModesAndCycle() throws Exception {
        runOnFxThread(() -> {
            File dummyFile = new File("sample.lua");
            TextEditorView luaEditor = new TextEditorView(false);
            EditorTab tab = new EditorTab(dummyFile, luaEditor);

            // 初始状态为 Lua 源码模式
            assertEquals(EditorTab.ViewMode.LUA_SOURCE, tab.getViewMode());
            assertFalse(tab.isSideBySide());
            assertFalse(tab.isShowingBytecode());

            // 切换为并排分屏模式
            tab.setViewMode(EditorTab.ViewMode.SIDE_BY_SIDE);
            assertEquals(EditorTab.ViewMode.SIDE_BY_SIDE, tab.getViewMode());
            assertTrue(tab.isSideBySide());
            assertFalse(tab.isShowingBytecode());

            // 切换为字节码模式
            tab.setViewMode(EditorTab.ViewMode.BYTECODE);
            assertEquals(EditorTab.ViewMode.BYTECODE, tab.getViewMode());
            assertFalse(tab.isSideBySide());
            assertTrue(tab.isShowingBytecode());

            // 循环切换：BYTECODE -> LUA_SOURCE
            tab.toggleBytecodeView();
            assertEquals(EditorTab.ViewMode.LUA_SOURCE, tab.getViewMode());

            // 循环切换：LUA_SOURCE -> SIDE_BY_SIDE
            tab.toggleBytecodeView();
            assertEquals(EditorTab.ViewMode.SIDE_BY_SIDE, tab.getViewMode());

            // 循环切换：SIDE_BY_SIDE -> BYTECODE
            tab.toggleBytecodeView();
            assertEquals(EditorTab.ViewMode.BYTECODE, tab.getViewMode());
        });
    }

    @Test
    @DisplayName("测试分屏模式下双向同步联动")
    public void testSideBySideSync() throws Exception {
        runOnFxThread(() -> {
            File dummyFile = new File("test_sync.lua");
            TextEditorView luaEditor = new TextEditorView(false);
            EditorTab tab = new EditorTab(dummyFile, luaEditor);

            // 设置代码与字节码
            luaEditor.setText("local a = 1\nprint(a)\nreturn a\n");
            tab.getBytecodeEditorView().setText(
                    "; Header\n" +
                    "   0 [line    1]: LOADK       \tR0 K0\n" +
                    "   1 [line    2]: GETGLOBAL   \tR1 K1\n" +
                    "   2 [line    2]: CALL        \tR1 1 1\n" +
                    "   3 [line    3]: RETURN      \tR0 1\n"
            );

            tab.setViewMode(EditorTab.ViewMode.SIDE_BY_SIDE);

            // 触发 Lua -> Bytecode 同步 (force = true)
            luaEditor.jumpToLine(2);
            tab.syncBytecodeFromLua(true);
            // Lua 第 2 行对应字节码第 3 行 ("   1 [line    2]: GETGLOBAL")
            assertEquals(3, tab.getBytecodeEditorView().getCurrentLineNumber());

            // 触发 Bytecode -> Lua 同步 (force = true)
            tab.getBytecodeEditorView().jumpToLine(5); // 字节码第 5 行为 "   3 [line    3]: RETURN"
            tab.syncLuaFromBytecode(true);
            assertEquals(3, luaEditor.getCurrentLineNumber());
        });
    }
}
