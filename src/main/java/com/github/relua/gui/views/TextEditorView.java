package com.github.relua.gui.views;

import com.github.relua.gui.utils.I18nUtil;
import javafx.scene.Node;
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
    
    // Lua语法高亮的正则表达式
    private static final String KEYWORDS = "\\b(and|break|do|else|elseif|end|false|for|function|goto|if|in|local|nil|not|or|repeat|return|then|true|until|while)\\b";
    private static final String STRINGS = "\\\"([^\\\\\"\\r\\n]|\\\\.)*\\\"";
    private static final String COMMENTS = "--.*$";
    private static final String NUMBERS = "\\b\\d+\\.?\\d*\\b";
    
    // 组合正则表达式
    private static final String PATTERN = "(?<KEYWORDS>" + KEYWORDS + ")|(?<STRINGS>" + STRINGS + ")|(?<COMMENTS>" + COMMENTS + ")|(?<NUMBERS>" + NUMBERS + ")";
    private static final Pattern SYNTAX_PATTERN = Pattern.compile(PATTERN);
    
    /**
     * 构造函数
     */
    public TextEditorView() {
        // 初始化CodeArea
        codeArea = new CodeArea();
        
        // 设置行号
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        
        // 启用撤销/重做功能（使用默认的UndoManager）
        
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
        
        // 设置初始文本
        codeArea.replaceText(I18nUtil.getString("editor.welcome"));
    }
    
    /**
     * 计算语法高亮样式
     * @param text 文本内容
     * @return 样式跨度
     */
    private StyleSpans<Collection<String>> computeHighlighting(String text) {
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
        return codeArea;
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