package com.github.relua.gui.handlers;

import com.github.relua.gui.services.I18nService;
import com.github.relua.gui.views.TextEditorView;
import com.github.relua.gui.views.FileTreeView;
import com.github.relua.gui.utils.ASTGraphConverter;
import com.github.relua.gui.utils.CFGGraphConverter;
import com.github.relua.decompiler.Decompiler;
import com.github.relua.parser.LuacParser;
import com.github.relua.model.LuacFile;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

/**
 * 文件菜单处理器，处理文件菜单的交互逻辑
 */
public class FileMenuHandler {
    private TextEditorView textEditorView;
    private ASTGraphConverter astGraphConverter;
    private CFGGraphConverter cfgGraphConverter;
    private I18nService i18nService;
    private Label statusLabel;
    private Label fileLabel;
    private FileTreeView fileTreeView;
    private File currentFile;
    private LuacFile currentLuacFile;

    /**
     * 构造函数
     * @param textEditorView 文本编辑器视图
     * @param astGraphConverter AST到图形的转换器
     * @param cfgGraphConverter CFG到图形的转换器
     * @param i18nService 国际化服务
     * @param statusLabel 状态栏标签
     * @param fileLabel 文件标签
     * @param fileTreeView 文件树视图
     */
    public FileMenuHandler(TextEditorView textEditorView, ASTGraphConverter astGraphConverter, CFGGraphConverter cfgGraphConverter, I18nService i18nService, Label statusLabel, Label fileLabel, FileTreeView fileTreeView) {
        this.textEditorView = textEditorView;
        this.astGraphConverter = astGraphConverter;
        this.cfgGraphConverter = cfgGraphConverter;
        this.i18nService = i18nService;
        this.statusLabel = statusLabel;
        this.fileLabel = fileLabel;
        this.fileTreeView = fileTreeView;
    }

    /**
     * 处理打开文件夹事件
     * @param event 事件对象
     */
    public void handleOpenFolder(ActionEvent event) {
        // 该方法暂时保留在MainController中，因为它与文件树视图紧密相关
    }

    /**
     * 处理打开文件事件
     * @param event 事件对象
     */
    public void handleOpenFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Luac File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Luac Files", "*.luac"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            currentFile = file;
            updateFileLabel(file.getName());
            updateStatus("Opening file...");

            // 添加单个文件到文件树中
            if (fileTreeView != null) {
                fileTreeView.addFile(file);
            }

            // 调用反编译功能并显示结果
            try {
                // 创建解析器和反编译器
                LuacParser parser = new LuacParser();
                Decompiler decompiler = new Decompiler();

                // 解析Luac文件
                updateStatus("Parsing file...");
                LuacFile luacFile = parser.parse(file.getAbsolutePath());
                this.currentLuacFile = luacFile;

                // 反编译
                updateStatus("Decompiling...");
                String luaCode = decompiler.decompile(luacFile, false);

                // 显示反编译结果
                textEditorView.setText(luaCode);

                // 可视化AST
                astGraphConverter.convertToGraph(luacFile);

                updateStatus("File opened successfully");
            } catch (Exception e) {
                updateStatus("Error opening file: " + e.getMessage());
                showError(i18nService.getErrorDialogTitle(), i18nService.getFileOpenFailedMessage(e.getMessage()));
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理保存文件事件
     * @param event 事件对象
     */
    public void handleSaveFile(ActionEvent event) {
        if (currentFile != null) {
            saveToFile(currentFile);
        } else {
            handleSaveAsFile(event);
        }
    }

    /**
     * 处理另存为文件事件
     * @param event 事件对象
     */
    public void handleSaveAsFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Lua File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Lua Files", "*.lua"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showSaveDialog(new Stage());
        if (file != null) {
            saveToFile(file);
        }
    }

    /**
     * 将文本保存到文件
     * @param file 文件对象
     */
    private void saveToFile(File file) {
        try {
            textEditorView.saveToFile(file);
            currentFile = file;
            updateFileLabel(file.getName());
            updateStatus("File saved successfully");
        } catch (IOException e) {
            updateStatus("Error saving file: " + e.getMessage());
            showError(i18nService.getErrorDialogTitle(), i18nService.getFileSaveFailedMessage(e.getMessage()));
        }
    }

    /**
     * 更新状态栏
     * @param status 状态信息
     */
    private void updateStatus(String status) {
        statusLabel.setText(status);
    }

    /**
     * 更新文件标签
     * @param fileName 文件名
     */
    private void updateFileLabel(String fileName) {
        fileLabel.setText(fileName);
    }

    /**
     * 显示错误对话框
     * @param title 对话框标题
     * @param message 错误消息
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 获取当前打开的文件
     * @return 当前打开的文件
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * 设置当前打开的文件
     * @param currentFile 当前打开的文件
     */
    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }
    
    /**
     * 获取当前打开的LuacFile对象
     * @return 当前打开的LuacFile对象
     */
    public LuacFile getCurrentLuacFile() {
        return currentLuacFile;
    }
}