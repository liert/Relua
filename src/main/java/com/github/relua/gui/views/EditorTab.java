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
import javafx.scene.control.CheckBox;
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
import java.util.function.Consumer;

/**
 * 管理打开文件的 Code/Bytecode 视图、独立分屏选择、分析注释与双向联动。
 */
public class EditorTab extends Tab {

    /**
     * 视图展示模式枚举
     */
    public enum ViewMode {
        LUA_SOURCE("Code"),
        SIDE_BY_SIDE("Split view"),
        BYTECODE("Bytecode");

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
    private ViewMode primaryViewMode = ViewMode.LUA_SOURCE;
    private ViewMode secondaryViewMode = ViewMode.BYTECODE;
    private boolean splitView;
    private boolean fileLoaded;
    
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
    private final VBox primaryContainer;
    private final VBox secondaryContainer;
    private final HBox primaryViewModeBar;
    private final HBox secondaryViewModeBar;
    private TextEditorView primaryEditor;
    private TextEditorView secondaryEditor;
    private TextEditorView duplicateLuaEditor;
    private TextEditorView duplicateBytecodeEditor;
    private TextEditorView lastActiveEditor;
    private boolean syncingEditorText;
    private Consumer<TextEditorView> editorConfigurator;

    // 双向光标同步防重入与去重
    private int lastSyncedLuaLine = -1;
    private int lastSyncedBytecodeParagraph = -1;
    private boolean isSyncingCaret = false;

    private VBox loadingOverlay;
    private ProgressIndicator progressIndicator;
    private Label loadingLabel;
    private Label warningLabel;
    
    private ToggleButton btnLuaView;
    private ToggleButton btnBytecodeView;
    private ToggleButton btnSecondaryLuaView;
    private ToggleButton btnSecondaryBytecodeView;
    private CheckBox splitViewCheckBox;

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

        // 顶层主容器（包含编辑器内容区与底部视图标签栏）
        VBox tabContentBox = new VBox();
        primaryViewModeBar = createViewModeBar(false);
        secondaryViewModeBar = createViewModeBar(true);

        // 初始化分屏容器与子容器
        contentSplitPane = new SplitPane();
        VBox.setVgrow(contentSplitPane, Priority.ALWAYS);

        primaryContainer = new VBox();
        secondaryContainer = new VBox();
        SplitPane.setResizableWithParent(primaryContainer, true);
        SplitPane.setResizableWithParent(secondaryContainer, true);

        // 初始为纯源码模式
        updateViewLayout();
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

        observeEditor(textEditorView, ViewMode.LUA_SOURCE);
        observeEditor(bytecodeEditorView, ViewMode.BYTECODE);
    }

    private void observeEditor(TextEditorView editor, ViewMode mode) {
        editor.getCodeArea().caretPositionProperty().addListener((obs, o, n) -> {
            if (syncingEditorText || (editor != primaryEditor && editor != secondaryEditor)) return;
            if (!isSyncingCaret) lastActiveEditor = editor;
            notifyCaretOrSelectionChanged();
            if (splitView) {
                if (mode == ViewMode.LUA_SOURCE) syncBytecodeFromLua(false);
                else syncLuaFromBytecode(false);
            }
        });
        editor.getCodeArea().selectionProperty().addListener((obs, o, n) -> notifyCaretOrSelectionChanged());
        editor.getCodeArea().focusedProperty().addListener((obs, o, focused) -> {
            if (focused) {
                lastActiveEditor = editor;
                notifyCaretOrSelectionChanged();
            }
        });
    }

    public void setEditorConfigurator(Consumer<TextEditorView> configurator) {
        editorConfigurator = configurator;
        if (configurator != null) {
            configurator.accept(textEditorView);
            configurator.accept(bytecodeEditorView);
            if (duplicateLuaEditor != null) configurator.accept(duplicateLuaEditor);
            if (duplicateBytecodeEditor != null) configurator.accept(duplicateBytecodeEditor);
        }
    }

    public void setOnCaretOrSelectionChanged(Runnable callback) {
        this.onCaretOrSelectionChanged = callback;
    }

    private void notifyCaretOrSelectionChanged() {
        if (onCaretOrSelectionChanged != null) {
            onCaretOrSelectionChanged.run();
        }
    }

