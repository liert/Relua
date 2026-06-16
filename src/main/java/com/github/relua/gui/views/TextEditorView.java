package com.github.relua.gui.views;

import com.github.relua.gui.utils.I18nUtil;
import javafx.scene.Node;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本编辑器视图类，使用RichTextFX实现高级文本编辑功能
 */
public class TextEditorView {
    // RichTextFX的CodeArea组件
    private final CodeArea codeArea;
    private final VirtualizedScrollPane<CodeArea> virtualizedScrollPane;
    
    // Lua语法高亮的正则表达式
    private static final String KEYWORDS = "\\b(and|break|do|else|elseif|end|false|for|function|goto|if|in|local|nil|not|or|repeat|return|then|true|until|while)\\b";
    private static final String STRINGS = "\\\"([^\\\\\"\\r\\n]|\\\\.)*\\\"|\\'([^\\\\\\'\\r\\n]|\\\\.)*\\'";
    private static final String COMMENTS = "--.*$";
    private static final String NUMBERS = "\\b\\d+\\.?\\d*\\b";
    private static final String GLOBALVAR = "\\b(global_|chunk_|module_)[A-Za-z0-9_]+\\b";
    private static final String METHODCALL = "(?<=:)[A-Za-z_][A-Za-z0-9_]*";
    private static final String TABLEFIELD = "(?<=\\.)[A-Za-z_][A-Za-z0-9_]*";
    private static final String FUNCTION = "\\b[A-Za-z_][A-Za-z0-9_]*(?=\\s*\\()";
    private static final String LOCALVAR = "\\b[A-Za-z_][A-Za-z0-9_]*\\b";
    private static final String OPERATORS = "==|~=|<=|>=|\\.\\.|[\\+\\-\\*\\/\\%\\^\\#\\=\\<\\>\\:]";
    
    // 组合正则表达式
    private static final String PATTERN = 
          "(?<COMMENTS>" + COMMENTS + ")"
        + "|(?<STRINGS>" + STRINGS + ")"
        + "|(?<KEYWORDS>" + KEYWORDS + ")"
        + "|(?<GLOBALVAR>" + GLOBALVAR + ")"
        + "|(?<METHODCALL>" + METHODCALL + ")"
        + "|(?<TABLEFIELD>" + TABLEFIELD + ")"
        + "|(?<FUNCTION>" + FUNCTION + ")"
        + "|(?<LOCALVAR>" + LOCALVAR + ")"
        + "|(?<NUMBERS>" + NUMBERS + ")"
        + "|(?<OPERATORS>" + OPERATORS + ")";
    private static final Pattern SYNTAX_PATTERN = Pattern.compile(PATTERN);
    
    private com.github.relua.gui.controllers.LuaCodeIntelligenceController codeIntelligenceController;

