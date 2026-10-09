package com.github.relua.gui.dialogs;

import com.github.relua.gui.utils.LuaVariableUtils.XrefItem;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

/**
 * IDA Pro 风格符号交叉引用对话框 (Xrefs Dialog)，展示符号在全文的所有引用行、作用域与上下文代码，并支持极速搜索过滤与双击跳转
 */
public class XrefsDialog extends Dialog<XrefItem> {

    private final TableView<XrefItem> tableView;
    private final TextField filterField;

    public XrefsDialog(Window owner, String symbol, List<XrefItem> xrefs) {
        initOwner(owner);
        setTitle("交叉引用 (Xrefs) - " + symbol);
        setHeaderText(null);
        setResizable(true);

        ButtonType jumpButtonType = new ButtonType("跳转 (Enter)", ButtonData.OK_DONE);
        ButtonType closeButtonType = new ButtonType("关闭 (Esc)", ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(jumpButtonType, closeButtonType);

        Button btnJump = (Button) getDialogPane().lookupButton(jumpButtonType);
        btnJump.setDefaultButton(true);

        VBox contentBox = new VBox(8);
        contentBox.setPadding(new Insets(10));
        contentBox.setPrefWidth(680);
        contentBox.setPrefHeight(420);

        Label headerLabel = new Label("符号 '" + symbol + "' 的交叉引用 (共 " + (xrefs != null ? xrefs.size() : 0) + " 处)：");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #333333;");

        filterField = new TextField();
        filterField.setPromptText("快速过滤代码或作用域 (输入关键词)...");

        // 初始化表格
        tableView = new TableView<>();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        TableColumn<XrefItem, Number> colLine = new TableColumn<>("行号");
        colLine.setPrefWidth(65);
        colLine.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getLine()));
        colLine.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas';");

        TableColumn<XrefItem, String> colScope = new TableColumn<>("作用域 (函数)");
        colScope.setPrefWidth(160);
        colScope.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getScope()));
        colScope.setStyle("-fx-font-family: 'Consolas'; -fx-text-fill: #0066cc;");

        TableColumn<XrefItem, String> colPreview = new TableColumn<>("代码预览");
        colPreview.setPrefWidth(420);
        colPreview.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLinePreview()));
        colPreview.setStyle("-fx-font-family: 'Consolas';");

        tableView.getColumns().addAll(colLine, colScope, colPreview);

        ObservableList<XrefItem> masterData = FXCollections.observableArrayList(xrefs != null ? xrefs : List.of());
        FilteredList<XrefItem> filteredData = new FilteredList<>(masterData, p -> true);

        filterField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(item -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    return true;
                }
                String lower = newVal.trim().toLowerCase();
                return String.valueOf(item.getLine()).contains(lower)
                        || item.getScope().toLowerCase().contains(lower)
                        || item.getLinePreview().toLowerCase().contains(lower);
            });
        });

        tableView.setItems(filteredData);

        // 默认选中第一行
        if (!filteredData.isEmpty()) {
            tableView.getSelectionModel().selectFirst();
        }

        // 双击行触发跳转
        tableView.setRowFactory(tv -> {
            TableRow<XrefItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    setResult(row.getItem());
                    close();
                }
            });
            return row;
        });

        // 键盘 Enter 直接跳转
        tableView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                XrefItem selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    setResult(selected);
                    close();
                }
            }
        });

        contentBox.getChildren().addAll(headerLabel, filterField, tableView);
        getDialogPane().setContent(contentBox);

        setResultConverter(dialogButton -> {
            if (dialogButton == jumpButtonType) {
                return tableView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        Platform.runLater(() -> {
            tableView.requestFocus();
        });
    }

    /**
     * 弹出交叉引用对话框并返回用户选择跳转的项
     * @param owner 父级窗口
     * @param symbol 目标符号
     * @param xrefs 交叉引用列表
     * @return 选中的项（若取消则为 Optional.empty()）
     */
    public static Optional<XrefItem> show(Window owner, String symbol, List<XrefItem> xrefs) {
        XrefsDialog dialog = new XrefsDialog(owner, symbol, xrefs);
        return dialog.showAndWait();
    }
}
