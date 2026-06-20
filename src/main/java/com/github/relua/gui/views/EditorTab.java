package com.github.relua.gui.views;

import com.github.relua.model.LuacFile;
import com.github.relua.model.LineRange;
import com.github.relua.decompiler.pipeline.DecompilerResult;
import javafx.scene.control.Tab;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.concurrent.Task;
import java.io.File;
import java.util.Map;

/**
 * 标签页类，用于管理单个打开的文件的状态和视图
 */
public class EditorTab extends Tab {
    private File file;
    private final TextEditorView textEditorView;
    private LuacFile luacFile;
    private DecompilerResult decompilerResult;
    private Map<String, LineRange> chunkLineRanges;
    private Task<?> activeTask;

    private VBox loadingOverlay;
    private ProgressIndicator progressIndicator;
    private Label loadingLabel;
    private Label warningLabel;

    public EditorTab(File file, TextEditorView textEditorView) {
        super(file.getName());
        this.file = file;
        this.textEditorView = textEditorView;

        // StackPane 包含编辑器和加载覆盖层
        StackPane container = new StackPane();
        container.getChildren().add(textEditorView.getView());

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

        container.getChildren().add(loadingOverlay);
        setContent(container);
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
