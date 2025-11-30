package com.github.relua.gui.handlers;

import com.github.relua.gui.views.TextEditorView;
import javafx.event.ActionEvent;

/**
 * 编辑菜单处理器，处理编辑菜单的交互逻辑
 */
public class EditMenuHandler {
    private TextEditorView textEditorView;

    /**
     * 构造函数
     * @param textEditorView 文本编辑器视图
     */
    public EditMenuHandler(TextEditorView textEditorView) {
        this.textEditorView = textEditorView;
    }

    /**
     * 处理撤销事件
     * @param event 事件对象
     */
    public void handleUndo(ActionEvent event) {
        textEditorView.undo();
    }

    /**
     * 处理重做事件
     * @param event 事件对象
     */
    public void handleRedo(ActionEvent event) {
        textEditorView.redo();
    }

    /**
     * 处理剪切事件
     * @param event 事件对象
     */
    public void handleCut(ActionEvent event) {
        textEditorView.cut();
    }

    /**
     * 处理复制事件
     * @param event 事件对象
     */
    public void handleCopy(ActionEvent event) {
        textEditorView.copy();
    }

    /**
     * 处理粘贴事件
     * @param event 事件对象
     */
    public void handlePaste(ActionEvent event) {
        textEditorView.paste();
    }
}