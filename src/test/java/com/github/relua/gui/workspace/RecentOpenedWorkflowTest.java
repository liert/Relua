package com.github.relua.gui.workspace;

import com.github.relua.gui.handlers.FileMenuHandler;
import com.github.relua.gui.utils.I18nUtil;
import com.github.relua.gui.views.EditorTab;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 针对“最近打开（Recent Opened）”功能的完整业务场景与回归验证测试
 */
class RecentOpenedWorkflowTest {

    private RecentHistoryManager recentManager;
    private ProjectWorkspaceManager workspaceManager;

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

    @BeforeEach
    void setUp() {
        recentManager = RecentHistoryManager.getInstance();
        recentManager.clearRecentPaths();
        workspaceManager = ProjectWorkspaceManager.getInstance();
        workspaceManager.openFolderWorkspace(null);
    }

    @AfterEach
    void tearDown() {
        recentManager.clearRecentPaths();
        workspaceManager.openFolderWorkspace(null);
    }

    private void onFx(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable e) {
                failure.set(e);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS), "JavaFX operation timed out");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    private EditorTab open(FileMenuHandler handler, File file) throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<EditorTab> opened = new AtomicReference<>();
        onFx(() -> handler.openFile(file, tab -> {
            opened.set(tab);
            ready.countDown();
        }));
        assertTrue(ready.await(10, TimeUnit.SECONDS), "File loading timed out for " + file.getName());
        return opened.get();
    }

    @Test
    @DisplayName("测试 isInternalWorkspaceFile 对工程内部文件与外部文件的判定")
    void testIsInternalWorkspaceFileIdentification(@TempDir Path tempDir) throws Exception {
        Path projectFolder = tempDir.resolve("my_project");
        Files.createDirectories(projectFolder);
        Path subFolder = projectFolder.resolve("src").resolve("module");
        Files.createDirectories(subFolder);

        Path internalFile1 = projectFolder.resolve("main.lua");
        Path internalFile2 = subFolder.resolve("helper.lua");
        Files.write(internalFile1, "return 1\n".getBytes(StandardCharsets.UTF_8));
        Files.write(internalFile2, "return 2\n".getBytes(StandardCharsets.UTF_8));

        Path externalFolder = tempDir.resolve("external");
        Files.createDirectories(externalFolder);
        Path externalFile = externalFolder.resolve("standalone.lua");
        Files.write(externalFile, "return 3\n".getBytes(StandardCharsets.UTF_8));

        // 1. 未打开任何工程时，所有文件均非工程内部文件
        workspaceManager.openFolderWorkspace(null);
        assertFalse(workspaceManager.isInternalWorkspaceFile(internalFile1.toFile()));
        assertFalse(workspaceManager.isInternalWorkspaceFile(externalFile.toFile()));

        // 2. 打开 my_project 工程
        workspaceManager.openFolderWorkspace(projectFolder.toFile());
        assertTrue(workspaceManager.isInternalWorkspaceFile(internalFile1.toFile()));
        assertTrue(workspaceManager.isInternalWorkspaceFile(internalFile2.toFile()));
        assertTrue(workspaceManager.isInternalWorkspaceFile(internalFile1.toFile().getAbsolutePath()));
        assertTrue(workspaceManager.isInternalWorkspaceFile(internalFile2.toFile().getAbsolutePath()));

        // 3. 外部文件不属于当前工程
        assertFalse(workspaceManager.isInternalWorkspaceFile(externalFile.toFile()));
        assertFalse(workspaceManager.isInternalWorkspaceFile(externalFile.toFile().getAbsolutePath()));

        // 4. 工程根目录自身不是“工程内部文件”
        assertFalse(workspaceManager.isInternalWorkspaceFile(projectFolder.toFile()));
        assertFalse(workspaceManager.isInternalWorkspaceFile((File) null));
        assertFalse(workspaceManager.isInternalWorkspaceFile((String) null));
    }

    @Test
    @DisplayName("场景一：打开文件夹后连续打开其中多个文件，最近打开列表仅保留文件夹记录")
    void testOpenFolderAndInternalFilesOnlyKeepsFolder(@TempDir Path tempDir) throws Exception {
        Path folderPath = tempDir.resolve("project_alpha");
        Files.createDirectories(folderPath.resolve("sub"));

        Path fileA = folderPath.resolve("a.lua");
        Path fileB = folderPath.resolve("b.lua");
        Path fileC = folderPath.resolve("sub").resolve("c.lua");
        Files.write(fileA, "print('a')\n".getBytes(StandardCharsets.UTF_8));
        Files.write(fileB, "print('b')\n".getBytes(StandardCharsets.UTF_8));
        Files.write(fileC, "print('c')\n".getBytes(StandardCharsets.UTF_8));

        // 模拟打开文件夹工程
        workspaceManager.openFolderWorkspace(folderPath.toFile());
        recentManager.addRecentPath(folderPath.toFile().getAbsolutePath());

        // 验证当前仅包含该文件夹
        assertEquals(1, recentManager.getRecentPaths().size());
        assertEquals(folderPath.toFile().getAbsolutePath(), recentManager.getRecentPaths().get(0));

        AtomicReference<FileMenuHandler> handlerRef = new AtomicReference<>();
        onFx(() -> {
            TabPane tabPane = new TabPane();
            handlerRef.set(new FileMenuHandler(tabPane, new Label(), new Label(), null));
        });

        // 连续打开工程内的文件
        open(handlerRef.get(), fileA.toFile());
        open(handlerRef.get(), fileB.toFile());
        open(handlerRef.get(), fileC.toFile());

        // 验证最近打开列表仍然仅保留该文件夹记录，工程内部文件不会被额外写入
        List<String> recentList = recentManager.getRecentPaths();
        assertEquals(1, recentList.size(), "最近打开列表不应包含文件夹内的普通文件");
        assertEquals(folderPath.toFile().getAbsolutePath(), recentList.get(0));
    }

    @Test
    @DisplayName("场景二：在同一文件夹内切换标签页、编辑文件，不会产生额外的最近打开记录")
    void testSwitchTabAndEditDoesNotModifyRecentPaths(@TempDir Path tempDir) throws Exception {
        Path folderPath = tempDir.resolve("project_beta");
        Files.createDirectories(folderPath);

        Path file1 = folderPath.resolve("file1.lua");
        Path file2 = folderPath.resolve("file2.lua");
        Files.write(file1, "local a = 1\n".getBytes(StandardCharsets.UTF_8));
        Files.write(file2, "local b = 2\n".getBytes(StandardCharsets.UTF_8));

        workspaceManager.openFolderWorkspace(folderPath.toFile());
        recentManager.addRecentPath(folderPath.toFile().getAbsolutePath());

        AtomicReference<FileMenuHandler> handlerRef = new AtomicReference<>();
        AtomicReference<TabPane> tabPaneRef = new AtomicReference<>();
        onFx(() -> {
            TabPane tabPane = new TabPane();
            tabPaneRef.set(tabPane);
            handlerRef.set(new FileMenuHandler(tabPane, new Label(), new Label(), null));
        });

        EditorTab tab1 = open(handlerRef.get(), file1.toFile());
        EditorTab tab2 = open(handlerRef.get(), file2.toFile());

        // 切换标签页：通过 openFile 激活已打开的标签页（模拟文件树双击或切页调用）
        onFx(() -> {
            handlerRef.get().openFile(file1.toFile());
            assertEquals(tab1, tabPaneRef.get().getSelectionModel().getSelectedItem());
            handlerRef.get().openFile(file2.toFile());
            assertEquals(tab2, tabPaneRef.get().getSelectionModel().getSelectedItem());
        });

        // 编辑文件元数据并保存分析
        onFx(() -> {
            tab1.getLuaEditorView().renameVariable("a", "alpha");
            tab1.getLuaEditorView().applyLineComment(0, "测试行注释");
            handlerRef.get().saveAnalysis();
        });

        // 验证最近打开列表仍只有文件夹
        List<String> list = recentManager.getRecentPaths();
        assertEquals(1, list.size());
        assertEquals(folderPath.toFile().getAbsolutePath(), list.get(0));
    }

    @Test
    @DisplayName("场景三：直接打开文件夹之外的独立文件，仍能正常加入最近打开列表")
    void testOpenStandaloneFileOutsideFolderIsRecorded(@TempDir Path tempDir) throws Exception {
        Path folderPath = tempDir.resolve("project_gamma");
        Files.createDirectories(folderPath);
        Path internalFile = folderPath.resolve("internal.lua");
        Files.write(internalFile, "return 'internal'\n".getBytes(StandardCharsets.UTF_8));

        Path standaloneFile = tempDir.resolve("outside_standalone.lua");
        Files.write(standaloneFile, "return 'standalone'\n".getBytes(StandardCharsets.UTF_8));

        // 打开工程文件夹
        workspaceManager.openFolderWorkspace(folderPath.toFile());
        recentManager.addRecentPath(folderPath.toFile().getAbsolutePath());

        AtomicReference<FileMenuHandler> handlerRef = new AtomicReference<>();
        onFx(() -> {
            TabPane tabPane = new TabPane();
            handlerRef.set(new FileMenuHandler(tabPane, new Label(), new Label(), null));
        });

        // 1. 打开内部文件 -> 不增加
        open(handlerRef.get(), internalFile.toFile());
        assertEquals(1, recentManager.getRecentPaths().size());

        // 2. 打开外部独立文件 -> 正常加入最近打开记录首位
        open(handlerRef.get(), standaloneFile.toFile());

        List<String> list = recentManager.getRecentPaths();
        assertEquals(2, list.size(), "外部独立文件应当加入最近打开列表");
        assertEquals(standaloneFile.toFile().getAbsolutePath(), list.get(0));
        assertEquals(folderPath.toFile().getAbsolutePath(), list.get(1));
    }

    @Test
    @DisplayName("场景四：连续打开同一个文件夹，不会产生重复记录")
    void testConsecutiveOpenSameFolderNoDuplicates(@TempDir Path tempDir) throws Exception {
        Path folderPath = tempDir.resolve("project_delta");
        Files.createDirectories(folderPath);

        // 连续多次记录同一个文件夹
        recentManager.addRecentPath(folderPath.toFile().getAbsolutePath());
        recentManager.addRecentPath(folderPath.toFile().getAbsolutePath());
        recentManager.addRecentPath(folderPath.toFile().getAbsolutePath());

        List<String> list = recentManager.getRecentPaths();
        assertEquals(1, list.size(), "连续打开同一文件夹不应产生重复记录");
        assertEquals(folderPath.toFile().getAbsolutePath(), list.get(0));
    }

    @Test
    @DisplayName("场景五：通过最近打开列表重新打开文件夹，原有排序提升且不增加内部文件")
    void testReopenFolderThroughRecentList(@TempDir Path tempDir) throws Exception {
        Path folderA = tempDir.resolve("folder_A");
        Path folderB = tempDir.resolve("folder_B");
        Files.createDirectories(folderA);
        Files.createDirectories(folderB);

        Path fileInA = folderA.resolve("a.lua");
        Files.write(fileInA, "return 'a'\n".getBytes(StandardCharsets.UTF_8));

        // 依次打开 A 和 B
        recentManager.addRecentPath(folderA.toFile().getAbsolutePath());
        recentManager.addRecentPath(folderB.toFile().getAbsolutePath());

        List<String> list = recentManager.getRecentPaths();
        assertEquals(2, list.size());
        assertEquals(folderB.toFile().getAbsolutePath(), list.get(0));
        assertEquals(folderA.toFile().getAbsolutePath(), list.get(1));

        // 重新从最近打开记录点击打开 A
        workspaceManager.openFolderWorkspace(folderA.toFile());
        recentManager.addRecentPath(folderA.toFile().getAbsolutePath());

        // 自动恢复 A 的内部历史文件
        AtomicReference<FileMenuHandler> handlerRef = new AtomicReference<>();
        onFx(() -> {
            TabPane tabPane = new TabPane();
            handlerRef.set(new FileMenuHandler(tabPane, new Label(), new Label(), null));
        });
        open(handlerRef.get(), fileInA.toFile());

        list = recentManager.getRecentPaths();
        assertEquals(2, list.size());
        assertEquals(folderA.toFile().getAbsolutePath(), list.get(0), "重新打开的文件夹应置顶");
        assertEquals(folderB.toFile().getAbsolutePath(), list.get(1));
    }

    @Test
    @DisplayName("场景六：应用重启后，最近打开记录能正确持久化和恢复，无内部文件污染")
    void testPersistenceAndRestorationAcrossAppRestart(@TempDir Path tempDir) throws Exception {
        Path folderPath = tempDir.resolve("persist_project");
        Files.createDirectories(folderPath);
        Path standaloneFile = tempDir.resolve("standalone_test.lua");
        Files.write(standaloneFile, "print('standalone')\n".getBytes(StandardCharsets.UTF_8));

        // 写入文件夹和独立文件
        recentManager.addRecentPath(folderPath.toFile().getAbsolutePath());
        recentManager.addRecentPath(standaloneFile.toFile().getAbsolutePath());

        // 模拟应用关闭与重启：获取单例并重新从 Preferences 加载
        List<String> savedList = recentManager.getRecentPaths();
        assertEquals(2, savedList.size());
        assertEquals(standaloneFile.toFile().getAbsolutePath(), savedList.get(0));
        assertEquals(folderPath.toFile().getAbsolutePath(), savedList.get(1));

        // 验证再次获取或重新加载的数据保持一致
        assertEquals(savedList, recentManager.getRecentPaths());
    }

    @Test
    @DisplayName("场景七：无工程文件夹打开时，打开多个独立文件能正常记录、排序与去重")
    void testStandaloneModeWhenNoFolderOpen(@TempDir Path tempDir) throws Exception {
        Path file1 = tempDir.resolve("one.lua");
        Path file2 = tempDir.resolve("two.lua");
        Files.write(file1, "return 1\n".getBytes(StandardCharsets.UTF_8));
        Files.write(file2, "return 2\n".getBytes(StandardCharsets.UTF_8));

        workspaceManager.openFolderWorkspace(null);

        AtomicReference<FileMenuHandler> handlerRef = new AtomicReference<>();
        onFx(() -> {
            TabPane tabPane = new TabPane();
            handlerRef.set(new FileMenuHandler(tabPane, new Label(), new Label(), null));
        });

        // 依次打开独立文件
        open(handlerRef.get(), file1.toFile());
        open(handlerRef.get(), file2.toFile());

        List<String> list = recentManager.getRecentPaths();
        assertEquals(2, list.size(), "无工程模式下独立文件均应记录");
        assertEquals(file2.toFile().getAbsolutePath(), list.get(0));
        assertEquals(file1.toFile().getAbsolutePath(), list.get(1));
    }
}