    private HBox createViewModeBar(boolean secondary) {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("editor-view-mode-bar");

        ToggleGroup group = new ToggleGroup();
        ToggleButton codeButton = createViewModeButton(ViewMode.LUA_SOURCE, group, secondary);
        ToggleButton bytecodeButton = createViewModeButton(ViewMode.BYTECODE, group, secondary);
        bar.getChildren().addAll(codeButton, bytecodeButton);
        if (secondary) {
            btnSecondaryLuaView = codeButton;
            btnSecondaryBytecodeView = bytecodeButton;
            bytecodeButton.setSelected(true);
        } else {
            btnLuaView = codeButton;
            btnBytecodeView = bytecodeButton;
            codeButton.setSelected(true);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            splitViewCheckBox = new CheckBox("Split view");
            splitViewCheckBox.getStyleClass().add("editor-split-view-toggle");
            splitViewCheckBox.setMinWidth(Region.USE_PREF_SIZE);
            splitViewCheckBox.setOnAction(e -> setSplitView(splitViewCheckBox.isSelected()));
            bar.getChildren().addAll(spacer, splitViewCheckBox);
        }
        return bar;
    }

    private ToggleButton createViewModeButton(ViewMode mode, ToggleGroup group, boolean secondary) {
        ToggleButton button = new ToggleButton(mode.getDisplayName());
        button.getStyleClass().add("editor-view-mode-tab");
        button.setToggleGroup(group);
        button.setMinWidth(Region.USE_PREF_SIZE);
        button.setOnAction(e -> {
            if (secondary) setSecondaryViewMode(mode);
            else setPrimaryViewMode(mode);
        });
        return button;
    }

    public ViewMode getPrimaryViewMode() {
        return primaryViewMode;
    }

    public ViewMode getSecondaryViewMode() {
        return secondaryViewMode;
    }

    public void setPrimaryViewMode(ViewMode mode) {
        ViewMode oldMode = primaryViewMode;
        TextEditorView oldEditor = primaryEditor;
        primaryViewMode = mode == ViewMode.BYTECODE ? ViewMode.BYTECODE : ViewMode.LUA_SOURCE;
        updateViewLayout();
        alignCaretAfterViewChange(oldEditor, oldMode, primaryEditor, primaryViewMode);
        lastActiveEditor = primaryEditor;
        notifyCaretOrSelectionChanged();
    }

    public void setSecondaryViewMode(ViewMode mode) {
        ViewMode oldMode = secondaryViewMode;
        TextEditorView oldEditor = secondaryEditor;
        secondaryViewMode = mode == ViewMode.BYTECODE ? ViewMode.BYTECODE : ViewMode.LUA_SOURCE;
        updateViewLayout();
        alignCaretAfterViewChange(oldEditor, oldMode, secondaryEditor, secondaryViewMode);
        if (splitView) lastActiveEditor = secondaryEditor;
        notifyCaretOrSelectionChanged();
    }

    public void setSplitView(boolean enabled) {
        if (enabled && !splitView) secondaryViewMode = ViewMode.BYTECODE;
        splitView = enabled;
        updateViewLayout();
        lastActiveEditor = primaryEditor;
        notifyCaretOrSelectionChanged();
    }

    private void updateViewLayout() {
        viewMode = splitView ? ViewMode.SIDE_BY_SIDE : primaryViewMode;
        splitViewCheckBox.setSelected(splitView);
        if ((primaryViewMode == ViewMode.BYTECODE || (splitView && secondaryViewMode == ViewMode.BYTECODE))
                && luacFile != null && bytecodeEditorView.getText().isEmpty()) {
            refreshBytecodeContent();
        }

        primaryEditor = primaryViewMode == ViewMode.BYTECODE ? bytecodeEditorView : textEditorView;
        secondaryEditor = null;
        if (splitView) {
            secondaryEditor = secondaryViewMode == primaryViewMode
                    ? getDuplicateEditor(secondaryViewMode)
                    : (secondaryViewMode == ViewMode.BYTECODE ? bytecodeEditorView : textEditorView);
        }

        // 先释放两个面板的节点，允许 Code/Bytecode 在两侧之间移动。
        primaryContainer.getChildren().clear();
        secondaryContainer.getChildren().clear();
        Node primaryNode = createEditorContent(primaryEditor, primaryViewMode);
        VBox.setVgrow(primaryNode, Priority.ALWAYS);
        primaryContainer.getChildren().addAll(primaryNode, primaryViewModeBar);
        if (splitView) {
            Node secondaryNode = createEditorContent(secondaryEditor, secondaryViewMode);
            VBox.setVgrow(secondaryNode, Priority.ALWAYS);
            secondaryContainer.getChildren().addAll(secondaryNode, secondaryViewModeBar);
            if (contentSplitPane.getItems().size() != 2) {
                contentSplitPane.getItems().setAll(primaryContainer, secondaryContainer);
                contentSplitPane.setDividerPositions(0.5);
            }
        } else {
            contentSplitPane.getItems().setAll(primaryContainer);
        }

        btnLuaView.setSelected(primaryViewMode == ViewMode.LUA_SOURCE);
        btnBytecodeView.setSelected(primaryViewMode == ViewMode.BYTECODE);
        btnSecondaryLuaView.setSelected(secondaryViewMode == ViewMode.LUA_SOURCE);
        btnSecondaryBytecodeView.setSelected(secondaryViewMode == ViewMode.BYTECODE);
        String title = file != null ? file.getName() : "未命名";
        setText(title + (splitView ? " [分屏]" : primaryViewMode == ViewMode.BYTECODE ? " [字节码]" : ""));
        lastSyncedLuaLine = -1;
        lastSyncedBytecodeParagraph = -1;
        if (splitView) Platform.runLater(() -> syncBytecodeFromLua(true));
    }

