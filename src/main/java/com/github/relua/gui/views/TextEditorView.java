package com.github.relua.gui.views;

import com.github.relua.gui.controllers.LuaCodeIntelligenceController;
import com.github.relua.gui.dialogs.EditCommentDialog;
import com.github.relua.gui.dialogs.RenameVariableDialog;
import com.github.relua.gui.dialogs.XrefsDialog;
import com.github.relua.gui.navigation.Bookmark;
import com.github.relua.gui.navigation.BookmarkManager;
import com.github.relua.gui.navigation.NavigationHistoryManager;
import com.github.relua.gui.navigation.NavigationLocation;
import com.github.relua.gui.theme.ThemeManager;
import com.github.relua.gui.preferences.AppSettings;
import com.github.relua.gui.preferences.EditorAppearance;
import com.github.relua.gui.preferences.SettingsManager;
import com.github.relua.gui.utils.I18nUtil;
import com.github.relua.gui.utils.LuaVariableUtils;
import com.github.relua.gui.utils.LuaVariableUtils.SymbolMatch;
import com.github.relua.gui.utils.LuaVariableUtils.SymbolRange;
import com.github.relua.gui.utils.LuaVariableUtils.XrefItem;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Popup;
import javafx.stage.Window;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.SimpleEditableStyledDocument;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.richtext.model.TwoDimensional.Bias;

import java.io.File;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 文本编辑器视图类，使用 RichTextFX 实现高级文本编辑、语法高亮与 IDA Pro 风格变量重命名与符号联动高亮
 */
public class TextEditorView {
    private static final Logger LOGGER = Logger.getLogger(TextEditorView.class.getName());

    // RichTextFX 的 CodeArea 组件
    private final FoldableCodeArea codeArea;
    private final VirtualizedScrollPane<FoldableCodeArea> virtualizedScrollPane;
    private final Popup searchPopup;
    private final SettingsManager settingsManager;
    private final Consumer<AppSettings> settingsListener;
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

    // 右键上下文菜单与重命名/交叉引用/行注释菜单项
    private ContextMenu contextMenu;
    private MenuItem renameMenuItem;
    private MenuItem xrefsMenuItem;
    private MenuItem commentMenuItem;

    // 符号联动高亮与变量重命名支持
    private final List<SymbolMatch> symbolHighlightMatches = new ArrayList<>();
    private String currentHighlightSymbol = null;
    private BiConsumer<String, String> variableRenameListener = null;
    private BiConsumer<Integer, String> commentChangeListener = null;
    
    // Lua 语法高亮与字节码反汇编高亮的正则表达式
    private static final String KEYWORDS = "\\b(and|break|do|else|elseif|end|false|for|function|goto|if|in|local|nil|not|or|repeat|return|then|true|until|while)\\b";
    private static final String OPCODES = "\\b(MOVE|LOADK|LOADBOOL|LOADNIL|GETUPVAL|GETGLOBAL|GETTABLE|SETGLOBAL|SETUPVAL|SETTABLE|NEWTABLE|SELF|ADD|SUB|MUL|DIV|MOD|POW|UNM|NOT|LEN|CONCAT|JMP|EQ|LT|LE|TEST|TESTSET|CALL|TAILCALL|RETURN|FORLOOP|FORPREP|TFORLOOP|SETLIST|CLOSE|CLOSURE|VARARG)\\b";
    private static final String STRINGS = "\\\"([^\\\\\"\\r\\n]|\\\\.)*\\\"|\\'([^\\\\\\'\\r\\n]|\\\\.)*\\'";
    private static final String COMMENTS = "(--.*$)|(;.*$)";
    private static final String NUMBERS = "\\b(0x[0-9a-fA-F]+|\\d+\\.?\\d*)\\b";
    private static final String GLOBALVAR = "\\b(global_|chunk_|module_)[A-Za-z0-9_]+\\b";
    private static final String METHODCALL = "(?<=:)[A-Za-z_][A-Za-z0-9_]*";
    private static final String TABLEFIELD = "(?<=\\.)[A-Za-z_][A-Za-z0-9_]*";
    private static final String FUNCTION = "\\b[A-Za-z_][A-Za-z0-9_]*(?=\\s*\\()";
    private static final String REGISTERS = "\\b(R\\d+|K\\d+|RK\\d+)\\b";
    private static final String DIRECTIVES = "\\.(function|constants|locals|upvalues|const|local|upvalue)\\b";
    private static final String LOCALVAR = "\\b[A-Za-z_][A-Za-z0-9_]*\\b";
    private static final String OPERATORS = "==|~=|<=|>=|\\.\\.|[\\+\\-\\*\\/\\%\\^\\#\\=\\<\\>\\:]";
    
    // 组合正则表达式
    private static final String PATTERN = 
          "(?<COMMENTS>" + COMMENTS + ")"
        + "|(?<STRINGS>" + STRINGS + ")"
        + "|(?<KEYWORDS>" + KEYWORDS + ")"
        + "|(?<OPCODES>" + OPCODES + ")"
        + "|(?<DIRECTIVES>" + DIRECTIVES + ")"
        + "|(?<REGISTERS>" + REGISTERS + ")"
        + "|(?<GLOBALVAR>" + GLOBALVAR + ")"
        + "|(?<METHODCALL>" + METHODCALL + ")"
        + "|(?<TABLEFIELD>" + TABLEFIELD + ")"
        + "|(?<FUNCTION>" + FUNCTION + ")"
        + "|(?<NUMBERS>" + NUMBERS + ")"
        + "|(?<LOCALVAR>" + LOCALVAR + ")"
        + "|(?<OPERATORS>" + OPERATORS + ")";
    private static final Pattern SYNTAX_PATTERN = Pattern.compile(PATTERN);
    
