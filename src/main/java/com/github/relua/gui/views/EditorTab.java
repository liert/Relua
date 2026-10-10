package com.github.relua.gui.views;

import com.github.relua.model.LuacFile;
import com.github.relua.model.LineRange;
import com.github.relua.decompiler.pipeline.DecompilerResult;
import com.github.relua.gui.utils.BytecodeSyncUtils;
import com.github.relua.util.BytecodeFormatter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 标签页类，用于管理单个打开文件的状态和视图（支持 Lua 源码、源码/字节码并排分屏、纯字节码三态视图切换，维护反编译符号重命名映射表与双向联动）
 */
public class EditorTab extends Tab {

    /**
     * 视图展示模式枚举
     */
    public enum ViewMode {
        LUA_SOURCE("Lua 源码"),
        SIDE_BY_SIDE("并排分屏"),
        BYTECODE("字节码");

        private final String displayName;

        ViewMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private File file;
    private final TextEditorView textEditorView; // Lua 代码编辑器视图
    private final TextEditorView bytecodeEditorView; // 字节码代码编辑器视图
    private ViewMode viewMode = ViewMode.LUA_SOURCE;
    private boolean showingBytecode = false;
    
    private LuacFile luacFile;
    private DecompilerResult decompilerResult;
    private Map<String, LineRange> chunkLineRanges;
    private Task<?> activeTask;

    // 记录用户在该标签页内进行的所有符号重命名映射（oldName -> newName）
    private final Map<String, String> renamedVariables = new LinkedHashMap<>();
    // 记录用户在该标签页内添加的行内分析注释（lineNumber -> comment）
    private final Map<Integer, String> lineComments = new LinkedHashMap<>();

    // 左右分屏容器与子面板
    private final SplitPane contentSplitPane;
    private final VBox luaContainer;
    private final VBox bytecodeContainer;
    private HBox luaSubHeader;
    private HBox bytecodeSubHeader;

    // 双向光标同步防重入与去重
    private int lastSyncedLuaLine = -1;
    private int lastSyncedBytecodeParagraph = -1;
    private boolean isSyncingCaret = false;

    private VBox loadingOverlay;
    private ProgressIndicator progressIndicator;
    private Label loadingLabel;
    private Label warningLabel;
    
    private ToggleButton btnLuaView;
    private ToggleButton btnSideBySideView;
    private ToggleButton btnBytecodeView;
    private Label viewModeHintLabel;

    private Runnable onCaretOrSelectionChanged;

