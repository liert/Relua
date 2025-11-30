package com.github.relua.gui.handlers;

import com.github.relua.gui.services.I18nService;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;

/**
 * 帮助菜单处理器，处理帮助菜单的交互逻辑
 */
public class HelpMenuHandler {
    private I18nService i18nService;

    /**
     * 构造函数
     * @param i18nService 国际化服务
     */
    public HelpMenuHandler(I18nService i18nService) {
        this.i18nService = i18nService;
    }

    /**
     * 处理关于事件
     * @param event 事件对象
     */
    public void handleAbout(ActionEvent event) {
        showInfo(i18nService.getAboutDialogTitle(), i18nService.getAboutDialogContent());
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