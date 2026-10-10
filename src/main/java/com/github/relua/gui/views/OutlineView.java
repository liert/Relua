package com.github.relua.gui.views;

import com.github.relua.gui.model.OutlineItem;
import com.github.relua.gui.services.OutlineService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * 结构大纲与函数符号导航面板视图组件
 * 展示当前激活文件的函数大纲层次结构，支持实时搜索过滤、代码行范围展示以及双击/点击平滑跳转
 */
public class OutlineView {

    private final VBox container;
    private final TextField filterField;
    private final TreeView<OutlineItem> treeView;
    private final Label countLabel;

    private final OutlineService outlineService = new OutlineService();
    private Consumer<Integer> navigationHandler;

    private OutlineItem currentRootItem;
    private boolean isSelfNavigating = false;

    public OutlineView() {
        container = new VBox(6);
        container.getStyleClass().add("outline-container");
        container.setPadding(new Insets(4, 6, 6, 6));

        // 1. 顶部标题栏
        HBox headerBar = new HBox(8);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.getStyleClass().add("sidebar-header");

        Label titleLabel = new Label("结构大纲 (OUTLINE)");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        countLabel = new Label("(0)");
        countLabel.setStyle("-fx-font-size: 11px; -fx-opacity: 0.75;");

        headerBar.getChildren().addAll(titleLabel, spacer, countLabel);

        // 2. 快速过滤搜索框
        filterField = new TextField();
        filterField.setPromptText("过滤函数/符号 (Filter)...");
        filterField.getStyleClass().add("editor-search-field");
        filterField.setStyle("-fx-font-size: 11px; -fx-padding: 3 6 3 6;");

        filterField.textProperty().addListener((obs, oldVal, newVal) -> {
            applyFilter(newVal);
        });

        // 3. 符号树形列表
        treeView = new TreeView<>();
        treeView.setShowRoot(true);
        VBox.setVgrow(treeView, Priority.ALWAYS);
        treeView.setCellFactory(tv -> new OutlineTreeCell());

        // 节点选中与跳转
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (isSelfNavigating || newItem == null || newItem.getValue() == null) {
                return;
            }
            if (navigationHandler != null) {
                navigationHandler.accept(newItem.getValue().getStartLine());
            }
        });

        container.getChildren().addAll(headerBar, filterField, treeView);
    }

    public Node getView() {
        return container;
    }

    public void setOnNavigate(Consumer<Integer> handler) {
        this.navigationHandler = handler;
    }

    /**
     * 更新大纲视图为指定编辑器标签页的符号数据
     * @param editorTab 目标标签页
     */
    public void updateOutline(EditorTab editorTab) {
        if (editorTab == null) {
            currentRootItem = null;
            treeView.setRoot(null);
            countLabel.setText("(0)");
            return;
        }

        String luaCode = editorTab.getLuaEditorView().getCodeArea().getText();
        OutlineItem root = outlineService.buildOutline(luaCode, editorTab.getLuacFile(), editorTab.getChunkLineRanges());
        this.currentRootItem = root;

        applyFilter(filterField.getText());
    }

    /**
     * 根据关键词对大纲树进行实时过滤显示
     * @param query 过滤词
     */
    private void applyFilter(String query) {
        if (currentRootItem == null) {
            treeView.setRoot(null);
            countLabel.setText("(0)");
            return;
        }

        String filter = (query != null) ? query.trim().toLowerCase() : "";
        TreeItem<OutlineItem> filteredRoot = buildTreeItem(currentRootItem, filter);

        treeView.setRoot(filteredRoot);

        int totalSymbols = countTreeItems(filteredRoot);
        countLabel.setText("(" + totalSymbols + ")");

        if (filteredRoot != null) {
            filteredRoot.setExpanded(true);
        }
    }

    private TreeItem<OutlineItem> buildTreeItem(OutlineItem item, String filter) {
        if (item == null) return null;

        boolean matches = filter.isEmpty()
                || item.getName().toLowerCase().contains(filter)
                || item.getSignature().toLowerCase().contains(filter);

        TreeItem<OutlineItem> treeItem = new TreeItem<>(item);
        boolean childMatches = false;

        for (OutlineItem child : item.getChildren()) {
            TreeItem<OutlineItem> childItem = buildTreeItem(child, filter);
            if (childItem != null) {
                treeItem.getChildren().add(childItem);
                childMatches = true;
            }
        }

        if (matches || childMatches) {
            if (!filter.isEmpty()) {
                treeItem.setExpanded(true);
            }
            return treeItem;
        }

        return null;
    }

    private int countTreeItems(TreeItem<OutlineItem> node) {
        if (node == null) return 0;
        int count = 1;
        for (TreeItem<OutlineItem> child : node.getChildren()) {
            count += countTreeItems(child);
        }
        return count;
    }

    /**
     * 根据当前光标行号高亮选中对应的大纲节点（面包屑联动）
     * @param line 当前源码行号 (1-based)
     */
    public void highlightEnclosingFunction(int line) {
        if (treeView.getRoot() == null || line <= 0) {
            return;
        }

        TreeItem<OutlineItem> bestMatch = findEnclosingItem(treeView.getRoot(), line);
        if (bestMatch != null && treeView.getSelectionModel().getSelectedItem() != bestMatch) {
            isSelfNavigating = true;
            try {
                treeView.getSelectionModel().select(bestMatch);
                treeView.scrollTo(treeView.getRow(bestMatch));
            } finally {
                isSelfNavigating = false;
            }
        }
    }

    private TreeItem<OutlineItem> findEnclosingItem(TreeItem<OutlineItem> current, int line) {
        if (current == null || current.getValue() == null) {
            return null;
        }

        OutlineItem item = current.getValue();
        if (!item.containsLine(line)) {
            return null;
        }

        // 优先在子节点中寻找更精确的匹配
        for (TreeItem<OutlineItem> child : current.getChildren()) {
            TreeItem<OutlineItem> childMatch = findEnclosingItem(child, line);
            if (childMatch != null) {
                return childMatch;
            }
        }

        return current;
    }

    /**
     * 大纲树节点渲染器
     */
    private static class OutlineTreeCell extends TreeCell<OutlineItem> {
        @Override
        protected void updateItem(OutlineItem item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            HBox cellBox = new HBox(5);
            cellBox.setAlignment(Pos.CENTER_LEFT);

            // 类型徽标
            Label badgeLabel = new Label(item.getType().getBadge());
            badgeLabel.setStyle(getBadgeStyle(item.getType()));

            // 符号名称
            Label nameLabel = new Label(item.getName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-family: 'JetBrains Mono', Consolas, monospace;");

            // 参数签名
            Label sigLabel = new Label(item.getSignature());
            sigLabel.setStyle("-fx-opacity: 0.75; -fx-font-family: 'JetBrains Mono', Consolas, monospace;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // 行号范围指示
            String lineRangeText = (item.getStartLine() == item.getEndLine())
                    ? ":" + item.getStartLine()
                    : ":" + item.getStartLine() + "-" + item.getEndLine();
            Label lineLabel = new Label(lineRangeText);
            lineLabel.setStyle("-fx-font-size: 10px; -fx-opacity: 0.6; -fx-font-family: Consolas, monospace;");

            cellBox.getChildren().addAll(badgeLabel, nameLabel, sigLabel, spacer, lineLabel);
            setGraphic(cellBox);
            setText(null);
        }

        private String getBadgeStyle(OutlineItem.ItemType type) {
            switch (type) {
                case FUNCTION:
                    return "-fx-text-fill: #007acc; -fx-font-weight: bold;";
                case LOCAL_FUNCTION:
                    return "-fx-text-fill: #4ec9b0; -fx-font-weight: bold;";
                case METHOD:
                    return "-fx-text-fill: #e5a93b; -fx-font-weight: bold;";
                case MAIN_CHUNK:
                default:
                    return "-fx-text-fill: #9cdcfe;";
            }
        }
    }
}