    public EditorTab(File file, TextEditorView textEditorView) {
        super(file != null ? file.getName() : "未命名");
        this.file = file;
        this.textEditorView = textEditorView;
        this.textEditorView.setCurrentFile(file);
        this.textEditorView.setEditable(false); // 确保 Lua 源码视图只读不可直接编辑，保护反编译成果

        // 监听变量重命名事件，维护符号重命名映射表
        this.textEditorView.setVariableRenameListener((oldName, newName) -> {
            this.renamedVariables.put(oldName, newName);
        });

        // 监听行注释变更事件，维护行注释映射表
        this.textEditorView.setCommentChangeListener((lineNum, comment) -> {
            if (comment != null && !comment.trim().isEmpty()) {
                this.lineComments.put(lineNum, comment.trim());
            } else {
                this.lineComments.remove(lineNum);
            }
        });
        
        // 创建字节码代码编辑器（与代码视图体验一致）
        this.bytecodeEditorView = new TextEditorView(false);
        this.bytecodeEditorView.setCurrentFile(file);
        this.bytecodeEditorView.getCodeArea().setEditable(false); // 字节码只读

        // 顶层主容器（包含顶部模式切换栏与编辑器内容区）
        VBox tabContentBox = new VBox();

        // 顶部精简模式切换条
        HBox headerBar = createHeaderBar();
        tabContentBox.getChildren().add(headerBar);

        // 初始化分屏容器与子容器
        contentSplitPane = new SplitPane();
        VBox.setVgrow(contentSplitPane, Priority.ALWAYS);

        luaSubHeader = createSubHeader("📄 Lua 源码 (反编译)");
        bytecodeSubHeader = createSubHeader("⚙️ 字节码反汇编 (Disassembly)");

        luaSubHeader.setVisible(false);
        luaSubHeader.setManaged(false);
        bytecodeSubHeader.setVisible(false);
        bytecodeSubHeader.setManaged(false);

        luaContainer = new VBox();
        Node luaNode = textEditorView.getView();
        VBox.setVgrow(luaNode, Priority.ALWAYS);
        luaContainer.getChildren().addAll(luaSubHeader, luaNode);

        bytecodeContainer = new VBox();
        Node bytecodeNode = bytecodeEditorView.getView();
        VBox.setVgrow(bytecodeNode, Priority.ALWAYS);
        bytecodeContainer.getChildren().addAll(bytecodeSubHeader, bytecodeNode);

        SplitPane.setResizableWithParent(luaContainer, true);
        SplitPane.setResizableWithParent(bytecodeContainer, true);

        // 初始为纯源码模式
        contentSplitPane.getItems().setAll(luaContainer);
        tabContentBox.getChildren().add(contentSplitPane);

        // StackPane 包含编辑器和加载覆盖层
        StackPane container = new StackPane();
        VBox.setVgrow(container, Priority.ALWAYS);
        container.getChildren().add(tabContentBox);

        // 初始化加载状态覆盖层
        loadingOverlay = new VBox();
        loadingOverlay.setAlignment(Pos.CENTER);
        loadingOverlay.setSpacing(15);
        loadingOverlay.getStyleClass().add("loading-overlay");

        progressIndicator = new ProgressIndicator();
        progressIndicator.setMinSize(50, 50);

        loadingLabel = new Label("正在反编译，请稍候...");
        loadingLabel.getStyleClass().add("loading-label");

        warningLabel = new Label("");
        warningLabel.setStyle("-fx-font-size: 12px; -fx-opacity: 0.75;");

        loadingOverlay.getChildren().addAll(progressIndicator, loadingLabel, warningLabel);
        loadingOverlay.setVisible(false);

        container.getChildren().add(loadingOverlay);
        setContent(container);

        // 监听光标与选区变化，支持状态栏实时更新及分屏模式双向联动
        textEditorView.getCodeArea().caretPositionProperty().addListener((obs, o, n) -> {
            notifyCaretOrSelectionChanged();
            if (viewMode == ViewMode.SIDE_BY_SIDE) {
                syncBytecodeFromLua(false);
            }
        });
        textEditorView.getCodeArea().selectionProperty().addListener((obs, o, n) -> notifyCaretOrSelectionChanged());

        bytecodeEditorView.getCodeArea().caretPositionProperty().addListener((obs, o, n) -> {
            notifyCaretOrSelectionChanged();
            if (viewMode == ViewMode.SIDE_BY_SIDE) {
                syncLuaFromBytecode(false);
            }
        });
        bytecodeEditorView.getCodeArea().selectionProperty().addListener((obs, o, n) -> notifyCaretOrSelectionChanged());
    }

    private HBox createSubHeader(String title) {
        HBox subHeader = new HBox(6);
        subHeader.setAlignment(Pos.CENTER_LEFT);
        subHeader.getStyleClass().add("editor-sub-header");
        Label label = new Label(title);
        label.getStyleClass().add("editor-sub-header-label");
        subHeader.getChildren().add(label);
        return subHeader;
    }

    public void setOnCaretOrSelectionChanged(Runnable callback) {
        this.onCaretOrSelectionChanged = callback;
    }

    private void notifyCaretOrSelectionChanged() {
        if (onCaretOrSelectionChanged != null) {
            onCaretOrSelectionChanged.run();
        }
    }

    private HBox createHeaderBar() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("editor-tab-header");

        viewModeHintLabel = new Label("Lua 源码视图");
        viewModeHintLabel.getStyleClass().add("view-mode-hint");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToggleGroup group = new ToggleGroup();
        btnLuaView = new ToggleButton("Lua 源码");
        btnSideBySideView = new ToggleButton("◫ 并排分屏");
        btnBytecodeView = new ToggleButton("字节码");

        btnLuaView.setToggleGroup(group);
        btnSideBySideView.setToggleGroup(group);
        btnBytecodeView.setToggleGroup(group);
        btnLuaView.setSelected(true);

        btnLuaView.setOnAction(e -> setViewMode(ViewMode.LUA_SOURCE));
        btnSideBySideView.setOnAction(e -> setViewMode(ViewMode.SIDE_BY_SIDE));
        btnBytecodeView.setOnAction(e -> setViewMode(ViewMode.BYTECODE));

