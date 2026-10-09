package com.github.relua.gui.dialogs;

import com.github.relua.gui.services.BatchExportService;
import com.github.relua.gui.theme.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 批量反编译导出工程进度对话框
 */
public class BatchExportDialog extends Stage {

    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label statusLabel = new Label("准备中...");
    private final Label statsLabel = new Label("进度: 0 / 0 | 成功: 0 | 失败: 0");
    private final Button btnCancel = new Button("取消 (Cancel)");
    private final Button btnOpenDir = new Button("打开导出目录");
    private final Button btnClose = new Button("完成");

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final BatchExportService exportService = new BatchExportService();

    public BatchExportDialog(Window owner, File sourceRoot, File targetRoot, List<File> files) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("批量反编译导出工程 - Relua");
        setResizable(false);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setMinWidth(480);

        Label titleLabel = new Label("正在批量反编译并导出为 Lua 工程...");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label pathInfoLabel = new Label("输出目标: " + targetRoot.getAbsolutePath());
        pathInfoLabel.setStyle("-fx-font-size: 11px; -fx-opacity: 0.75;");
        pathInfoLabel.setWrapText(true);

        progressBar.setPrefWidth(440);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        statusLabel.setStyle("-fx-font-size: 12px;");
        statusLabel.setWrapText(true);
        statusLabel.setMinHeight(36);

        statsLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #007acc;");

        btnOpenDir.setVisible(false);
        btnOpenDir.setManaged(false);
        btnClose.setVisible(false);
        btnClose.setManaged(false);

        btnCancel.setOnAction(e -> {
            cancelled.set(true);
            btnCancel.setDisable(true);
            statusLabel.setText("正在取消任务...");
        });

        btnOpenDir.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported() && targetRoot.exists()) {
                    Desktop.getDesktop().open(targetRoot);
                }
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.initOwner(this);
                alert.setTitle("打开目录失败");
                alert.setHeaderText(null);
                alert.setContentText("无法打开目录: " + ex.getMessage());
                alert.showAndWait();
            }
        });

        btnClose.setOnAction(e -> close());

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.getChildren().addAll(btnOpenDir, btnCancel, btnClose);

        root.getChildren().addAll(titleLabel, pathInfoLabel, progressBar, statusLabel, statsLabel, btnBox);
        Scene scene = new Scene(root);
        ThemeManager.getInstance().applyTheme(scene);
        setScene(scene);

        // 启动后台线程执行批量导出
        Thread thread = new Thread(() -> runExport(sourceRoot, targetRoot, files), "Relua-BatchExport-Thread");
        thread.setDaemon(true);
        thread.start();

        setOnCloseRequest(e -> {
            cancelled.set(true);
        });
    }

    private void runExport(File sourceRoot, File targetRoot, List<File> files) {
        int total = files.size();
        BatchExportService.ExportResult result = exportService.exportBatch(sourceRoot, targetRoot, files, cancelled,
                (completed, totalCount, currentFile) -> {
                    Platform.runLater(() -> {
                        double progress = (totalCount > 0) ? ((double) completed / totalCount) : 0;
                        progressBar.setProgress(progress);
                        statusLabel.setText("正在处理: " + currentFile);
                        statsLabel.setText("进度: " + completed + " / " + totalCount);
                    });
                }
        );

        Platform.runLater(() -> {
            progressBar.setProgress(1.0);
            btnCancel.setVisible(false);
            btnCancel.setManaged(false);
            btnOpenDir.setVisible(true);
            btnOpenDir.setManaged(true);
            btnClose.setVisible(true);
            btnClose.setManaged(true);

            if (result.isCancelled()) {
                statusLabel.setText("用户已取消操作。部分文件已生成。");
                statsLabel.setText(String.format("已处理: %d / %d | 成功: %d | 失败: %d",
                        result.getSuccessCount() + result.getFailedCount(), result.getTotalFiles(),
                        result.getSuccessCount(), result.getFailedCount()));
            } else {
                double seconds = result.getDurationMs() / 1000.0;
                statusLabel.setText(String.format("导出完成！共耗时 %.2f 秒。", seconds));
                statsLabel.setText(String.format("总计: %d | 成功: %d | 失败: %d",
                        result.getTotalFiles(), result.getSuccessCount(), result.getFailedCount()));

                if (result.getFailedCount() > 0) {
                    statsLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #cc3300;");
                } else {
                    statsLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #008800;");
                }
            }
        });
    }
}