    private LuaCodeIntelligenceController codeIntelligenceController;
    private final List<SearchMatch> searchMatches = new ArrayList<>();
    private final List<FunctionFold> functionFolds = new ArrayList<>();
    private final Set<Integer> foldedFunctionStarts = new HashSet<>();
    private int currentSearchIndex = -1;
    private boolean refreshingStyles = false;
    private boolean rebuildingFolds = false;
    private static final Pattern LUA_TOKEN_PATTERN = Pattern.compile("\\b(function|end|repeat|until|if|for|while)\\b");
    private boolean suppressNextKeyTyped = false;
    private File currentFile;
    private java.util.function.Consumer<Boolean> onNavigateHistoryRequested; // true = back, false = forward
    private java.util.function.BiConsumer<File, Integer> onNavigateRequested; // (file, line)

    /**
     * 默认构造函数（显示欢迎文本）
     */
    public TextEditorView() {
        this(true);
    }

    /**
     * 构造函数
     * @param showWelcome 是否显示欢迎文本
     */
    public TextEditorView(boolean showWelcome) {
        this(showWelcome, SettingsManager.getInstance());
    }

    public TextEditorView(boolean showWelcome, SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
        settingsListener = settings -> {
            if (Platform.isFxApplicationThread()) applyEditorSettings(settings);
            else Platform.runLater(() -> applyEditorSettings(settings));
        };
        // 初始化 CodeArea
        codeArea = new FoldableCodeArea();
        codeArea.setWrapText(false); // 禁用自动换行，长行出现水平滚动条
        codeArea.setEditable(false); // 正常情况下不可直接编辑反编译代码，只允许选择、复制与逆向操作
        codeArea.setCursor(Cursor.TEXT); // 保持文字选择工字光标，便于用户划词与复制
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
        
        // 初始化行号和折叠标识
        installParagraphGraphics();
        applyEditorSettings(settingsManager.getSettings());
        settingsManager.addListener(settingsListener);
        
        // 加载当前现代化主题样式表并注册主题动态切换监听
        applyThemeStyles();
        ThemeManager.getInstance().addListener(newTheme -> Platform.runLater(this::applyThemeStyles));

        // 注册书签变动全局监听
        BookmarkManager.getInstance().addListener(this::handleBookmarksChanged);
        
        // 监听文本变化以更新折叠、搜索和高亮
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (!refreshingStyles) {
                rebuildFunctionFolds();
                updateSearchMatches(false);
                updateSymbolHighlighting(codeArea.getCaretPosition());
                refreshHighlighting();
            }
        });

        // 监听光标移动，实现同名变量联动高亮与光标导航历史记录 (Alt+Left/Right)
        codeArea.caretPositionProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateSymbolHighlighting(newVal);
                if (currentFile != null) {
                    int paragraph = codeArea.getCurrentParagraph() + 1;
                    int column = codeArea.getCaretColumn();
                    NavigationHistoryManager.getInstance().recordLocation(currentFile, paragraph, column);
                }
            }
        });
        
        // 设置 CodeArea 的尺寸约束
        codeArea.setMaxWidth(Double.MAX_VALUE);
        codeArea.setMaxHeight(Double.MAX_VALUE);
        codeArea.setMinHeight(200);

        virtualizedScrollPane.setMaxWidth(Double.MAX_VALUE);
        virtualizedScrollPane.setMaxHeight(Double.MAX_VALUE);
        
        // 安装快捷键与右键菜单
        installSearchHandlers();
        installContextMenu();
        
        // 设置初始文本
        if (showWelcome) {
            codeArea.replaceText(I18nUtil.getString("editor.welcome"));
        }
    }

    SettingsManager getSettingsManager() {
        return settingsManager;
    }

    private HBox createSearchBar() {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("editor-search-bar");
        searchField.setPromptText("搜索 (Enter 下一个 / Shift+Enter 上一个)");
        searchField.getStyleClass().add("editor-search-field");
        searchField.setPrefColumnCount(22);
        previousButton.getStyleClass().add("editor-search-button");
        previousButton.setTooltip(new Tooltip("上一个匹配项 (Shift+Enter)"));
        nextButton.getStyleClass().add("editor-search-button");
        nextButton.setTooltip(new Tooltip("下一个匹配项 (Enter)"));
        closeSearchButton.getStyleClass().add("editor-search-button");
        closeSearchButton.setTooltip(new Tooltip("关闭搜索 (Esc)"));
        matchCaseCheck.setTooltip(new Tooltip("区分大小写 (Match Case)"));
        wholeWordCheck.setTooltip(new Tooltip("全字匹配 (Whole Word)"));
        regexCheck.setTooltip(new Tooltip("正则表达式 (Regex)"));
        collapseAllButton.getStyleClass().add("editor-search-button");
        expandAllButton.getStyleClass().add("editor-search-button");
        searchStatusLabel.getStyleClass().add("editor-search-status");
        bar.getChildren().addAll(
                new Label("查找:"), searchField,
                previousButton, nextButton,
                matchCaseCheck, wholeWordCheck, regexCheck,
                searchStatusLabel, collapseAllButton, expandAllButton, closeSearchButton);
        return bar;
    }

    private void installSearchHandlers() {
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Ctrl+F 搜索
            if (event.isControlDown() && event.getCode() == KeyCode.F) {
                openSearch();
                event.consume();
                return;
            }
            // ESC 关闭搜索浮层
            if (event.getCode() == KeyCode.ESCAPE && searchPopup.isShowing()) {
                closeSearch();
                event.consume();
                return;
            }
            // Shift+F6 或 Ctrl+R 触发符号重命名
            if ((event.isShiftDown() && event.getCode() == KeyCode.F6) ||
                (event.isControlDown() && !event.isShiftDown() && !event.isAltDown() && event.getCode() == KeyCode.R)) {
                event.consume();
                suppressNextKeyTyped = true;
                Platform.runLater(this::promptRenameVariable);
                return;
            }
            // Alt+X 或 Ctrl+Shift+X 或 Shift+F7 触发交叉引用
            if ((event.isAltDown() && event.getCode() == KeyCode.X) ||
                (event.isControlDown() && event.isShiftDown() && event.getCode() == KeyCode.X) ||
                (event.isShiftDown() && event.getCode() == KeyCode.F7)) {
                event.consume();
                suppressNextKeyTyped = true;
                Platform.runLater(this::promptXrefs);
                return;
            }
            // Ctrl+; 或 Alt+; 触发添加/修改行注释
            if ((event.isControlDown() || event.isAltDown()) && event.getCode() == KeyCode.SEMICOLON) {
                event.consume();
                suppressNextKeyTyped = true;
                Platform.runLater(this::promptEditComment);
                return;
            }
            // 单按 'N' 键重命名（IDA 经典单键操作）
            if (event.getCode() == KeyCode.N && !event.isControlDown() && !event.isAltDown() && !event.isMetaDown()) {
                event.consume();
                suppressNextKeyTyped = true;
                SymbolRange symbolRange = getCurrentSymbolRange();
                if (symbolRange != null) {
                    Platform.runLater(this::promptRenameVariable);
                }
                return;
            }
            // 单按 'X' 键查看交叉引用（IDA 经典单键操作）
            if (event.getCode() == KeyCode.X && !event.isControlDown() && !event.isAltDown() && !event.isMetaDown()) {
                event.consume();
                suppressNextKeyTyped = true;
                SymbolRange symbolRange = getCurrentSymbolRange();
                if (symbolRange != null) {
                    Platform.runLater(this::promptXrefs);
                }
                return;
            }
            // 单按 ';' 键添加/修改行注释（IDA 经典单键操作）
            if (event.getCode() == KeyCode.SEMICOLON && !event.isControlDown() && !event.isAltDown() && !event.isMetaDown()) {
                event.consume();
                suppressNextKeyTyped = true;
                Platform.runLater(this::promptEditComment);
                return;
            }

            // Alt+Left / Alt+Right 触发光标导航历史后退与前进
            if (event.isAltDown() && !event.isControlDown() && !event.isShiftDown() &&
                    (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.RIGHT)) {
                event.consume();
                suppressNextKeyTyped = true;
                if (onNavigateHistoryRequested != null) {
                    onNavigateHistoryRequested.accept(event.getCode() == KeyCode.LEFT);
                }
                return;
            }

            // Ctrl+F11 触发切换当前行书签
            if (event.isControlDown() && !event.isAltDown() && event.getCode() == KeyCode.F11) {
                event.consume();
                suppressNextKeyTyped = true;
                if (currentFile != null) {
                    int line = codeArea.getCurrentParagraph() + 1;
                    String lineText = codeArea.getParagraph(line - 1).getText();
                    BookmarkManager.getInstance().toggleBookmark(currentFile, line, lineText);
                }
                return;
            }

            // F2 / Shift+F2 跳转到下一个/上一个书签
            if (event.getCode() == KeyCode.F2 && !event.isControlDown() && !event.isAltDown()) {
                event.consume();
                suppressNextKeyTyped = true;
                int currentLine = codeArea.getCurrentParagraph() + 1;
                Bookmark target = event.isShiftDown()
                        ? BookmarkManager.getInstance().getPreviousBookmark(currentFile, currentLine)
                        : BookmarkManager.getInstance().getNextBookmark(currentFile, currentLine);
                if (target != null) {
                    if (Objects.equals(target.getFile(), currentFile)) {
                        jumpToLine(target.getLine());
                    } else if (onNavigateRequested != null) {
                        onNavigateRequested.accept(target.getFile(), target.getLine());
                    }
                }
                return;
            }

            // Ctrl+1~9 或 Ctrl+Shift+1~9 助记符书签跳转与设置
            if (event.isControlDown() && !event.isAltDown() && event.getCode().isDigitKey()) {
                int digit = -1;
                switch (event.getCode()) {
                    case DIGIT1: case NUMPAD1: digit = 1; break;
                    case DIGIT2: case NUMPAD2: digit = 2; break;
                    case DIGIT3: case NUMPAD3: digit = 3; break;
                    case DIGIT4: case NUMPAD4: digit = 4; break;
                    case DIGIT5: case NUMPAD5: digit = 5; break;
                    case DIGIT6: case NUMPAD6: digit = 6; break;
                    case DIGIT7: case NUMPAD7: digit = 7; break;
                    case DIGIT8: case NUMPAD8: digit = 8; break;
                    case DIGIT9: case NUMPAD9: digit = 9; break;
                    default: break;
                }
                if (digit >= 1 && digit <= 9) {
                    event.consume();
                    suppressNextKeyTyped = true;
                    if (event.isShiftDown()) {
                        if (currentFile != null) {
                            int line = codeArea.getCurrentParagraph() + 1;
                            String lineText = codeArea.getParagraph(line - 1).getText();
                            BookmarkManager.getInstance().setMnemonicBookmark(currentFile, line, digit, lineText);
                        }
                    } else {
                        Bookmark b = BookmarkManager.getInstance().getBookmarkByMnemonic(digit);
                        if (b != null) {
                            if (Objects.equals(b.getFile(), currentFile)) {
                                jumpToLine(b.getLine());
                            } else if (onNavigateRequested != null) {
                                onNavigateRequested.accept(b.getFile(), b.getLine());
                            }
                        }
                    }
                    return;
                }
            }
        });

        // 拦截紧随快捷键派发的 KEY_TYPED 字符，防止字符漏入文本破坏反编译文件
        codeArea.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (suppressNextKeyTyped) {
                suppressNextKeyTyped = false;
                event.consume();
                return;
            }
            String ch = event.getCharacter();
            if ("n".equalsIgnoreCase(ch) || "x".equalsIgnoreCase(ch) || ";".equals(ch)) {
                SymbolRange symbolRange = getCurrentSymbolRange();
                if (symbolRange != null) {
                    event.consume();
                }
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

    /**
     * 安装右键上下文菜单，支持重命名变量、交叉引用、行注释以及常用文本操作
     */
    private void installContextMenu() {
        contextMenu = new ContextMenu();

        renameMenuItem = new MenuItem("重命名符号 (N / Shift+F6)...");
        renameMenuItem.setOnAction(event -> promptRenameVariable());

        xrefsMenuItem = new MenuItem("查看交叉引用 (X)...");
        xrefsMenuItem.setOnAction(event -> promptXrefs());

        commentMenuItem = new MenuItem("添加/修改行注释 (;)...");
        commentMenuItem.setOnAction(event -> promptEditComment());

        MenuItem bookmarkMenuItem = new MenuItem("切换当前行书签 (Ctrl+F11)");
        bookmarkMenuItem.setOnAction(event -> {
            if (currentFile != null) {
                int line = codeArea.getCurrentParagraph() + 1;
                String lineText = codeArea.getParagraph(line - 1).getText();
                BookmarkManager.getInstance().toggleBookmark(currentFile, line, lineText);
            }
        });

        MenuItem undoItem = new MenuItem("撤销 (Undo)");
        undoItem.setOnAction(event -> undo());

        MenuItem redoItem = new MenuItem("重做 (Redo)");
        redoItem.setOnAction(event -> redo());

        MenuItem cutItem = new MenuItem("剪切 (Cut)");
        cutItem.setOnAction(event -> cut());

        MenuItem copyItem = new MenuItem("复制 (Copy)");
        copyItem.setOnAction(event -> copy());

        MenuItem pasteItem = new MenuItem("粘贴 (Paste)");
        pasteItem.setOnAction(event -> paste());

        MenuItem selectAllItem = new MenuItem("全选 (Select All)");
        selectAllItem.setOnAction(event -> selectAll());

        contextMenu.getItems().addAll(
                renameMenuItem,
                xrefsMenuItem,
                commentMenuItem,
                bookmarkMenuItem,
                new SeparatorMenuItem(),
                undoItem,
                redoItem,
                new SeparatorMenuItem(),
                cutItem,
                copyItem,
                pasteItem,
                new SeparatorMenuItem(),
                selectAllItem
        );

        // 弹出前动态检查当前光标或选区是否位于有效标识符上并刷新提示
        contextMenu.setOnShowing(event -> {
            SymbolRange symbolRange = getCurrentSymbolRange();
            if (symbolRange != null) {
                renameMenuItem.setText("重命名符号 '" + symbolRange.symbol + "' (N / Shift+F6)...");
                renameMenuItem.setDisable(false);
                xrefsMenuItem.setText("查看交叉引用 '" + symbolRange.symbol + "' (X)...");
                xrefsMenuItem.setDisable(false);
            } else {
                renameMenuItem.setText("重命名符号 (N / Shift+F6)...");
                renameMenuItem.setDisable(true);
                xrefsMenuItem.setText("查看交叉引用 (X)...");
                xrefsMenuItem.setDisable(true);
            }

            int currentLine = codeArea.getCurrentParagraph() + 1;
            commentMenuItem.setText("添加/修改行注释 (行 " + currentLine + ") (;)...");
            boolean hasBookmark = (currentFile != null && BookmarkManager.getInstance().getBookmark(currentFile, currentLine) != null);
            bookmarkMenuItem.setText(hasBookmark ? "移除当前行书签 (行 " + currentLine + ") (Ctrl+F11)" : "切换当前行书签 (行 " + currentLine + ") (Ctrl+F11)");

            // 只读保护：当不可直接编辑时，剪切和粘贴禁用，仅支持选择复制
            boolean editable = codeArea.isEditable();
            boolean hasSelection = !codeArea.getSelectedText().isEmpty();
            cutItem.setDisable(!editable || !hasSelection);
            pasteItem.setDisable(!editable);
            copyItem.setDisable(!hasSelection);
        });

        // 鼠标右键按下时如果未选中文本，将光标自动定位至右击字符处
        codeArea.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                if (codeArea.getSelectedText().isEmpty()) {
                    CharacterHit hit = codeArea.hit(event.getX(), event.getY());
                    hit.getCharacterIndex().ifPresent(idx -> {
                        codeArea.moveTo(idx);
                        updateSymbolHighlighting(idx);
                    });
                }
            }
        });

        codeArea.setContextMenu(contextMenu);
    }

    private void installParagraphGraphics() {
        IntFunction<Node> lineNumbers = LineNumberFactory.get(codeArea);
        codeArea.setParagraphGraphicFactory(paragraph -> {
            if (isParagraphCollapsed(paragraph)) {
                Region empty = new Region();
                empty.setVisible(false);
                empty.setManaged(false);
                empty.setMinSize(0, 0);
                empty.setPrefSize(0, 0);
                empty.setMaxSize(0, 0);
                return empty;
            }

            HBox gutterBox = new HBox(0);
            gutterBox.setAlignment(Pos.CENTER_RIGHT);
            gutterBox.getStyleClass().add("editor-gutter-line");

            // 1. 书签指示图标或占位符 (宽14px)
            Bookmark bookmark = (currentFile != null)
                    ? BookmarkManager.getInstance().getBookmark(currentFile, paragraph + 1)
                    : null;

            Label bookmarkBadge = new Label();
            bookmarkBadge.setMinWidth(14);
            bookmarkBadge.setPrefWidth(14);
            bookmarkBadge.setMaxWidth(14);
            bookmarkBadge.setAlignment(Pos.CENTER);

            if (bookmark != null) {
                if (bookmark.getMnemonic() != null) {
                    bookmarkBadge.setText(String.valueOf(bookmark.getMnemonic()));
                    bookmarkBadge.getStyleClass().add("bookmark-mnemonic-marker");
                } else {
                    bookmarkBadge.setText("🔖");
                    bookmarkBadge.getStyleClass().add("bookmark-marker");
                }
                Tooltip.install(bookmarkBadge, new Tooltip("书签: 第 " + (paragraph + 1) + " 行" +
                        (bookmark.getMnemonic() != null ? " [助记符 " + bookmark.getMnemonic() + "]" : "")));
            } else {
                bookmarkBadge.getStyleClass().add("bookmark-slot");
            }

            bookmarkBadge.setOnMouseClicked(e -> {
                if (currentFile != null && e.getButton() == MouseButton.PRIMARY) {
                    e.consume();
                    String lineText = codeArea.getParagraph(paragraph).getText();
                    BookmarkManager.getInstance().toggleBookmark(currentFile, paragraph + 1, lineText);
                }
            });

            // 2. 行号文本
            Node lineNumNode = lineNumbers.apply(paragraph);
            AppSettings settings = settingsManager.getSettings();
            lineNumNode.setStyle(EditorAppearance.fontStyle(settings));
            lineNumNode.setVisible(settings.isShowLineNumbers());
            lineNumNode.setManaged(settings.isShowLineNumbers());

            // 3. 折叠标识或右侧占位符
            FunctionFold fold = findFoldStartingAt(paragraph);
            if (fold != null && fold.endLine > fold.startLine) {
                boolean isFolded = foldedFunctionStarts.contains(fold.startLine);
                Button foldButton = new Button(isFolded ? "▸" : "▾");
                foldButton.getStyleClass().add("fold-marker");

                if (isFolded) {
                    foldButton.getStyleClass().add("fold-marker-collapsed");
                    foldButton.setOpacity(1.0);
                } else {
                    foldButton.setOpacity(0.0);
                    // VSCode 体验：展开状态下默认空白，仅当鼠标移入该行行号区域时显示折叠箭头
                    gutterBox.setOnMouseEntered(e -> foldButton.setOpacity(1.0));
                    gutterBox.setOnMouseExited(e -> foldButton.setOpacity(0.0));
                }

                foldButton.setOnAction(event -> {
                    event.consume();
                    toggleFold(fold);
                });
                gutterBox.getChildren().addAll(bookmarkBadge, lineNumNode, foldButton);
            } else {
                // 普通不可折叠行：行号右侧填充透明占位空间（宽14px），鼠标悬浮时不显示任何按钮
                Region spacer = new Region();
                spacer.setMinWidth(14);
                spacer.setPrefWidth(14);
                spacer.setMaxWidth(14);
                spacer.setMouseTransparent(true);
                gutterBox.getChildren().addAll(bookmarkBadge, lineNumNode, spacer);
            }

            return gutterBox;
        });
    }

    private boolean isParagraphCollapsed(int paragraph) {
        if (paragraph >= 0 && paragraph < codeArea.getParagraphs().size()) {
            return codeArea.getParagraph(paragraph).getParagraphStyle().contains("collapse");
        }
        return false;
    }
    
    /**
     * 初始化代码智能提示与跳转功能
     * @param statusLabel 状态栏标签
     */
    public void initCodeIntelligence(Label statusLabel) {
        this.codeIntelligenceController = new LuaCodeIntelligenceController(codeArea, statusLabel);
        this.codeIntelligenceController.rebuildIndex();
    }
    
    public LuaCodeIntelligenceController getCodeIntelligenceController() {
        return codeIntelligenceController;
    }

    /**
     * 设置代码区域是否允许直接键盘编辑
     * @param editable 是否可编辑
     */
    public void setEditable(boolean editable) {
        codeArea.setEditable(editable);
    }

    /**
     * 检查当前代码区域是否允许直接编辑
     * @return 是否可编辑
     */
    public boolean isEditable() {
        return codeArea.isEditable();
    }

    private void handleBookmarksChanged(File file) {
        if (file == null || file.equals(currentFile)) {
            Platform.runLater(() -> {
                for (int i = 0; i < codeArea.getParagraphs().size(); i++) {
                    codeArea.recreateParagraphGraphic(i);
                }
            });
        }
    }

    public void setCurrentFile(File file) {
        this.currentFile = file;
        handleBookmarksChanged(file);
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void setOnNavigateHistoryRequested(java.util.function.Consumer<Boolean> cb) {
        this.onNavigateHistoryRequested = cb;
    }

    public void setOnNavigateRequested(java.util.function.BiConsumer<File, Integer> cb) {
        this.onNavigateRequested = cb;
    }

    /**
     * 更新符号联动高亮（光标移动或文本变动时调用）
     * @param caretOffset 当前光标字符偏移
     */
    private void updateSymbolHighlighting(int caretOffset) {
        if (refreshingStyles) {
            return;
        }
        String text = codeArea.getText();
        SymbolRange range = LuaVariableUtils.getSymbolAt(text, caretOffset);
        if (range == null) {
            if (currentHighlightSymbol != null || !symbolHighlightMatches.isEmpty()) {
                currentHighlightSymbol = null;
                symbolHighlightMatches.clear();
                refreshHighlighting();
            }
            return;
        }

        String symbol = range.symbol;
        // 如果处于同一个符号上，仅更新 isCurrent 标志以提升光标移动性能
        if (symbol.equals(currentHighlightSymbol) && !symbolHighlightMatches.isEmpty()) {
            boolean stateChanged = false;
            for (int i = 0; i < symbolHighlightMatches.size(); i++) {
                SymbolMatch oldMatch = symbolHighlightMatches.get(i);
                boolean nowCurrent = (caretOffset >= oldMatch.start && caretOffset <= oldMatch.end);
                if (oldMatch.isCurrent != nowCurrent) {
                    symbolHighlightMatches.set(i, new SymbolMatch(oldMatch.start, oldMatch.end, nowCurrent));
                    stateChanged = true;
                }
            }
            if (stateChanged) {
                refreshHighlighting();
            }
            return;
        }

        // 识别出新符号，全文检索该符号的所有引用位置
        currentHighlightSymbol = symbol;
        symbolHighlightMatches.clear();
        symbolHighlightMatches.addAll(LuaVariableUtils.findVariableOccurrences(text, symbol, caretOffset));
        refreshHighlighting();
    }

    /**
     * 获取当前光标位置或选中文本处的变量符号
     * @return 符号信息，若不在合法变量上则返回 null
     */
    public SymbolRange getCurrentSymbolRange() {
        String selected = codeArea.getSelectedText();
        if (selected != null && !selected.isEmpty()) {
            String trimmed = selected.trim();
            if (LuaVariableUtils.isValidIdentifier(trimmed) && !LuaVariableUtils.isKeyword(trimmed)) {
                int selStart = codeArea.getSelection().getStart();
                int selEnd = codeArea.getSelection().getEnd();
                if (!LuaVariableUtils.isInIgnoredLiteral(codeArea.getText(), selStart, selEnd)) {
                    return new SymbolRange(trimmed, selStart, selEnd);
                }
            }
        }
        return LuaVariableUtils.getSymbolAt(codeArea.getText(), codeArea.getCaretPosition());
    }

    /**
     * 弹出变量重命名对话框并执行批量替换
     */
    public void promptRenameVariable() {
        SymbolRange symbolRange = getCurrentSymbolRange();
        if (symbolRange == null) {
            return;
        }

        Window window = null;
        if (codeArea.getScene() != null) {
            window = codeArea.getScene().getWindow();
        }

        Optional<String> result = RenameVariableDialog.show(window, symbolRange.symbol);
        if (result.isPresent()) {
            String newName = result.get();
            if (!newName.equals(symbolRange.symbol)) {
                renameVariable(symbolRange.symbol, newName);
            }
        }
    }

    /**
     * 执行变量全局重命名替换（倒序安全替换，支持撤销/重做）
     * @param oldName 原变量名
     * @param newName 新变量名
     */
    public void renameVariable(String oldName, String newName) {
        if (oldName == null || newName == null || oldName.equals(newName)) {
            return;
        }
        String text = codeArea.getText();
        List<SymbolMatch> matches = LuaVariableUtils.findVariableOccurrences(text, oldName, -1);
        if (matches.isEmpty()) {
            return;
        }

        // 倒序替换以保持前面所有匹配项的起始索引不变
        for (int i = matches.size() - 1; i >= 0; i--) {
            SymbolMatch match = matches.get(i);
            codeArea.replaceText(match.start, match.end, newName);
        }

        // 触发外部重命名监听器
        if (variableRenameListener != null) {
            variableRenameListener.accept(oldName, newName);
        }

        // 刷新符号联动高亮
        currentHighlightSymbol = null;
        updateSymbolHighlighting(codeArea.getCaretPosition());
    }

    /**
     * 注册变量重命名事件监听器
     * @param listener 监听器 (oldName, newName)
     */
    public void setVariableRenameListener(BiConsumer<String, String> listener) {
        this.variableRenameListener = listener;
    }

    /**
     * 注册行注释变更事件监听器
     * @param listener 监听器 (lineNumber, newComment)
     */
    public void setCommentChangeListener(BiConsumer<Integer, String> listener) {
        this.commentChangeListener = listener;
    }

    /**
     * 弹出当前符号的交叉引用 (Xrefs) 对话框并支持双击跳转
     */
    public void promptXrefs() {
        SymbolRange symbolRange = getCurrentSymbolRange();
        if (symbolRange == null) {
            return;
        }

        List<XrefItem> xrefs = LuaVariableUtils.findXrefs(codeArea.getText(), symbolRange.symbol);
        Window window = codeArea.getScene() != null ? codeArea.getScene().getWindow() : null;

        Optional<XrefItem> result = XrefsDialog.show(window, symbolRange.symbol, xrefs);
        if (result.isPresent()) {
            XrefItem target = result.get();
            jumpToRange(target.startOffset, target.endOffset);
        }
    }

    /**
     * 精准平滑跳转并聚焦选中指定的字符偏移范围
     * @param startOffset 起始偏移
     * @param endOffset 结束偏移
     */
    public void jumpToRange(int startOffset, int endOffset) {
        if (startOffset < 0 || endOffset > codeArea.getText().length()) {
            return;
        }
        int paragraph = codeArea.offsetToPosition(startOffset, Bias.Forward).getMajor();
        expandFoldsContaining(startOffset, endOffset);
        codeArea.showParagraphAtTop(Math.max(0, paragraph - 3));
        codeArea.moveTo(startOffset);
        codeArea.selectRange(startOffset, endOffset);
        codeArea.requestFollowCaret();
        updateSymbolHighlighting(startOffset);
    }

    /**
     * 平滑跳转到指定的行号（1-based），展开折叠并高亮选中整行
     * @param lineNum 目标行号
     */
    public void jumpToLine(int lineNum) {
        if (codeArea == null || codeArea.getParagraphs() == null || codeArea.getParagraphs().isEmpty()) {
            return;
        }
        int totalParagraphs = codeArea.getParagraphs().size();
        if (lineNum < 1 || lineNum > totalParagraphs) {
            return;
        }
        int paragraphIdx = lineNum - 1;
        int startOffset = codeArea.getAbsolutePosition(paragraphIdx, 0);
        int lineLen = codeArea.getParagraph(paragraphIdx).getText().length();
        jumpToRange(startOffset, startOffset + lineLen);
    }

    /**
     * 平滑跳转到指定的行号和列范围（1-based 行号，0-based 列偏移），并选中高亮该匹配区间
     * @param lineNum 目标行号 (1-based)
     * @param startCol 起始列偏移 (0-based)
     * @param endCol 结束列偏移 (0-based)
     */
    public void jumpToLineAndColumn(int lineNum, int startCol, int endCol) {
        if (codeArea == null || codeArea.getParagraphs() == null || codeArea.getParagraphs().isEmpty()) {
            return;
        }
        int totalParagraphs = codeArea.getParagraphs().size();
        if (lineNum < 1 || lineNum > totalParagraphs) {
            return;
        }
        int paragraphIdx = lineNum - 1;
        int lineStartOffset = codeArea.getAbsolutePosition(paragraphIdx, 0);
        int lineLen = codeArea.getParagraph(paragraphIdx).getText().length();
        int clampedStart = Math.min(Math.max(0, startCol), lineLen);
        int clampedEnd = Math.min(Math.max(clampedStart, endCol), lineLen);
        jumpToRange(lineStartOffset + clampedStart, lineStartOffset + clampedEnd);
    }

    /**
     * 获取当前光标所在的 1-based 行号
     * @return 当前行号
     */
    public int getCurrentLineNumber() {
        if (codeArea == null) {
            return 1;
        }
        return codeArea.getCurrentParagraph() + 1;
    }

    /**
     * 弹出添加或修改当前光标所在行的分析注释对话框
     */
    public void promptEditComment() {
        int paragraphIdx = codeArea.getCurrentParagraph();
        int lineNum = paragraphIdx + 1;
        String lineText = codeArea.getParagraph(paragraphIdx).getText();

        String existingComment = extractLineComment(lineText);

        Window window = codeArea.getScene() != null ? codeArea.getScene().getWindow() : null;
        Optional<String> result = EditCommentDialog.show(window, lineNum, lineText, existingComment);
        if (result.isPresent()) {
            String newComment = result.get();
            applyLineComment(paragraphIdx, newComment);
        }
    }

    /**
     * 从单行文本中提取已有的行注释内容
     */
    private String extractLineComment(String line) {
        if (line == null) {
            return "";
        }
        int idx = line.lastIndexOf("-- [Note] ");
        if (idx >= 0) {
            return line.substring(idx + 10).trim();
        }
        int generalIdx = line.lastIndexOf("--");
        if (generalIdx >= 0) {
            return line.substring(generalIdx + 2).trim();
        }
        return "";
    }

    /**
     * 在指定行末尾追加或更新 -- [Note] 注释
     * @param paragraphIdx 行索引（0-based）
     * @param newComment 新注释内容，为空则清除该行已有注释
     */
    public void applyLineComment(int paragraphIdx, String newComment) {
        if (paragraphIdx < 0 || paragraphIdx >= codeArea.getParagraphs().size()) {
            return;
        }
        String line = codeArea.getParagraph(paragraphIdx).getText();
        int commentIdx = line.lastIndexOf("-- [Note] ");
        String baseLine = (commentIdx >= 0) ? line.substring(0, commentIdx).replaceAll("\\s+$", "") : line.replaceAll("\\s+$", "");

        String updatedLine;
        if (newComment != null && !newComment.trim().isEmpty()) {
            updatedLine = baseLine + "  -- [Note] " + newComment.trim();
        } else {
            updatedLine = baseLine;
        }

        if (!updatedLine.equals(line)) {
            int start = codeArea.getAbsolutePosition(paragraphIdx, 0);
            int end = start + line.length();
            codeArea.replaceText(start, end, updatedLine);
        }
        if (commentChangeListener != null) {
            commentChangeListener.accept(paragraphIdx + 1, newComment);
        }
    }
    
    /**
     * 计算语法高亮与符号联动高亮样式
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
            if (matcher.group("KEYWORDS") != null || matcher.group("OPCODES") != null) {
                styleClass = "keyword";
            } else if (matcher.group("STRINGS") != null) {
                styleClass = "string";
            } else if (matcher.group("COMMENTS") != null) {
                styleClass = "comment";
            } else if (matcher.group("NUMBERS") != null) {
                styleClass = "number";
            } else if (matcher.group("GLOBALVAR") != null || matcher.group("DIRECTIVES") != null) {
                styleClass = "global-var";
            } else if (matcher.group("METHODCALL") != null) {
                styleClass = "method-call";
            } else if (matcher.group("TABLEFIELD") != null) {
                styleClass = "table-field";
            } else if (matcher.group("FUNCTION") != null) {
                styleClass = "function";
            } else if (matcher.group("LOCALVAR") != null || matcher.group("REGISTERS") != null) {
                styleClass = "local-var";
            } else if (matcher.group("OPERATORS") != null) {
                styleClass = "operator";
            }
            if (styleClass != null) {
                ranges.add(new StyleRange(matcher.start(), matcher.end(), styleClass));
            }
        }
        // 搜索高亮区间
        for (int i = 0; i < searchMatches.size(); i++) {
            SearchMatch match = searchMatches.get(i);
            ranges.add(new StyleRange(match.start, match.end, i == currentSearchIndex ? "search-current" : "search-hit"));
        }
        // IDA Pro 风格符号联动高亮区间
        for (SymbolMatch match : symbolHighlightMatches) {
            ranges.add(new StyleRange(match.start, match.end, match.isCurrent ? "symbol-current" : "symbol-highlight"));
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
        Map<String, Integer> active = new HashMap<>();
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
     * 获取 RichTextFX 的 CodeArea 组件
     * @return CodeArea 组件
     */
    public FoldableCodeArea getCodeArea() {
        return codeArea;
    }
    
    /**
     * 设置文本内容
     * @param text 文本内容
     */
    public void setText(String text) {
        clearFoldState();
        currentHighlightSymbol = null;
        symbolHighlightMatches.clear();
        codeArea.replaceText(text);
        codeArea.moveTo(0);
        codeArea.requestFollowCaret();
        rebuildFunctionFolds();
        updateSearchMatches(false);
        refreshHighlighting();
        if (codeIntelligenceController != null) {
            codeIntelligenceController.rebuildIndex();
        }
    }

    /**
     * 打开浮动搜索栏 (Ctrl+F)
     */
    public void openSearch() {
        if (virtualizedScrollPane.getScene() == null) {
            return;
        }
        Window window = virtualizedScrollPane.getScene().getWindow();
        if (window == null) {
            return;
        }

        double scrollWidth = virtualizedScrollPane.getWidth();
        double barWidth = (searchBar != null && searchBar.getWidth() > 0) ? searchBar.getWidth() : 530;
        double targetX = Math.max(16, scrollWidth - barWidth - 30);
        Point2D point = virtualizedScrollPane.localToScreen(targetX, 10);
        if (point != null) {
            if (!searchPopup.isShowing()) {
                searchPopup.show(window, point.getX(), point.getY());
            } else {
                searchPopup.setX(point.getX());
                searchPopup.setY(point.getY());
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

    /**
     * 关闭浮动搜索栏
     */
    public void closeSearch() {
        if (searchPopup.isShowing()) {
            searchPopup.hide();
        }
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
        int startLine = codeArea.offsetToPosition(startOffset, Bias.Forward).getMajor();
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
            int start = Math.max(0, fold.startLine);
            int end = Math.min(fold.endLine, codeArea.getParagraphs().size() - 1);
            for (int i = start; i <= end; i++) {
                codeArea.recreateParagraphGraphic(i);
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
     * 用户手动导出文本到文件。
     * @param file 文件对象
     * @throws IOException IO异常
     */
    public void exportToFile(File file) throws IOException {
        try (BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                file.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
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

    public static class FoldableCodeArea extends StyledTextArea<Collection<String>, Collection<String>> {
        public FoldableCodeArea() {
            super(
                Collections.emptyList(),
                (textFlow, styles) -> {
                    textFlow.getStyleClass().addAll(styles);
                    boolean collapsed = styles.contains("collapse");
                    textFlow.setVisible(!collapsed);
                    textFlow.setManaged(!collapsed);
                },
                Collections.emptyList(),
                (text, styles) -> text.getStyleClass().addAll(styles),
                new SimpleEditableStyledDocument<>(Collections.emptyList(), Collections.emptyList()),
                false
            );
            getStyleClass().add("code-area");
            getStylesheets().add(CodeArea.class.getResource("code-area.css").toExternalForm());
            setUseInitialStyleForInsertion(true);
            setStyleCodecs(
                Codec.collectionCodec(Codec.STRING_CODEC),
                Codec.styledTextCodec(Codec.collectionCodec(Codec.STRING_CODEC))
            );
        }

        public void foldFunctionParagraphs(int startParagraph, int endParagraph, String marker) {
            if (startParagraph < endParagraph) {
                foldParagraphs(startParagraph, endParagraph, styles -> {
                    Set<String> next = new HashSet<>(styles);
                    next.add(marker);
                    next.add("collapse");
                    return next;
                });
            }
        }

        public void unfoldFunctionParagraphs(int paragraph, String marker) {
            unfoldParagraphs(paragraph, styles -> styles.contains(marker), styles -> {
                Set<String> next = new HashSet<>(styles);
                next.remove(marker);
                next.remove("collapse");
                return next;
            });
        }
    }

    /**
     * 应用当前主题的语法高亮与组件样式表
     */
    private void applyThemeStyles() {
        try {
            codeArea.getStylesheets().clear();
            searchBar.getStylesheets().clear();
            java.net.URL url = getClass().getResource("/css/" + ThemeManager.getInstance().getCurrentTheme().getCssFileName());
            if (url != null) {
                String cssPath = url.toExternalForm();
                codeArea.getStylesheets().add(cssPath);
                searchBar.getStylesheets().add(cssPath);
            }
        } catch (Exception e) {
            LOGGER.warning("无法加载主题样式表: " + e.getMessage());
        }
    }

    private void applyEditorSettings(AppSettings settings) {
        codeArea.setStyle(EditorAppearance.fontStyle(settings));
        codeArea.setWrapText(settings.isWrapText());
        installParagraphGraphics();
    }
}
