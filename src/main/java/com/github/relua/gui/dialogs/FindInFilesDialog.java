package com.github.relua.gui.dialogs;

import com.github.relua.gui.services.GlobalSearchService;
import com.github.relua.gui.services.GlobalSearchService.SearchOptions;
import com.github.relua.gui.services.GlobalSearchService.SearchResultItem;
import com.github.relua.gui.services.GlobalSearchService.SearchResultSummary;
import com.github.relua.gui.theme.ThemeManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * IntelliJ / VS Code 风格全局项目文本与符号检索窗口 (Find in Files)
 * 支持区分大小写、全词匹配、正则、文件后缀过滤、实时搜索结果流式展示与双击直接跳转
 */
public class FindInFilesDialog extends Stage {

    @FunctionalInterface
    public interface JumpCallback {
        void onJump(File file, int line, int startCol, int endCol);
    }

    private final File rootFolder;
    private final Map<File, String> openTabContents;
    private final JumpCallback jumpCallback;

    private final TextField searchField = new TextField();
    private final ToggleButton btnMatchCase = new ToggleButton("Aa");
    private final ToggleButton btnMatchWord = new ToggleButton("|W|");
    private final ToggleButton btnRegex = new ToggleButton(".*");
    private final Button btnSearch = new Button("🔍 搜索");

    private final TextField fileFilterField = new TextField("*.lua, *.luac, *.*");
    private final CheckBox checkOpenTabsOnly = new CheckBox("仅搜索已打开标签页");
    private final Label scopeLabel = new Label();

    private final ProgressBar progressBar = new ProgressBar();
    private final Label statusLabel = new Label("输入关键词后按回车开始全局检索");
    private final Button btnStop = new Button("停止");

    private final TableView<SearchResultItem> resultsTable = new TableView<>();
    private final ObservableList<SearchResultItem> tableData = FXCollections.observableArrayList();
    private final TextArea previewTextArea = new TextArea();

    private final Button btnJump = new Button("跳转至代码 (Enter)");
    private final Button btnClose = new Button("关闭 (Esc)");

    private final GlobalSearchService searchService = new GlobalSearchService();
    private Task<SearchResultSummary> activeSearchTask;

