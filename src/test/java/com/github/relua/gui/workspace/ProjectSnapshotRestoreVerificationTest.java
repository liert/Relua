package com.github.relua.gui.workspace;

import com.github.relua.gui.handlers.FileMenuHandler;
import com.github.relua.gui.utils.I18nUtil;
import com.github.relua.gui.views.EditorTab;
import com.github.relua.model.LineRange;
import com.github.relua.parser.ChunkParser;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("工程分析快照恢复与防重复解析回归测试")
public class ProjectSnapshotRestoreVerificationTest {

    private static final byte[] SAMPLE_BYTECODE = loadSampleBytecode();

    private static byte[] loadSampleBytecode() {
        try {
            return Files.readAllBytes(new File("src/test/resources/xiaomi/aeslua.lua").toPath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load sample bytecode", e);
        }
    }

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.class.getMethod("startup", Runnable.class).invoke(null, (Runnable) () -> {});
        } catch (Throwable ignored) {}
        try {
            Platform.setImplicitExit(false);
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
    @DisplayName("场景1&2: 已保存的有效快照在重新打开后被命中，且不会调用 ChunkParser 原始字节码解析")
    void testSnapshotHitAndZeroChunkParserCalls(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("module1.luac");
        Files.write(file, SAMPLE_BYTECODE);

        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        manager.openFolderWorkspace(tempDir.toFile());

        AtomicReference<TabPane> tabs = new AtomicReference<>();
        AtomicReference<FileMenuHandler> handler = new AtomicReference<>();
        onFx(() -> {
            tabs.set(new TabPane());
            handler.set(new FileMenuHandler(tabs.get(), new Label(), new Label(), null));
        });

        // 首次打开：未命中快照，必须触发解析
        ChunkParser.resetParseCallCount();
        EditorTab tab = open(handler.get(), file.toFile());
        assertTrue(ChunkParser.getParseCallCount() > 0, "首次打开文件时必须调用 ChunkParser 解析字节码");
        assertNotNull(tab.getTextEditorView().getText());
        String initialDecompiledCode = tab.getTextEditorView().getText();

        // 用户编辑与重命名变量并保存分析
        onFx(() -> {
            tab.getTextEditorView().renameVariable("local_var", "renamed_var");
            tab.getLuaEditorView().getCodeArea().moveTo(15);
            handler.get().saveAnalysis();
            handler.get().resetLoadingState();
        });

        // 重新打开同一工程文件夹
        manager.openFolderWorkspace(tempDir.toFile());
        assertEquals(1, manager.getCurrentWorkspace().getModifiedFiles().size(), "工程数据库应包含 1 项分析快照");

        // 重置计数器，开始恢复标签页
        ChunkParser.resetParseCallCount();
        CountDownLatch restoredLatch = new CountDownLatch(1);
        onFx(() -> {
            handler.get().setOnFileOpenedCallback(restoredLatch::countDown);
            handler.get().restoreWorkspaceTabs();
        });
        assertTrue(restoredLatch.await(10, TimeUnit.SECONDS));

        // 验证：有效快照恢复过程中，严禁调用 ChunkParser
        assertEquals(0, ChunkParser.getParseCallCount(), "【核心断言】命中有效快照恢复标签页时，ChunkParser 调用次数必须为 0！");

        onFx(() -> {
            EditorTab restoredTab = (EditorTab) tabs.get().getSelectionModel().getSelectedItem();
            assertNotNull(restoredTab);
            assertEquals(file.toFile(), restoredTab.getFile());
            assertEquals(initialDecompiledCode, restoredTab.getLuaEditorView().getText());
            assertFalse(restoredTab.isLoading(), "加载蒙层必须已隐藏");
        });
    }

    @Test
    @DisplayName("场景3: 对同一个有效快照重复关闭和重新打开工程，绝不重复解析")
    void testConsecutiveCloseAndReopenZeroParsing(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("consecutive.luac");
        Files.write(file, SAMPLE_BYTECODE);

        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        manager.openFolderWorkspace(tempDir.toFile());

        AtomicReference<TabPane> tabs = new AtomicReference<>();
        AtomicReference<FileMenuHandler> handler = new AtomicReference<>();
        onFx(() -> {
            tabs.set(new TabPane());
            handler.set(new FileMenuHandler(tabs.get(), new Label(), new Label(), null));
        });

        open(handler.get(), file.toFile());
        onFx(() -> {
            handler.get().saveAnalysis();
            handler.get().resetLoadingState();
        });

        // 连续关闭与重新打开 3 次
        for (int i = 1; i <= 3; i++) {
            onFx(() -> handler.get().resetLoadingState());
            manager.openFolderWorkspace(tempDir.toFile());
            ChunkParser.resetParseCallCount();
            CountDownLatch latch = new CountDownLatch(1);
            onFx(() -> {
                handler.get().setOnFileOpenedCallback(latch::countDown);
                handler.get().restoreWorkspaceTabs();
            });
            assertTrue(latch.await(10, TimeUnit.SECONDS));
            assertEquals(0, ChunkParser.getParseCallCount(), "第 " + i + " 次重新打开工程时，ChunkParser 调用次数必须为 0！");
        }
    }

    @Test
    @DisplayName("场景4: 多个分析记录均存在时，每个文件都能准确匹配各自的快照")
    void testMultipleFilesMatchOwnSnapshots(@TempDir Path tempDir) throws Exception {
        Path fileA = tempDir.resolve("mod_a.luac");
        Path fileB = tempDir.resolve("mod_b.luac");
        Path fileC = tempDir.resolve("mod_c.luac");
        Files.write(fileA, SAMPLE_BYTECODE);
        Files.write(fileB, SAMPLE_BYTECODE);
        Files.write(fileC, SAMPLE_BYTECODE);

        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        manager.openFolderWorkspace(tempDir.toFile());

        AtomicReference<TabPane> tabs = new AtomicReference<>();
        AtomicReference<FileMenuHandler> handler = new AtomicReference<>();
        onFx(() -> {
            tabs.set(new TabPane());
            handler.set(new FileMenuHandler(tabs.get(), new Label(), new Label(), null));
        });

        EditorTab tabA = open(handler.get(), fileA.toFile());
        EditorTab tabB = open(handler.get(), fileB.toFile());
        EditorTab tabC = open(handler.get(), fileC.toFile());

        // 各自设置独立的专属内容
        onFx(() -> {
            tabA.getTextEditorView().setText("-- Custom Source A --\nreturn 'A'");
            tabB.getTextEditorView().setText("-- Custom Source B --\nreturn 'B'");
            tabC.getTextEditorView().setText("-- Custom Source C --\nreturn 'C'");
            handler.get().saveAnalysis();
            handler.get().resetLoadingState();
        });

        // 重新加载工程
        manager.openFolderWorkspace(tempDir.toFile());
        assertEquals(3, manager.getCurrentWorkspace().getModifiedFiles().size());

        ChunkParser.resetParseCallCount();
        CountDownLatch latch = new CountDownLatch(3);
        onFx(() -> {
            handler.get().setOnFileOpenedCallback(latch::countDown);
            handler.get().restoreWorkspaceTabs();
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(0, ChunkParser.getParseCallCount(), "恢复 3 个有效快照时，ChunkParser 调用次数必须为 0");

        onFx(() -> {
            Map<String, String> loadedTexts = new HashMap<>();
            for (javafx.scene.control.Tab t : tabs.get().getTabs()) {
                EditorTab et = (EditorTab) t;
                loadedTexts.put(et.getFile().getName(), et.getLuaEditorView().getText());
            }
            assertEquals("-- Custom Source A --\nreturn 'A'", loadedTexts.get("mod_a.luac"));
            assertEquals("-- Custom Source B --\nreturn 'B'", loadedTexts.get("mod_b.luac"));
            assertEquals("-- Custom Source C --\nreturn 'C'", loadedTexts.get("mod_c.luac"));
        });
    }

    @Test
    @DisplayName("场景5: 不同目录下存在同名文件时，按相对路径隔离快照，互不干扰")
    void testSameFileNameInDifferentDirectories(@TempDir Path tempDir) throws Exception {
        Path dir1 = tempDir.resolve("dir1");
        Path dir2 = tempDir.resolve("dir2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);

        Path file1 = dir1.resolve("config.luac");
        Path file2 = dir2.resolve("config.luac");
        Files.write(file1, SAMPLE_BYTECODE);
        Files.write(file2, SAMPLE_BYTECODE);

        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        manager.openFolderWorkspace(tempDir.toFile());

        AtomicReference<TabPane> tabs = new AtomicReference<>();
        AtomicReference<FileMenuHandler> handler = new AtomicReference<>();
        onFx(() -> {
            tabs.set(new TabPane());
            handler.set(new FileMenuHandler(tabs.get(), new Label(), new Label(), null));
        });

        EditorTab tab1 = open(handler.get(), file1.toFile());
        EditorTab tab2 = open(handler.get(), file2.toFile());

        onFx(() -> {
            tab1.getTextEditorView().setText("-- DIR1 CONFIG --");
            tab2.getTextEditorView().setText("-- DIR2 CONFIG --");
            handler.get().saveAnalysis();
            handler.get().resetLoadingState();
        });

        manager.openFolderWorkspace(tempDir.toFile());
        ChunkParser.resetParseCallCount();
        CountDownLatch latch = new CountDownLatch(2);
        onFx(() -> {
            handler.get().setOnFileOpenedCallback(latch::countDown);
            handler.get().restoreWorkspaceTabs();
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(0, ChunkParser.getParseCallCount(), "恢复同名文件快照时 ChunkParser 调用次数必须为 0");

        onFx(() -> {
            for (javafx.scene.control.Tab t : tabs.get().getTabs()) {
                EditorTab et = (EditorTab) t;
                if (et.getFile().equals(file1.toFile())) {
                    assertEquals("-- DIR1 CONFIG --", et.getLuaEditorView().getText());
                } else if (et.getFile().equals(file2.toFile())) {
                    assertEquals("-- DIR2 CONFIG --", et.getLuaEditorView().getText());
                }
            }
        });
    }

    @Test
    @DisplayName("场景6: 文件被外部修改、SHA-256 不匹配时，正常触发重新解析")
    void testFileExternallyModifiedTriggersReParse(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("target.luac");
        Files.write(file, SAMPLE_BYTECODE);

        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        manager.openFolderWorkspace(tempDir.toFile());

        AtomicReference<TabPane> tabs = new AtomicReference<>();
        AtomicReference<FileMenuHandler> handler = new AtomicReference<>();
        onFx(() -> {
            tabs.set(new TabPane());
            handler.set(new FileMenuHandler(tabs.get(), new Label(), new Label(), null));
        });

        open(handler.get(), file.toFile());
        onFx(() -> {
            handler.get().saveAnalysis();
            handler.get().resetLoadingState();
        });

        // 外部修改原文件内容
        Files.write(file, "print('externally modified text')\n".getBytes(StandardCharsets.UTF_8));

        manager.openFolderWorkspace(tempDir.toFile());
        ChunkParser.resetParseCallCount();
        CountDownLatch latch = new CountDownLatch(1);
        onFx(() -> {
            handler.get().setOnFileOpenedCallback(latch::countDown);
            handler.get().restoreWorkspaceTabs();
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));

        // 验证：指纹不匹配时必须触发重新解析与读取
        onFx(() -> {
            EditorTab modifiedTab = (EditorTab) tabs.get().getSelectionModel().getSelectedItem();
            assertNotNull(modifiedTab);
            assertTrue(modifiedTab.getLuaEditorView().getText().contains("externally modified"), "必须读取更新后的文件内容");
            assertFalse(modifiedTab.isLoading());
        });
    }

    @Test
    @DisplayName("场景7: 快照缺失或损坏时，正常回退到重新解析")
    void testMissingOrCorruptedSnapshotFallsBackToReParse(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("corrupted.luac");
        Files.write(file, SAMPLE_BYTECODE);

        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        manager.openFolderWorkspace(tempDir.toFile());

        AtomicReference<TabPane> tabs = new AtomicReference<>();
        AtomicReference<FileMenuHandler> handler = new AtomicReference<>();
        onFx(() -> {
            tabs.set(new TabPane());
            handler.set(new FileMenuHandler(tabs.get(), new Label(), new Label(), null));
        });

        open(handler.get(), file.toFile());
        onFx(() -> {
            handler.get().saveAnalysis();
            handler.get().resetLoadingState();
        });

        // 故意破坏工程数据库文件
        File dbFile = manager.getWorkspaceDatabaseFile(tempDir.toFile());
        assertTrue(dbFile.exists());
        Files.write(dbFile.toPath(), new byte[]{ 0x00, 0x01, 0x02, 0x03 }); // 损坏的魔数

        manager.openFolderWorkspace(tempDir.toFile());
        // 数据库损坏回退后，缓存应为空
        assertEquals(0, manager.getCurrentWorkspace().getModifiedFiles().size());

        ChunkParser.resetParseCallCount();
        EditorTab tab = open(handler.get(), file.toFile());
        assertTrue(ChunkParser.getParseCallCount() > 0, "快照损坏时必须回退到正常的 ChunkParser 解析流程");
        assertNotNull(tab.getTextEditorView().getText());
    }

    @Test
    @DisplayName("场景8: 快照恢复后，按需访问 Bytecode / AST 功能依然完全可用")
    void testRestoredFeaturesAvailableOnDemand(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("demand.luac");
        Files.write(file, SAMPLE_BYTECODE);

        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        manager.openFolderWorkspace(tempDir.toFile());

        AtomicReference<TabPane> tabs = new AtomicReference<>();
        AtomicReference<FileMenuHandler> handler = new AtomicReference<>();
        onFx(() -> {
            tabs.set(new TabPane());
            handler.set(new FileMenuHandler(tabs.get(), new Label(), new Label(), null));
        });

        open(handler.get(), file.toFile());
        onFx(() -> {
            handler.get().saveAnalysis();
            handler.get().resetLoadingState();
        });

        manager.openFolderWorkspace(tempDir.toFile());
        ChunkParser.resetParseCallCount();
        CountDownLatch latch = new CountDownLatch(1);
        onFx(() -> {
            handler.get().setOnFileOpenedCallback(latch::countDown);
            handler.get().restoreWorkspaceTabs();
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(0, ChunkParser.getParseCallCount(), "恢复快照时不解析字节码");

        onFx(() -> {
            EditorTab tab = (EditorTab) tabs.get().getSelectionModel().getSelectedItem();
            assertTrue(tab.isBytecodeFile(), "应正确判定为字节码文件");
            // 首次显式触发字节码查看
            tab.refreshBytecodeContent();
            assertFalse(tab.getBytecodeEditorView().getText().isEmpty(), "字节码内容应成功按需生成");
            assertNotNull(tab.getLuacFile(), "LuacFile 模型已按需加载就绪");
            assertTrue(ChunkParser.getParseCallCount() > 0, "用户显式查看字节码时触发了按需解析");
        });
    }

    @Test
    @DisplayName("场景9: Windows UNC 路径相对化与大小写容错检索")
    void testUncPathHandling() {
        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        File uncRoot = new File("\\\\wsl.localhost\\Ubuntu\\home\\kali\\iot\\XiaoMi\\BE6500\\squashfs-root\\usr\\lib\\lua\\");
        manager.openFolderWorkspace(uncRoot);

        File uncFile = new File("\\\\wsl.localhost\\Ubuntu\\home\\kali\\iot\\XiaoMi\\BE6500\\squashfs-root\\usr\\lib\\lua\\aeslua.lua");
        String relPath = manager.toRelativePath(uncFile);
        assertEquals("aeslua.lua", relPath);

        File subFile = new File("\\\\wsl.localhost\\Ubuntu\\home\\kali\\iot\\XiaoMi\\BE6500\\squashfs-root\\usr\\lib\\lua\\sub\\crypto.lua");
        String subRelPath = manager.toRelativePath(subFile);
        assertEquals("sub/crypto.lua", subRelPath);

        // 内部文件判定
        assertTrue(manager.isInternalWorkspaceFile(uncFile));
        assertTrue(manager.isInternalWorkspaceFile(subFile));
        assertFalse(manager.isInternalWorkspaceFile(new File("C:\\Windows\\System32\\calc.exe")));
    }

    @Test
    @DisplayName("场景10: 命中快照后按需异步切换 Bytecode 与 AST 视图，无卡顿且模型就绪")
    void testBytecodeAndAstAsyncDemandLoading(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("async_demand.luac");
        Files.write(file, SAMPLE_BYTECODE);

        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        manager.openFolderWorkspace(tempDir.toFile());

        AtomicReference<TabPane> tabs = new AtomicReference<>();
        AtomicReference<FileMenuHandler> handler = new AtomicReference<>();
        onFx(() -> {
            tabs.set(new TabPane());
            handler.set(new FileMenuHandler(tabs.get(), new Label(), new Label(), null));
        });

        open(handler.get(), file.toFile());
        onFx(() -> {
            handler.get().saveAnalysis();
            handler.get().resetLoadingState();
        });

        // 重新打开工程并恢复
        manager.openFolderWorkspace(tempDir.toFile());
        ChunkParser.resetParseCallCount();
        CountDownLatch latch = new CountDownLatch(1);
        onFx(() -> {
            handler.get().setOnFileOpenedCallback(latch::countDown);
            handler.get().restoreWorkspaceTabs();
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(0, ChunkParser.getParseCallCount(), "快照恢复阶段绝不解析字节码");

        EditorTab tab = (EditorTab) tabs.get().getSelectionModel().getSelectedItem();
        assertNotNull(tab);
        assertNull(tab.getLoadedLuacFile(), "恢复后 LuacFile 应为懒加载 null");

        // 异步切换到 BYTECODE 视图
        onFx(() -> tab.setViewMode(EditorTab.ViewMode.BYTECODE));
        // 等待异步加载完成
        long start = System.currentTimeMillis();
        while (tab.getLoadedLuacFile() == null && System.currentTimeMillis() - start < 5000) {
            Thread.sleep(50);
        }
        assertNotNull(tab.getLoadedLuacFile(), "Bytecode 视图应在后台异步解析完成并设置 LuacFile");
        onFx(() -> {
            String bytecode = tab.getBytecodeEditorView().getText();
            assertNotNull(bytecode);
            assertFalse(bytecode.isEmpty(), "字节码编辑器应有内容");
            assertFalse(bytecode.contains("没有可显示的字节码"), "字节码文件不应显示不可用占位符");
        });

        // 异步切换到 AST 视图
        onFx(() -> tab.setViewMode(EditorTab.ViewMode.AST));
        start = System.currentTimeMillis();
        while (tab.getDecompilerResult() == null && System.currentTimeMillis() - start < 5000) {
            Thread.sleep(50);
        }
        assertNotNull(tab.getDecompilerResult(), "AST 视图应在后台异步生成 DecompilerResult");

        // 切回源码视图，验证源码无损
        onFx(() -> {
            tab.setViewMode(EditorTab.ViewMode.LUA_SOURCE);
            assertFalse(tab.getLuaEditorView().getText().isEmpty(), "Lua 源码视图内容完整");
        });
    }

    @Test
    @DisplayName("场景11: 快照模式下的函数大纲提取与源码编辑保存全流程测试")
    void testFunctionOutlineAndEditingWithSnapshot(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("outline_edit.luac");
        Files.write(file, SAMPLE_BYTECODE);

        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        manager.openFolderWorkspace(tempDir.toFile());

        AtomicReference<TabPane> tabs = new AtomicReference<>();
        AtomicReference<FileMenuHandler> handler = new AtomicReference<>();
        onFx(() -> {
            tabs.set(new TabPane());
            handler.set(new FileMenuHandler(tabs.get(), new Label(), new Label(), null));
        });

        EditorTab tab = open(handler.get(), file.toFile());
        onFx(() -> {
            tab.getTextEditorView().setText("function foo(a, b)\n    return a + b\nend\n\nlocal function bar()\n    return 42\nend\n");
            handler.get().saveAnalysis();
            handler.get().resetLoadingState();
        });

        // 重新打开
        manager.openFolderWorkspace(tempDir.toFile());
        ChunkParser.resetParseCallCount();
        CountDownLatch latch = new CountDownLatch(1);
        onFx(() -> {
            handler.get().setOnFileOpenedCallback(latch::countDown);
            handler.get().restoreWorkspaceTabs();
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(0, ChunkParser.getParseCallCount());

        // 验证函数大纲提取在 luacFile == null 时工作正常且无 NPE
        com.github.relua.gui.services.FileFunctionService functionService = new com.github.relua.gui.services.FileFunctionService();
        java.util.List<com.github.relua.gui.model.OutlineItem> functions = functionService.load(file.toFile());
        assertNotNull(functions);
        assertFalse(functions.isEmpty(), "函数大纲应成功提取");
        boolean foundFoo = functions.stream().anyMatch(item -> item.getName().contains("foo"));
        assertTrue(foundFoo, "应提取到 foo 函数");

        // 在快照基础上继续编辑并保存
        EditorTab restoredTab = (EditorTab) tabs.get().getSelectionModel().getSelectedItem();
        onFx(() -> {
            restoredTab.getTextEditorView().applyLineComment(0, "-- Added test comment");
            restoredTab.getTextEditorView().renameVariable("a", "alpha");
            handler.get().saveAnalysis();
        });

        FileMetadata updatedMeta = manager.getFileMetadata(file.toFile());
        assertNotNull(updatedMeta);
        assertEquals("alpha", updatedMeta.getRenamedVariables().get("a"));
        assertEquals("-- Added test comment", updatedMeta.getLineComments().get(1));
    }

    @Test
    @DisplayName("场景12: 同名文件跨目录严格隔离，回退匹配不串用，未记录文件返回 null")
    void testStrictIsolationForSameNameFiles(@TempDir Path tempDir) throws Exception {
        Path dirA = tempDir.resolve("dirA");
        Path dirB = tempDir.resolve("dirB");
        Path dirC = tempDir.resolve("dirC");
        Files.createDirectories(dirA);
        Files.createDirectories(dirB);
        Files.createDirectories(dirC);

        Path rootFile = tempDir.resolve("module.luac");
        Path fileA = dirA.resolve("module.luac");
        Path fileB = dirB.resolve("module.luac");
        Path fileC = dirC.resolve("module.luac"); // 未保存任何快照
        Files.write(rootFile, SAMPLE_BYTECODE);
        Files.write(fileA, SAMPLE_BYTECODE);
        Files.write(fileB, SAMPLE_BYTECODE);
        Files.write(fileC, SAMPLE_BYTECODE);

        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        manager.openFolderWorkspace(tempDir.toFile());

        // 分别为 rootFile, fileA, fileB 记录不同的元数据
        manager.recordFileModification(rootFile.toFile(), Collections.singletonMap("v", "root_var"), "-- ROOT FILE --");
        manager.recordFileModification(fileA.toFile(), Collections.singletonMap("v", "a_var"), "-- A FILE --");
        manager.recordFileModification(fileB.toFile(), Collections.singletonMap("v", "b_var"), "-- B FILE --");

        // 验证精确匹配
        FileMetadata rootMeta = manager.getFileMetadata(rootFile.toFile());
        FileMetadata metaA = manager.getFileMetadata(fileA.toFile());
        FileMetadata metaB = manager.getFileMetadata(fileB.toFile());
        assertNotNull(rootMeta);
        assertNotNull(metaA);
        assertNotNull(metaB);
        assertEquals("-- ROOT FILE --", rootMeta.getEditedText());
        assertEquals("-- A FILE --", metaA.getEditedText());
        assertEquals("-- B FILE --", metaB.getEditedText());

        // 核心验证：dirC/module.luac 绝不能因为同名为 module.luac 或相同 SHA-256 而串用 A, B 或 ROOT 的快照！
        FileMetadata metaC = manager.getFileMetadata(fileC.toFile());
        assertNull(metaC, "【核心断言】不同目录下的未记录同名文件绝不能匹配到其他目录或根目录的快照！");

        // 验证大小写容错仅匹配对应的相对路径
        File caseFileA = new File(tempDir.toFile(), "DIRA/module.luac");
        FileMetadata caseMetaA = manager.getFileMetadata(caseFileA);
        assertNotNull(caseMetaA, "大小写不敏感应能正确唯一定位 dirA/module.luac");
        assertEquals("-- A FILE --", caseMetaA.getEditedText());
    }

    @Test
    @DisplayName("场景13: 多线程并行解析时上下文及计数器完全隔离，无串扰无泄漏")
    void testParallelParsingThreadIsolation(@TempDir Path tempDir) throws Exception {
        Path file1 = tempDir.resolve("file1.luac");
        Path file2 = tempDir.resolve("file2.luac");
        Files.write(file1, SAMPLE_BYTECODE);
        Files.write(file2, SAMPLE_BYTECODE);

        int threadCount = 4;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicBoolean hasError = new java.util.concurrent.atomic.AtomicBoolean(false);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            final File target = (i % 2 == 0) ? file1.toFile() : file2.toFile();
            executor.submit(() -> {
                try {
                    startLatch.await();
                    ChunkParser.resetThreadParseCallCount();
                    assertNull(ChunkParser.getCurrentParsingFile(), "任务开始前上下文必须为空");

                    try (ChunkParser.ParsingScope scope = new ChunkParser.ParsingScope("test-" + index, "reason-" + index)) {
                        assertEquals("test-" + index, ChunkParser.getCurrentParsingFile());
                        assertEquals("reason-" + index, ChunkParser.getCurrentTriggerReason());

                        new com.github.relua.parser.LuacParser().parse(target.getAbsolutePath());

                        // 解析过程中 ThreadLocal 计数器正常递增
                        assertTrue(ChunkParser.getThreadParseCallCount() > 0, "当前线程解析计数应大于0");
                        assertEquals("test-" + index, ChunkParser.getCurrentParsingFile(), "解析过程中上下文不应被污染");
                    }

                    // 离开作用域后上下文必须完全清空
                    assertNull(ChunkParser.getCurrentParsingFile(), "离开作用域后上下文必须清空");
                    assertNull(ChunkParser.getCurrentTriggerReason(), "离开作用域后触发原因必须清空");
                } catch (Throwable t) {
                    t.printStackTrace();
                    hasError.set(true);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(endLatch.await(15, TimeUnit.SECONDS), "并发解析任务超时");
        executor.shutdown();
        assertFalse(hasError.get(), "并发多线程解析过程中不应存在上下文串扰或异常");
    }
}
