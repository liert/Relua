package com.github.relua.gui.handlers;

import com.github.relua.gui.utils.I18nUtil;
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

import com.github.relua.decompiler.pipeline.DecompilerResult;
import com.github.relua.model.LineRange;
import com.github.relua.gui.workspace.FileMetadata;
import com.github.relua.gui.workspace.ProjectWorkspaceManager;
import com.github.relua.gui.workspace.RecentHistoryManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 文件菜单处理器，处理文件菜单的交互逻辑与工程持久化调度
 */
public class FileMenuHandler {
    private TabPane textEditorTabPane;
    private ASTGraphConverter astGraphConverter;
    private CFGGraphConverter cfgGraphConverter;
    private Label statusLabel;
    private Label fileLabel;
    private FileTreeView fileTreeView;
    
    private int currentTaskId = 0;
    private Runnable onFileOpenedCallback;
    private Runnable onCaretPositionChangedCallback;
    private final Map<EditorTab, List<Consumer<EditorTab>>> pendingReadyCallbacks = new HashMap<>();

    private static class DecompileResult {
        final String luaCode;
        final LuacFile luacFile;
        final Map<String, LineRange> chunkLineRanges;
        final boolean isText;
        final DecompilerResult decompilerResult;
        
        DecompileResult(String luaCode, LuacFile luacFile, Map<String, LineRange> chunkLineRanges, boolean isText, DecompilerResult decompilerResult) {
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
     * @param statusLabel 状态栏标签
     * @param fileLabel 文件标签
     * @param fileTreeView 文件树视图
     */
    public FileMenuHandler(TabPane textEditorTabPane, ASTGraphConverter astGraphConverter, CFGGraphConverter cfgGraphConverter, Label statusLabel, Label fileLabel, FileTreeView fileTreeView) {
        this.textEditorTabPane = textEditorTabPane;
        this.astGraphConverter = astGraphConverter;
        this.cfgGraphConverter = cfgGraphConverter;
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
        pendingReadyCallbacks.clear();
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
        openFile(file, null);
    }

    /**
     * 直接打开指定文件，并在文件加载/反编译就绪后触发回调
     * @param file 文件对象
     * @param onReady 就绪回调 (可为 null)
     */
    public void openFile(File file, Consumer<EditorTab> onReady) {
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
                    RecentHistoryManager.getInstance().addRecentPath(file.getAbsolutePath());
                    if (onReady != null) {
                        if (editorTab.isFileLoaded()) onReady.accept(editorTab);
                        else pendingReadyCallbacks.computeIfAbsent(editorTab, key -> new ArrayList<>()).add(onReady);
                    }
                    return;
                }
            }
        }

        RecentHistoryManager.getInstance().addRecentPath(file.getAbsolutePath());

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
        
        // 关闭时只保存 .relua 分析信息，不写回原始文件。
        tab.setOnCloseRequest(e -> {
            tab.cancelTask();
            ProjectWorkspaceManager.getInstance().save(collectOpenedTabs(), tab);
            if (fileTreeView != null) fileTreeView.untrackEditor(tab);
        });

        // 注册光标与选区监听器以实现 AST/CFG 同步及状态栏位置更新
        tab.setOnCaretOrSelectionChanged(() -> {
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

                    // 自动应用历史持久化元数据（恢复变量重命名、行注释或用户编辑文本）
                    FileMetadata metadata = ProjectWorkspaceManager.getInstance().getFileMetadata(file);
                    if (metadata != null) {
                        if (metadata.getEditedText() != null && !metadata.getEditedText().isEmpty()) {
                            tab.getTextEditorView().setText(metadata.getEditedText());
                        } else {
                            if (!metadata.getRenamedVariables().isEmpty()) {
                                for (Map.Entry<String, String> entry : metadata.getRenamedVariables().entrySet()) {
                                    tab.getTextEditorView().renameVariable(entry.getKey(), entry.getValue());
                                }
                            }
                            if (!metadata.getLineComments().isEmpty()) {
                                for (Map.Entry<Integer, String> entry : metadata.getLineComments().entrySet()) {
                                    tab.getTextEditorView().applyLineComment(entry.getKey() - 1, entry.getValue());
                                }
                            }
                        }
                        tab.restoreAnalysisState(metadata.getRenamedVariables(), metadata.getLineComments());
                    }

                    if (result.isText) {
                         updateStatus("Opened text file: " + file.getName());
                    } else {
                         updateStatus("File opened successfully");
                    }

                    if (fileTreeView != null) fileTreeView.trackEditor(tab);
                    if (onFileOpenedCallback != null) {
                        onFileOpenedCallback.run();
                    }

                    if (onReady != null) {
                        onReady.accept(tab);
                    }
                    List<Consumer<EditorTab>> callbacks = pendingReadyCallbacks.remove(tab);
                    if (callbacks != null) {
                        for (Consumer<EditorTab> callback : callbacks) callback.accept(tab);
                    }
                }
            }