        bar.getChildren().addAll(viewModeHintLabel, spacer, btnLuaView, btnSideBySideView, btnBytecodeView);
        return bar;
    }

    /**
     * 循环切换视图模式 (LUA_SOURCE -> SIDE_BY_SIDE -> BYTECODE -> LUA_SOURCE)
     */
    public void toggleBytecodeView() {
        switch (viewMode) {
            case LUA_SOURCE:
                setViewMode(ViewMode.SIDE_BY_SIDE);
                break;
            case SIDE_BY_SIDE:
                setViewMode(ViewMode.BYTECODE);
                break;
            case BYTECODE:
            default:
                setViewMode(ViewMode.LUA_SOURCE);
                break;
        }
    }

    /**
     * 设置是否显示纯字节码视图（兼容旧接口）
     * @param show 是否显示字节码
     */
    public void setShowingBytecode(boolean show) {
        setViewMode(show ? ViewMode.BYTECODE : ViewMode.LUA_SOURCE);
    }

    /**
     * 设置视图展示模式
     * @param mode 视图模式 (LUA_SOURCE, SIDE_BY_SIDE, BYTECODE)
     */
    public void setViewMode(ViewMode mode) {
        if (mode == null) {
            mode = ViewMode.LUA_SOURCE;
        }
        ViewMode oldMode = this.viewMode;
        this.viewMode = mode;
        this.showingBytecode = (mode == ViewMode.BYTECODE);

        // 如果需要字节码且尚未生成且有 luacFile，尝试即时生成
        if ((mode == ViewMode.BYTECODE || mode == ViewMode.SIDE_BY_SIDE)
                && luacFile != null
                && (bytecodeEditorView.getCodeArea().getText() == null || bytecodeEditorView.getCodeArea().getText().isEmpty())) {
            refreshBytecodeContent();
        }

        switch (mode) {
            case LUA_SOURCE:
                luaSubHeader.setVisible(false);
                luaSubHeader.setManaged(false);
                bytecodeSubHeader.setVisible(false);
                bytecodeSubHeader.setManaged(false);
                contentSplitPane.getItems().setAll(luaContainer);
                btnLuaView.setSelected(true);
                viewModeHintLabel.setText("Lua 源码视图");
                setText(file != null ? file.getName() : "未命名");

                if (oldMode == ViewMode.BYTECODE) {
                    int currentBytecodeParagraph = bytecodeEditorView.getCodeArea().getCurrentParagraph();
                    String lineText = (currentBytecodeParagraph >= 0 && currentBytecodeParagraph < bytecodeEditorView.getCodeArea().getParagraphs().size())
                            ? bytecodeEditorView.getCodeArea().getParagraph(currentBytecodeParagraph).getText() : null;
                    int sourceLine = BytecodeSyncUtils.extractSourceLineFromBytecode(lineText);
                    if (sourceLine > 0) {
                        Platform.runLater(() -> textEditorView.jumpToLine(sourceLine));
                    }
                }
                break;

            case BYTECODE:
                luaSubHeader.setVisible(false);
                luaSubHeader.setManaged(false);
                bytecodeSubHeader.setVisible(false);
                bytecodeSubHeader.setManaged(false);
                contentSplitPane.getItems().setAll(bytecodeContainer);
                btnBytecodeView.setSelected(true);
                viewModeHintLabel.setText("字节码反汇编视图");
                setText(file != null ? file.getName() + " [字节码]" : "字节码");

                int currentLuaLine = textEditorView.getCurrentLineNumber();
                Platform.runLater(() -> {
                    String bytecodeText = bytecodeEditorView.getCodeArea().getText();
                    int targetParagraph = BytecodeSyncUtils.findBytecodeParagraphForSourceLine(bytecodeText, currentLuaLine);
                    if (targetParagraph >= 0) {
                        bytecodeEditorView.jumpToLine(targetParagraph + 1);
                    }
                });
                break;

            case SIDE_BY_SIDE:
                luaSubHeader.setVisible(true);
                luaSubHeader.setManaged(true);
                bytecodeSubHeader.setVisible(true);
                bytecodeSubHeader.setManaged(true);
                contentSplitPane.getItems().setAll(luaContainer, bytecodeContainer);
                contentSplitPane.setDividerPositions(0.5);
                btnSideBySideView.setSelected(true);
                viewModeHintLabel.setText("源码与字节码分屏对照 (Side-by-Side)");
                setText(file != null ? file.getName() + " [分屏]" : "分屏");

                Platform.runLater(() -> syncBytecodeFromLua(true));
                break;
        }
        notifyCaretOrSelectionChanged();
    }