    public FindInFilesDialog(Window owner, File rootFolder, Map<File, String> openTabContents, String initialQuery, JumpCallback jumpCallback) {
        this.rootFolder = rootFolder;
        this.openTabContents = openTabContents;
        this.jumpCallback = jumpCallback;

        initOwner(owner);
        initModality(Modality.NONE); // 采用非模态，方便用户边对照边搜索
        setTitle("全局项目查找 (Find in Files) - Relua");
        setMinWidth(720);
        setMinHeight(520);
        setWidth(820);
        setHeight(580);

        VBox rootBox = new VBox(10);
        rootBox.setPadding(new Insets(12));
        rootBox.getStyleClass().add("global-search-dialog");

        // 1. 顶部搜索主行
        HBox searchRow = createSearchRow(initialQuery);

        // 2. 过滤与范围设置行
        HBox filterRow = createFilterRow();

        // 3. 搜索状态与进度指示行
        HBox statusRow = createStatusRow();

        // 4. 中间分屏结果与预览区域
        SplitPane splitPane = createContentSplitPane();
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // 5. 底部操作按钮行
        HBox actionRow = createActionRow();

        rootBox.getChildren().addAll(searchRow, filterRow, statusRow, splitPane, actionRow);

        Scene scene = new Scene(rootBox);
        ThemeManager.getInstance().applyTheme(scene);
        setScene(scene);

        // 键盘全局 ESC 监听与关闭处理
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                cancelCurrentTask();
                close();
            }
        });

        setOnCloseRequest(e -> cancelCurrentTask());

        // 如果传入了初始关键词，自动触发搜索
        if (initialQuery != null && !initialQuery.trim().isEmpty()) {
            Platform.runLater(this::startSearch);
        }
    }

    private HBox createSearchRow(String initialQuery) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("搜索文本、函数、变量符号或正则表达式...");
        searchField.getStyleClass().add("editor-search-field");
        if (initialQuery != null) {
            searchField.setText(initialQuery.trim());
            searchField.selectAll();
        }
        HBox.setHgrow(searchField, Priority.ALWAYS);

        btnMatchCase.setTooltip(new Tooltip("区分大小写 (Match Case)"));
        btnMatchWord.setTooltip(new Tooltip("全词匹配 (Match Whole Word)"));
        btnRegex.setTooltip(new Tooltip("正则表达式 (Regular Expression)"));

        btnMatchCase.getStyleClass().add("editor-search-button");
        btnMatchWord.getStyleClass().add("editor-search-button");
        btnRegex.getStyleClass().add("editor-search-button");
        btnSearch.getStyleClass().add("editor-search-button");

        btnSearch.setDefaultButton(true);
        btnSearch.setOnAction(e -> startSearch());

        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                startSearch();
                e.consume();
            } else if (e.getCode() == KeyCode.DOWN) {
                if (!resultsTable.getItems().isEmpty()) {
                    resultsTable.requestFocus();
                    resultsTable.getSelectionModel().select(0);
                    e.consume();
                }
            }
        });

        row.getChildren().addAll(searchField, btnMatchCase, btnMatchWord, btnRegex, btnSearch);
        return row;
    }

    private HBox createFilterRow() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lblFilter = new Label("文件过滤:");
        fileFilterField.setPrefWidth(180);
        fileFilterField.setPromptText("*.lua, *.luac");

        fileFilterField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                startSearch();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (rootFolder != null && rootFolder.exists()) {
            scopeLabel.setText("📁 范围: " + rootFolder.getAbsolutePath());
        } else {
            scopeLabel.setText("📁 范围: 当前已打开的文件");
            checkOpenTabsOnly.setSelected(true);
            checkOpenTabsOnly.setDisable(true);
        }
        scopeLabel.setStyle("-fx-font-size: 11px; -fx-opacity: 0.8;");

        row.getChildren().addAll(lblFilter, fileFilterField, checkOpenTabsOnly, spacer, scopeLabel);
        return row;
    }

    private HBox createStatusRow() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        progressBar.setPrefWidth(120);
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        statusLabel.setStyle("-fx-font-size: 11px;");
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        btnStop.setVisible(false);
        btnStop.setManaged(false);
        btnStop.setOnAction(e -> cancelCurrentTask());

        row.getChildren().addAll(progressBar, statusLabel, btnStop);
        return row;
    }

    private SplitPane createContentSplitPane() {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.setDividerPositions(0.65);

        // 表格初始化
        resultsTable.setItems(tableData);
        resultsTable.getStyleClass().add("global-search-table");

        TableColumn<SearchResultItem, String> colPath = new TableColumn<>("文件路径");
        colPath.setPrefWidth(240);
        colPath.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDisplayPath()));
        colPath.setStyle("-fx-font-family: 'JetBrains Mono', 'Cascadia Code', Consolas, monospace;");

        TableColumn<SearchResultItem, Number> colLine = new TableColumn<>("行号");
        colLine.setPrefWidth(65);
        colLine.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getLineNumber()));
        colLine.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'JetBrains Mono', 'Cascadia Code', Consolas, monospace;");

        TableColumn<SearchResultItem, String> colContent = new TableColumn<>("匹配内容");
        colContent.setPrefWidth(470);
        colContent.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLineContent()));
        colContent.setStyle("-fx-font-family: 'JetBrains Mono', 'Cascadia Code', Consolas, monospace;");

        resultsTable.getColumns().addAll(colPath, colLine, colContent);

        // 双击或回车触发跳转
        resultsTable.setRowFactory(tv -> {
            TableRow<SearchResultItem> tableRow = new TableRow<>();
            tableRow.setOnMouseClicked(event -> {
                if (!tableRow.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    jumpToSelected();
                }
            });
            return tableRow;
        });

        resultsTable.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                jumpToSelected();
                e.consume();
            }
        });

        // 选中行时在下方更新预览
        resultsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updatePreview(newVal);
        });

        // 预览面板初始化
        VBox previewBox = new VBox(4);
        Label lblPreview = new Label("代码预览上下文 (Preview):");
        lblPreview.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-opacity: 0.85;");

        previewTextArea.setEditable(false);
        previewTextArea.setWrapText(false);
        previewTextArea.setStyle("-fx-font-family: 'JetBrains Mono', 'Cascadia Code', Consolas, monospace; -fx-font-size: 12px;");
        VBox.setVgrow(previewTextArea, Priority.ALWAYS);

        previewBox.getChildren().addAll(lblPreview, previewTextArea);

        splitPane.getItems().addAll(resultsTable, previewBox);
        return splitPane;
    }

    private HBox createActionRow() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_RIGHT);

        btnJump.setOnAction(e -> jumpToSelected());
        btnClose.setOnAction(e -> {
            cancelCurrentTask();
            close();
        });

        row.getChildren().addAll(btnJump, btnClose);
        return row;
    }

    private void updatePreview(SearchResultItem item) {
        if (item == null) {
            previewTextArea.setText("");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("// 文件: ").append(item.getDisplayPath()).append(" (第 ").append(item.getLineNumber()).append(" 行)\n");
        sb.append(item.getLineContent()).append("\n");
        previewTextArea.setText(sb.toString());
    }

    private void startSearch() {
        String query = searchField.getText();
        if (query == null || query.trim().isEmpty()) {
            statusLabel.setText("请输入要检索的关键词");
            return;
        }

        cancelCurrentTask();
        tableData.clear();
        previewTextArea.setText("");

        progressBar.setVisible(true);
        progressBar.setManaged(true);
        btnStop.setVisible(true);
        btnStop.setManaged(true);
        btnSearch.setDisable(true);
        statusLabel.setText("正在搜索: " + query + " ...");

        SearchOptions options = new SearchOptions(
                query.trim(),
                btnMatchCase.isSelected(),
                btnMatchWord.isSelected(),
                btnRegex.isSelected(),
                fileFilterField.getText(),
                checkOpenTabsOnly.isSelected()
        );

        activeSearchTask = searchService.createSearchTask(
                rootFolder,
                openTabContents,
                options,
                batch -> {
                    tableData.addAll(batch);
                    statusLabel.setText("已找到 " + tableData.size() + " 个匹配项...");
                }
        );

        progressBar.progressProperty().bind(activeSearchTask.progressProperty());
        statusLabel.textProperty().bind(activeSearchTask.messageProperty());

        activeSearchTask.setOnSucceeded(e -> {
            unbindProgress();
            SearchResultSummary summary = activeSearchTask.getValue();
            if (summary != null) {
                statusLabel.setText("检索完成：找到 " + summary.getTotalMatches() + " 个匹配项，涉及 "
                        + summary.getMatchedFilesCount() + " 个文件 (用时 " + summary.getDurationMs() + " ms)");
            } else {
                statusLabel.setText("检索完成");
            }
            if (!tableData.isEmpty() && resultsTable.getSelectionModel().getSelectedItem() == null) {
                resultsTable.getSelectionModel().select(0);
            }
        });

        activeSearchTask.setOnFailed(e -> {
            unbindProgress();
            Throwable ex = activeSearchTask.getException();
            statusLabel.setText("搜索出错: " + (ex != null ? ex.getMessage() : "未知错误"));
        });

        activeSearchTask.setOnCancelled(e -> {
            unbindProgress();
            statusLabel.setText("搜索已取消，找到 " + tableData.size() + " 项");
        });

        Thread searchThread = new Thread(activeSearchTask, "global-search-worker");
        searchThread.setDaemon(true);
        searchThread.start();
    }

    private void cancelCurrentTask() {
        if (activeSearchTask != null && activeSearchTask.isRunning()) {
            activeSearchTask.cancel();
        }
        unbindProgress();
    }

    private void unbindProgress() {
        progressBar.progressProperty().unbind();
        statusLabel.textProperty().unbind();
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        btnStop.setVisible(false);
        btnStop.setManaged(false);
        btnSearch.setDisable(false);
    }

    private void jumpToSelected() {
        SearchResultItem selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected == null && !tableData.isEmpty()) {
            selected = tableData.get(0);
        }
        if (selected != null && jumpCallback != null) {
            jumpCallback.onJump(selected.getFile(), selected.getLineNumber(), selected.getStartColumn(), selected.getEndColumn());
            close();
        }
    }
}
