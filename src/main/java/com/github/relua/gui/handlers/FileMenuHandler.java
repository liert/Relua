package com.github.relua.gui.handlers;

import com.github.relua.gui.services.I18nService;
import com.github.relua.gui.views.TextEditorView;
import com.github.relua.gui.views.FileTreeView;
import com.github.relua.gui.utils.ASTGraphConverter;
import com.github.relua.gui.utils.CFGGraphConverter;
import com.github.relua.decompiler.Decompiler;
import com.github.relua.parser.LuacParser;
import com.github.relua.model.LuacFile;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import com.github.relua.gui.views.EditorTab;
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
    private TabPane textEditorTabPane;
    private ASTGraphConverter astGraphConverter;
    private CFGGraphConverter cfgGraphConverter;
    private I18nService i18nService;
    private Label statusLabel;
    private Label fileLabel;
    private FileTreeView fileTreeView;
    
    private int currentTaskId = 0;
    private Runnable onFileOpenedCallback;
    private Runnable onCaretPositionChangedCallback;

    private static class DecompileResult {
        final String luaCode;
        final LuacFile luacFile;
        final java.util.Map<String, com.github.relua.model.LineRange> chunkLineRanges;
        final boolean isText;
        final com.github.relua.decompiler.pipeline.DecompilerResult decompilerResult;
        
        DecompileResult(String luaCode, LuacFile luacFile, java.util.Map<String, com.github.relua.model.LineRange> chunkLineRanges, boolean isText, com.github.relua.decompiler.pipeline.DecompilerResult decompilerResult) {
            this.luaCode = luaCode;
            this.luacFile = luacFile;
            this.chunkLineRanges = chunkLineRanges;
            this.isText = isText;
            this.decompilerResult = decompilerResult;
        }
    }

    /**
     * 构造函数
     * @param textEditorTabPane 标签页容器
     * @param astGraphConverter AST到图形的转换器
     * @param cfgGraphConverter CFG到图形的转换器
     * @param i18nService 国际化服务
     * @param statusLabel 状态栏标签
     * @param fileLabel 文件标签
     * @param fileTreeView 文件树视图
     */
    public FileMenuHandler(TabPane textEditorTabPane, ASTGraphConverter astGraphConverter, CFGGraphConverter cfgGraphConverter, I18nService i18nService, Label statusLabel, Label fileLabel, FileTreeView fileTreeView) {
        this.textEditorTabPane = textEditorTabPane;
        this.astGraphConverter = astGraphConverter;
        this.cfgGraphConverter = cfgGraphConverter;
        this.i18nService = i18nService;
        this.statusLabel = statusLabel;
        this.fileLabel = fileLabel;
        this.fileTreeView = fileTreeView;
    }

    /**
     * 设置文件成功打开后的回调
     * @param callback 回调函数
     */
    public void setOnFileOpenedCallback(Runnable callback) {
        this.onFileOpenedCallback = callback;
    }

    /**
     * 设置光标位置改变后的回调
     * @param callback 回调函数
     */
    public void setOnCaretPositionChangedCallback(Runnable callback) {
        this.onCaretPositionChangedCallback = callback;
    }

    /**
     * 重置加载状态，关闭所有打开的标签页并取消所有运行中的任务
     */
    public void resetLoadingState() {
        for (Tab tab : textEditorTabPane.getTabs()) {
            if (tab instanceof EditorTab) {
                ((EditorTab) tab).cancelTask();
            }
        }
        textEditorTabPane.getTabs().clear();
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

        // 检查是否已经打开了该文件，如果是则切换到对应标签页
        for (Tab tab : textEditorTabPane.getTabs()) {
            if (tab instanceof EditorTab) {
                EditorTab editorTab = (EditorTab) tab;
                if (editorTab.getFile().getAbsolutePath().equals(file.getAbsolutePath())) {
                    textEditorTabPane.getSelectionModel().select(editorTab);
                    updateStatus("File already open: " + file.getName());
                    if (fileTreeView != null) {
                        fileTreeView.selectFile(file);
                    }
                    return;
                }
            }
        }

        // 创建新的文本编辑器和标签页
        TextEditorView textEditorView = new TextEditorView(false);
        textEditorView.initCodeIntelligence(statusLabel);
        if (textEditorView.getCodeIntelligenceController() != null) {
            textEditorView.getCodeIntelligenceController().setRootFolderSupplier(() -> {
                return fileTreeView != null ? fileTreeView.getRootFolder() : null;
            });
            textEditorView.getCodeIntelligenceController().setFileNavigationHandler(targetFile -> {
                openFile(targetFile);
            });
        }
        
        EditorTab tab = new EditorTab(file, textEditorView);
        
        // 注册关闭事件，取消运行中的任务
        tab.setOnCloseRequest(e -> {
            tab.cancelTask();
        });

        // 注册光标监听器以实现 AST/CFG 同步
        textEditorView.getCodeArea().caretPositionProperty().addListener((obs, oldVal, newVal) -> {
            if (textEditorTabPane.getSelectionModel().getSelectedItem() == tab) {
                if (onCaretPositionChangedCallback != null) {
                    onCaretPositionChangedCallback.run();
                }
            }
        });

        textEditorTabPane.getTabs().add(tab);
        textEditorTabPane.getSelectionModel().select(tab);

        if (fileTreeView != null) {
            fileTreeView.addFile(file);
            fileTreeView.selectFile(file);
        }

        updateFileLabel(file.getName());
        updateStatus("Opening file...");

        tab.showLoading("正在反编译，请稍候...");

        final int taskId = ++currentTaskId;

        // 如果文件处理耗时超过 3 秒，显示额外提示信息
        Timeline warningTimeline = new Timeline(
            new KeyFrame(Duration.seconds(3), e -> {
                if (tab.getTabPane() != null && textEditorTabPane.getSelectionModel().getSelectedItem() == tab) {
                    tab.setWarningText("文件较大，处理时间可能较长，请耐心等待...");
                }
            })
        );
        warningTimeline.play();

        // 创建异步加载与反编译 Task
        Task<DecompileResult> task = new Task<DecompileResult>() {
            @Override
            protected DecompileResult call() throws Exception {
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
                        return new DecompileResult(textContent, null, new java.util.HashMap<>(), true, null);
                    } else {
                        throw e;
                    }
                }

                if (isCancelled()) return null;

                // 进行反编译并生成 Lua 代码
                updateMessage("正在生成 Lua 代码...");
                Decompiler decompiler = new Decompiler();
                String luaCode = decompiler.decompile(luacFile, false);
                java.util.Map<String, com.github.relua.model.LineRange> lineRanges = decompiler.getChunkLineRanges();

                if (isCancelled()) return null;

                return new DecompileResult(luaCode, luacFile, lineRanges, false, decompiler.getLastResult());
            }

            @Override
            protected void succeeded() {
                warningTimeline.stop();
                tab.unbindTaskMessage();
                tab.hideLoading();

                DecompileResult result = getValue();
                if (result != null) {
                    tab.setLuacFile(result.luacFile);
                    tab.setChunkLineRanges(result.chunkLineRanges);
                    tab.setDecompilerResult(result.decompilerResult);

                    // 显示反编译结果
                    tab.getTextEditorView().setText(result.luaCode);

                    if (result.isText) {
                         updateStatus("Opened text file: " + file.getName());
                    } else {
                         updateStatus("File opened successfully");
                    }

                    if (onFileOpenedCallback != null) {
                        onFileOpenedCallback.run();
                    }
                }
            }

            @Override
            protected void failed() {
                warningTimeline.stop();
                tab.unbindTaskMessage();
                tab.hideLoading();

                Throwable e = getException();
                updateStatus("Unsupported file format: " + file.getName());
                showError(i18nService.getErrorDialogTitle(), "不支持的文件格式: " + e.getMessage());
                
                // 如果打开失败，则关闭该标签页
                textEditorTabPane.getTabs().remove(tab);
            }

            @Override
            protected void cancelled() {
                warningTimeline.stop();
                tab.unbindTaskMessage();
            }
        };

        tab.bindTaskMessage(task);
        tab.setActiveTask(task);

        // 启动后台线程执行任务
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 处理保存文件事件
     * @param event 事件对象
     */
    public void handleSaveFile(ActionEvent event) {
        EditorTab activeTab = getActiveTab();
        if (activeTab != null) {
            saveToFile(activeTab.getFile(), activeTab);
        } else {
            handleSaveAsFile(event);
        }
    }

    /**
     * 处理另存为文件事件
     * @param event 事件对象
     */
    public void handleSaveAsFile(ActionEvent event) {
        EditorTab activeTab = getActiveTab();
        if (activeTab == null) {
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Lua File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Lua Files", "*.lua"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showSaveDialog(new Stage());
        if (file != null) {
            saveToFile(file, activeTab);
        }
    }

    /**
     * 将文本保存到文件
     * @param file 文件对象
     * @param tab 标签页对象
     */
    private void saveToFile(File file, EditorTab tab) {
        try {
            tab.getTextEditorView().saveToFile(file);
            tab.setFile(file);
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
     * 获取当前激活的标签页
     * @return 当前激活的标签页，如果没有则为 null
     */
    public EditorTab getActiveTab() {
        Tab selectedTab = textEditorTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab instanceof EditorTab) {
            return (EditorTab) selectedTab;
        }
        return null;
    }

    /**
     * 获取当前打开的文件
     * @return 当前打开的文件
     */
    public File getCurrentFile() {
        EditorTab activeTab = getActiveTab();
        return activeTab != null ? activeTab.getFile() : null;
    }

    /**
     * 设置当前打开的文件
     * @param currentFile 当前打开的文件
     */
    public void setCurrentFile(File currentFile) {
        EditorTab activeTab = getActiveTab();
        if (activeTab != null) {
            activeTab.setFile(currentFile);
        }
    }
    
    /**
     * 获取当前打开的LuacFile对象
     * @return 当前打开的LuacFile对象
     */
    public LuacFile getCurrentLuacFile() {
        EditorTab activeTab = getActiveTab();
        return activeTab != null ? activeTab.getLuacFile() : null;
    }

    /**
     * 获取当前反编译结果对象
     * @return 当前反编译结果对象
     */
    public com.github.relua.decompiler.pipeline.DecompilerResult getCurrentDecompilerResult() {
        EditorTab activeTab = getActiveTab();
        return activeTab != null ? activeTab.getDecompilerResult() : null;
    }

    /**
     * 获取各个Chunk对应的代码行区间映射
     * @return 代码行区间映射
     */
    public java.util.Map<String, com.github.relua.model.LineRange> getChunkLineRanges() {
        EditorTab activeTab = getActiveTab();
        return activeTab != null ? activeTab.getChunkLineRanges() : new java.util.HashMap<>();
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