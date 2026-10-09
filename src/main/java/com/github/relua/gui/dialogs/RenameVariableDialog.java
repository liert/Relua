package com.github.relua.gui.dialogs;
 
import com.github.relua.gui.theme.ThemeManager;
import com.github.relua.gui.utils.LuaVariableUtils;
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
import javafx.scene.paint.Color;
import javafx.stage.Window;

import java.util.Optional;

/**
 * 变量重命名对话框，提供符合 Lua 标识符规范的实时校验与便捷快捷键交互
 */
public class RenameVariableDialog extends Dialog<String> {

    private final TextField nameInput;
    private final Label validationLabel;
    private final Button btnOk;

    public RenameVariableDialog(Window owner, String oldVariableName) {
        initOwner(owner);
        setTitle("重命名符号 - Relua");
        setHeaderText(null);

        // 应用现代化主题
        ThemeManager.getInstance().applyTheme(this);

        // 设置按钮类型
        ButtonType okButtonType = new ButtonType("确定", ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

        btnOk = (Button) getDialogPane().lookupButton(okButtonType);
        btnOk.setDefaultButton(true);

        // 创建内容表单布局
        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(15, 20, 10, 20));
        contentBox.setMinWidth(380);

        Label promptLabel = new Label("将当前符号（变量/函数名）重命名为新名称（所有引用的出现位置将同步更新）：");
        promptLabel.setWrapText(true);
        promptLabel.setStyle("-fx-font-size: 13px;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER_LEFT);

        Label currentLabel = new Label("原符号名:");
        currentLabel.setStyle("-fx-font-weight: bold;");
        Label currentVal = new Label(oldVariableName != null ? oldVariableName : "");
        currentVal.setStyle("-fx-font-family: 'Consolas'; -fx-font-weight: bold; -fx-text-fill: #007acc;");

        Label newLabel = new Label("新符号名:");
        newLabel.setStyle("-fx-font-weight: bold;");

        nameInput = new TextField(oldVariableName != null ? oldVariableName : "");
        nameInput.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px;");
        GridPane.setHgrow(nameInput, Priority.ALWAYS);

        grid.add(currentLabel, 0, 0);
        grid.add(currentVal, 1, 0);
        grid.add(newLabel, 0, 1);
        grid.add(nameInput, 1, 1);

        // 校验信息提示标签
        validationLabel = new Label();
        validationLabel.setStyle("-fx-font-size: 12px;");

        contentBox.getChildren().addAll(promptLabel, grid, validationLabel);
        getDialogPane().setContent(contentBox);

        // 实时输入校验监听
        nameInput.textProperty().addListener((obs, oldVal, newVal) -> validateInput(oldVariableName, newVal));

        // 初始校验（初始状态由于与原名相同，应提示并禁用确定按钮）
        validateInput(oldVariableName, nameInput.getText());

        // 结果转换器
        setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return nameInput.getText().trim();
            }
            return null;
        });

        // 自动聚焦并全选输入框
        Platform.runLater(() -> {
            nameInput.requestFocus();
            nameInput.selectAll();
        });
    }

    /**
     * 执行输入合法性校验，更新提示样式与确认按钮启用状态
     */
    private void validateInput(String oldName, String newName) {
        String error = LuaVariableUtils.validateVariableName(oldName, newName);
        if (error != null) {
            validationLabel.setTextFill(Color.web("#d32f2f"));
            validationLabel.setText("⚠ " + error);
            btnOk.setDisable(true);
        } else {
            validationLabel.setTextFill(Color.web("#2e7d32"));
            validationLabel.setText("✓ 变量名有效");
            btnOk.setDisable(false);
        }
    }

    /**
     * 静态便捷调用方法，弹出重命名对话框并返回用户确认的新名称
     * @param owner 父级窗口
     * @param oldVariableName 原变量名称
     * @return 确认输入的新变量名（若取消则为 Optional.empty()）
     */
    public static Optional<String> show(Window owner, String oldVariableName) {
        RenameVariableDialog dialog = new RenameVariableDialog(owner, oldVariableName);
        return dialog.showAndWait();
    }
}
