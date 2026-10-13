package com.github.relua.gui.dialogs;

import com.github.relua.gui.preferences.AppSettings;
import com.github.relua.gui.preferences.SettingsManager;
import com.github.relua.gui.theme.ThemeManager.Theme;
import com.github.relua.gui.views.EditorTab;
import com.github.relua.gui.views.LogView;
import com.github.relua.gui.views.TextEditorView;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class PreferencesDialogTest {
    @BeforeAll static void initializeJavaFx() {
        try { Platform.class.getMethod("startup", Runnable.class).invoke(null, (Runnable) () -> {}); }
        catch (Throwable ignored) {}
    }

    private void onFx(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try { action.run(); } catch (Throwable error) { failure.set(error); }
            finally { done.countDown(); }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    @Test void previewsDraftValidatesSizeAndAppliesToExistingViews() throws Exception {
        onFx(() -> {
            SettingsManager manager = new SettingsManager(null);
            TextEditorView source = new TextEditorView(false, manager);
            source.setText("local message = 1\nreturn message\n");
            EditorTab existing = new EditorTab(new File("test.lua"), source);
            LogView log = new LogView(manager);
            log.setLogLevelComboBox(new ComboBox<>());
            PreferencesDialog dialog = new PreferencesDialog(null, manager);
            DialogPane pane = dialog.getDialogPane();
            TextField size = (TextField) pane.lookup("#preferencesFontSize");
            Button apply = (Button) pane.lookupButton(pane.getButtonTypes().stream()
                    .filter(type -> type.getButtonData() == javafx.scene.control.ButtonBar.ButtonData.APPLY)
                    .findFirst().orElseThrow(AssertionError::new));
            ComboBox<String> family = (ComboBox<String>) pane.lookup("#preferencesFontFamily");
            family.setValue("Courier New");
            size.setText("22");
            ((CheckBox) pane.lookup("#preferencesWrapText")).setSelected(true);
            ((CheckBox) pane.lookup("#preferencesLineNumbers")).setSelected(false);
            ((CheckBox) pane.lookup("#preferencesDefaultSplit")).setSelected(true);
            ((ComboBox<String>) pane.lookup("#preferencesLogLevel")).setValue("WARNING");
            assertEquals(AppSettings.defaults(), manager.getSettings(), "Preview does not apply preferences");
            size.setText("99");
            assertTrue(apply.isDisabled());
            size.setText("22");
            assertFalse(apply.isDisabled());
            apply.fire();
            assertEquals(22, manager.getSettings().getFontSize());
            assertTrue(source.getCodeArea().isWrapText());
            assertTrue(existing.getBytecodeEditorView().getCodeArea().isWrapText());
            assertTrue(source.getCodeArea().getStyle().contains("22px"));
            assertTrue(log.getLogCodeArea().getStyle().contains("22px"));
            assertEquals("WARNING", log.getLogLevelComboBox().getValue());
            Node gutter = source.getCodeArea().getParagraphGraphicFactory().apply(0);
            assertFalse(((javafx.scene.layout.HBox) gutter).getChildren().get(1).isManaged());
            assertFalse(existing.isSideBySide(), "Changing default split does not alter existing tabs");
            EditorTab newTab = new EditorTab(new File("new.lua"), new TextEditorView(false, manager));
            assertTrue(newTab.isSideBySide());
            assertEquals(EditorTab.ViewMode.BYTECODE, newTab.getSecondaryViewMode());
            size.setText("31");
            dialog.close();
            assertEquals(22, manager.getSettings().getFontSize(), "Unapplied edits remain a draft");
            Button reset = (Button) pane.lookupButton(pane.getButtonTypes().stream()
                    .filter(type -> type.getButtonData() == javafx.scene.control.ButtonBar.ButtonData.LEFT)
                    .findFirst().orElseThrow(AssertionError::new));
            reset.fire();
            assertEquals("14", size.getText());
            assertEquals(22, manager.getSettings().getFontSize(), "Reset remains a draft until applied");
        });
    }

    @Test void actualTextAndLineNumbersUseSelectedFontSizeAfterCss() throws Exception {
        onFx(() -> {
            try {
                SettingsManager manager = new SettingsManager(null);
                TextEditorView editor = new TextEditorView(false, manager);
                editor.setText("message\n");
                StackPane root = new StackPane(editor.getView());
                new Scene(root, 600, 300);
                manager.apply(new AppSettings("Courier New", 24, false, true, false, Theme.DARK, "INFO"));
                root.applyCss();
                root.layout();
                Text text = editor.getCodeArea().lookupAll(".text").stream().filter(node -> node instanceof Text)
                        .map(node -> (Text) node).filter(node -> "message".equals(node.getText())).findFirst().orElseThrow(AssertionError::new);
                assertEquals(24, text.getFont().getSize());
                assertEquals(javafx.scene.text.Font.font("Courier New").getFamily(), text.getFont().getFamily());
                javafx.scene.layout.HBox gutter = (javafx.scene.layout.HBox) editor.getCodeArea().getParagraphGraphicFactory().apply(0);
                Label number = (Label) gutter.getChildren().get(1);
                assertTrue(number.getStyle().contains("24px"));
                assertEquals("message\n", editor.getText());
                assertFalse(editor.isEditable());
            } catch (Exception error) { throw new RuntimeException(error); }
        });
    }
}
