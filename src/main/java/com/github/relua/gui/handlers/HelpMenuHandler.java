package com.github.relua.gui.handlers;

import com.github.relua.gui.utils.I18nUtil;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;

/**
 * 帮助菜单处理器，处理帮助菜单的交互逻辑
 */
public class HelpMenuHandler {

    /**
     * 构造函数
     */
    public HelpMenuHandler() {
    }

    /**
     * 处理关于事件
     * @param event 事件对象
     */
    public void handleAbout(ActionEvent event) {
        showInfo(I18nUtil.getString("dialog.about.title"), I18nUtil.getString("dialog.about.content"));
    }

    /**
     * 显示信息对话框
     * @param title 对话框标题
     * @param message 信息消息
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}