package com.github.relua.gui.services;

import com.github.relua.gui.model.OutlineItem;
import com.github.relua.model.Chunk;
import com.github.relua.model.LineRange;
import com.github.relua.model.LocalVar;
import com.github.relua.model.LuacFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class OutlineServiceTest {

    private final OutlineService service = new OutlineService();

    @Test
    @DisplayName("测试从纯 Lua 源码提取函数大纲与符号")
    public void testBuildOutlineFromSource() {
        String source =
                "local function helper(a, b)\n" +      // line 1
                "    return a + b\n" +                  // line 2
                "end\n" +                               // line 3
                "\n" +                                  // line 4
                "function calculate(x)\n" +             // line 5
                "    return helper(x, 10)\n" +          // line 6
                "end\n" +                               // line 7
                "\n" +                                  // line 8
                "MyClass.doWork = function(self, msg)\n" + // line 9
                "    print(msg)\n" +                    // line 10
                "end\n";                                // line 11

        OutlineItem root = service.buildOutlineFromSource(source);
        assertNotNull(root);
        assertEquals(OutlineItem.ItemType.MAIN_CHUNK, root.getType());

        // 应解析出 3 个函数符号
        assertEquals(3, root.getChildren().size());

        OutlineItem item1 = root.getChildren().get(0);
        assertEquals("helper", item1.getName());
        assertEquals("(a, b)", item1.getSignature());
        assertEquals(OutlineItem.ItemType.LOCAL_FUNCTION, item1.getType());
        assertEquals(1, item1.getStartLine());

        OutlineItem item2 = root.getChildren().get(1);
        assertEquals("calculate", item2.getName());
        assertEquals("(x)", item2.getSignature());
        assertEquals(OutlineItem.ItemType.FUNCTION, item2.getType());
        assertEquals(5, item2.getStartLine());

        OutlineItem item3 = root.getChildren().get(2);
        assertEquals("MyClass.doWork", item3.getName());
        assertEquals("(self, msg)", item3.getSignature());
        assertEquals(OutlineItem.ItemType.METHOD, item3.getType());
        assertEquals(9, item3.getStartLine());
    }

    @Test
    @DisplayName("测试从 LuacFile 模型和 LineRange 映射构建大纲树")
    public void testBuildOutlineFromLuac() {
        LuacFile luacFile = new LuacFile();
        Chunk mainChunk = new Chunk();
        mainChunk.setFunction("main");
        mainChunk.setLineDefined(0);
        mainChunk.setLastLineDefined(100);

        Chunk sub1 = new Chunk();
        sub1.setFunction("sub_func");
        sub1.setNumParams(2);
        sub1.getLocalVars().add(new LocalVar("paramA", 0, 10));
        sub1.getLocalVars().add(new LocalVar("paramB", 0, 10));
        sub1.setLineDefined(15);
        sub1.setLastLineDefined(30);

        mainChunk.getSubChunks().add(sub1);
        luacFile.setMainChunk(mainChunk);

        Map<String, LineRange> ranges = new HashMap<>();
        ranges.put("main", new LineRange(1, 100));
        ranges.put("sub_func", new LineRange(15, 30));

        OutlineItem root = service.buildOutline(null, luacFile, ranges);
        assertNotNull(root);
        assertEquals("main", root.getName());
        assertEquals(1, root.getStartLine());
        assertEquals(100, root.getEndLine());

        assertEquals(1, root.getChildren().size());
        OutlineItem child = root.getChildren().get(0);
        assertEquals("sub_func", child.getName());
        assertEquals("(paramA, paramB)", child.getSignature());
        assertEquals(15, child.getStartLine());
        assertEquals(30, child.getEndLine());

        assertTrue(child.containsLine(20));
        assertFalse(child.containsLine(50));
    }
}
