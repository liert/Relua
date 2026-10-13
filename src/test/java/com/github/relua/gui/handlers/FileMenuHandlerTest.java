package com.github.relua.gui.handlers;

import com.github.relua.gui.views.EditorTab;
import com.github.relua.gui.views.TextEditorView;
import com.github.relua.gui.workspace.BinaryPersistenceService;
import com.github.relua.gui.workspace.FileMetadata;
import com.github.relua.gui.workspace.ProjectWorkspaceManager;
import com.github.relua.gui.utils.I18nUtil;
import com.github.relua.parser.LuacParser;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FileMenuHandlerTest {
    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.class.getMethod("startup", Runnable.class).invoke(null, (Runnable) () -> {});
        } catch (Throwable ignored) {}
        I18nUtil.initialize(Locale.CHINA);
    }

    private void onFx(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try { action.run(); } catch (Throwable e) { failure.set(e); }
            finally { done.countDown(); }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private EditorTab open(FileMenuHandler handler, File file) throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<EditorTab> opened = new AtomicReference<>();
        onFx(() -> handler.openFile(file, tab -> {
            opened.set(tab);
            ready.countDown();
        }));
        assertTrue(ready.await(10, TimeUnit.SECONDS), "File should load");
        return opened.get();
    }

    @Test
    void navigationWaitsForAnAlreadyOpeningFile(@TempDir Path directory) throws Exception {
        Path input = directory.resolve("pending.lua");
        String source = "function ready()\nend\n";
        Files.write(input, source.getBytes(StandardCharsets.UTF_8));
        ProjectWorkspaceManager.getInstance().openFolderWorkspace(directory.toFile());
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        onFx(() -> {
            FileMenuHandler handler = new FileMenuHandler(new TabPane(), null, null, new Label(), new Label(), null);
            handler.openFile(input.toFile());
            handler.openFile(input.toFile(), tab -> {
                try {
                    assertTrue(tab.isFileLoaded());
                    assertEquals(source, tab.getLuaEditorView().getText());
                } catch (Throwable error) { failure.set(error); }
                finally { ready.countDown(); }
            });
        });
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        assertNull(failure.get());
    }

    @Test
    void savesAndRestoresAnalysisWithoutWritingSource(@TempDir Path directory) throws Exception {
        Path input = directory.resolve("source.lua");
        byte[] original = "local value = 1\nprint(value)\n".getBytes(StandardCharsets.UTF_8);
        Files.write(input, original);
        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        manager.openFolderWorkspace(directory.toFile());
        AtomicReference<FileMenuHandler> handler = new AtomicReference<>();
        onFx(() -> handler.set(new FileMenuHandler(new TabPane(), null, null, new Label(), new Label(), null)));
        EditorTab tab = open(handler.get(), input.toFile());
        AtomicReference<String> analyzed = new AtomicReference<>();
        onFx(() -> {
            tab.getLuaEditorView().renameVariable("value", "result");
            tab.getLuaEditorView().applyLineComment(0, "分析注释");
            analyzed.set(tab.getLuaEditorView().getText());
            handler.get().saveAnalysis();
            handler.get().resetLoadingState();
        });
        assertArrayEquals(original, Files.readAllBytes(input));
        assertTrue(Files.exists(directory.resolve(".relua/project.relua")));

        manager.openFolderWorkspace(directory.toFile());
        EditorTab reopened = open(handler.get(), input.toFile());
        onFx(() -> {
            assertEquals(analyzed.get(), reopened.getLuaEditorView().getText());
            assertEquals("result", reopened.getRenamedVariables().get("value"));
            assertEquals("分析注释", reopened.getLineComments().get(1));
            reopened.getLuaEditorView().applyLineComment(0, "");
            analyzed.set(reopened.getLuaEditorView().getText());
            handler.get().saveAnalysis();
            handler.get().resetLoadingState();
        });
        manager.openFolderWorkspace(directory.toFile());
        EditorTab withoutComment = open(handler.get(), input.toFile());
        onFx(() -> {
            assertEquals(analyzed.get(), withoutComment.getLuaEditorView().getText());
            assertTrue(withoutComment.getLineComments().isEmpty());
        });
        assertArrayEquals(original, Files.readAllBytes(input));
    }

    @Test
    void savesAllStandaloneFilesWithMatchingNames(@TempDir Path directory) throws Exception {
        ProjectWorkspaceManager.getInstance().openFolderWorkspace(null);
        onFx(() -> {
            try {
                TabPane tabs = new TabPane();
                FileMenuHandler handler = new FileMenuHandler(tabs, null, null, new Label(), new Label(), null);
                for (String folder : new String[] {"first", "second"}) {
                    Path input = directory.resolve(folder).resolve("same.lua");
                    Files.createDirectories(input.getParent());
                    Files.write(input, "local value = 1\n".getBytes(StandardCharsets.UTF_8));
                    TextEditorView editor = new TextEditorView(false);
                    EditorTab tab = new EditorTab(input.toFile(), editor);
                    tab.setLuacFile(null);
                    editor.setText("local value = 1\n");
                    editor.renameVariable("value", folder);
                    tabs.getTabs().add(tab);
                }
                handler.saveAnalysis();
                for (String folder : new String[] {"first", "second"}) {
                    Path input = directory.resolve(folder).resolve("same.lua");
                    assertEquals("local value = 1\n", new String(Files.readAllBytes(input), StandardCharsets.UTF_8));
                    FileMetadata metadata = BinaryPersistenceService.load(new File(input.toString() + ".relua"))
                            .getModifiedFiles().get("same.lua");
                    assertEquals(folder, metadata.getRenamedVariables().get("value"));
                    assertEquals("local " + folder + " = 1\n", metadata.getEditedText());
                }
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }

    @Test
    void exportsOnlyToSeparateFilesAndKeepsBytecodeBinding(@TempDir Path directory) throws Exception {
        Path input = directory.resolve("compiled.lua");
        byte[] original = Files.readAllBytes(new File("src/test/resources/xiaomi/aeslua.lua").toPath());
        Files.write(input, original);
        onFx(() -> {
            try {
                TextEditorView editor = new TextEditorView(false);
                EditorTab tab = new EditorTab(input.toFile(), editor);
                tab.setLuacFile(new LuacParser().parse(input.toString()));
                editor.setText("return 42 -- 分析结果\n");
                TabPane tabs = new TabPane(tab);
                FileMenuHandler handler = new FileMenuHandler(tabs, null, null, new Label(), new Label(), null);
                assertThrows(IOException.class, () -> handler.exportSource(input.toFile()));
                Path exported = directory.resolve("exported.lua");
                handler.exportSource(exported.toFile());
                assertEquals(editor.getText(), new String(Files.readAllBytes(exported), StandardCharsets.UTF_8));
                assertEquals(input.toFile(), tab.getFile());
                assertNotNull(tab.getLuacFile());
                assertArrayEquals(original, Files.readAllBytes(input));
                tabs.getTabs().add(new EditorTab(directory.resolve("loading.lua").toFile(), new TextEditorView(false)));
                tabs.getSelectionModel().selectLast();
                assertThrows(IOException.class, () -> handler.exportSource(directory.resolve("not-written.lua").toFile()));
                assertFalse(Files.exists(directory.resolve("not-written.lua")));
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
