package com.github.relua.gui.services;

import com.github.relua.gui.model.OutlineItem;
import com.github.relua.gui.workspace.ProjectWorkspaceManager;
import com.github.relua.model.Chunk;
import com.github.relua.model.LineRange;
import com.github.relua.model.LuacFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileFunctionServiceTest {
    private final FileFunctionService service = new FileFunctionService();

    @Test
    void indexesSourceFunctionsWithoutChangingFile(@TempDir Path directory) throws Exception {
        Path input = directory.resolve("sample.lua");
        byte[] source = ("local function helper(a)\nend\n\n"
                + "function api.run(value)\nend\n").getBytes(StandardCharsets.UTF_8);
        Files.write(input, source);
        ProjectWorkspaceManager.getInstance().openFolderWorkspace(directory.toFile());
        List<OutlineItem> functions = service.load(input.toFile());
        assertEquals(2, functions.size());
        assertEquals("helper", functions.get(0).getName());
        assertEquals("(a)", functions.get(0).getSignature());
        assertEquals(1, functions.get(0).getStartLine());
        assertEquals("api.run", functions.get(1).getName());
        assertEquals(4, functions.get(1).getStartLine());
        assertArrayEquals(source, Files.readAllBytes(input));
        assertFalse(Files.exists(directory.resolve(".relua")));
    }

    @Test
    void indexesRealBytecodeFunctionsWithoutMainWrapper() throws Exception {
        File input = new File("src/test/resources/xiaomi/aeslua.lua");
        byte[] original = Files.readAllBytes(input.toPath());
        ProjectWorkspaceManager.getInstance().openFolderWorkspace(null);
        List<OutlineItem> functions = service.load(input);
        assertEquals(3, functions.size());
        assertTrue(functions.stream().anyMatch(item -> item.getName().endsWith("pwToKey")));
        assertTrue(functions.stream().anyMatch(item -> item.getName().endsWith("encrypt")));
        assertTrue(functions.stream().anyMatch(item -> item.getName().endsWith("decrypt")));
        assertTrue(functions.stream().noneMatch(item -> item.getType() == OutlineItem.ItemType.MAIN_CHUNK));
        assertArrayEquals(original, Files.readAllBytes(input.toPath()));
    }

    @Test
    void reflectsCurrentFunctionNamesAndPersistedSource(@TempDir Path directory) throws Exception {
        LuacFile luac = new LuacFile();
        Chunk main = new Chunk();
        main.setFunction("main");
        Chunk child = new Chunk();
        child.setFunction("oldName");
        child.setLineDefined(1);
        child.setLastLineDefined(3);
        main.getSubChunks().add(child);
        luac.setMainChunk(main);
        List<OutlineItem> current = service.build("function newName(arg)\nend\n", luac,
                Collections.singletonMap("oldName", new LineRange(1, 3)));
        assertEquals("newName", current.get(0).getName());
        assertEquals("(arg)", current.get(0).getSignature());

        Path input = directory.resolve("saved.lua");
        Files.write(input, "function oldName()\nend\n".getBytes(StandardCharsets.UTF_8));
        ProjectWorkspaceManager manager = ProjectWorkspaceManager.getInstance();
        manager.openFolderWorkspace(directory.toFile());
        manager.recordFileModification(input.toFile(), Collections.emptyMap(), "function restored()\nend\n");
        assertEquals("restored", service.load(input.toFile()).get(0).getName());
        assertFalse(Files.exists(directory.resolve(".relua")));
    }
}
