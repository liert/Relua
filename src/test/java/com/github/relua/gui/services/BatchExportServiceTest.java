package com.github.relua.gui.services;

import com.github.relua.gui.workspace.FileMetadata;
import com.github.relua.gui.workspace.ProjectWorkspaceManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 批量反编译与工程导出服务单元测试
 */
public class BatchExportServiceTest {

    @Test
    @DisplayName("验证候选文件扫描与忽略规则")
    public void testScanCandidateFiles(@TempDir Path tempDir) throws IOException {
        File root = tempDir.toFile();

        // 创建多层级子目录与测试文件
        File sub1 = new File(root, "scripts");
        File sub2 = new File(root, "scripts/net");
        File gitDir = new File(root, ".git");
        File reluaDir = new File(root, ".relua");
        sub2.mkdirs();
        gitDir.mkdirs();
        reluaDir.mkdirs();

        File f1 = new File(sub1, "main.lua");
        File f2 = new File(sub2, "socket.luac");
        File fIgnored1 = new File(sub1, "readme.txt");
        File fGit = new File(gitDir, "HEAD.luac"); // .git 目录下应被忽略
        File fRelua = new File(reluaDir, "project.relua"); // .relua 目录下应被忽略

        Files.write(f1.toPath(), "print('hello')".getBytes(StandardCharsets.UTF_8));
        Files.write(f2.toPath(), "print('socket')".getBytes(StandardCharsets.UTF_8));
        Files.write(fIgnored1.toPath(), "readme text".getBytes(StandardCharsets.UTF_8));
        Files.write(fGit.toPath(), "git file".getBytes(StandardCharsets.UTF_8));
        Files.write(fRelua.toPath(), "db".getBytes(StandardCharsets.UTF_8));

        BatchExportService service = new BatchExportService();
        List<File> candidates = service.scanCandidateFiles(root);

        assertEquals(2, candidates.size());
        assertTrue(candidates.contains(f1));
        assertTrue(candidates.contains(f2));
        assertFalse(candidates.contains(fIgnored1));
        assertFalse(candidates.contains(fGit));
        assertFalse(candidates.contains(fRelua));
    }

    @Test
    @DisplayName("验证批量导出镜像目录结构与持久化修改合并")
    public void testBatchExportWithModifications(@TempDir Path tempDir) throws IOException {
        File sourceRoot = tempDir.resolve("source").toFile();
        File targetRoot = tempDir.resolve("exported_project").toFile();
        sourceRoot.mkdirs();
        targetRoot.mkdirs();

        File scriptDir = new File(sourceRoot, "game/logic");
        scriptDir.mkdirs();
        File luaFile = new File(scriptDir, "player.lua");
        Files.write(luaFile.toPath(), "local oldName = 100\nprint(oldName)".getBytes(StandardCharsets.UTF_8));

        // 注册用户持久化修改（变量重命名与行注释）
        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        manager.openFolderWorkspace(sourceRoot);
        Map<String, String> renamedVars = new HashMap<>();
        renamedVars.put("oldName", "heroLevel");
        Map<Integer, String> lineComments = new HashMap<>();
        lineComments.put(1, "玩家等级初始定义");
        FileMetadata meta = new FileMetadata("game/logic/player.lua", "sha_123", renamedVars, null, lineComments);
        manager.recordFileModification(luaFile, renamedVars, null, lineComments);

        BatchExportService service = new BatchExportService();
        List<File> files = service.scanCandidateFiles(sourceRoot);
        assertEquals(1, files.size());

        AtomicBoolean cancelled = new AtomicBoolean(false);
        BatchExportService.ExportResult result = service.exportBatch(sourceRoot, targetRoot, files, cancelled, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalFiles());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailedCount());
        assertFalse(result.isCancelled());

        // 验证目标目录镜像生成
        File exportedFile = new File(targetRoot, "game/logic/player.lua");
        assertTrue(exportedFile.exists());

        String exportedContent = new String(Files.readAllBytes(exportedFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(exportedContent.contains("local heroLevel = 100"), "重命名变量应被正确导出");
        assertTrue(exportedContent.contains("-- [Note] 玩家等级初始定义"), "行注释应被正确导出");
    }
}
