package com.github.relua.gui.handlers;

import com.github.relua.gui.views.TextEditorView;
import javafx.event.ActionEvent;

/**
 * 编辑菜单处理器，处理编辑菜单的交互逻辑
 */
public class EditMenuHandler {
    private final java.util.function.Supplier<TextEditorView> editorSupplier;

    /**
     * 构造函数
     * @param editorSupplier 文本编辑器视图的提供者
     */
    public EditMenuHandler(java.util.function.Supplier<TextEditorView> editorSupplier) {
        this.editorSupplier = editorSupplier;
    }

    /**
     * 处理撤销事件
     * @param event 事件对象
     */
    public void handleUndo(ActionEvent event) {
        TextEditorView textEditorView = editorSupplier.get();
        if (textEditorView != null) {
            textEditorView.undo();
        }
    }

    /**
     * 处理重做事件
     * @param event 事件对象
     */
    public void handleRedo(ActionEvent event) {
        TextEditorView textEditorView = editorSupplier.get();
        if (textEditorView != null) {
            textEditorView.redo();
        }
    }

    /**
     * 处理剪切事件
     * @param event 事件对象
     */
    public void handleCut(ActionEvent event) {
        TextEditorView textEditorView = editorSupplier.get();
        if (textEditorView != null) {
            textEditorView.cut();
        }
    }

    /**
     * 处理复制事件
     * @param event 事件对象
     */
    public void handleCopy(ActionEvent event) {
        TextEditorView textEditorView = editorSupplier.get();
        if (textEditorView != null) {
            textEditorView.copy();
        }
    }

    /**
     * 处理粘贴事件
     * @param event 事件对象
     */
    public void handlePaste(ActionEvent event) {
        TextEditorView textEditorView = editorSupplier.get();
        if (textEditorView != null) {
            textEditorView.paste();
        }
    }
}