package com.github.relua.gui.views;

import com.github.relua.gui.utils.I18nUtil;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 文本编辑器视图类，使用RichTextFX实现高级文本编辑功能
 */
public class TextEditorView {
    // RichTextFX的CodeArea组件
    private final FoldableCodeArea codeArea;
    private final VirtualizedScrollPane<CodeArea> virtualizedScrollPane;
    private final Popup searchPopup;
    private final HBox searchBar;
    private final TextField searchField;
    private final Label searchStatusLabel;
    private final CheckBox matchCaseCheck;
    private final CheckBox wholeWordCheck;
    private final CheckBox regexCheck;
    private final Button previousButton;
    private final Button nextButton;
    private final Button closeSearchButton;
    private final Button collapseAllButton;
    private final Button expandAllButton;
    
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
    private final List<SearchMatch> searchMatches = new ArrayList<>();
    private final List<FunctionFold> functionFolds = new ArrayList<>();
    private final Set<Integer> foldedFunctionStarts = new HashSet<>();
    private int currentSearchIndex = -1;
    private boolean refreshingStyles = false;
    private boolean rebuildingFolds = false;
    private static final Pattern LUA_TOKEN_PATTERN = Pattern.compile("\\b(function|end|repeat|until|if|for|while)\\b");

    /**
     * 构造函数
     */
    public TextEditorView() {
        // 初始化CodeArea
        codeArea = new FoldableCodeArea();
        codeArea.setWrapText(false); // 禁用自动换行，长行出现水平滚动条
        virtualizedScrollPane = new VirtualizedScrollPane<>(codeArea);
        searchPopup = new Popup();
        searchField = new TextField();
        searchStatusLabel = new Label("");
        matchCaseCheck = new CheckBox("Aa");
        wholeWordCheck = new CheckBox("Word");
        regexCheck = new CheckBox(".*");
        previousButton = new Button("↑");
        nextButton = new Button("↓");
        closeSearchButton = new Button("×");
        collapseAllButton = new Button("折叠全部");
        expandAllButton = new Button("展开全部");
        searchBar = createSearchBar();
        searchPopup.getContent().add(searchBar);
        searchPopup.setAutoFix(true);
        searchPopup.setHideOnEscape(false);
        
        // 设置行号和折叠标识
        installParagraphGraphics();
        
        // 加载 Lua 语法高亮样式表
        try {
            String cssPath = getClass().getResource("/css/lua-highlight.css").toExternalForm();
            codeArea.getStylesheets().add(cssPath);
            searchBar.getStylesheets().add(cssPath);
        } catch (Exception e) {
            java.util.logging.Logger.getLogger("TextEditorView").warning("无法加载 Lua 语法高亮样式表: " + e.getMessage());
        }
        
        // 启用撤销/重做功能（使用默认 of UndoManager）
        
        // 启用语法高亮
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (!refreshingStyles) {
                rebuildFunctionFolds();
                updateSearchMatches(false);
                refreshHighlighting();
            }
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
        installSearchHandlers();
        
        // 设置初始文本
        codeArea.replaceText(I18nUtil.getString("editor.welcome"));
    }

    private HBox createSearchBar() {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("editor-search-bar");
        searchField.setPromptText("搜索当前文件");
        searchField.getStyleClass().add("editor-search-field");
        searchField.setPrefColumnCount(28);
        previousButton.getStyleClass().add("editor-search-button");
        nextButton.getStyleClass().add("editor-search-button");
        closeSearchButton.getStyleClass().add("editor-search-button");
        collapseAllButton.getStyleClass().add("editor-search-button");
        expandAllButton.getStyleClass().add("editor-search-button");
        searchStatusLabel.getStyleClass().add("editor-search-status");
        bar.getChildren().addAll(
                new Label("查找"), searchField,
                previousButton, nextButton,
                matchCaseCheck, wholeWordCheck, regexCheck,
                searchStatusLabel, collapseAllButton, expandAllButton, closeSearchButton);
        return bar;
    }

