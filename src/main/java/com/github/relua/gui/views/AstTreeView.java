package com.github.relua.gui.views;

import com.github.relua.gui.model.AstTreeNode;
import com.github.relua.gui.preferences.EditorAppearance;
import com.github.relua.gui.preferences.SettingsManager;
import com.github.relua.gui.preferences.AppSettings;
import com.github.relua.gui.services.AstTreeService.FunctionTree;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;
import java.util.function.Consumer;

/** AST 侧栏结构树；只分页顶层语句，每条语句的子树保持完整。 */
public final class AstTreeView {
    private final VBox view = new VBox();
    private final ComboBox<FunctionTree> functions = new ComboBox<>();
    private final TextField search = new TextField();
    private final ComboBox<Integer> pageSize = new ComboBox<>();
    private final Pagination pagination = new Pagination(1);
    private final Label pageInfo = new Label();
    private final CheckBox followSource = new CheckBox("跟随源码");
    private final Label message = new Label("请先打开字节码文件");
    private final TextArea details = new TextArea();
    private final Button navigate = new Button("定位源码");
    private final StackPane treeContainer = new StackPane();
    private final Set<String> expanded = new HashSet<>();
    private List<AstTreeNode> filtered = Collections.emptyList();
    private TreeView<AstTreeNode> tree;
    private Consumer<AstTreeNode> navigation;
    private AstTreeNode selected;
    private boolean updating;
    private int lastSourceLine = 1;
    private final Consumer<com.github.relua.gui.preferences.AppSettings> appearanceListener;
    private AppSettings appearance;

    public AstTreeView(SettingsManager settings) {
        view.getStyleClass().add("ast-tree-view");
        view.setMinWidth(0);
        functions.setId("ast-functions");
        functions.setMaxWidth(Double.MAX_VALUE);
        functions.setPromptText("选择函数");
        HBox header = new HBox(8, new Label("AST"), functions);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("ast-toolbar");
        HBox.setHgrow(functions, Priority.ALWAYS);
        search.setId("ast-search");
        search.setPromptText("搜索变量、调用或节点类型…");
        search.setMinWidth(0);
        followSource.setSelected(true);
        followSource.setMinWidth(Region.USE_PREF_SIZE);
        HBox filters = new HBox(8, search, followSource);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("ast-toolbar");
        HBox.setHgrow(search, Priority.ALWAYS);
        pageSize.getItems().setAll(25, 50, 100);
        pageSize.setValue(50);
        pageSize.setId("ast-page-size");
        Button expand = new Button("展开本页");
        Button collapse = new Button("收起");
        HBox paging = new HBox(8, new Label("每页语句"), pageSize, expand, collapse);
        paging.getStyleClass().add("ast-toolbar");
        paging.setAlignment(Pos.CENTER_LEFT);
        pagination.setId("ast-pagination");
        pagination.setMaxPageIndicatorCount(5);
        pagination.setMinSize(0, 100);
        pagination.setPageFactory(this::createPage);
        VBox.setVgrow(treeContainer, Priority.ALWAYS);
        treeContainer.setMinHeight(100);
        treeContainer.getChildren().setAll(pagination, message);
        message.setWrapText(true);
        message.setMaxWidth(360);
        details.setId("ast-details");
        details.setEditable(false);
        details.setWrapText(true);
        details.setPrefRowCount(3);
        details.setPrefHeight(110);
        details.setMinHeight(100);
        details.setMaxHeight(160);
        details.getStyleClass().add("ast-node-details");
        navigate.setDisable(true);
        HBox detailHeader = new HBox(8, pageInfo, new Region(), navigate);
        HBox.setHgrow(detailHeader.getChildren().get(1), Priority.ALWAYS);
        detailHeader.setAlignment(Pos.CENTER_LEFT);
        detailHeader.getStyleClass().add("ast-toolbar");
        view.getChildren().addAll(header, filters, paging, treeContainer, detailHeader, details);
        functions.valueProperty().addListener((obs, old, value) -> { if (!updating) resetPages(); });
        search.textProperty().addListener((obs, old, value) -> resetPages());
        pageSize.valueProperty().addListener((obs, old, value) -> resetPages());
        followSource.setOnAction(event -> { if (followSource.isSelected()) followSourceLine(lastSourceLine); });
        expand.setOnAction(event -> {
            if (tree != null && tree.getRoot() != null) expandAll(tree.getRoot(), true);
        });
        collapse.setOnAction(event -> {
            if (tree != null && tree.getRoot() != null) {
                expandAll(tree.getRoot(), false);
                tree.getRoot().setExpanded(true);
            }
        });
        navigate.setOnAction(event -> navigateSelected());
        appearanceListener = value -> {
            appearance = value;
            details.setStyle(EditorAppearance.fontStyle(value));
            pagination.setStyle(EditorAppearance.fontStyle(value));
            if (tree != null) tree.refresh();
        };
        settings.addListener(appearanceListener);
        appearanceListener.accept(settings.getSettings());
        showMessage("请先打开字节码文件");
    }

