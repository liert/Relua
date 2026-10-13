package com.github.relua.gui.views;

import com.github.relua.gui.model.FileNode;
import com.github.relua.gui.workspace.ProjectWorkspaceManager;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FileTreeFunctionsTest {
    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.class.getMethod("startup", Runnable.class).invoke(null, (Runnable) () -> {});
        } catch (Throwable ignored) {}
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

    @Test
    void expandsUnopenedFileAndNavigatesToOwnedFunction(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("sample.lua");
        byte[] original = "local function helper(a)\nend\nfunction run()\nend\n".getBytes(StandardCharsets.UTF_8);
        Files.write(file, original);
        ProjectWorkspaceManager.getInstance().openFolderWorkspace(directory.toFile());
        CountDownLatch indexed = new CountDownLatch(1);
        AtomicReference<FileTreeView> tree = new AtomicReference<>();
        AtomicReference<TreeItem<FileNode>> fileItem = new AtomicReference<>();
        AtomicInteger navigations = new AtomicInteger();
        onFx(() -> {
            FileTreeView view = new FileTreeView();
            view.loadFolder(directory.toFile());
            TreeItem<FileNode> item = view.getView().getRoot().getChildren().get(0).getChildren().get(0);
            assertFalse(item.isLeaf(), "Lua file has an expansion arrow before loading");
            view.setOnFunctionNavigate((owner, function) -> {
                assertEquals(file.toFile(), owner);
                assertEquals("helper", function.getName());
                assertEquals(1, function.getStartLine());
                navigations.incrementAndGet();
            });
            item.getChildren().addListener((ListChangeListener<TreeItem<FileNode>>) change -> {
                if (item.getChildren().size() == 2 && item.getChildren().get(0).getValue().isFunction()) indexed.countDown();
            });
            tree.set(view);
            fileItem.set(item);
            item.setExpanded(true);
        });
        assertTrue(indexed.await(10, TimeUnit.SECONDS), "Expanding a file should load its functions");
        onFx(() -> {
            TreeItem<FileNode> helper = fileItem.get().getChildren().get(0);
            tree.get().getView().getSelectionModel().select(helper);
            assertEquals(1, navigations.get());
            tree.get().selectFile(file.toFile());
            assertSame(helper, tree.get().getView().getSelectionModel().getSelectedItem());
            fileItem.get().setExpanded(false);
            assertEquals(2, fileItem.get().getChildren().size());
            fileItem.get().setExpanded(true);
            assertEquals(2, fileItem.get().getChildren().size());
            assertEquals(1, navigations.get(), "Cache refresh must not trigger navigation");
        });
        assertArrayEquals(original, Files.readAllBytes(file));
        assertFalse(Files.exists(directory.resolve(".relua")));
    }

    @Test
    void refreshesFunctionsAfterRenameAndDisplaysEmptyFiles(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("sample.lua");
        Files.write(file, "function original()\nend\n".getBytes(StandardCharsets.UTF_8));
        ProjectWorkspaceManager.getInstance().openFolderWorkspace(directory.toFile());
        CountDownLatch initial = new CountDownLatch(1);
        CountDownLatch renamed = new CountDownLatch(1);
        CountDownLatch empty = new CountDownLatch(1);
        AtomicReference<TextEditorView> editor = new AtomicReference<>();
        AtomicReference<FileTreeView> tree = new AtomicReference<>();
        AtomicReference<TreeItem<FileNode>> fileItem = new AtomicReference<>();
        onFx(() -> {
            FileTreeView view = new FileTreeView();
            view.addFile(file.toFile());
            TreeItem<FileNode> item = view.getView().getRoot().getChildren().get(0);
            item.getChildren().addListener((ListChangeListener<TreeItem<FileNode>>) change -> {
                if (item.getChildren().isEmpty()) return;
                FileNode child = item.getChildren().get(0).getValue();
                if (child.isFunction() && child.getName().equals("original")) initial.countDown();
                if (child.isFunction() && child.getName().equals("renamed")) renamed.countDown();
                if (child.isStatus() && child.getName().equals("无函数")) empty.countDown();
            });
            TextEditorView source = new TextEditorView(false);
            EditorTab tab = new EditorTab(file.toFile(), source);
            tab.setLuacFile(null);
            source.setText("function original()\nend\n");
            view.trackEditor(tab);
            editor.set(source);
            tree.set(view);
            fileItem.set(item);
        });
        assertTrue(initial.await(10, TimeUnit.SECONDS));
        onFx(() -> editor.get().renameVariable("original", "renamed"));
        assertTrue(renamed.await(10, TimeUnit.SECONDS), "Names reflect current session edits");
        onFx(() -> editor.get().setText("return 1\n"));
        assertTrue(empty.await(10, TimeUnit.SECONDS));
        onFx(() -> {
            assertEquals(1, tree.get().getView().getRoot().getChildren().size());
            assertTrue(fileItem.get().getChildren().get(0).getValue().isStatus());
            tree.get().loadFolder(directory.toFile());
        });
        assertEquals("function original()\nend\n", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

}
