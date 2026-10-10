package com.github.relua.gui.views;

import com.github.relua.gui.utils.HexFormatter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * 十六进制 Hex 与底层二进制 Inspector 联动视窗
 */
public class HexView {

    private static final int MAX_DISPLAY_BYTES = 1024 * 1024; // 最多加载 1MB 防止极端大文件卡顿

    private final VBox root;
    private final CodeArea codeArea;
    private final Label inspectorLabel;
    private final TextField jumpTextField;
    private final Button btnJump;
    private final Label infoLabel;

    private byte[] currentData = new byte[0];
    private String currentFileName = "";

    public HexView() {
        this.root = new VBox();
        this.root.getStyleClass().add("hex-view-container");

        // 1. 顶部操作与跳转栏
        HBox topBar = new HBox(8);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(4, 8, 4, 8));
        topBar.getStyleClass().add("hex-top-bar");

        infoLabel = new Label("未加载数据");
        infoLabel.getStyleClass().add("hex-info-label");
        HBox.setHgrow(infoLabel, Priority.ALWAYS);

        Label jumpPrompt = new Label("跳转偏移:");
        jumpPrompt.getStyleClass().add("hex-jump-label");

        jumpTextField = new TextField();
        jumpTextField.setPromptText("0x00 或 10");
        jumpTextField.setPrefWidth(95);
        jumpTextField.getStyleClass().add("hex-jump-field");

        btnJump = new Button("定位");
        btnJump.getStyleClass().add("hex-jump-button");
        btnJump.setOnAction(e -> handleJump());
        jumpTextField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleJump();
            }
        });

        topBar.getChildren().addAll(infoLabel, jumpPrompt, jumpTextField, btnJump);

        // 2. 中间只读等宽 Hex 代码区域
        codeArea = new CodeArea();
        codeArea.setEditable(false);
        codeArea.getStyleClass().add("hex-code-area");

        // 监听光标移动，动态更新底部的二进制 Inspector
        codeArea.caretPositionProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && currentData != null && currentData.length > 0) {
                int byteOffset = HexFormatter.calculateByteOffsetFromCharOffset(newVal, currentData.length);
                updateInspector(byteOffset);
            }
        });

        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // 3. 底部 Binary Inspector 状态信息栏
        HBox bottomBar = new HBox(8);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(4, 8, 4, 8));
        bottomBar.getStyleClass().add("hex-inspector-bar");

        inspectorLabel = new Label("Offset: - | UInt8: - | Int16: - | Int32: - | Float: -");
        inspectorLabel.getStyleClass().add("hex-inspector-label");
        bottomBar.getChildren().add(inspectorLabel);

        root.getChildren().addAll(topBar, scrollPane, bottomBar);
    }

    public Node getView() {
        return root;
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public byte[] getCurrentData() {
        return currentData;
    }

    public String getCurrentFileName() {
        return currentFileName;
    }

    public void loadFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            clear();
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            loadData(bytes, file.getName());
        } catch (IOException e) {
            infoLabel.setText("读取失败: " + e.getMessage());
        }
    }

    public void loadData(byte[] data, String sourceName) {
        if (data == null || data.length == 0) {
            clear();
            return;
        }

        this.currentFileName = (sourceName != null) ? sourceName : "";
        boolean truncated = data.length > MAX_DISPLAY_BYTES;
        this.currentData = truncated ? Arrays.copyOf(data, MAX_DISPLAY_BYTES) : data;

        String formatted = HexFormatter.formatHex(this.currentData);
        codeArea.replaceText(formatted);

        String lengthDesc = truncated
                ? String.format("%s (已加载前 %d 字节 / 文件总计 %d 字节)", currentFileName, currentData.length, data.length)
                : String.format("%s (共 %d 字节 / 0x%X)", currentFileName, currentData.length, currentData.length);
        infoLabel.setText(lengthDesc);

        jumpToOffset(0);
    }

    public void clear() {
        this.currentData = new byte[0];
        this.currentFileName = "";
        codeArea.replaceText("");
        infoLabel.setText("未加载数据");
        inspectorLabel.setText("Offset: - | UInt8: - | Int16: - | Int32: - | Float: -");
    }

    public void jumpToOffset(int byteOffset) {
        if (currentData == null || currentData.length == 0) {
            return;
        }
        int clamped = Math.max(0, Math.min(byteOffset, currentData.length - 1));
        int charOffset = HexFormatter.calculateCharOffsetFromByteOffset(clamped);
        if (charOffset >= 0 && charOffset <= codeArea.getLength()) {
            codeArea.moveTo(charOffset);
            codeArea.requestFollowCaret();
        }
        updateInspector(clamped);
    }

    private void updateInspector(int byteOffset) {
        if (currentData == null || currentData.length == 0 || byteOffset < 0 || byteOffset >= currentData.length) {
            inspectorLabel.setText("Offset: - | UInt8: - | Int16: - | Int32: - | Float: -");
            return;
        }
        inspectorLabel.setText(HexFormatter.inspectOffset(currentData, byteOffset));
    }

    private void handleJump() {
        String text = jumpTextField.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        text = text.trim();
        try {
            int offset;
            if (text.startsWith("0x") || text.startsWith("0X")) {
                offset = Integer.parseInt(text.substring(2), 16);
            } else {
                offset = Integer.parseInt(text);
            }
            jumpToOffset(offset);
        } catch (NumberFormatException ignored) {
            // 输入格式不合规则忽略
        }
    }
}
