package com.github.relua.gui.dialogs;

import com.github.relua.gui.preferences.AppSettings;
import com.github.relua.gui.preferences.EditorAppearance;
import com.github.relua.gui.preferences.SettingsManager;
import com.github.relua.gui.theme.ThemeManager;
import com.github.relua.gui.theme.ThemeManager.Theme;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 编辑草稿和字体预览，只有应用或确定才保存程序设置。 */
public class PreferencesDialog extends Dialog<Void> {
    private final SettingsManager manager;
    private final ComboBox<String> fontFamily = new ComboBox<>();
    private final TextField fontSize = new TextField();
    private final CheckBox wrapText = new CheckBox("代码自动换行");
    private final CheckBox lineNumbers = new CheckBox("显示行号");
    private final CheckBox defaultSplit = new CheckBox("新文件默认开启 Split view");
    private final ComboBox<Theme> theme = new ComboBox<>();
    private final ComboBox<String> logLevel = new ComboBox<>();
    private final CodeArea preview = new CodeArea();
    private final Label errorLabel = new Label();
    private final Button applyButton;
    private final Button okButton;
    private boolean loading;

    public PreferencesDialog(Window owner) {
        this(owner, SettingsManager.getInstance());
    }

    public PreferencesDialog(Window owner, SettingsManager manager) {
        this.manager = manager;
        if (owner != null) initOwner(owner);
        setTitle("首选项");
        setResizable(true);
        getDialogPane().getStyleClass().add("preferences-dialog");
        ThemeManager.getInstance().applyTheme(this);

        ButtonType reset = new ButtonType("恢复默认", ButtonData.LEFT);
        ButtonType apply = new ButtonType("应用", ButtonData.APPLY);
        ButtonType ok = new ButtonType("确定", ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(reset, apply, ok,
                new ButtonType("取消", ButtonData.CANCEL_CLOSE));
        applyButton = (Button) getDialogPane().lookupButton(apply);
        okButton = (Button) getDialogPane().lookupButton(ok);
        applyButton.setId("preferencesApply");
        okButton.setId("preferencesOk");
        getDialogPane().lookupButton(reset).addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            loadDraft(AppSettings.defaults());
        });
        applyButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            applyDraft();
        });
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!applyDraft()) event.consume();
        });

        List<String> families = new ArrayList<>(Font.getFamilies());
        families.sort(String.CASE_INSENSITIVE_ORDER);
        if (!families.contains(manager.getSettings().getFontFamily())) families.add(0, manager.getSettings().getFontFamily());
        fontFamily.getItems().setAll(families);
        fontFamily.setMaxWidth(Double.MAX_VALUE);
        fontFamily.setId("preferencesFontFamily");
        fontSize.setId("preferencesFontSize");
        fontSize.setPrefColumnCount(4);
        fontSize.setMaxWidth(90);
        fontSize.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("[0-9]{0,2}") ? change : null));
        wrapText.setId("preferencesWrapText");
        lineNumbers.setId("preferencesLineNumbers");
        defaultSplit.setId("preferencesDefaultSplit");
        theme.setId("preferencesTheme");
        theme.getItems().setAll(Theme.values());
        theme.setMaxWidth(Double.MAX_VALUE);
        theme.setConverter(new StringConverter<Theme>() {
            @Override public String toString(Theme value) { return value == Theme.DARK ? "深色" : value == Theme.LIGHT ? "浅色" : ""; }
            @Override public Theme fromString(String value) { return "浅色".equals(value) ? Theme.LIGHT : Theme.DARK; }
        });
        logLevel.setId("preferencesLogLevel");
        logLevel.getItems().setAll(AppSettings.LOG_LEVELS);
        logLevel.setMaxWidth(Double.MAX_VALUE);
        logLevel.setConverter(new StringConverter<String>() {
            @Override public String toString(String value) {
                if (value == null) return "";
                switch (value) {
                    case "ALL": return "ALL · 全部";
                    case "DEBUG": return "DEBUG · 调试";
                    case "WARNING": return "WARNING · 警告";
                    case "ERROR": return "ERROR · 错误";
                    default: return "INFO · 信息";
                }
            }
            @Override public String fromString(String value) { return value.split(" ")[0]; }
        });

        GridPane editorGrid = createGrid();
        editorGrid.addRow(0, new Label("代码字体"), fontFamily);
        editorGrid.addRow(1, new Label("字号（8–48 px）"), fontSize);
        VBox editorOptions = new VBox(9, wrapText, lineNumbers, defaultSplit);
        Label splitHint = hint("默认分屏仅影响之后打开的文件。字体同步用于 Code、Bytecode 和日志。");
        GridPane appearanceGrid = createGrid();
        appearanceGrid.addRow(0, new Label("界面主题"), theme);
        appearanceGrid.addRow(1, new Label("日志级别"), logLevel);

        preview.setEditable(false);
        preview.setId("preferencesFontPreview");
        preview.replaceText("local message = \"你好，Relua\"\nfunction greet(name)\n    return message .. name\nend");
        Matcher keywords = Pattern.compile("\\b(local|function|return|end)\\b").matcher(preview.getText());
        while (keywords.find()) preview.setStyleClass(keywords.start(), keywords.end(), "keyword");
        VirtualizedScrollPane<CodeArea> previewPane = new VirtualizedScrollPane<>(preview);
        previewPane.setPrefHeight(140);
        previewPane.setMinHeight(100);
        errorLabel.getStyleClass().add("preferences-error");
        errorLabel.setWrapText(true);
        VBox content = new VBox(12, heading("编辑器"), editorGrid, editorOptions, splitHint,
                heading("字体预览"), previewPane, new Separator(), heading("外观与日志"), appearanceGrid,
                errorLabel, hint("点击应用后即时生效，设置会在下次启动时恢复。"));
        content.setPadding(new Insets(18));
        content.setPrefWidth(580);
        getDialogPane().setContent(content);

        fontFamily.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        fontSize.textProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        wrapText.selectedProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        lineNumbers.selectedProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        defaultSplit.selectedProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        theme.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        logLevel.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
        loadDraft(manager.getSettings());
    }

    private GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        javafx.scene.layout.ColumnConstraints label = new javafx.scene.layout.ColumnConstraints(130);
        javafx.scene.layout.ColumnConstraints control = new javafx.scene.layout.ColumnConstraints();
        control.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(label, control);
        return grid;
    }

    private Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("preferences-heading");
        return label;
    }

    private Label hint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("preferences-hint");
        label.setWrapText(true);
        return label;
    }

    private void loadDraft(AppSettings settings) {
        loading = true;
        fontFamily.setValue(settings.getFontFamily());
        fontSize.setText(Integer.toString(settings.getFontSize()));
        wrapText.setSelected(settings.isWrapText());
        lineNumbers.setSelected(settings.isShowLineNumbers());
        defaultSplit.setSelected(settings.isDefaultSplitView());
        theme.setValue(settings.getTheme());
        logLevel.setValue(settings.getLogLevel());
        loading = false;
        updatePreview();
    }

    private AppSettings readDraft() {
        int size;
        try { size = Integer.parseInt(fontSize.getText()); }
        catch (NumberFormatException error) { throw new IllegalArgumentException("请输入 8–48 之间的整数字号"); }
        return new AppSettings(fontFamily.getValue(), size, wrapText.isSelected(), lineNumbers.isSelected(),
                defaultSplit.isSelected(), theme.getValue(), logLevel.getValue());
    }

    private void updatePreview() {
        if (loading) return;
        try {
            AppSettings settings = readDraft();
            String css = getClass().getResource("/css/" + settings.getTheme().getCssFileName()).toExternalForm();
            getDialogPane().getStylesheets().setAll(css);
            preview.getStylesheets().setAll(css);
            preview.setStyle(EditorAppearance.fontStyle(settings));
            preview.setWrapText(settings.isWrapText());
            java.util.function.IntFunction<Node> numbers = LineNumberFactory.get(preview);
            preview.setParagraphGraphicFactory(settings.isShowLineNumbers() ? paragraph -> {
                Node number = numbers.apply(paragraph);
                number.setStyle(EditorAppearance.fontStyle(settings));
                return number;
            } : null);
            showError("");
            applyButton.setDisable(false);
            okButton.setDisable(false);
        } catch (IllegalArgumentException error) {
            showError(error.getMessage());
            applyButton.setDisable(true);
            okButton.setDisable(true);
        }
    }

    private boolean applyDraft() {
        try {
            manager.apply(readDraft());
            showError("");
            return true;
        } catch (BackingStoreException | IllegalArgumentException error) {
            showError("无法保存首选项：" + error.getMessage());
            return false;
        }
    }

    private void showError(String text) {
        errorLabel.setText(text);
        errorLabel.setVisible(!text.isEmpty());
        errorLabel.setManaged(!text.isEmpty());
    }
}