    private Node createEditorContent(TextEditorView editor, ViewMode mode) {
        if (mode != ViewMode.BYTECODE || !fileLoaded || luacFile != null || !editor.getText().isEmpty()) {
            return editor.getView();
        }
        Label title = new Label("没有可显示的字节码");
        title.getStyleClass().add("bytecode-unavailable-title");
        Label description = new Label("当前文件为 Lua 文本，未包含原始字节码。\n请打开原始编译文件查看 Bytecode。");
        description.setWrapText(true);
        description.setMaxWidth(360);
        description.getStyleClass().add("bytecode-unavailable-description");
        VBox placeholder = new VBox(10, title, description);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.getStyleClass().add("bytecode-unavailable");
        return placeholder;
    }

    private TextEditorView getDuplicateEditor(ViewMode mode) {
        TextEditorView duplicate = mode == ViewMode.BYTECODE ? duplicateBytecodeEditor : duplicateLuaEditor;
        if (duplicate != null) return duplicate;

        TextEditorView original = mode == ViewMode.BYTECODE ? bytecodeEditorView : textEditorView;
        duplicate = new TextEditorView(false);
        duplicate.setCurrentFile(file);
        duplicate.setEditable(false);
        duplicate.setText(original.getText());
        if (mode == ViewMode.BYTECODE) duplicateBytecodeEditor = duplicate;
        else duplicateLuaEditor = duplicate;
        observeEditor(duplicate, mode);
        if (mode == ViewMode.LUA_SOURCE) {
            duplicate.setVariableRenameListener((oldName, newName) -> renamedVariables.put(oldName, newName));
            duplicate.setCommentChangeListener((lineNum, comment) -> {
                if (comment != null && !comment.trim().isEmpty()) lineComments.put(lineNum, comment.trim());
                else lineComments.remove(lineNum);
            });
        }
        TextEditorView mirror = duplicate;
        original.getCodeArea().textProperty().addListener((obs, oldText, newText) -> mirrorEditorText(mirror, newText));
        mirror.getCodeArea().textProperty().addListener((obs, oldText, newText) -> mirrorEditorText(original, newText));
        if (editorConfigurator != null) editorConfigurator.accept(duplicate);
        return duplicate;
    }

    private void alignCaretAfterViewChange(TextEditorView oldEditor, ViewMode oldMode,
                                          TextEditorView newEditor, ViewMode newMode) {
        if (oldEditor == null || newEditor == null || oldMode == newMode) return;
        int targetLine;
        if (newMode == ViewMode.BYTECODE) {
            int paragraph = BytecodeSyncUtils.findBytecodeParagraphForSourceLine(
                    newEditor.getText(), oldEditor.getCurrentLineNumber());
            targetLine = paragraph >= 0 ? paragraph + 1 : -1;
        } else {
            targetLine = BytecodeSyncUtils.extractSourceLineFromBytecode(
                    oldEditor.getCodeArea().getParagraph(oldEditor.getCodeArea().getCurrentParagraph()).getText());
        }
        if (targetLine > 0) {
            Platform.runLater(() -> {
                if (newEditor == primaryEditor || newEditor == secondaryEditor) newEditor.jumpToLine(targetLine);
            });
        }
    }

    private void mirrorEditorText(TextEditorView target, String text) {
        if (syncingEditorText || target.getText().equals(text)) return;
        syncingEditorText = true;
        try {
            int caret = target.getCodeArea().getCaretPosition();
            target.setText(text);
            target.getCodeArea().moveTo(Math.min(caret, text.length()));
        } finally {
            syncingEditorText = false;
        }
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
        splitView = mode == ViewMode.SIDE_BY_SIDE;
        primaryViewMode = mode == ViewMode.BYTECODE ? ViewMode.BYTECODE : ViewMode.LUA_SOURCE;
        if (splitView && oldMode != ViewMode.SIDE_BY_SIDE) secondaryViewMode = ViewMode.BYTECODE;
        updateViewLayout();
        lastActiveEditor = primaryEditor;
        switch (mode) {
            case LUA_SOURCE:
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
                break;
        }
        notifyCaretOrSelectionChanged();
    }

