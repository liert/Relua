package com.github.relua.gui.services;

import com.github.relua.gui.services.GlobalSearchService.SearchOptions;
import com.github.relua.gui.services.GlobalSearchService.SearchResultItem;
import com.github.relua.gui.services.GlobalSearchService.SearchResultSummary;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalSearchServiceTest {

    @BeforeAll
    public static void initJFX() {
        try {
            Method startupMethod = Platform.class.getMethod("startup", Runnable.class);
            startupMethod.invoke(null, (Runnable) () -> {});
        } catch (Throwable ignored) {
        }
        try {
            Platform.setImplicitExit(false);
        } catch (Throwable ignored) {
        }
    }

    @Test
    @DisplayName("测试构建搜索正则 (区分大小写、全词、正则)")
    public void testBuildSearchPattern() {
        // 普通不区分大小写
        Pattern p1 = GlobalSearchService.buildSearchPattern("print", false, false, false);
        assertNotNull(p1);
        assertTrue(p1.matcher("PRINT('hello')").find());

        // 区分大小写
        Pattern p2 = GlobalSearchService.buildSearchPattern("print", true, false, false);
        assertNotNull(p2);
        assertFalse(p2.matcher("PRINT('hello')").find());
        assertTrue(p2.matcher("print('hello')").find());

        // 全词匹配
        Pattern p3 = GlobalSearchService.buildSearchPattern("cat", false, true, false);
        assertNotNull(p3);
        assertFalse(p3.matcher("category").find());
        assertTrue(p3.matcher("a cat here").find());

        // 正则表达式
        Pattern p4 = GlobalSearchService.buildSearchPattern("foo\\d+", false, false, true);
        assertNotNull(p4);
        assertTrue(p4.matcher("test foo123 bar").find());
        assertFalse(p4.matcher("test foo bar").find());

        // 非法正则应返回 null 而非崩溃
        Pattern p5 = GlobalSearchService.buildSearchPattern("[abc", false, false, true);
        assertNull(p5);
    }

    @Test
    @DisplayName("测试文件类型通配符过滤")
    public void testMatchesFileMask() {
        assertTrue(GlobalSearchService.matchesFileMask("main.lua", "*.lua"));
        assertFalse(GlobalSearchService.matchesFileMask("main.txt", "*.lua"));

        assertTrue(GlobalSearchService.matchesFileMask("main.luac", "*.lua, *.luac"));
        assertTrue(GlobalSearchService.matchesFileMask("main.lua", "*.lua, *.luac"));
        assertFalse(GlobalSearchService.matchesFileMask("main.cpp", "*.lua, *.luac"));

        assertTrue(GlobalSearchService.matchesFileMask("anything.txt", "*.*"));
        assertTrue(GlobalSearchService.matchesFileMask("anything.txt", ""));
        assertTrue(GlobalSearchService.matchesFileMask("anything.txt", null));
    }

    @Test
    @DisplayName("测试文本内按行检索匹配项及精确行列坐标")
    public void testSearchInText() {
        String text = "local a = 1\n" +
                      "print('hello world')\n" +
                      "local b = print\n";

        Pattern p = GlobalSearchService.buildSearchPattern("print", false, false, false);
        List<SearchResultItem> results = GlobalSearchService.searchInText(new File("dummy.lua"), "dummy.lua", text, p);

        assertEquals(2, results.size());

        SearchResultItem hit1 = results.get(0);
        assertEquals(2, hit1.getLineNumber());
        assertEquals(0, hit1.getStartColumn());
        assertEquals(5, hit1.getEndColumn());
        assertEquals("print('hello world')", hit1.getLineContent());
        assertEquals("print", hit1.getMatchedText());

        SearchResultItem hit2 = results.get(1);
        assertEquals(3, hit2.getLineNumber());
        assertEquals(10, hit2.getStartColumn());
        assertEquals(15, hit2.getEndColumn());
        assertEquals("print", hit2.getMatchedText());
    }

    @Test
    @DisplayName("测试异步 Task 检索工程目录与打开的内存标签页")
    public void testCreateSearchTask(@TempDir Path tempDir) throws Exception {
        // 创建磁盘测试文件
        Path file1 = tempDir.resolve("test1.lua");
        Files.write(file1, "function greet()\n    return 'Hello Relua'\nend\n".getBytes(StandardCharsets.UTF_8));

        Path file2 = tempDir.resolve("sub/test2.lua");
        Files.createDirectories(file2.getParent());
        Files.write(file2, "local x = 'Hello World'\n".getBytes(StandardCharsets.UTF_8));

        Path ignoredFile = tempDir.resolve("test.txt");
        Files.write(ignoredFile, "Hello but in txt".getBytes(StandardCharsets.UTF_8));

        // 模拟已打开但未保存到磁盘的标签页
        File openFile = tempDir.resolve("open.lua").toFile();
        Map<File, String> openTabTexts = new HashMap<>();
        openTabTexts.put(openFile, "local memory = 'Hello Live Editor'\n");

        GlobalSearchService service = new GlobalSearchService();
        SearchOptions options = new SearchOptions("Hello", true, false, false, "*.lua", false);

        List<SearchResultItem> streamedItems = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        SearchResultSummary[] resultHolder = new SearchResultSummary[1];
        Throwable[] exHolder = new Throwable[1];

        Platform.runLater(() -> {
            try {
                Task<SearchResultSummary> task = service.createSearchTask(tempDir.toFile(), openTabTexts, options, streamedItems::addAll);
                task.setOnSucceeded(e -> {
                    resultHolder[0] = task.getValue();
                    latch.countDown();
                });
                task.setOnFailed(e -> {
                    exHolder[0] = task.getException();
                    latch.countDown();
                });

                Thread t = new Thread(task);
                t.setDaemon(true);
                t.start();
            } catch (Throwable t) {
                exHolder[0] = t;
                latch.countDown();
            }
        });

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "Search task timed out");
        if (exHolder[0] != null) {
            exHolder[0].printStackTrace();
            fail("Task failed with: " + exHolder[0].getMessage(), exHolder[0]);
        }

        SearchResultSummary summary = resultHolder[0];
        assertNotNull(summary, "summary should not be null");
        // 应检索到 3 处匹配 (open.lua, test1.lua, sub/test2.lua)，忽略 test.txt
        assertEquals(3, summary.getTotalMatches());
        assertEquals(3, summary.getMatchedFilesCount());
    }
}