    public Node getView() { return view; }
    public TreeView<AstTreeNode> getTree() { return tree; }
    public FunctionTree getCurrentFunction() { return functions.getValue(); }
    public Pagination getPagination() { return pagination; }
    public void setOnNavigate(Consumer<AstTreeNode> navigation) { this.navigation = navigation; }
    public void showMessage(String text) {
        message.setText(text); message.setVisible(true); message.setManaged(true);
        pagination.setVisible(false); pagination.setManaged(false);
        functions.setDisable(true); search.setDisable(true); pageSize.setDisable(true);
        selected = null; details.clear(); navigate.setDisable(true);
    }
    public void setFunctions(List<FunctionTree> values) {
        String current = functions.getValue() == null ? null : functions.getValue().name;
        updating = true;
        functions.getItems().setAll(values);
        FunctionTree next = values.stream().filter(value -> value.name.equals(current)).findFirst()
                .orElse(values.isEmpty() ? null : values.get(0));
        functions.setValue(next);
        updating = false;
        functions.setDisable(values.isEmpty()); search.setDisable(values.isEmpty()); pageSize.setDisable(values.isEmpty());
        if (values.isEmpty()) { showMessage("当前文件没有可显示的 AST"); return; }
        message.setVisible(false); message.setManaged(false);
        pagination.setVisible(true); pagination.setManaged(true);
        resetPages();
        followSourceLine(lastSourceLine);
    }
    private void resetPages() {
        if (updating) return;
        FunctionTree function = functions.getValue();
        List<AstTreeNode> matches = new ArrayList<>();
        if (function != null) for (AstTreeNode node : function.root.children) {
            AstTreeNode filteredNode = node.filter(search.getText());
            if (filteredNode != null) matches.add(filteredNode);
        }
        filtered = matches;
        selected = null; details.clear(); navigate.setDisable(true);
        int count = Math.max(1, (filtered.size() + pageSize.getValue() - 1) / pageSize.getValue());
        pagination.setCurrentPageIndex(0);
        pagination.setPageCount(count);
        // 更新同一页的数据时 Pagination 不会自动重新调用 pageFactory。
        pagination.setPageFactory(null);
        pagination.setPageFactory(this::createPage);
    }
    private Node createPage(int index) {
        FunctionTree function = functions.getValue();
        tree = new TreeView<>();
        tree.setId("ast-tree");
        tree.getStyleClass().add("ast-structure-tree");
        tree.setCellFactory(control -> new TreeCell<AstTreeNode>() {
            @Override protected void updateItem(AstTreeNode value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value.toString());
                setGraphic(null);
                if (appearance != null) setStyle(EditorAppearance.fontStyle(appearance));
                setTooltip(empty || value == null ? null : new Tooltip(value.summary));
            }
        });
        if (function != null) {
            TreeItem<AstTreeNode> root = new TreeItem<>(function.root);
            int from = index * pageSize.getValue();
            int to = Math.min(filtered.size(), from + pageSize.getValue());
            for (int i = from; i < to; i++) root.getChildren().add(item(filtered.get(i)));
            root.setExpanded(true);
            tree.setRoot(root);
            pageInfo.setText(filtered.isEmpty() ? "0 条语句" : "语句 " + (from + 1) + "–" + to + " / " + filtered.size());
        } else pageInfo.setText("0 条语句");
        tree.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            selected = value == null ? null : value.getValue();
            updateDetails();
        });
        tree.setOnMouseClicked(event -> { if (event.getClickCount() == 2) navigateSelected(); });
        StackPane page = new StackPane(tree);
        if (filtered.isEmpty()) {
            tree.setVisible(false);
            page.getChildren().add(new Label("没有匹配的语句"));
        }
        return page;
    }
    private TreeItem<AstTreeNode> item(AstTreeNode value) {
        TreeItem<AstTreeNode> item = new TreeItem<AstTreeNode>(value) {
            private boolean populated;
            @Override public javafx.collections.ObservableList<TreeItem<AstTreeNode>> getChildren() {
                if (!populated) {
                    populated = true;
                    for (AstTreeNode child : value.children) super.getChildren().add(item(child));
                }
                return super.getChildren();
            }
            @Override public boolean isLeaf() { return value.children.isEmpty(); }
        };
        item.setExpanded(expanded.contains(value.id) || !search.getText().trim().isEmpty());
        item.expandedProperty().addListener((obs, old, open) -> {
            if (open) expanded.add(value.id); else expanded.remove(value.id);
        });
        return item;
    }
    private void updateDetails() {
        navigate.setDisable(selected == null || selected.sourceLine < 1);
        if (selected == null) { details.clear(); return; }
        details.setText("类型: " + selected.type + "    字段: " + (selected.role.isEmpty() ? "根节点" : selected.role)
                + "\n源码行: " + (selected.sourceLine > 0 ? selected.sourceLine : "—")
                + "    字节码 PC: " + (selected.pc >= 0 ? selected.pc : "—")
                + "    原始调试行: " + (selected.originalLine > 0 ? selected.originalLine : "—")
                + "\n" + selected.summary);
    }
    private void navigateSelected() {
        if (selected != null && selected.sourceLine > 0 && navigation != null) navigation.accept(selected);
    }
    private void expandAll(TreeItem<AstTreeNode> item, boolean open) {
        item.setExpanded(open);
        for (TreeItem<AstTreeNode> child : item.getChildren()) expandAll(child, open);
    }
    public void followSourceLine(int line) {
        lastSourceLine = line;
        if (!followSource.isSelected() || updating || functions.getItems().isEmpty()) return;
        FunctionTree best = null;
        for (FunctionTree value : functions.getItems()) {
            if (value.range != null && line >= value.range.startLine && line <= value.range.endLine
                    && (best == null || value.range.endLine - value.range.startLine < best.range.endLine - best.range.startLine)) best = value;
        }
        if (best == null) best = functions.getItems().get(0);
        if (best != functions.getValue()) functions.setValue(best);
        if (!search.getText().trim().isEmpty()) return;
        int target = -1;
        for (int i = 0; i < filtered.size(); i++) {
            if (filtered.get(i).sourceLine > 0 && filtered.get(i).sourceLine <= line) target = i;
        }
        if (target < 0) return;
        int page = target / pageSize.getValue();
        if (pagination.getCurrentPageIndex() != page) pagination.setCurrentPageIndex(page);
        if (tree != null && tree.getRoot() != null) {
            int row = target % pageSize.getValue();
            if (row < tree.getRoot().getChildren().size()) {
                TreeItem<AstTreeNode> item = tree.getRoot().getChildren().get(row);
                tree.getSelectionModel().select(item);
                tree.scrollTo(tree.getRow(item));
            }
        }
    }
}