    /**
     * 在分屏模式下，根据 Lua 源码行号同步定位字节码反汇编指令
     * @param force 是否强制更新跳转
     */
    public void syncBytecodeFromLua(boolean force) {
        TextEditorView luaEditor = getVisibleEditor(ViewMode.LUA_SOURCE);
        TextEditorView bytecodeEditor = getVisibleEditor(ViewMode.BYTECODE);
        if (!splitView || luaEditor == null || bytecodeEditor == null) return;
        if (isSyncingCaret && !force) {
            return;
        }
        int currentLuaLine = luaEditor.getCurrentLineNumber();
        if (!force && currentLuaLine == lastSyncedLuaLine) {
            return;
        }
        if (currentLuaLine <= 0) {
            return;
        }
        lastSyncedLuaLine = currentLuaLine;
        String bytecodeText = bytecodeEditor.getCodeArea().getText();
        if (bytecodeText == null || bytecodeText.isEmpty()) {
            return;
        }
        int targetParagraph = BytecodeSyncUtils.findBytecodeParagraphForSourceLine(bytecodeText, currentLuaLine);
        if (targetParagraph >= 0 && (force || targetParagraph != lastSyncedBytecodeParagraph)) {
            lastSyncedBytecodeParagraph = targetParagraph;
            try {
                isSyncingCaret = true;
                bytecodeEditor.jumpToLine(targetParagraph + 1);
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
        TextEditorView luaEditor = getVisibleEditor(ViewMode.LUA_SOURCE);
        TextEditorView bytecodeEditor = getVisibleEditor(ViewMode.BYTECODE);
        if (!splitView || luaEditor == null || bytecodeEditor == null) return;
        if (isSyncingCaret && !force) {
            return;
        }
        int paragraph = bytecodeEditor.getCodeArea().getCurrentParagraph();
        if (!force && paragraph == lastSyncedBytecodeParagraph) {
            return;
        }
        if (paragraph < 0 || paragraph >= bytecodeEditor.getCodeArea().getParagraphs().size()) {
            return;
        }
        lastSyncedBytecodeParagraph = paragraph;
        String lineText = bytecodeEditor.getCodeArea().getParagraph(paragraph).getText();
        int sourceLine = BytecodeSyncUtils.extractSourceLineFromBytecode(lineText);
        if (sourceLine > 0 && (force || sourceLine != lastSyncedLuaLine)) {
            lastSyncedLuaLine = sourceLine;
            try {
                isSyncingCaret = true;
                luaEditor.jumpToLine(sourceLine);
            } finally {
                isSyncingCaret = false;
            }
        }
    }

    private TextEditorView getVisibleEditor(ViewMode mode) {
        if (primaryViewMode == mode) return primaryEditor;
        return splitView && secondaryViewMode == mode ? secondaryEditor : null;
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
        if (secondaryEditor != null && secondaryEditor.getCodeArea().isFocused()) return secondaryEditor;
        if (primaryEditor.getCodeArea().isFocused()) return primaryEditor;
        return lastActiveEditor == secondaryEditor && secondaryEditor != null ? secondaryEditor : primaryEditor;
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
        if (duplicateLuaEditor != null) duplicateLuaEditor.setCurrentFile(file);
        if (duplicateBytecodeEditor != null) duplicateBytecodeEditor.setCurrentFile(file);
    }

    public TextEditorView getTextEditorView() {
        return textEditorView;
    }

    public LuacFile getLuacFile() {
        return luacFile;
    }

    public boolean isFileLoaded() {
        return fileLoaded;
    }

    public void setLuacFile(LuacFile luacFile) {
        this.luacFile = luacFile;
        fileLoaded = true;
        // 文件类型由解析结果决定，清除之前显示的内容，避免误用旧字节码。
        bytecodeEditorView.setText("");
        if (luacFile != null && (viewMode == ViewMode.BYTECODE || viewMode == ViewMode.SIDE_BY_SIDE)) {
            refreshBytecodeContent();
        }
        updateViewLayout();
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

    public void restoreAnalysisState(Map<String, String> renamed, Map<Integer, String> comments) {
        renamedVariables.clear();
        if (renamed != null) renamedVariables.putAll(renamed);
        lineComments.clear();
        if (comments != null) lineComments.putAll(comments);
    }

    /**
     * 获取所有已记录的行注释映射（只读）
     * @return 行号 -> 注释内容 映射
     */
    public Map<Integer, String> getLineComments() {
        return Collections.unmodifiableMap(lineComments);
    }
}
