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
    private java.util.Map<String, com.github.relua.model.LineRange> chunkLineRanges = new java.util.HashMap<>();

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
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            openFile(file);
        }
    }
    
    /**
     * 直接打开指定文件
     * @param file 文件对象
     */
    public void openFile(File file) {
        if (file == null) {
            return;
        }

        // 如果当前已经打开了该文件，直接忽略，避免重复反编译和高亮失效
        if (currentFile != null && currentFile.getAbsolutePath().equals(file.getAbsolutePath()) 
                && textEditorView.getText() != null && !textEditorView.getText().isEmpty()) {
            updateStatus("File already open: " + file.getName());
            if (fileTreeView != null) {
                fileTreeView.selectFile(file);
            }
            return;
        }

        currentFile = file;
        updateFileLabel(file.getName());
        updateStatus("Opening file...");

        // 添加单个文件或在文件树中高亮当前选中的节点
        if (fileTreeView != null) {
            fileTreeView.addFile(file);
            fileTreeView.selectFile(file);
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
                this.chunkLineRanges = decompiler.getChunkLineRanges();

                // 显示反编译结果
                textEditorView.setText(luaCode);

                // 仅加载代码视图，跳过AST和CFG转换（按需加载）
                updateStatus("File opened successfully");
            } catch (Exception e) {
                // 如果解析失败，判断是否为文本文件。是则正常打开，否则提示不支持的文件
                if (isTextFile(file)) {
                    try {
                        updateStatus("Reading text file...");
                        String textContent = readTextFileContent(file);
                        this.currentLuacFile = null;
                        this.chunkLineRanges = new java.util.HashMap<>();
                        textEditorView.setText(textContent);
                        updateStatus("Opened text file: " + file.getName());
                    } catch (Exception textEx) {
                        updateStatus("Error reading text file: " + textEx.getMessage());
                        showError(i18nService.getErrorDialogTitle(), "无法读取文本文件: " + textEx.getMessage());
                    }
                } else {
                    updateStatus("Unsupported file format: " + file.getName());
                    showError(i18nService.getErrorDialogTitle(), "不支持的文件格式: 该文件既不是文本文件，也无法识别为有效的 Lua 字节码");
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

    /**
     * 获取各个Chunk对应的代码行区间映射
     * @return 代码行区间映射
     */
    public java.util.Map<String, com.github.relua.model.LineRange> getChunkLineRanges() {
        return chunkLineRanges;
    }

    private boolean isTextFile(File file) {
        if (!file.exists() || file.isDirectory()) {
            return false;
        }
        if (file.length() > 10 * 1024 * 1024) { // 限制最大为 10MB
            return false;
        }
        try (java.io.BufferedInputStream in = new java.io.BufferedInputStream(new java.io.FileInputStream(file))) {
            byte[] buffer = new byte[1024];
            int read = in.read(buffer, 0, buffer.length);
            if (read == -1) {
                return true;
            }
            for (int i = 0; i < read; i++) {
                if (buffer[i] == 0) { // 如果前 1KB 含有 0 字节，判定为二进制
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String readTextFileContent(File file) throws IOException {
        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}