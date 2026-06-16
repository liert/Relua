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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.concurrent.Task;

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

    // 加载动画及提示覆盖层
    private StackPane textEditorStackPane;
    private VBox loadingOverlay;
    private ProgressIndicator progressIndicator;
    private Label loadingLabel;
    private Label warningLabel;
    private Task<DecompileResult> activeTask;
    private int currentTaskId = 0;

    private static class DecompileResult {
        final String luaCode;
        final LuacFile luacFile;
        final java.util.Map<String, com.github.relua.model.LineRange> chunkLineRanges;
        final boolean isText;
        
        DecompileResult(String luaCode, LuacFile luacFile, java.util.Map<String, com.github.relua.model.LineRange> chunkLineRanges, boolean isText) {
            this.luaCode = luaCode;
            this.luacFile = luacFile;
            this.chunkLineRanges = chunkLineRanges;
            this.isText = isText;
        }
    }

    /**
     * 构造函数
     * @param textEditorView 文本编辑器视图
     * @param astGraphConverter AST到图形的转换器
     * @param cfgGraphConverter CFG到图形的转换器
     * @param i18nService 国际化服务
     * @param statusLabel 状态栏标签
     * @param fileLabel 文件标签
     * @param fileTreeView 文件树视图
     * @param textEditorStackPane 文本编辑器StackPane容器
     */
    public FileMenuHandler(TextEditorView textEditorView, ASTGraphConverter astGraphConverter, CFGGraphConverter cfgGraphConverter, I18nService i18nService, Label statusLabel, Label fileLabel, FileTreeView fileTreeView, StackPane textEditorStackPane) {
        this.textEditorView = textEditorView;
        this.astGraphConverter = astGraphConverter;
        this.cfgGraphConverter = cfgGraphConverter;
        this.i18nService = i18nService;
        this.statusLabel = statusLabel;
        this.fileLabel = fileLabel;
        this.fileTreeView = fileTreeView;
        this.textEditorStackPane = textEditorStackPane;

        // 初始化加载状态覆盖层
        initLoadingOverlay();
    }

    private void initLoadingOverlay() {
        if (textEditorStackPane == null) {
            return;
        }

        loadingOverlay = new VBox();
        loadingOverlay.setAlignment(javafx.geometry.Pos.CENTER);
        loadingOverlay.setSpacing(15);
        loadingOverlay.setStyle("-fx-background-color: rgba(255, 255, 255, 0.85);");

        progressIndicator = new ProgressIndicator();
        progressIndicator.setMinSize(50, 50);

        loadingLabel = new Label("正在反编译，请稍候...");
        loadingLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        warningLabel = new Label("");
        warningLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");

        loadingOverlay.getChildren().addAll(progressIndicator, loadingLabel, warningLabel);
        loadingOverlay.setVisible(false);

        textEditorStackPane.getChildren().add(loadingOverlay);
    }

    /**
     * 重置加载状态，取消所有运行中的任务并隐藏遮罩层
     */
    public void resetLoadingState() {
        if (activeTask != null && activeTask.isRunning()) {
            activeTask.cancel();
        }
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(false);
        }
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
                && textEditorView.getText() != null && !textEditorView.getText().isEmpty()
                && (activeTask == null || !activeTask.isRunning())) {
            updateStatus("File already open: " + file.getName());
            if (fileTreeView != null) {
                fileTreeView.selectFile(file);
            }
            return;
        }

        // 取消旧任务
        if (activeTask != null && activeTask.isRunning()) {
            activeTask.cancel();
        }

        currentFile = file;
        updateFileLabel(file.getName());
        updateStatus("Opening file...");

        // 添加单个文件或在文件树中高亮当前选中的节点
        if (fileTreeView != null) {
            fileTreeView.addFile(file);
            fileTreeView.selectFile(file);
        }

        final int taskId = ++currentTaskId;

        if (loadingOverlay != null) {
            loadingOverlay.setVisible(true);
            loadingLabel.setText("正在反编译，请稍候...");
            warningLabel.setText("");
        }

        // 如果文件处理耗时超过 3 秒，显示额外提示信息
        Timeline warningTimeline = new Timeline(
            new KeyFrame(Duration.seconds(3), e -> {
                if (taskId == currentTaskId && loadingOverlay != null && loadingOverlay.isVisible()) {
                    warningLabel.setText("文件较大，处理时间可能较长，请耐心等待...");
                }
            })
        );
        warningTimeline.play();

        // 创建异步加载与反编译 Task
        activeTask = new Task<DecompileResult>() {
            @Override
            protected DecompileResult call() throws Exception {
                // 阶段一：解析字节码
                updateMessage("正在解析字节码...");
                LuacParser parser = new LuacParser();
                
                if (isCancelled()) return null;

                LuacFile luacFile = null;
                try {
                    luacFile = parser.parse(file.getAbsolutePath());
                } catch (Exception e) {
                    // 解析失败时尝试作为普通文本文件读取
                    if (isCancelled()) return null;
                    if (isTextFile(file)) {
                        updateMessage("正在读取文本文件...");
                        String textContent = readTextFileContent(file);
                        return new DecompileResult(textContent, null, new java.util.HashMap<>(), true);
                    } else {
                        throw e;
                    }
                }

                if (isCancelled()) return null;

                // 阶段二：进行反编译并生成 Lua 代码
                updateMessage("正在生成 Lua 代码...");
                Decompiler decompiler = new Decompiler();
                String luaCode = decompiler.decompile(luacFile, false);
                java.util.Map<String, com.github.relua.model.LineRange> lineRanges = decompiler.getChunkLineRanges();

                if (isCancelled()) return null;

                return new DecompileResult(luaCode, luacFile, lineRanges, false);
            }

            @Override
            protected void succeeded() {
                warningTimeline.stop();
                if (taskId != currentTaskId) {
                    return;
                }

                if (loadingOverlay != null) {
                    loadingOverlay.setVisible(false);
                }

                DecompileResult result = getValue();
                if (result != null) {
                    currentLuacFile = result.luacFile;
                    chunkLineRanges = result.chunkLineRanges;

                    // 显示反编译结果
                    textEditorView.setText(result.luaCode);

                    if (result.isText) {
                        updateStatus("Opened text file: " + file.getName());
                    } else {
                        updateStatus("File opened successfully");
                    }
                }
            }

            @Override
            protected void failed() {
                warningTimeline.stop();
                if (taskId != currentTaskId) {
                    return;
                }

                if (loadingOverlay != null) {
                    loadingOverlay.setVisible(false);
                }

                Throwable e = getException();
                updateStatus("Unsupported file format: " + file.getName());
                showError(i18nService.getErrorDialogTitle(), "不支持的文件格式: " + e.getMessage());
            }

            @Override
            protected void cancelled() {
                warningTimeline.stop();
            }
        };

        if (loadingOverlay != null) {
            loadingLabel.textProperty().bind(activeTask.messageProperty());
        }

        // 启动后台线程执行任务
        Thread thread = new Thread(activeTask);
        thread.setDaemon(true);
        thread.start();
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