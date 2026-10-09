package com.github.relua.gui.dialogs;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;

/**
 * IDA Pro 风格行内分析注释对话框，支持在任意代码逻辑行快速添加、修改或清空分析笔记
 */
public class EditCommentDialog extends Dialog<String> {

    private final TextField commentInput;

    public EditCommentDialog(Window owner, int lineNumber, String lineText, String existingComment) {
        initOwner(owner);
        setTitle("添加/修改行注释 - 行 " + lineNumber);
        setHeaderText(null);

        ButtonType okButtonType = new ButtonType("确定 (Enter)", ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消 (Esc)", ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

        Button btnOk = (Button) getDialogPane().lookupButton(okButtonType);
        btnOk.setDefaultButton(true);

        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(15, 20, 10, 20));
        contentBox.setPrefWidth(520);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER_LEFT);

        Label lineLabel = new Label("当前行代码:");
        lineLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555555;");
        Label lineVal = new Label(lineText != null ? lineText.trim() : "");
        lineVal.setStyle("-fx-font-family: 'Consolas'; -fx-text-fill: #333333;");

        Label commentLabel = new Label("分析注释:");
        commentLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555555;");

        commentInput = new TextField(existingComment != null ? existingComment : "");
        commentInput.setPromptText("输入该行分析笔记（留空确定将清除现有注释）");
        commentInput.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px;");
        GridPane.setHgrow(commentInput, Priority.ALWAYS);

        grid.add(lineLabel, 0, 0);
        grid.add(lineVal, 1, 0);
        grid.add(commentLabel, 0, 1);
        grid.add(commentInput, 1, 1);

        Label hintLabel = new Label("提示: 注释将随 .relua 二进制工程数据库持久化保存并在对应行行末呈现。");
        hintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

        contentBox.getChildren().addAll(grid, hintLabel);
        getDialogPane().setContent(contentBox);

        setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return commentInput.getText().trim();
            }
            return null;
        });

        Platform.runLater(() -> {
            commentInput.requestFocus();
            commentInput.selectAll();
        });
    }

    /**
     * 弹出编辑注释对话框
     * @param owner 父级窗口
     * @param lineNumber 当前行号
     * @param lineText 当前行文本内容
     * @param existingComment 已有注释
     * @return 用户输入的注释文本（若取消则为 Optional.empty()）
     */
    public static Optional<String> show(Window owner, int lineNumber, String lineText, String existingComment) {
        EditCommentDialog dialog = new EditCommentDialog(owner, lineNumber, lineText, existingComment);
        return dialog.showAndWait();
    }
}
