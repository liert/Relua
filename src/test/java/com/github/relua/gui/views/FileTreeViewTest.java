package com.github.relua.gui.views;

import com.github.relua.gui.model.FileNode;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件树视图单元测试，验证 Windows 文件夹排序规则：
 * 文件夹在上面，文件在下面，同类节点内部按 Windows 自然语言与数值顺序排列
 */
public class FileTreeViewTest {

    @BeforeAll
    public static void initJFX() {
        // 初始化 JavaFX 工具包环境（避免依赖 Swing/JFXPanel）
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX 工具包已初始化时会抛出该异常，直接忽略
        }
    }

    @Test
    public void testCompareFilesAndNodes() throws Exception {
        FileTreeView ftv = new FileTreeView();

        File dir = new File("src");
        File file = new File("pom.xml");

        // 验证 File 比较规则：文件夹必须在文件上面
        assertTrue(ftv.compareFiles(dir, file) < 0, "文件夹在上面，比较结果应为负数");
        assertTrue(ftv.compareFiles(file, dir) > 0, "文件在下面，比较结果应为正数");

        // 验证 FileNode 比较规则
        FileNode dirNode = new FileNode(dir);
        FileNode fileNode = new FileNode(file);
        assertTrue(ftv.compareFileNodes(dirNode, fileNode) < 0, "文件夹节点在上面，比较结果应为负数");
        assertTrue(ftv.compareFileNodes(fileNode, dirNode) > 0, "文件节点在下面，比较结果应为正数");

        // 验证 TreeItem 比较规则
        TreeItem<FileNode> dirItem = new TreeItem<>(dirNode);
        TreeItem<FileNode> fileItem = new TreeItem<>(fileNode);
        assertTrue(ftv.compareTreeItems(dirItem, fileItem) < 0, "文件夹树节点在上面，比较结果应为负数");
        assertTrue(ftv.compareTreeItems(fileItem, dirItem) > 0, "文件树节点在下面，比较结果应为正数");
    }

    @Test
    public void testNaturalSortOrder() throws Exception {
        FileTreeView ftv = new FileTreeView();
        Method compareNaturalMethod = FileTreeView.class.getDeclaredMethod("compareNatural", String.class, String.class);
        compareNaturalMethod.setAccessible(true);

        // 验证自然数字比较：2 应排在 10 前面
        int comp1 = (int) compareNaturalMethod.invoke(ftv, "file2.lua", "file10.lua");
        assertTrue(comp1 < 0, "file2.lua 必须排在 file10.lua 前面");

        int comp2 = (int) compareNaturalMethod.invoke(ftv, "file10.lua", "file2.lua");
        assertTrue(comp2 > 0, "file10.lua 必须排在 file2.lua 后面");

        // 验证忽略大小写
        int compCase = (int) compareNaturalMethod.invoke(ftv, "FolderA", "folderb");
        assertTrue(compCase < 0, "FolderA 应排在 folderb 前面");

        // 验证中文拼音排序
        int compCn = (int) compareNaturalMethod.invoke(ftv, "李", "张");
        assertTrue(compCn < 0, "中文拼音 L(李) 应排在 Z(张) 前面");
    }

    @Test
    public void testLoadFolderStructureSorting() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                FileTreeView ftv = new FileTreeView();
                File projectDir = new File(".");
                ftv.loadFolder(projectDir);

                TreeItem<FileNode> root = ftv.getView().getRoot();
                assertNotNull(root);
                assertEquals(1, root.getChildren().size());

                TreeItem<FileNode> folderItem = root.getChildren().get(0);
                assertNotNull(folderItem);
                assertTrue(folderItem.getValue().isDirectory());

                // 获取已加载的子节点列表
                List<TreeItem<FileNode>> children = folderItem.getChildren();
                assertFalse(children.isEmpty(), "根文件夹子项不应为空");

                // 验证排序：所有文件夹都在前面，所有文件都在后面
                boolean seenFile = false;
                for (TreeItem<FileNode> child : children) {
                    FileNode node = child.getValue();
                    assertNotNull(node);
                    if (node.isDirectory()) {
                        assertFalse(seenFile, "一旦出现文件，后面不应再出现文件夹（文件夹必须都在上面）: " + node.getName());
                    } else {
                        seenFile = true;
                    }
                }
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }
}