    /**
     * 构造函数
     */
    public TextEditorView() {
        // 初始化CodeArea
        codeArea = new CodeArea();
        codeArea.setWrapText(false); // 禁用自动换行，长行出现水平滚动条
        virtualizedScrollPane = new VirtualizedScrollPane<>(codeArea);
        
        // 设置行号
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        
        // 加载 Lua 语法高亮样式表
        try {
            String cssPath = getClass().getResource("/css/lua-highlight.css").toExternalForm();
            codeArea.getStylesheets().add(cssPath);
        } catch (Exception e) {
            java.util.logging.Logger.getLogger("TextEditorView").warning("无法加载 Lua 语法高亮样式表: " + e.getMessage());
        }
        
        // 启用撤销/重做功能（使用默认 of UndoManager）
        
        // 启用语法高亮
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            codeArea.setStyleSpans(0, computeHighlighting(newText));
        });
        
        // 设置字体和字体大小
        codeArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14px;");
        
        // 设置CodeArea的尺寸约束，确保它能够扩展到可用空间
        codeArea.setPrefWidth(Double.MAX_VALUE);
        codeArea.setPrefHeight(Double.MAX_VALUE);
        codeArea.setMaxWidth(Double.MAX_VALUE);
        codeArea.setMaxHeight(Double.MAX_VALUE);
        // 设置最小高度，确保至少能显示10行文本内容（假设每行高度为20px）
        codeArea.setMinHeight(200);

        // 设置 virtualizedScrollPane 的尺寸约束，确保能填充 ScrollPane Content 区域
        virtualizedScrollPane.setPrefWidth(Double.MAX_VALUE);
        virtualizedScrollPane.setPrefHeight(Double.MAX_VALUE);
        virtualizedScrollPane.setMaxWidth(Double.MAX_VALUE);
        virtualizedScrollPane.setMaxHeight(Double.MAX_VALUE);
        
        // 设置初始文本
        codeArea.replaceText(I18nUtil.getString("editor.welcome"));
    }
    
    /**
     * 初始化代码智能提示与跳转功能
     * @param statusLabel 状态栏标签
     */
    public void initCodeIntelligence(javafx.scene.control.Label statusLabel) {
        this.codeIntelligenceController = new com.github.relua.gui.controllers.LuaCodeIntelligenceController(codeArea, statusLabel);
        this.codeIntelligenceController.rebuildIndex();
    }
    
    public com.github.relua.gui.controllers.LuaCodeIntelligenceController getCodeIntelligenceController() {
        return codeIntelligenceController;
    }
    
    /**
     * 计算语法高亮样式
     * @param text 文本内容
     * @return 样式跨度
     */
    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        if (text == null || text.isEmpty()) {
            StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
            spansBuilder.add(Collections.emptyList(), 0);
            return spansBuilder.create();
        }
        Matcher matcher = SYNTAX_PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        
        while (matcher.find()) {
            String styleClass = null;
            if (matcher.group("KEYWORDS") != null) {
                styleClass = "keyword";
            } else if (matcher.group("STRINGS") != null) {
                styleClass = "string";
            } else if (matcher.group("COMMENTS") != null) {
                styleClass = "comment";
            } else if (matcher.group("NUMBERS") != null) {
                styleClass = "number";
            } else if (matcher.group("GLOBALVAR") != null) {
                styleClass = "global-var";
            } else if (matcher.group("METHODCALL") != null) {
                styleClass = "method-call";
            } else if (matcher.group("TABLEFIELD") != null) {
                styleClass = "table-field";
            } else if (matcher.group("FUNCTION") != null) {
                styleClass = "function";
            } else if (matcher.group("LOCALVAR") != null) {
                styleClass = "local-var";
            } else if (matcher.group("OPERATORS") != null) {
                styleClass = "operator";
            }
            
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
    
    /**
     * 获取视图节点
     * @return 视图节点
     */
    public Node getView() {
        return virtualizedScrollPane;
    }
    
    /**
     * 获取RichTextFX的CodeArea组件
     * @return CodeArea组件
     */
    public org.fxmisc.richtext.CodeArea getCodeArea() {
        return codeArea;
    }
    
    /**
     * 设置文本内容
     * @param text 文本内容
     */
    public void setText(String text) {
        codeArea.replaceText(text);
        if (codeIntelligenceController != null) {
            codeIntelligenceController.rebuildIndex();
        }
    }
    
    /**
     * 获取文本内容
     * @return 文本内容
     */
    public String getText() {
        return codeArea.getText();
    }
    
    /**
     * 将文本保存到文件
     * @param file 文件对象
     * @throws IOException IO异常
     */
    public void saveToFile(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(codeArea.getText());
        }
    }
    
    // 编辑操作方法
    
    /**
     * 撤销操作
     */
    public void undo() {
        codeArea.undo();
    }
    
    /**
     * 重做操作
     */
    public void redo() {
        codeArea.redo();
    }
    
    /**
     * 剪切操作
     */
    public void cut() {
        codeArea.cut();
    }
    
    /**
     * 复制操作
     */
    public void copy() {
        codeArea.copy();
    }
    
    /**
     * 粘贴操作
     */
    public void paste() {
        codeArea.paste();
    }
    
    /**
     * 选择所有文本
     */
    public void selectAll() {
        codeArea.selectAll();
    }
}