    private void installSearchHandlers() {
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.F) {
                openSearch();
                event.consume();
            }
        });
        searchField.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    jumpToPreviousMatch();
                } else {
                    jumpToNextMatch();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                closeSearch();
                event.consume();
            }
        });
        searchField.textProperty().addListener((obs, oldValue, newValue) -> updateSearchMatches(true));
        matchCaseCheck.selectedProperty().addListener((obs, oldValue, newValue) -> updateSearchMatches(true));
        wholeWordCheck.selectedProperty().addListener((obs, oldValue, newValue) -> updateSearchMatches(true));
        regexCheck.selectedProperty().addListener((obs, oldValue, newValue) -> updateSearchMatches(true));
        previousButton.setOnAction(event -> jumpToPreviousMatch());
        nextButton.setOnAction(event -> jumpToNextMatch());
        closeSearchButton.setOnAction(event -> closeSearch());
        collapseAllButton.setOnAction(event -> collapseAllFunctions());
        expandAllButton.setOnAction(event -> expandAllFunctions());
    }

    private void installParagraphGraphics() {
        IntFunction<Node> lineNumbers = LineNumberFactory.get(codeArea);
        codeArea.setParagraphGraphicFactory(paragraph -> {
            HBox box = new HBox(2);
            box.setAlignment(Pos.CENTER_LEFT);
            Button foldButton = new Button("");
            foldButton.getStyleClass().add("fold-marker");
            FunctionFold fold = findFoldStartingAt(paragraph);
            if (fold != null && fold.endLine > fold.startLine) {
                foldButton.setText(foldedFunctionStarts.contains(fold.startLine) ? "+" : "-");
                foldButton.setOnAction(event -> toggleFold(fold));
            } else {
                foldButton.setText(" ");
                foldButton.setDisable(true);
                foldButton.setMouseTransparent(true);
            }
            box.getChildren().addAll(foldButton, lineNumbers.apply(paragraph));
            return box;
        });
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
        List<StyleRange> ranges = new ArrayList<>();
        Matcher matcher = SYNTAX_PATTERN.matcher(text);
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
            if (styleClass != null) {
                ranges.add(new StyleRange(matcher.start(), matcher.end(), styleClass));
            }
        }
        for (int i = 0; i < searchMatches.size(); i++) {
            SearchMatch match = searchMatches.get(i);
            ranges.add(new StyleRange(match.start, match.end, i == currentSearchIndex ? "search-current" : "search-hit"));
        }
        TreeMap<Integer, List<StyleEvent>> events = new TreeMap<>();
        events.put(0, new ArrayList<>());
        events.put(text.length(), new ArrayList<>());
        for (StyleRange range : ranges) {
            if (range.start < range.end && range.start >= 0 && range.end <= text.length()) {
                events.computeIfAbsent(range.start, key -> new ArrayList<>()).add(new StyleEvent(range.styleClass, true));
                events.computeIfAbsent(range.end, key -> new ArrayList<>()).add(new StyleEvent(range.styleClass, false));
            }
        }
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        Map<String, Integer> active = new java.util.HashMap<>();
        Integer pos = events.firstKey();
        while (pos != null && pos < text.length()) {
            for (StyleEvent event : events.getOrDefault(pos, Collections.emptyList())) {
                if (event.add) {
                    active.put(event.styleClass, active.getOrDefault(event.styleClass, 0) + 1);
                } else {
                    int count = active.getOrDefault(event.styleClass, 0) - 1;
                    if (count <= 0) {
                        active.remove(event.styleClass);
                    } else {
                        active.put(event.styleClass, count);
                    }
                }
            }
            Integer next = events.higherKey(pos);
            if (next == null) {
                next = text.length();
            }
            spansBuilder.add(new HashSet<>(active.keySet()), next - pos);
            pos = next;
        }
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
        clearFoldState();
        codeArea.replaceText(text);
        rebuildFunctionFolds();
        updateSearchMatches(false);
        refreshHighlighting();
        if (codeIntelligenceController != null) {
            codeIntelligenceController.rebuildIndex();
        }
    }

    private void openSearch() {
        if (!searchPopup.isShowing() && virtualizedScrollPane.getScene() != null) {
            javafx.geometry.Point2D point = virtualizedScrollPane.localToScreen(8, 8);
            if (point != null) {
                searchPopup.show(virtualizedScrollPane.getScene().getWindow(), point.getX(), point.getY());
            }
        }
        String selected = codeArea.getSelectedText();
        if (selected != null && !selected.isEmpty() && !selected.contains("\n") && selected.length() <= 80) {
            searchField.setText(selected);
        } else {
            updateSearchMatches(true);
        }
        Platform.runLater(() -> {
            searchField.requestFocus();
            searchField.selectAll();
        });
    }

    private void closeSearch() {
        searchPopup.hide();
        codeArea.requestFocus();
    }

    private void updateSearchMatches(boolean jumpToFirst) {
        searchMatches.clear();
        currentSearchIndex = -1;
        String query = searchField.getText();
        if (query == null || query.isEmpty()) {
            searchStatusLabel.setText("");
            refreshHighlighting();
            return;
        }

        try {
            Pattern pattern = buildSearchPattern(query);
            Matcher matcher = pattern.matcher(codeArea.getText());
            while (matcher.find()) {
                if (matcher.end() > matcher.start()) {
                    searchMatches.add(new SearchMatch(matcher.start(), matcher.end()));
                }
            }
            if (searchMatches.isEmpty()) {
                searchStatusLabel.setText("无结果");
            } else {
                currentSearchIndex = 0;
                searchStatusLabel.setText("1/" + searchMatches.size());
                if (jumpToFirst) {
                    jumpToMatch(0);
                }
            }
        } catch (PatternSyntaxException e) {
            searchStatusLabel.setText("正则无效");
        }
        refreshHighlighting();
    }

    private Pattern buildSearchPattern(String query) {
        String expr = regexCheck.isSelected() ? query : Pattern.quote(query);
        if (wholeWordCheck.isSelected()) {
            expr = "\\b(?:" + expr + ")\\b";
        }
        int flags = Pattern.MULTILINE;
        if (!matchCaseCheck.isSelected()) {
            flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        return Pattern.compile(expr, flags);
    }

    private void jumpToNextMatch() {
        if (searchMatches.isEmpty()) {
            updateSearchMatches(false);
            return;
        }
        jumpToMatch((currentSearchIndex + 1 + searchMatches.size()) % searchMatches.size());
    }

    private void jumpToPreviousMatch() {
        if (searchMatches.isEmpty()) {
            updateSearchMatches(false);
            return;
        }
        jumpToMatch((currentSearchIndex - 1 + searchMatches.size()) % searchMatches.size());
    }

    private void jumpToMatch(int index) {
        if (index < 0 || index >= searchMatches.size()) {
            return;
        }
        currentSearchIndex = index;
        SearchMatch match = searchMatches.get(index);
        expandFoldsContaining(match.start, match.end);
        codeArea.selectRange(match.start, match.end);
        codeArea.requestFollowCaret();
        searchStatusLabel.setText((index + 1) + "/" + searchMatches.size());
        refreshHighlighting();
    }

    private void refreshHighlighting() {
        if (refreshingStyles) {
            return;
        }
        refreshingStyles = true;
        try {
            codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
        } finally {
            refreshingStyles = false;
        }
    }

    private void rebuildFunctionFolds() {
        if (rebuildingFolds) {
            return;
        }
        rebuildingFolds = true;
        try {
            List<FunctionFold> old = new ArrayList<>(functionFolds);
            for (FunctionFold fold : old) {
                if (foldedFunctionStarts.contains(fold.startLine)) {
                    unfoldFunction(fold);
                }
            }
            foldedFunctionStarts.clear();
            functionFolds.clear();
            String[] lines = codeArea.getText().split("\\R", -1);
            ArrayDeque<BlockToken> stack = new ArrayDeque<>();
            for (int i = 0; i < lines.length; i++) {
                String scanLine = sanitizeLuaLine(lines[i]);
                Matcher matcher = LUA_TOKEN_PATTERN.matcher(scanLine);
                while (matcher.find()) {
                    String token = matcher.group(1);
                    if ("function".equals(token)) {
                        stack.push(new BlockToken("function", i));
                    } else if ("if".equals(token) || "for".equals(token) || "while".equals(token)) {
                        stack.push(new BlockToken(token, i));
                    } else if ("repeat".equals(token)) {
                        stack.push(new BlockToken(token, i));
                    } else if ("until".equals(token)) {
                        popUntil(stack, "repeat");
                    } else if ("end".equals(token)) {
                        BlockToken opener = stack.isEmpty() ? null : stack.pop();
                        if (opener != null && "function".equals(opener.type) && i > opener.line) {
                            functionFolds.add(new FunctionFold(opener.line, i));
                        }
                    }
                }
            }
            functionFolds.sort(Comparator.comparingInt(fold -> fold.startLine));
            refreshFoldGraphics();
        } finally {
            rebuildingFolds = false;
        }
    }

    private String sanitizeLuaLine(String line) {
        int comment = line.indexOf("--");
        if (comment >= 0) {
            line = line.substring(0, comment);
        }
        return line.replaceAll("\"([^\\\\\"\\r\\n]|\\\\.)*\"|'([^\\\\'\\r\\n]|\\\\.)*'", "\"\"");
    }

    private void popUntil(ArrayDeque<BlockToken> stack, String type) {
        while (!stack.isEmpty()) {
            BlockToken token = stack.pop();
            if (type.equals(token.type)) {
                return;
            }
        }
    }

    private FunctionFold findFoldStartingAt(int paragraph) {
        for (FunctionFold fold : functionFolds) {
            if (fold.startLine == paragraph) {
                return fold;
            }
        }
        return null;
    }

    private void toggleFold(FunctionFold fold) {
        if (foldedFunctionStarts.contains(fold.startLine)) {
            unfoldFunction(fold);
        } else {
            foldFunction(fold);
        }
        refreshFoldGraphics();
    }

    private void foldFunction(FunctionFold fold) {
        if (fold.endLine <= fold.startLine || foldedFunctionStarts.contains(fold.startLine)) {
            return;
        }
        foldedFunctionStarts.add(fold.startLine);
        codeArea.foldFunctionParagraphs(fold.startLine, fold.endLine, fold.marker());
    }

    private void unfoldFunction(FunctionFold fold) {
        if (!foldedFunctionStarts.remove(fold.startLine)) {
            return;
        }
        codeArea.unfoldFunctionParagraphs(fold.startLine, fold.marker());
    }

    private void expandFoldsContaining(int startOffset, int endOffset) {
        int startLine = codeArea.offsetToPosition(startOffset, org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMajor();
        List<FunctionFold> toOpen = new ArrayList<>();
        for (FunctionFold fold : functionFolds) {
            if (foldedFunctionStarts.contains(fold.startLine) && startLine > fold.startLine && startLine <= fold.endLine) {
                toOpen.add(fold);
            }
        }
        toOpen.sort(Comparator.comparingInt(fold -> -fold.startLine));
        for (FunctionFold fold : toOpen) {
            unfoldFunction(fold);
        }
        if (!toOpen.isEmpty()) {
            refreshFoldGraphics();
        }
    }

    private void collapseAllFunctions() {
        for (FunctionFold fold : functionFolds) {
            foldFunction(fold);
        }
        refreshFoldGraphics();
    }

    private void expandAllFunctions() {
        List<FunctionFold> folds = new ArrayList<>(functionFolds);
        folds.sort(Comparator.comparingInt(fold -> -fold.startLine));
        for (FunctionFold fold : folds) {
            if (foldedFunctionStarts.contains(fold.startLine)) {
                unfoldFunction(fold);
            }
        }
        refreshFoldGraphics();
    }

    private void clearFoldState() {
        expandAllFunctions();
        functionFolds.clear();
        foldedFunctionStarts.clear();
    }

    private void refreshFoldGraphics() {
        for (FunctionFold fold : functionFolds) {
            if (fold.startLine >= 0 && fold.startLine < codeArea.getParagraphs().size()) {
                codeArea.recreateParagraphGraphic(fold.startLine);
            }
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

    private static class SearchMatch {
        final int start;
        final int end;

        SearchMatch(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private static class StyleRange {
        final int start;
        final int end;
        final String styleClass;

        StyleRange(int start, int end, String styleClass) {
            this.start = start;
            this.end = end;
            this.styleClass = styleClass;
        }
    }

    private static class StyleEvent {
        final String styleClass;
        final boolean add;

        StyleEvent(String styleClass, boolean add) {
            this.styleClass = styleClass;
            this.add = add;
        }
    }

    private static class FunctionFold {
        final int startLine;
        final int endLine;

        FunctionFold(int startLine, int endLine) {
            this.startLine = startLine;
            this.endLine = endLine;
        }

        String marker() {
            return "folded-function-" + startLine + "-" + endLine;
        }
    }

    private static class BlockToken {
        final String type;
        final int line;

        BlockToken(String type, int line) {
            this.type = type;
            this.line = line;
        }
    }

    private static class FoldableCodeArea extends CodeArea {
        void foldFunctionParagraphs(int startParagraph, int endParagraph, String marker) {
            if (startParagraph <= endParagraph) {
                foldParagraphs(startParagraph, endParagraph, styles -> {
                    Set<String> next = new HashSet<>(styles);
                    next.add(marker);
                    next.add("collapse");
                    return next;
                });
            }
        }

        void unfoldFunctionParagraphs(int paragraph, String marker) {
            unfoldParagraphs(paragraph, styles -> styles.contains(marker), styles -> {
                Set<String> next = new HashSet<>(styles);
                next.remove(marker);
                next.remove("collapse");
                return next;
            });
        }
    }
}
