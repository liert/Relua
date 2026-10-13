package com.github.relua.gui.views;

import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import com.github.relua.parser.LuacParser;
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
    @DisplayName("底部 Code/Bytecode 独立切换，Split view 默认在右侧显示 Bytecode")
    public void testBottomSelectorsAndSplitCheckbox() throws Exception {
        runOnFxThread(() -> {
            EditorTab tab = new EditorTab(new File("selectors.lua"), new TextEditorView(false));
            VBox content = (VBox) ((StackPane) tab.getContent()).getChildren().get(0);
            SplitPane split = (SplitPane) content.getChildren().get(0);
            VBox left = (VBox) split.getItems().get(0);
            HBox leftBar = (HBox) left.getChildren().get(1);
            ToggleButton code = (ToggleButton) leftBar.getChildren().get(0);
            ToggleButton bytecode = (ToggleButton) leftBar.getChildren().get(1);
            ToggleButton ast = (ToggleButton) leftBar.getChildren().get(2);
            CheckBox splitView = (CheckBox) leftBar.getChildren().get(4);
            assertEquals("Code", code.getText());
            assertEquals("Bytecode", bytecode.getText());
            assertEquals("AST", ast.getText());
            assertEquals("Split view", splitView.getText());
            assertTrue(code.isSelected());
            assertFalse(splitView.isSelected());

            splitView.fire();
            assertTrue(tab.isSideBySide());
            assertEquals(2, split.getItems().size());
            VBox right = (VBox) split.getItems().get(1);
            HBox rightBar = (HBox) right.getChildren().get(1);
            ToggleButton rightCode = (ToggleButton) rightBar.getChildren().get(0);
            ToggleButton rightBytecode = (ToggleButton) rightBar.getChildren().get(1);
            assertTrue(rightBytecode.isSelected());
            assertEquals(EditorTab.ViewMode.BYTECODE, tab.getSecondaryViewMode());

            rightCode.fire();
            assertTrue(code.isSelected());
            assertTrue(rightCode.isSelected());
            assertNotSame(left.getChildren().get(0), right.getChildren().get(0));
            bytecode.fire();
            assertTrue(splitView.isSelected());
            assertTrue(bytecode.isSelected());
            assertTrue(rightCode.isSelected());
            rightBytecode.fire();
            assertTrue(bytecode.isSelected());
            assertTrue(rightBytecode.isSelected());
            assertNotSame(left.getChildren().get(0), right.getChildren().get(0));
            rightBytecode.fire();
            assertTrue(rightBytecode.isSelected());

            rightCode.fire();
            splitView.fire();
            assertEquals(1, split.getItems().size());
            assertEquals(EditorTab.ViewMode.BYTECODE, tab.getViewMode());
            splitView.fire();
            assertTrue(rightBytecode.isSelected(), "Reopening split defaults to Bytecode");
            assertTrue(bytecode.isSelected(), "Primary view is preserved");

            ToggleButton rightAst = (ToggleButton) rightBar.getChildren().get(2);
            rightAst.fire();
            assertEquals(EditorTab.ViewMode.AST, tab.getSecondaryViewMode());
            assertTrue(splitView.isSelected());
            ast.fire();
            assertEquals(EditorTab.ViewMode.AST, tab.getPrimaryViewMode());
            assertTrue(rightAst.isSelected());
            assertNotSame(left.getChildren().get(0), right.getChildren().get(0), "Both AST panes need independent trees");
            assertNull(tab.getActiveEditorView(), "AST is not a text editor");
            code.fire();
            assertSame(tab.getLuaEditorView(), tab.getActiveEditorView());
        });
    }

    @Test
    void loadsAstOnDemandFromCachedSourceWithoutReplacingAnalysisText() throws Exception {
        com.github.relua.model.LuacFile input = new LuacParser().parse("src/test/resources/xiaomi/aeslua.lua");
        com.github.relua.decompiler.Decompiler decompiler = new com.github.relua.decompiler.Decompiler();
        String cached = decompiler.decompile(input) + "\n-- preserved analysis snapshot\n";
        java.util.Map<String, com.github.relua.model.LineRange> ranges = new java.util.HashMap<>(decompiler.getChunkLineRanges());
        CountDownLatch ready = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<EditorTab> reference = new java.util.concurrent.atomic.AtomicReference<>();
        runOnFxThread(() -> {
            EditorTab tab = new EditorTab(new File("cached.lua"), new TextEditorView(false));
            reference.set(tab);
            tab.setLuacFile(input);
            tab.setChunkLineRanges(ranges);
            tab.getLuaEditorView().setText(cached);
            assertNull(tab.getDecompilerResult());
            tab.setSplitView(true);
            AstTreeView ast = tab.getAstTreeView();
            javafx.scene.control.ComboBox<?> functions = (javafx.scene.control.ComboBox<?>)ast.getView().lookup("#ast-functions");
            functions.valueProperty().addListener((obs, old, value) -> { if (value != null) ready.countDown(); });
            tab.setSecondaryViewMode(EditorTab.ViewMode.AST);
            assertSame(tab.getLuaEditorView(), tab.getActiveEditorView());
        });
        assertTrue(ready.await(15, TimeUnit.SECONDS), "AST should finish loading in the background");
        runOnFxThread(() -> {
            EditorTab tab = reference.get();
            assertNotNull(tab.getDecompilerResult());
            assertNotNull(tab.getAstTreeView().getCurrentFunction());
            assertEquals(cached, tab.getLuaEditorView().getText());
            assertEquals(EditorTab.ViewMode.AST, tab.getSecondaryViewMode());
            tab.setPrimaryViewMode(EditorTab.ViewMode.AST);
            assertNull(tab.getActiveEditorView());
            tab.cancelTask();
        });
    }

    @Test
    @DisplayName("两侧显示相同内容时使用独立编辑器，源码修改和分析记录保持同步")
    public void testMatchingViewsShareContentAndAnalysis() throws Exception {
        runOnFxThread(() -> {
            TextEditorView source = new TextEditorView(false);
            EditorTab tab = new EditorTab(new File("mirrors.lua"), source);
            source.setText("local value = 1\nprint(value)\n");
            tab.setSplitView(true);
            tab.setSecondaryViewMode(EditorTab.ViewMode.LUA_SOURCE);
            TextEditorView rightSource = tab.getActiveEditorView();
            assertNotSame(source, rightSource);
            assertEquals(source.getText(), rightSource.getText());
            rightSource.renameVariable("value", "renamed");
            assertEquals(rightSource.getText(), source.getText());
            assertEquals("renamed", tab.getRenamedVariables().get("value"));
            source.setText("return renamed\n");
            assertEquals(source.getText(), rightSource.getText());

            tab.setPrimaryViewMode(EditorTab.ViewMode.BYTECODE);
            tab.setSecondaryViewMode(EditorTab.ViewMode.BYTECODE);
            TextEditorView rightBytecode = tab.getActiveEditorView();
            assertNotSame(tab.getBytecodeEditorView(), rightBytecode);
            tab.getBytecodeEditorView().setText("   0 [line    1]: RETURN R0 1\n");
            assertEquals(tab.getBytecodeEditorView().getText(), rightBytecode.getText());
        });
    }

    @Test
    @DisplayName("源码无字节码时显示说明，加载真实字节码后显示指令")
    public void testBytecodeAvailabilityAfterLoading() throws Exception {
        runOnFxThread(() -> {
            try {
                EditorTab tab = new EditorTab(new File("source.lua"), new TextEditorView(false));
                tab.setSplitView(true);
                tab.setLuacFile(null);
                VBox content = (VBox) ((StackPane) tab.getContent()).getChildren().get(0);
                SplitPane split = (SplitPane) content.getChildren().get(0);
                VBox right = (VBox) split.getItems().get(1);
                assertTrue(right.getChildren().get(0).getStyleClass().contains("bytecode-unavailable"));
                tab.setLuacFile(new LuacParser().parse("src/test/resources/xiaomi/aeslua.lua"));
                assertSame(tab.getBytecodeEditorView().getView(), right.getChildren().get(0));
                assertTrue(tab.getBytecodeEditorView().getText().contains("; 指令列表"));
            } catch (Exception e) { throw new RuntimeException(e); }
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
