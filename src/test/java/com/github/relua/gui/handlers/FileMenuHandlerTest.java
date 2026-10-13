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
            FileMenuHandler handler = new FileMenuHandler(new TabPane(), new Label(), new Label(), null);
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
        AtomicReference<Label> status = new AtomicReference<>();
        onFx(() -> {
            status.set(new Label());
            handler.set(new FileMenuHandler(new TabPane(), status.get(), new Label(), null));
        });
        EditorTab tab = open(handler.get(), input.toFile());
        AtomicReference<String> analyzed = new AtomicReference<>();
        onFx(() -> {
            tab.getLuaEditorView().renameVariable("value", "result");
            tab.getLuaEditorView().applyLineComment(0, "分析注释");
            analyzed.set(tab.getLuaEditorView().getText());
            handler.get().handleSaveAnalysis(null);
            assertTrue(status.get().getText().startsWith("分析已保存:"));
            assertTrue(status.get().getText().contains("project.relua"));
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
    void restoresUnmodifiedBytecodeWithoutDecompilingAgain(@TempDir Path directory) throws Exception {
        Path compiled = directory.resolve("compiled.lua");
        byte[] original = Files.readAllBytes(new File("src/test/resources/xiaomi/aeslua.lua").toPath());
        Files.write(compiled, original);
        Path source = directory.resolve("second.lua");
        Files.write(source, "return 2\n".getBytes(StandardCharsets.UTF_8));
        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        manager.openFolderWorkspace(directory.toFile());
        AtomicReference<TabPane> tabs = new AtomicReference<>();
        AtomicReference<FileMenuHandler> handler = new AtomicReference<>();
        onFx(() -> {
            tabs.set(new TabPane());
            handler.set(new FileMenuHandler(tabs.get(), new Label(), new Label(), null));
        });
        EditorTab first = open(handler.get(), compiled.toFile());
        open(handler.get(), source.toFile());
        AtomicReference<String> generated = new AtomicReference<>();
        AtomicReference<java.util.Map<String, com.github.relua.model.LineRange>> ranges = new AtomicReference<>();
        onFx(() -> {
            assertNotNull(first.getDecompilerResult());
            assertTrue(first.getRenamedVariables().isEmpty());
            assertTrue(first.getLineComments().isEmpty());
            generated.set(first.getLuaEditorView().getText());
            ranges.set(new java.util.HashMap<>(first.getChunkLineRanges()));
            first.getLuaEditorView().getCodeArea().moveTo(25);
            tabs.get().getSelectionModel().select(first);
            handler.get().saveAnalysis();
            handler.get().resetLoadingState();
        });
        manager.openFolderWorkspace(directory.toFile());
        assertEquals(2, manager.getCurrentWorkspace().getModifiedFiles().size());
        assertNotNull(manager.getValidSourceSnapshot(compiled.toFile()));
        CountDownLatch restored = new CountDownLatch(2);
        onFx(() -> {
            handler.get().setOnFileOpenedCallback(restored::countDown);
            handler.get().restoreWorkspaceTabs();
            assertEquals(0, tabs.get().getSelectionModel().getSelectedIndex());
        });
        assertTrue(restored.await(10, TimeUnit.SECONDS));
        onFx(() -> {
            EditorTab cached = (EditorTab) tabs.get().getSelectionModel().getSelectedItem();
            assertEquals(compiled.toFile(), cached.getFile());
            assertEquals(generated.get(), cached.getLuaEditorView().getText());
            assertEquals(25, cached.getLuaEditorView().getCodeArea().getCaretPosition());
            assertNull(cached.getDecompilerResult(), "Saved source should bypass the decompiler pipeline");
            assertNotNull(cached.getLuacFile(), "Bytecode remains available");
            cached.refreshBytecodeContent();
            assertFalse(cached.getBytecodeEditorView().getText().isEmpty());
            assertEquals(ranges.get().keySet(), cached.getChunkLineRanges().keySet());
            ranges.get().forEach((name, range) -> {
                assertEquals(range.startLine, cached.getChunkLineRanges().get(name).startLine);
                assertEquals(range.endLine, cached.getChunkLineRanges().get(name).endLine);
            });
            handler.get().saveAnalysis();
            handler.get().resetLoadingState();
        });
        manager.openFolderWorkspace(directory.toFile());
        assertEquals(ranges.get().size(), manager.getValidSourceSnapshot(compiled.toFile()).getChunkLineRanges().size());
        assertArrayEquals(original, Files.readAllBytes(compiled));
    }

    @Test
    void ignoresSavedSourceAndAnnotationsWhenOriginalChanges(@TempDir Path directory) throws Exception {
        Path input = directory.resolve("changed.lua");
        Files.write(input, "local value = 1\n".getBytes(StandardCharsets.UTF_8));
        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        manager.openFolderWorkspace(directory.toFile());
        AtomicReference<FileMenuHandler> handler = new AtomicReference<>();
        onFx(() -> handler.set(new FileMenuHandler(new TabPane(), new Label(), new Label(), null)));
        EditorTab first = open(handler.get(), input.toFile());
        onFx(() -> {
            first.getLuaEditorView().renameVariable("value", "oldResult");
            handler.get().saveAnalysis();
            handler.get().resetLoadingState();
        });
        String changed = "local fresh = 2\nreturn fresh\n";
        Files.write(input, changed.getBytes(StandardCharsets.UTF_8));
        manager.openFolderWorkspace(directory.toFile());
        assertNull(manager.getValidSourceSnapshot(input.toFile()));
        EditorTab reopened = open(handler.get(), input.toFile());
        onFx(() -> {
            assertEquals(changed, reopened.getLuaEditorView().getText());
            assertTrue(reopened.getRenamedVariables().isEmpty());
            handler.get().resetLoadingState();
        });
    }

    @Test
    void savesAllStandaloneFilesWithMatchingNames(@TempDir Path directory) throws Exception {
        ProjectWorkspaceManager.getInstance().openFolderWorkspace(null);
        onFx(() -> {
            try {
                TabPane tabs = new TabPane();
                FileMenuHandler handler = new FileMenuHandler(tabs, new Label(), new Label(), null);
                for (String folder : new String[] {"first", "second", "untouched"}) {
                    Path input = directory.resolve(folder).resolve("same.lua");
                    Files.createDirectories(input.getParent());
                    Files.write(input, "local value = 1\n".getBytes(StandardCharsets.UTF_8));
                    TextEditorView editor = new TextEditorView(false);
                    EditorTab tab = new EditorTab(input.toFile(), editor);
                    tab.setLuacFile(null);
                    editor.setText("local value = 1\n");
                    if (!folder.equals("untouched")) editor.renameVariable("value", folder);
                    tabs.getTabs().add(tab);
                }
                handler.saveAnalysis();
                for (String folder : new String[] {"first", "second", "untouched"}) {
                    Path input = directory.resolve(folder).resolve("same.lua");
                    assertEquals("local value = 1\n", new String(Files.readAllBytes(input), StandardCharsets.UTF_8));
                    FileMetadata metadata = BinaryPersistenceService.load(new File(input.toString() + ".relua"))
                            .getModifiedFiles().get("same.lua");
                    assertTrue(metadata.hasSourceSnapshot());
                    if (folder.equals("untouched")) {
                        assertTrue(metadata.getRenamedVariables().isEmpty());
                        assertEquals("local value = 1\n", metadata.getEditedText());
                    } else {
                        assertEquals(folder, metadata.getRenamedVariables().get("value"));
                        assertEquals("local " + folder + " = 1\n", metadata.getEditedText());
                    }
                }
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }

    @Test
    void reportsPersistenceFailureWithoutWritingSource(@TempDir Path directory) throws Exception {
        Path input = directory.resolve("source.lua");
        byte[] original = "local value = 1\n".getBytes(StandardCharsets.UTF_8);
        Files.write(input, original);
        Files.write(directory.resolve(".relua"), new byte[] {1});
        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        manager.openFolderWorkspace(directory.toFile());
        onFx(() -> {
            TextEditorView editor = new TextEditorView(false);
            EditorTab tab = new EditorTab(input.toFile(), editor);
            tab.setLuacFile(null);
            editor.setText("local value = 1\n");
            editor.renameVariable("value", "result");
            assertThrows(IOException.class, () -> manager.saveChecked(java.util.Collections.singletonList(tab), tab));
        });
        assertArrayEquals(original, Files.readAllBytes(input));
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
                FileMenuHandler handler = new FileMenuHandler(tabs, new Label(), new Label(), null);
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
