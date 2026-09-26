package com.github.relua.gui.views;

import com.github.relua.model.LuacFile;
import com.github.relua.model.LineRange;
import com.github.relua.decompiler.pipeline.DecompilerResult;
import com.github.relua.util.BytecodeFormatter;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.concurrent.Task;
import java.io.File;
import java.util.Map;

/**
 * 标签页类，用于管理单个打开的文件的状态和视图（支持在Lua代码与字节码视图之间切换）
 */
public class EditorTab extends Tab {
    private File file;
    private final TextEditorView textEditorView; // Lua代码编辑器视图
    private final TextEditorView bytecodeEditorView; // 字节码代码编辑器视图
    private boolean showingBytecode = false;
    
    private LuacFile luacFile;
    private DecompilerResult decompilerResult;
    private Map<String, LineRange> chunkLineRanges;
    private Task<?> activeTask;

    private VBox loadingOverlay;
    private ProgressIndicator progressIndicator;
    private Label loadingLabel;
    private Label warningLabel;
    
    private ToggleButton btnLuaView;
    private ToggleButton btnBytecodeView;
    private Label viewModeHintLabel;

    public EditorTab(File file, TextEditorView textEditorView) {
        super(file.getName());
        this.file = file;
        this.textEditorView = textEditorView;
        
        // 创建字节码代码编辑器（与代码视图体验一致）
        this.bytecodeEditorView = new TextEditorView(false);
        this.bytecodeEditorView.getCodeArea().setEditable(false); // 字节码只读

        // 顶层主容器（包含顶部模式切换栏与编辑器内容区）
        VBox tabContentBox = new VBox();

        // 顶部精简模式切换条
        HBox headerBar = createHeaderBar();
        tabContentBox.getChildren().add(headerBar);

        // 编辑器堆叠区域（Lua代码与字节码视图切换显示）
        StackPane editorStackPane = new StackPane();
        VBox.setVgrow(editorStackPane, Priority.ALWAYS);

        javafx.scene.Node luaNode = textEditorView.getView();
        javafx.scene.Node bytecodeNode = bytecodeEditorView.getView();

        bytecodeNode.setVisible(false);
        bytecodeNode.setManaged(false);

        editorStackPane.getChildren().addAll(luaNode, bytecodeNode);

        // StackPane 包含编辑器和加载覆盖层
        StackPane container = new StackPane();
        VBox.setVgrow(container, Priority.ALWAYS);
        container.getChildren().add(tabContentBox);

        // 初始化加载状态覆盖层
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

        tabContentBox.getChildren().add(editorStackPane);
        container.getChildren().add(loadingOverlay);
        setContent(container);
    }

    private HBox createHeaderBar() {
        HBox bar = new HBox(8);
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #f7f9fa; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0; -fx-padding: 3 8 3 8;");

        viewModeHintLabel = new Label("Lua 源码视图");
        viewModeHintLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4a5568; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToggleGroup group = new ToggleGroup();
        btnLuaView = new ToggleButton("Lua 源码");
        btnBytecodeView = new ToggleButton("字节码");

        btnLuaView.setToggleGroup(group);
        btnBytecodeView.setToggleGroup(group);
        btnLuaView.setSelected(true);

        String btnStyle = "-fx-font-size: 11px; -fx-padding: 2 8 2 8; -fx-cursor: hand;";
        btnLuaView.setStyle(btnStyle);
        btnBytecodeView.setStyle(btnStyle);

        btnLuaView.setOnAction(e -> setShowingBytecode(false));
        btnBytecodeView.setOnAction(e -> setShowingBytecode(true));

        bar.getChildren().addAll(viewModeHintLabel, spacer, btnLuaView, btnBytecodeView);
        return bar;
    }

    /**
     * 切换代码视图和字节码视图
     */
    public void toggleBytecodeView() {
        setShowingBytecode(!showingBytecode);
    }

    /**
     * 设置是否显示字节码视图
     * @param show 是否显示字节码
     */
    public void setShowingBytecode(boolean show) {
        this.showingBytecode = show;
        javafx.scene.Node luaNode = textEditorView.getView();
        javafx.scene.Node bytecodeNode = bytecodeEditorView.getView();

        if (show) {
            // 如果字节码尚未生成且有 luacFile，尝试即时生成
            if (luacFile != null && (bytecodeEditorView.getCodeArea().getText() == null || bytecodeEditorView.getCodeArea().getText().isEmpty())) {
                refreshBytecodeContent();
            }
            luaNode.setVisible(false);
            luaNode.setManaged(false);
            bytecodeNode.setVisible(true);
            bytecodeNode.setManaged(true);
            btnBytecodeView.setSelected(true);
            viewModeHintLabel.setText("字节码反汇编视图");
            setText(file != null ? file.getName() + " [字节码]" : "字节码");
        } else {
            bytecodeNode.setVisible(false);
            bytecodeNode.setManaged(false);
            luaNode.setVisible(true);
            luaNode.setManaged(true);
            btnLuaView.setSelected(true);
            viewModeHintLabel.setText("Lua 源码视图");
            setText(file != null ? file.getName() : "未命名");
        }
    }

    /**
     * 刷新并填充字节码内容
     */
    public void refreshBytecodeContent() {
        if (luacFile != null) {
            String formattedBytecode = BytecodeFormatter.formatLuacFile(luacFile);
            bytecodeEditorView.setText(formattedBytecode);
        }
    }

    public boolean isShowingBytecode() {
        return showingBytecode;
    }

    public TextEditorView getLuaEditorView() {
        return textEditorView;
    }

    public TextEditorView getBytecodeEditorView() {
        return bytecodeEditorView;
    }

    public TextEditorView getActiveEditorView() {
        return showingBytecode ? bytecodeEditorView : textEditorView;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        this.setText(file.getName());
    }

    public TextEditorView getTextEditorView() {
        return textEditorView;
    }

    public LuacFile getLuacFile() {
        return luacFile;
    }

    public void setLuacFile(LuacFile luacFile) {
        this.luacFile = luacFile;
        if (luacFile != null) {
            refreshBytecodeContent();
        }
    }

    public DecompilerResult getDecompilerResult() {
        return decompilerResult;
    }

    public void setDecompilerResult(DecompilerResult decompilerResult) {
        this.decompilerResult = decompilerResult;
    }

    public Map<String, LineRange> getChunkLineRanges() {
        return chunkLineRanges;
    }

    public void setChunkLineRanges(Map<String, LineRange> chunkLineRanges) {
        this.chunkLineRanges = chunkLineRanges;
    }

    public void showLoading(String message) {
        loadingOverlay.setVisible(true);
        loadingLabel.setText(message);
        warningLabel.setText("");
    }

    public void hideLoading() {
        loadingOverlay.setVisible(false);
    }

    public void setWarningText(String text) {
        warningLabel.setText(text);
    }

    public void bindTaskMessage(Task<?> task) {
        loadingLabel.textProperty().bind(task.messageProperty());
    }

    public void unbindTaskMessage() {
        loadingLabel.textProperty().unbind();
    }

    public void setActiveTask(Task<?> task) {
        if (this.activeTask != null) {
            this.activeTask.cancel();
        }
        this.activeTask = task;
    }

    public void cancelTask() {
        if (this.activeTask != null) {
            this.activeTask.cancel();
        }
    }
}
