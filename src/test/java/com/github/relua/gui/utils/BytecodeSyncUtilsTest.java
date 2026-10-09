package com.github.relua.gui.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 源码与字节码双向联动映射工具单元测试
 */
public class BytecodeSyncUtilsTest {

    @Test
    @DisplayName("验证从字节码反汇编行解析源码行号与PC索引")
    public void testExtractLineAndPc() {
        String line1 = "   0 [line    1]: GETGLOBAL   \tR0 K0                             ; R0 := print";
        String line2 = "  15 [line  240]: CALL        \tR0 2 1                            ; R0(R1)";
        String line3 = "   2 [line ----]: RETURN      \tR0 1                              ;";
        String line4 = "; 常量池 (共 5 个):";

        assertEquals(1, BytecodeSyncUtils.extractSourceLineFromBytecode(line1));
        assertEquals(0, BytecodeSyncUtils.extractPcFromBytecode(line1));

        assertEquals(240, BytecodeSyncUtils.extractSourceLineFromBytecode(line2));
        assertEquals(15, BytecodeSyncUtils.extractPcFromBytecode(line2));

        assertEquals(-1, BytecodeSyncUtils.extractSourceLineFromBytecode(line3));
        assertEquals(2, BytecodeSyncUtils.extractPcFromBytecode(line3));

        assertEquals(-1, BytecodeSyncUtils.extractSourceLineFromBytecode(line4));
        assertEquals(-1, BytecodeSyncUtils.extractPcFromBytecode(line4));
    }

    @Test
    @DisplayName("验证根据源码行号检索字节码段落（精确匹配与就近匹配）")
    public void testFindBytecodeParagraph() {
        String bytecode =
                "; Header info\n" +
                "; Constants\n" +
                "   0 [line    1]: GETGLOBAL   \tR0 K0\n" +
                "   1 [line    1]: LOADK       \tR1 K1\n" +
                "   2 [line    3]: CALL        \tR0 2 1\n" +
                "   3 [line    5]: RETURN      \tR0 1\n";

        // 精确匹配 line 1 -> 对应段落 2 ("   0 [line    1]")
        assertEquals(2, BytecodeSyncUtils.findBytecodeParagraphForSourceLine(bytecode, 1));

        // 精确匹配 line 3 -> 对应段落 4 ("   2 [line    3]")
        assertEquals(4, BytecodeSyncUtils.findBytecodeParagraphForSourceLine(bytecode, 3));

        // 源码行 2 无直接指令（例如空行或表达式），应就近定位至前面最近的 line 1 指令（段落 2）
        assertEquals(2, BytecodeSyncUtils.findBytecodeParagraphForSourceLine(bytecode, 2));

        // 源码行 4 应就近定位至前面最近的 line 3 指令（段落 4）
        assertEquals(4, BytecodeSyncUtils.findBytecodeParagraphForSourceLine(bytecode, 4));

        // 检索该源码行对应的全部指令
        List<Integer> line1Paragraphs = BytecodeSyncUtils.findAllBytecodeParagraphsForSourceLine(bytecode, 1);
        assertEquals(2, line1Paragraphs.size());
        assertEquals(2, line1Paragraphs.get(0));
        assertEquals(3, line1Paragraphs.get(1));
    }
}
