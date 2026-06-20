package com.github.relua.gui.services;

import com.github.relua.gui.utils.I18nUtil;
import javafx.scene.control.*;

/**
 * 国际化服务类，负责处理应用程序的国际化相关功能
 */
public class I18nService {
    
    /**
     * 初始化菜单的国际化文本
     * @param menuFile 文件菜单
     * @param menuEdit 编辑菜单
     * @param menuView 视图菜单
     * @param menuHelp 帮助菜单
     */
    public void initializeMenus(Menu menuFile, Menu menuEdit, Menu menuView, Menu menuHelp) {
        menuFile.setText(I18nUtil.getString("menu.file"));
        menuEdit.setText(I18nUtil.getString("menu.edit"));
        menuView.setText(I18nUtil.getString("menu.view"));
        menuHelp.setText(I18nUtil.getString("menu.help"));
    }
    
    /**
     * 初始化文件菜单项的国际化文本
     * @param menuItemOpen 打开菜单项
     * @param menuItemOpenFolder 打开文件夹菜单项
     * @param menuItemSave 保存菜单项
     * @param menuItemSaveAs 另存为菜单项
     * @param menuItemExit 退出菜单项
     */
    public void initializeFileMenuItems(MenuItem menuItemOpen, MenuItem menuItemOpenFolder, MenuItem menuItemSave, MenuItem menuItemSaveAs, MenuItem menuItemExit) {
        menuItemOpen.setText(I18nUtil.getString("menu.file.open"));
        menuItemOpenFolder.setText(I18nUtil.getString("menu.file.openFolder"));
        menuItemSave.setText(I18nUtil.getString("menu.file.save"));
        menuItemSaveAs.setText(I18nUtil.getString("menu.file.saveAs"));
        menuItemExit.setText(I18nUtil.getString("menu.file.exit"));
    }
    
    /**
     * 初始化编辑菜单项的国际化文本
     * @param menuItemUndo 撤销菜单项
     * @param menuItemRedo 重做菜单项
     * @param menuItemCut 剪切菜单项
     * @param menuItemCopy 复制菜单项
     * @param menuItemPaste 粘贴菜单项
     */
    public void initializeEditMenuItems(MenuItem menuItemUndo, MenuItem menuItemRedo, MenuItem menuItemCut, MenuItem menuItemCopy, MenuItem menuItemPaste) {
        menuItemUndo.setText(I18nUtil.getString("menu.edit.undo"));
        menuItemRedo.setText(I18nUtil.getString("menu.edit.redo"));
        menuItemCut.setText(I18nUtil.getString("menu.edit.cut"));
        menuItemCopy.setText(I18nUtil.getString("menu.edit.copy"));
        menuItemPaste.setText(I18nUtil.getString("menu.edit.paste"));
    }
    
    /**
     * 初始化视图菜单项的国际化文本
     * @param menuItemZoomIn 放大菜单项
     * @param menuItemZoomOut 缩小菜单项
     * @param menuItemResetZoom 重置缩放菜单项
     */
    public void initializeViewMenuItems(MenuItem menuItemZoomIn, MenuItem menuItemZoomOut, MenuItem menuItemResetZoom) {
        menuItemZoomIn.setText(I18nUtil.getString("menu.view.zoomIn"));
        menuItemZoomOut.setText(I18nUtil.getString("menu.view.zoomOut"));
        menuItemResetZoom.setText(I18nUtil.getString("menu.view.resetZoom"));
    }
    
    /**
     * 初始化帮助菜单项的国际化文本
     * @param menuItemAbout 关于菜单项
     */
    public void initializeHelpMenuItems(MenuItem menuItemAbout) {
        menuItemAbout.setText(I18nUtil.getString("menu.help.about"));
    }
    
    /**
     * 初始化工具栏按钮的国际化文本
     * @param btnOpen 打开按钮
     * @param btnSave 保存按钮
     * @param btnUndo 撤销按钮
     * @param btnRedo 重做按钮
     * @param btnZoomIn 放大按钮
     * @param btnZoomOut 缩小按钮
     * @param btnResetZoom 重置缩放按钮
     */
    public void initializeToolbarButtons(Button btnOpen, Button btnSave, Button btnUndo, Button btnRedo, Button btnZoomIn, Button btnZoomOut, Button btnResetZoom) {
        btnOpen.setText(I18nUtil.getString("toolbar.open"));
        btnSave.setText(I18nUtil.getString("toolbar.save"));
        btnUndo.setText(I18nUtil.getString("toolbar.undo"));
        btnRedo.setText(I18nUtil.getString("toolbar.redo"));
        btnZoomIn.setText(I18nUtil.getString("toolbar.zoomIn"));
        btnZoomOut.setText(I18nUtil.getString("toolbar.zoomOut"));
        btnResetZoom.setText(I18nUtil.getString("toolbar.resetZoom"));
    }
    
    /**
     * 初始化标签的国际化文本
     * @param astGraphLabel AST图形标签
     * @param logLabel 日志标签
     * @param btnClearLog 清空日志按钮
     */
    public void initializeLabels(Label astGraphLabel, Label logLabel, Button btnClearLog) {
        astGraphLabel.setText(I18nUtil.getString("label.astGraph"));
        logLabel.setText(I18nUtil.getString("label.log"));
        btnClearLog.setText(I18nUtil.getString("btn.clearLog"));
    }
    
    /**
     * 获取初始状态文本
     * @return 初始状态文本
     */
    public String getInitialStatusText() {
        return I18nUtil.getString("status.ready");
    }
    
    /**
     * 获取初始文件标签文本
     * @return 初始文件标签文本
     */
    public String getInitialFileLabelText() {
        return I18nUtil.getString("status.noFileOpened");
    }
    
    /**
     * 获取错误对话框标题
     * @return 错误对话框标题
     */
    public String getErrorDialogTitle() {
        return I18nUtil.getString("dialog.error.title");
    }
    
    /**
     * 获取文件打开失败消息
     * @param message 错误消息
     * @return 文件打开失败消息
     */
    public String getFileOpenFailedMessage(String message) {
        return I18nUtil.getString("dialog.error.fileOpenFailed", message);
    }
    
    /**
     * 获取文件保存失败消息
     * @param message 错误消息
     * @return 文件保存失败消息
     */
    public String getFileSaveFailedMessage(String message) {
        return I18nUtil.getString("dialog.error.fileSaveFailed", message);
    }
    
    /**
     * 获取关于对话框标题
     * @return 关于对话框标题
     */
    public String getAboutDialogTitle() {
        return I18nUtil.getString("dialog.about.title");
    }
    
    /**
     * 获取关于对话框内容
     * @return 关于对话框内容
     */
    public String getAboutDialogContent() {
        return I18nUtil.getString("dialog.about.content");
    }
}