            @Override
            protected void failed() {
                pendingReadyCallbacks.remove(tab);
                warningTimeline.stop();
                tab.unbindTaskMessage();
                tab.hideLoading();

                Throwable e = getException();
                updateStatus("Unsupported file format: " + file.getName());
                showError(I18nUtil.getString("dialog.error.title"), "不支持的文件格式: " + e.getMessage());
                
                // 如果打开失败，则关闭该标签页
                textEditorTabPane.getTabs().remove(tab);
            }

            @Override
            protected void cancelled() {
                pendingReadyCallbacks.remove(tab);
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
     * 收集当前所有打开的 EditorTab 列表
     * @return EditorTab 列表
     */
    public List<EditorTab> collectOpenedTabs() {
        List<EditorTab> list = new ArrayList<>();
        for (Tab t : textEditorTabPane.getTabs()) {
            if (t instanceof EditorTab) {
                list.add((EditorTab) t);
            }
        }
        return list;
    }

    public void saveAnalysis() {
        ProjectWorkspaceManager.getInstance().save(collectOpenedTabs(), getActiveTab());
    }

    /**
     * 用户手动导出当前源码到独立文件。
     * @param event 事件对象
     */
    public void handleExportSource(ActionEvent event) {
        EditorTab activeTab = getActiveTab();
        if (activeTab == null) {
            return;
        }
        if (!activeTab.isFileLoaded()) {
            updateStatus("文件仍在加载，请加载完成后再导出");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出 Lua 源码");
        if (activeTab.getFile() != null) {
            String name = activeTab.getFile().getName();
            fileChooser.setInitialFileName(name.endsWith(".lua") ? name : name + ".lua");
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Lua Files", "*.lua"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showSaveDialog(textEditorTabPane.getScene() != null
                ? textEditorTabPane.getScene().getWindow() : null);
        if (file != null) {
            try {
                exportSource(file);
                updateStatus("源码已导出: " + file.getAbsolutePath());
            } catch (IOException e) {
                updateStatus("导出失败: " + e.getMessage());
                showError(I18nUtil.getString("dialog.error.title"),
                        I18nUtil.getString("dialog.error.fileExportFailed", e.getMessage()));
            }
        }
    }

    /**
     * 显式导出入口；所有已打开的源文件均不可作为导出目标。
     */
    public void exportSource(File file) throws IOException {
        EditorTab tab = getActiveTab();
        if (tab == null || !tab.isFileLoaded()) {
            throw new IOException("请先打开文件并等待加载完成");
        }
        for (EditorTab openedTab : collectOpenedTabs()) {
            File source = openedTab.getFile();
            if (source != null && (file.getCanonicalFile().equals(source.getCanonicalFile())
                    || (file.exists() && source.exists()
                    && java.nio.file.Files.isSameFile(file.toPath(), source.toPath())))) {
                throw new IOException("打开的文件为只读，请选择其他导出目标");
            }
        }
        tab.getTextEditorView().exportToFile(file);
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
    public DecompilerResult getCurrentDecompilerResult() {
        EditorTab activeTab = getActiveTab();
        return activeTab != null ? activeTab.getDecompilerResult() : null;
    }

    /**
     * 获取各个Chunk对应的代码行区间映射
     * @return 代码行区间映射
     */
    public Map<String, LineRange> getChunkLineRanges() {
        EditorTab activeTab = getActiveTab();
        return activeTab != null ? activeTab.getChunkLineRanges() : new HashMap<>();
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
