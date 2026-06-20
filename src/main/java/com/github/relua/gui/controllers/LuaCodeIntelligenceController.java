package com.github.relua.gui.controllers;

import com.github.relua.gui.services.LuaSymbolIndexer;
import com.github.relua.gui.services.LuaSymbolIndexer.LuaSymbol;
import com.github.relua.gui.services.LuaSymbolIndexer.SymbolType;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.event.MouseOverTextEvent;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class LuaCodeIntelligenceController {
    private final CodeArea codeArea;
    private final LuaSymbolIndexer indexer;
    private final Label statusLabel;
    
    private java.util.function.Supplier<File> rootFolderSupplier;
    private java.util.function.Consumer<File> fileNavigationHandler;
    
    public void setRootFolderSupplier(java.util.function.Supplier<File> rootFolderSupplier) {
        this.rootFolderSupplier = rootFolderSupplier;
    }
    
    public void setFileNavigationHandler(java.util.function.Consumer<File> fileNavigationHandler) {
        this.fileNavigationHandler = fileNavigationHandler;
    }
    
    // Hover popup
    private final Popup hoverPopup = new Popup();
    private final Label hoverLabel = new Label();
    
    // Autocomplete popup
    private final Popup autocompletePopup = new Popup();
    private final ListView<LuaSymbol> autocompleteList = new ListView<>();
    
    // Navigation back stack
    private static class JumpLocation {
        final int caretPosition;
        JumpLocation(int caretPosition) {
            this.caretPosition = caretPosition;
        }
    }
    private final Stack<JumpLocation> jumpHistory = new Stack<>();
    
    private boolean isRebuilding = false;
    private javafx.animation.Timeline rebuildTimeline;
    
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "and", "break", "do", "else", "elseif", "end", "false", "for", "function", 
        "goto", "if", "in", "local", "nil", "not", "or", "repeat", "return", "then", 
        "true", "until", "while"
    ));

    public LuaCodeIntelligenceController(CodeArea codeArea, Label statusLabel) {
        this.codeArea = codeArea;
        this.statusLabel = statusLabel;
        this.indexer = new LuaSymbolIndexer();
        
        initHoverPopup();
        initAutocompletePopup();
        initEventHandlers();
    }

    private void initHoverPopup() {
        hoverLabel.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.98);" +
            "-fx-text-fill: #333333;" +
            "-fx-padding: 8px 12px;" +
            "-fx-border-color: rgba(0, 0, 0, 0.15);" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 4px;" +
            "-fx-background-radius: 4px;" +
            "-fx-font-family: 'Consolas', 'Monaco', monospace;" +
            "-fx-font-size: 13px;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 0);"
        );
        hoverPopup.getContent().add(hoverLabel);
    }

    private void initAutocompletePopup() {
        autocompleteList.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.98);" +
            "-fx-border-color: rgba(0, 0, 0, 0.15);" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 4px;" +
            "-fx-background-radius: 4px;" +
            "-fx-font-family: 'Consolas', 'Monaco', monospace;" +
            "-fx-font-size: 13px;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 0);"
        );
        autocompleteList.setCellFactory(lv -> new ListCell<LuaSymbol>() {
            @Override
            protected void updateItem(LuaSymbol item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.name);
                    Label typeLabel = new Label(" (" + item.type.name().toLowerCase() + ")");
                    typeLabel.setStyle("-fx-text-fill: #808080; -fx-font-size: 11px;");
                    setGraphic(typeLabel);
                    setStyle("-fx-text-fill: #333333; -fx-padding: 4px 8px;");
                }
            }
        });
        
        autocompleteList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                commitCompletion();
            }
        });
        
        autocompletePopup.getContent().add(autocompleteList);
    }

    private void initEventHandlers() {
        // Enable mouse-over text events
        codeArea.setMouseOverTextDelay(java.time.Duration.ofMillis(500));
        
        // 1. Mouse hover tooltips
        codeArea.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN, event -> {
            int charIdx = event.getCharacterIndex();
            String text = codeArea.getText();
            
            // Check require match first (only in open folder mode)
            if (rootFolderSupplier != null && rootFolderSupplier.get() != null) {
                RequireMatch reqMatch = findRequireMatchAt(text, charIdx);
                if (reqMatch != null) {
                    File resolvedFile = resolveRequireFile(rootFolderSupplier.get(), reqMatch.moduleName);
                    if (resolvedFile != null) {
                        showRequireHoverTip(reqMatch.moduleName, resolvedFile, event.getScreenPosition());
                        return;
                    }
                }
            }

            String word = getWordAt(text, charIdx);
            if (word != null && !KEYWORDS.contains(word)) {
                int line = getLineOfCharacter(text, charIdx);
                LuaSymbol def = indexer.findDefinition(word, line);
                if (def != null) {
                    showHoverTip(def, event.getScreenPosition());
                }
            }
        });
        
        codeArea.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_END, event -> {
            hoverPopup.hide();
        });
        
        // 2. Mouse moved with Ctrl (show hand cursor on clickable symbols)
        codeArea.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            if (event.isControlDown()) {
                org.fxmisc.richtext.CharacterHit hit = codeArea.hit(event.getX(), event.getY());
                hit.getCharacterIndex().ifPresent(idx -> {
                    // Check require match first (only in open folder mode)
                    if (rootFolderSupplier != null && rootFolderSupplier.get() != null) {
                        RequireMatch reqMatch = findRequireMatchAt(codeArea.getText(), idx);
                        if (reqMatch != null) {
                            File resolvedFile = resolveRequireFile(rootFolderSupplier.get(), reqMatch.moduleName);
                            if (resolvedFile != null) {
                                codeArea.setCursor(Cursor.HAND);
                                return;
                            }
                        }
                    }

                    String word = getWordAt(codeArea.getText(), idx);
                    if (word != null && !KEYWORDS.contains(word)) {
                        int line = codeArea.offsetToPosition(idx, org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMajor() + 1;
                        LuaSymbol def = indexer.findDefinition(word, line);
                        if (def != null) {
                            codeArea.setCursor(Cursor.HAND);
                            return;
                        }
                    }
                    codeArea.setCursor(Cursor.DEFAULT);
                });
            } else {
                codeArea.setCursor(Cursor.DEFAULT);
            }
        });
        
        // 3. Ctrl + Click Navigation
        codeArea.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.isControlDown() && event.getButton() == MouseButton.PRIMARY) {
                org.fxmisc.richtext.CharacterHit hit = codeArea.hit(event.getX(), event.getY());
                hit.getCharacterIndex().ifPresent(idx -> {
                    // Check require match first (only in open folder mode)
                    if (rootFolderSupplier != null && rootFolderSupplier.get() != null) {
                        RequireMatch reqMatch = findRequireMatchAt(codeArea.getText(), idx);
                        if (reqMatch != null) {
                            File resolvedFile = resolveRequireFile(rootFolderSupplier.get(), reqMatch.moduleName);
                            if (resolvedFile != null) {
                                if (fileNavigationHandler != null) {
                                    fileNavigationHandler.accept(resolvedFile);
                                    showStatusMessage("已导航到模块: " + reqMatch.moduleName);
                                }
                                return;
                            } else {
                                showStatusMessage("未在当前文件夹中找到模块文件: " + reqMatch.moduleName);
                                return;
                            }
                        }
                    }

                    String word = getWordAt(codeArea.getText(), idx);
                    if (word != null && !KEYWORDS.contains(word)) {
                        int line = codeArea.offsetToPosition(idx, org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMajor() + 1;
                        LuaSymbol def = indexer.findDefinition(word, line);
                        if (def != null) {
                            if (def.line == line) {
                                // Clicked on definition -> search for references
                                showReferencesPopup(word, line, event.getScreenX(), event.getScreenY());
                            } else {
                                // Clicked on reference -> jump to definition
                                jumpHistory.push(new JumpLocation(codeArea.getCaretPosition()));
                                jumpToLine(def.line, word);
                            }
                        }
                    }
                });
            }
        });
        
        // 4. Autocomplete and debounced index rebuilding on text change
        codeArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isRebuilding) return;
            Platform.runLater(this::showAutocompleteIfNeeded);
            
            if (rebuildTimeline != null) {
                rebuildTimeline.stop();
            }
            rebuildTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(500), event -> {
                    rebuildIndex();
                })
            );
            rebuildTimeline.play();
        });
        
        // 5. Key events filter (Autocomplete navigation & Alt+Left jump back)
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Alt + Left to navigate back
            if (event.isAltDown() && event.getCode() == KeyCode.LEFT) {
                navigateBack();
                event.consume();
                return;
            }
            
            // Autocomplete handling
            if (autocompletePopup.isShowing()) {
                switch (event.getCode()) {
                    case UP:
                        autocompleteList.getSelectionModel().selectPrevious();
                        event.consume();
                        break;
                    case DOWN:
                        autocompleteList.getSelectionModel().selectNext();
                        event.consume();
                        break;
                    case ENTER:
                    case TAB:
                        commitCompletion();
                        event.consume();
                        break;
                    case ESCAPE:
                        autocompletePopup.hide();
                        event.consume();
                        break;
                    default:
                        break;
                }
            }
        });
        
        // Hide autocomplete when clicking elsewhere
        codeArea.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            autocompletePopup.hide();
        });
    }

    public void rebuildIndex() {
        if (rebuildTimeline != null) {
            rebuildTimeline.stop();
        }
        this.isRebuilding = true;
        String text = codeArea.getText();
        
        Task<Void> indexTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                indexer.rebuild(text);
                return null;
            }
            
            @Override
            protected void succeeded() {
                jumpHistory.clear();
                autocompletePopup.hide();
                hoverPopup.hide();
                isRebuilding = false;
            }
            
            @Override
            protected void failed() {
                getException().printStackTrace();
                isRebuilding = false;
            }
        };
        
        Thread thread = new Thread(indexTask);
        thread.setDaemon(true);
        thread.start();
    }

    private String getWordAt(String text, int charIdx) {
        if (charIdx < 0 || charIdx >= text.length()) {
            return null;
        }
        int start = charIdx;
        while (start > 0 && isWordChar(text.charAt(start - 1))) {
            start--;
        }
        int end = charIdx;
        while (end < text.length() && isWordChar(text.charAt(end))) {
            end++;
        }
        if (start < end) {
            return text.substring(start, end);
        }
        return null;
    }

    private boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private int getLineOfCharacter(String text, int charIdx) {
        int line = 1;
        for (int i = 0; i < charIdx && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private void showHoverTip(LuaSymbol def, Point2D screenPos) {
        StringBuilder sb = new StringBuilder();
        if (def.type == SymbolType.FUNCTION) {
            sb.append("(function) ").append(def.signature).append("\n");
        } else if (def.type == SymbolType.LOCAL_VAR) {
            sb.append("(local) ").append(def.name).append("\n");
        } else if (def.type == SymbolType.IMPORT_MODULE) {
            sb.append("(module) ").append(def.signature).append("\n");
        } else {
            sb.append(def.name).append("\n");
        }
        sb.append(def.description);
        
        hoverLabel.setText(sb.toString());
        hoverPopup.show(codeArea.getScene().getWindow(), screenPos.getX() + 10, screenPos.getY() + 10);
    }

    private void showReferencesPopup(String word, int defLine, double screenX, double screenY) {
        List<Integer> refs = indexer.findReferences(codeArea.getText(), word, defLine);
        if (refs.isEmpty()) {
            showStatusMessage("没有找到 '" + word + "' 的其他引用");
            return;
        }

        ContextMenu menu = new ContextMenu();
        MenuItem titleItem = new MenuItem("--- '" + word + "' 的所有引用位置 ---");
        titleItem.setDisable(true);
        menu.getItems().add(titleItem);

        for (int refLine : refs) {
            String linePreview = getLinePreview(refLine);
            MenuItem item = new MenuItem("行 " + refLine + ": " + linePreview);
            item.setOnAction(e -> {
                jumpHistory.push(new JumpLocation(codeArea.getCaretPosition()));
                jumpToLine(refLine, word);
            });
            menu.getItems().add(item);
        }
        menu.show(codeArea.getScene().getWindow(), screenX, screenY);
    }

    private String getLinePreview(int lineNum) {
        String[] lines = codeArea.getText().split("\\r?\\n");
        if (lineNum > 0 && lineNum <= lines.length) {
            String preview = lines[lineNum - 1].trim();
            if (preview.length() > 30) {
                preview = preview.substring(0, 27) + "...";
            }
            return preview;
        }
        return "";
    }

    private void jumpToLine(int lineNum, String word) {
        if (lineNum <= 0) return;
        
        int paragraphIdx = lineNum - 1;
        if (paragraphIdx >= codeArea.getParagraphs().size()) {
            return;
        }
        
        codeArea.showParagraphAtTop(paragraphIdx);
        
        String lineText = codeArea.getParagraph(paragraphIdx).getText();
        int idx = lineText.indexOf(word);
        if (idx != -1) {
            int startOffset = codeArea.getAbsolutePosition(paragraphIdx, idx);
            codeArea.moveTo(startOffset);
            codeArea.selectRange(startOffset, startOffset + word.length());
        } else {
            codeArea.moveTo(paragraphIdx, 0);
        }
        codeArea.requestFollowCaret();
        showStatusMessage("已跳转到行 " + lineNum);
    }

    private void navigateBack() {
        if (!jumpHistory.isEmpty()) {
            JumpLocation loc = jumpHistory.pop();
            if (loc.caretPosition >= 0 && loc.caretPosition <= codeArea.getText().length()) {
                codeArea.moveTo(loc.caretPosition);
                codeArea.requestFollowCaret();
                showStatusMessage("返回到前一次查看的位置");
            }
        }
    }

    private String getTypedPrefix() {
        int caret = codeArea.getCaretPosition();
        String text = codeArea.getText();
        if (caret <= 0 || caret > text.length()) {
            return "";
        }
        int start = caret;
        while (start > 0 && (Character.isLetterOrDigit(text.charAt(start - 1)) || text.charAt(start - 1) == '_')) {
            start--;
        }
        return text.substring(start, caret);
    }

    private void showAutocompleteIfNeeded() {
        String prefix = getTypedPrefix();
        if (prefix.isEmpty()) {
            autocompletePopup.hide();
            return;
        }

        int currentLine = codeArea.getCurrentParagraph() + 1;
        List<LuaSymbol> suggestions = indexer.getAutocompleteSuggestions(prefix, currentLine);
        if (suggestions.isEmpty()) {
            autocompletePopup.hide();
            return;
        }

        autocompleteList.getItems().setAll(suggestions);
        autocompleteList.getSelectionModel().selectFirst();
        
        int itemsCount = Math.min(10, suggestions.size());
        autocompleteList.setPrefHeight(itemsCount * 24 + 6);
        autocompleteList.setPrefWidth(220);

        if (!autocompletePopup.isShowing()) {
            Optional<Bounds> caretBounds = codeArea.getCaretBoundsOnScreen(codeArea.getCaretSelectionBind().getUnderlyingCaret());
            if (caretBounds.isPresent()) {
                Bounds bounds = caretBounds.get();
                autocompletePopup.show(codeArea.getScene().getWindow(), bounds.getMinX(), bounds.getMaxY());
            } else {
                autocompletePopup.hide();
            }
        }
    }

    private void commitCompletion() {
        LuaSymbol selected = autocompleteList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            int caret = codeArea.getCaretPosition();
            String prefix = getTypedPrefix();
            codeArea.replaceText(caret - prefix.length(), caret, selected.name);
            autocompletePopup.hide();
        }
    }

    private void showStatusMessage(String msg) {
        if (statusLabel != null) {
            statusLabel.setText(msg);
        }
    }

    private static class RequireMatch {
        final int start;
        final int end;
        final String moduleName;
        RequireMatch(int start, int end, String moduleName) {
            this.start = start;
            this.end = end;
            this.moduleName = moduleName;
        }
    }

    private RequireMatch findRequireMatchAt(String text, int charIdx) {
        if (text == null || charIdx < 0 || charIdx >= text.length()) {
            return null;
        }
        // Define patterns with group 1 as the module name
        Pattern[] patterns = {
            Pattern.compile("\\brequire\\s*\\(\\s*\"([^\"]*)\"\\s*\\)"),
            Pattern.compile("\\brequire\\s*\\(\\s*'([^']*)'\\s*\\)"),
            Pattern.compile("\\brequire\\s*\\(\\s*\\[\\[([^\\]]*)\\]\\]\\s*\\)"),
            Pattern.compile("\\brequire\\s*\"([^\"]*)\""),
            Pattern.compile("\\brequire\\s*'([^']*)'"),
            Pattern.compile("\\brequire\\s*\\[\\[([^\\]]*)\\]\\]")
        };
        for (Pattern p : patterns) {
            Matcher m = p.matcher(text);
            while (m.find()) {
                if (charIdx >= m.start() && charIdx < m.end()) {
                    return new RequireMatch(m.start(), m.end(), m.group(1));
                }
            }
        }
        return null;
    }

    private File resolveRequireFile(File rootFolder, String moduleName) {
        if (rootFolder == null || moduleName == null || moduleName.trim().isEmpty()) {
            return null;
        }
        if (moduleName.contains("..")) {
            return null;
        }
        
        // Normalize separators: replace '.' and '\\' with '/'
        String normalized = moduleName.replace('.', '/').replace('\\', '/');
        
        // Strip leading and trailing slashes
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        
        if (normalized.isEmpty()) {
            return null;
        }
        
        // Try exact match first (fast path)
        File file1 = new File(rootFolder, normalized + ".lua");
        if (file1.exists() && file1.isFile() && isUnderDirectory(rootFolder, file1)) {
            return file1;
        }
        File file2 = new File(rootFolder, normalized + "/init.lua");
        if (file2.exists() && file2.isFile() && isUnderDirectory(rootFolder, file2)) {
            return file2;
        }
        
        // Try case-insensitive match (slow path / compatible path)
        String[] parts1 = (normalized + ".lua").split("/");
        File matched1 = findFileCaseInsensitive(rootFolder, parts1, 0);
        if (matched1 != null && isUnderDirectory(rootFolder, matched1)) {
            return matched1;
        }
        
        String[] parts2 = (normalized + "/init.lua").split("/");
        File matched2 = findFileCaseInsensitive(rootFolder, parts2, 0);
        if (matched2 != null && isUnderDirectory(rootFolder, matched2)) {
            return matched2;
        }
        
        return null;
    }

    private File findFileCaseInsensitive(File currentDir, String[] parts, int index) {
        if (currentDir == null || !currentDir.exists() || !currentDir.isDirectory()) {
            return null;
        }
        if (index >= parts.length) {
            return null;
        }
        
        String part = parts[index];
        File[] children = currentDir.listFiles();
        if (children == null) {
            return null;
        }
        
        for (File child : children) {
            if (child.getName().equalsIgnoreCase(part)) {
                if (index == parts.length - 1) {
                    if (child.isFile()) {
                        return child;
                    }
                } else {
                    if (child.isDirectory()) {
                        File found = findFileCaseInsensitive(child, parts, index + 1);
                        if (found != null) {
                            return found;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isUnderDirectory(File directory, File file) {
        try {
            String dirPath = directory.getCanonicalPath();
            String filePath = file.getCanonicalPath();
            return filePath.startsWith(dirPath + File.separator) || filePath.equals(dirPath);
        } catch (IOException e) {
            return false;
        }
    }

    private void showRequireHoverTip(String moduleName, File resolvedFile, Point2D screenPos) {
        String tip = "(module) " + moduleName + "\n" +
                     "Ctrl + Click to navigate to " + resolvedFile.getName();
        hoverLabel.setText(tip);
        hoverPopup.show(codeArea.getScene().getWindow(), screenPos.getX() + 10, screenPos.getY() + 10);
    }
}
