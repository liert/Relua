package com.github.relua.gui.views;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TextEditorViewTest {

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

    private Button findFoldButton(Node graphic) {
        if (graphic instanceof HBox) {
            HBox hbox = (HBox) graphic;
            for (Node child : hbox.getChildren()) {
                if (child instanceof Button && child.getStyleClass().contains("fold-marker")) {
                    return (Button) child;
                }
            }
        }
        return null;
    }

    @Test
    public void testClickFoldAndUnfold() throws Exception {
        runOnFxThread(() -> {
            TextEditorView editor = new TextEditorView(false);
            String code = "function add(a, b)\n" +
                          "    local sum = a + b\n" +
                          "    return sum\n" +
                          "end\n" +
                          "\n" +
                          "function sub(a, b)\n" +
                          "    return a - b\n" +
                          "end";
            editor.setText(code);

            Stage stage = new Stage();
            Scene scene = new Scene(new StackPane(editor.getView()), 600, 400);
            stage.setScene(scene);
            stage.show();

            editor.getCodeArea().applyCss();
            editor.getCodeArea().layout();

            // 1. 验证初始状态：行 0 有展开按钮 ▾
            Node graphic0 = editor.getCodeArea().getParagraphGraphic(0);
            assertNotNull(graphic0, "行 0 应该有行号图形");
            Button foldBtn0 = findFoldButton(graphic0);
            assertNotNull(foldBtn0, "行 0 应该有折叠按钮");
            assertEquals("▾", foldBtn0.getText(), "初始状态应为展开 (▾)");

            // 验证第 1 行初始高度大于 0
            double p1HeightBefore = editor.getCodeArea().getParagraphBoundsOnScreen(1).get().getHeight();
            assertTrue(p1HeightBefore > 0, "折叠前第 1 行高度应大于 0");
            double p4MinYBefore = editor.getCodeArea().getParagraphBoundsOnScreen(4).get().getMinY();

            // 2. 模拟用户点击折叠按钮
            foldBtn0.fire();
            editor.getCodeArea().applyCss();
            editor.getCodeArea().layout();

            // 验证行 0 按钮文本变为 ▸
            Node graphic0Folded = editor.getCodeArea().getParagraphGraphic(0);
            Button foldBtn0Folded = findFoldButton(graphic0Folded);
            assertNotNull(foldBtn0Folded);
            assertEquals("▸", foldBtn0Folded.getText(), "折叠后状态应为收起 (▸)");

            // 验证第 1、2、3 行样式包含 collapse
            assertTrue(editor.getCodeArea().getParagraph(1).getParagraphStyle().contains("collapse"));
            assertTrue(editor.getCodeArea().getParagraph(2).getParagraphStyle().contains("collapse"));
            assertTrue(editor.getCodeArea().getParagraph(3).getParagraphStyle().contains("collapse"));

            // 验证第 1 行高度归零 (完全折叠不占视觉高度)
            assertEquals(0.0, editor.getCodeArea().getParagraphBoundsOnScreen(1).get().getHeight(), 0.001);

            // 验证第 1 行行号区域不可见
            Node graphic1Folded = editor.getCodeArea().getParagraphGraphic(1);
            assertFalse(graphic1Folded.isVisible(), "折叠行的行号节点应为不可见");

            // 验证第 4 行向上移动
            double p4MinYFolded = editor.getCodeArea().getParagraphBoundsOnScreen(4).get().getMinY();
            assertTrue(p4MinYFolded < p4MinYBefore, "折叠后第 4 行应向上平移紧贴函数头");

            // 3. 再次点击折叠按钮展开
            foldBtn0Folded.fire();
            editor.getCodeArea().applyCss();
            editor.getCodeArea().layout();

            // 验证行 0 按钮还原为 ▾
            Node graphic0Unfolded = editor.getCodeArea().getParagraphGraphic(0);
            Button foldBtn0Unfolded = findFoldButton(graphic0Unfolded);
            assertNotNull(foldBtn0Unfolded);
            assertEquals("▾", foldBtn0Unfolded.getText(), "再次点击后应恢复为展开 (▾)");

            // 验证第 1、2、3 行不再包含 collapse
            assertFalse(editor.getCodeArea().getParagraph(1).getParagraphStyle().contains("collapse"));
            assertFalse(editor.getCodeArea().getParagraph(2).getParagraphStyle().contains("collapse"));
            assertFalse(editor.getCodeArea().getParagraph(3).getParagraphStyle().contains("collapse"));

            // 验证第 1 行高度与行号完全恢复
            assertTrue(editor.getCodeArea().getParagraphBoundsOnScreen(1).get().getHeight() > 0);
            Node graphic1Unfolded = editor.getCodeArea().getParagraphGraphic(1);
            assertTrue(graphic1Unfolded.isVisible(), "展开后行号应重新可见");

            stage.close();
        });
    }
}
