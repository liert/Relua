package com.github.relua.gui.workspace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 二进制紧凑持久化引擎与工作区管理器单元测试
 */
public class BinaryPersistenceServiceTest {

    @Test
    @DisplayName("验证工作区二进制压缩保存与反序列化完整性")
    public void testSaveAndLoadWorkspace(@TempDir Path tempDir) throws IOException {
        File dbFile = tempDir.resolve("test_project.relua").toFile();

        WorkspaceData original = new WorkspaceData();
        original.setActiveFilePath("scripts/login.luac");
        original.addOpenedTabPath("scripts/login.luac");
        original.addOpenedTabPath("common/network.luac");

        // 构造文件 1 的修改
        Map<String, String> vars1 = new HashMap<>();
        vars1.put("a0", "playerId");
        vars1.put("R1_2", "authToken");
        FileMetadata meta1 = new FileMetadata("scripts/login.luac", "fake_sha256_hash_1", vars1, null);
        original.putFileMetadata("scripts/login.luac", meta1);

        // 构造文件 2 的修改（含编辑文本）
        Map<String, String> vars2 = new HashMap<>();
        vars2.put("R0_0", "socketConn");
        FileMetadata meta2 = new FileMetadata("common/network.luac", "fake_sha256_hash_2", vars2, "-- custom edit code");
        original.putFileMetadata("common/network.luac", meta2);

        // 执行二进制压缩保存
        BinaryPersistenceService.save(dbFile, original);

        assertTrue(dbFile.exists());
        assertTrue(dbFile.length() > 0);
        // 验证文件体积极其紧凑（GZIP压缩后通常小于 300 字节）
        assertTrue(dbFile.length() < 1024, "紧凑元数据大小应小于 1KB，实际为: " + dbFile.length() + " 字节");

        // 执行反序列化加载
        WorkspaceData loaded = BinaryPersistenceService.load(dbFile);
        assertNotNull(loaded);
        assertEquals(BinaryPersistenceService.FORMAT_VERSION, loaded.getVersion());
        assertEquals("scripts/login.luac", loaded.getActiveFilePath());
        assertEquals(2, loaded.getOpenedTabPaths().size());
        assertTrue(loaded.getOpenedTabPaths().contains("scripts/login.luac"));
        assertTrue(loaded.getOpenedTabPaths().contains("common/network.luac"));

        // 验证修改文件列表
        assertEquals(2, loaded.getModifiedFiles().size());

        FileMetadata loadedMeta1 = loaded.getFileMetadata("scripts/login.luac");
        assertNotNull(loadedMeta1);
        assertEquals("fake_sha256_hash_1", loadedMeta1.getFileSha256());
        assertEquals(2, loadedMeta1.getRenamedVariables().size());
        assertEquals("playerId", loadedMeta1.getRenamedVariables().get("a0"));
        assertEquals("authToken", loadedMeta1.getRenamedVariables().get("R1_2"));
        assertNull(loadedMeta1.getEditedText());

        FileMetadata loadedMeta2 = loaded.getFileMetadata("common/network.luac");
        assertNotNull(loadedMeta2);
        assertEquals("fake_sha256_hash_2", loadedMeta2.getFileSha256());
        assertEquals("socketConn", loadedMeta2.getRenamedVariables().get("R0_0"));
        assertEquals("-- custom edit code", loadedMeta2.getEditedText());
    }

    @Test
    @DisplayName("验证稀疏存储（未修改的文件不占用任何二进制空间）")
    public void testSparseStorage(@TempDir Path tempDir) throws IOException {
        File dbFile = tempDir.resolve("sparse.relua").toFile();

        WorkspaceData original = new WorkspaceData();
        original.setActiveFilePath("unmodified.luac");

        // 放入一个没有任何修改的元数据
        FileMetadata emptyMeta = new FileMetadata("unmodified.luac", null, new HashMap<>(), null);
        assertFalse(emptyMeta.hasModifications());
        original.putFileMetadata("unmodified.luac", emptyMeta);

        BinaryPersistenceService.save(dbFile, original);

        WorkspaceData loaded = BinaryPersistenceService.load(dbFile);
        assertNotNull(loaded);
        // 未修改的文件应被自动过滤，不存入磁盘
        assertTrue(loaded.getModifiedFiles().isEmpty());
    }

    @Test
    @DisplayName("验证超长代码文本（突破 64KB writeUTF 限制）安全保存与读取")
    public void testLargeCodeExceeding64KB(@TempDir Path tempDir) throws IOException {
        File dbFile = tempDir.resolve("large_code.relua").toFile();

        // 构造一个超过 100KB 的超长文本
        StringBuilder largeSb = new StringBuilder();
        String line = "local x = 1234567890; -- this is a long line testing buffer overflow\n";
        while (largeSb.length() < 120 * 1024) { // 120KB
            largeSb.append(line);
        }
        String largeCode = largeSb.toString();
        assertTrue(largeCode.getBytes().length > 65535, "测试文本必须大于 64KB");

        WorkspaceData original = new WorkspaceData();
        Map<String, String> vars = new HashMap<>();
        vars.put("x", "counter");
        FileMetadata meta = new FileMetadata("large_script.luac", "large_sha", vars, largeCode);
        original.putFileMetadata("large_script.luac", meta);

        // 写入与加载，绝不能抛出 UTFDataFormatException
        assertDoesNotThrow(() -> BinaryPersistenceService.save(dbFile, original));

        WorkspaceData loaded = BinaryPersistenceService.load(dbFile);
        assertNotNull(loaded);
        FileMetadata loadedMeta = loaded.getFileMetadata("large_script.luac");
        assertNotNull(loadedMeta);
        assertEquals(largeCode, loadedMeta.getEditedText());
    }

    @Test
    @DisplayName("验证非法文件与魔数防呆校验")
    public void testInvalidMagicNumber(@TempDir Path tempDir) throws IOException {
        File corruptedFile = tempDir.resolve("corrupted.relua").toFile();

        // 写入伪造的非法魔数
        try (FileOutputStream fos = new FileOutputStream(corruptedFile);
             java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(fos);
             java.io.DataOutputStream out = new java.io.DataOutputStream(gzip)) {
            out.writeInt(0x12345678); // 错误的魔数
            out.writeShort(1);
        }

        IOException ex = assertThrows(IOException.class, () -> BinaryPersistenceService.load(corruptedFile));
        assertTrue(ex.getMessage().contains("魔数不匹配"));
    }

    @Test
    @DisplayName("验证伴生文件规则与相对路径计算")
    public void testCompanionFileAndRelativePath() {
        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();

        File sampleFile = new File("D:/games/scripts/login.luac");
        File companion = manager.getCompanionFile(sampleFile);
        assertEquals("login.luac.relua", companion.getName());
        assertEquals(sampleFile.getParentFile(), companion.getParentFile());

        File root = new File("D:/games");
        manager.openFolderWorkspace(root);
        assertEquals("scripts/login.luac", manager.toRelativePath(sampleFile));
    }
}