    /**
     * 在分屏模式下，根据 Lua 源码行号同步定位字节码反汇编指令
     * @param force 是否强制更新跳转
     */
    public void syncBytecodeFromLua(boolean force) {
        if (isSyncingCaret && !force) {
            return;
        }
        int currentLuaLine = textEditorView.getCurrentLineNumber();
        if (!force && currentLuaLine == lastSyncedLuaLine) {
            return;
        }
        if (currentLuaLine <= 0) {
            return;
        }
        lastSyncedLuaLine = currentLuaLine;
        String bytecodeText = bytecodeEditorView.getCodeArea().getText();
        if (bytecodeText == null || bytecodeText.isEmpty()) {
            return;
        }
        int targetParagraph = BytecodeSyncUtils.findBytecodeParagraphForSourceLine(bytecodeText, currentLuaLine);
        if (targetParagraph >= 0 && (force || targetParagraph != lastSyncedBytecodeParagraph)) {
            lastSyncedBytecodeParagraph = targetParagraph;
            try {
                isSyncingCaret = true;
                bytecodeEditorView.jumpToLine(targetParagraph + 1);
            } finally {
                isSyncingCaret = false;
            }
        }
    }

    /**
     * 在分屏模式下，根据当前字节码指令提取源行号并同步定位 Lua 源码
     * @param force 是否强制更新跳转
     */
    public void syncLuaFromBytecode(boolean force) {
        if (isSyncingCaret && !force) {
            return;
        }
        int paragraph = bytecodeEditorView.getCodeArea().getCurrentParagraph();
        if (!force && paragraph == lastSyncedBytecodeParagraph) {
            return;
        }
        if (paragraph < 0 || paragraph >= bytecodeEditorView.getCodeArea().getParagraphs().size()) {
            return;
        }
        lastSyncedBytecodeParagraph = paragraph;
        String lineText = bytecodeEditorView.getCodeArea().getParagraph(paragraph).getText();
        int sourceLine = BytecodeSyncUtils.extractSourceLineFromBytecode(lineText);
        if (sourceLine > 0 && (force || sourceLine != lastSyncedLuaLine)) {
            lastSyncedLuaLine = sourceLine;
            try {
                isSyncingCaret = true;
                textEditorView.jumpToLine(sourceLine);
            } finally {
                isSyncingCaret = false;
            }
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

    public ViewMode getViewMode() {
        return viewMode;
    }

    public boolean isSideBySide() {
        return viewMode == ViewMode.SIDE_BY_SIDE;
    }

    public boolean isShowingBytecode() {
        return viewMode == ViewMode.BYTECODE;
    }

    public TextEditorView getLuaEditorView() {
        return textEditorView;
    }

    public TextEditorView getBytecodeEditorView() {
        return bytecodeEditorView;
    }

    public TextEditorView getActiveEditorView() {
        if (viewMode == ViewMode.BYTECODE) {
            return bytecodeEditorView;
        } else if (viewMode == ViewMode.SIDE_BY_SIDE) {
            return bytecodeEditorView.getCodeArea().isFocused() ? bytecodeEditorView : textEditorView;
        } else {
            return textEditorView;
        }
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        this.setText(file.getName());
        if (textEditorView != null) {
            textEditorView.setCurrentFile(file);
        }
        if (bytecodeEditorView != null) {
            bytecodeEditorView.setCurrentFile(file);
        }
    }

    public TextEditorView getTextEditorView() {
        return textEditorView;
    }

    public LuacFile getLuacFile() {
        return luacFile;
    }

    public void setLuacFile(LuacFile luacFile) {
        this.luacFile = luacFile;
        if (luacFile != null && (viewMode == ViewMode.BYTECODE || viewMode == ViewMode.SIDE_BY_SIDE)) {
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

    /**
     * 获取所有已记录的变量重命名映射（只读）
     * @return 原变量名 -> 新变量名 映射
     */
    public Map<String, String> getRenamedVariables() {
        return Collections.unmodifiableMap(renamedVariables);
    }

    /**
     * 获取所有已记录的行注释映射（只读）
     * @return 行号 -> 注释内容 映射
     */
    public Map<Integer, String> getLineComments() {
        return Collections.unmodifiableMap(lineComments);
    }
}
