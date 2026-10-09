package com.github.relua.gui.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 源码与字节码双向联动映射工具类，提供行号互查、指令PC定位与段落解析
 */
public final class BytecodeSyncUtils {

    // 匹配字节码反汇编指令行开头的 PC 索引与源行号，例如 "   0 [line   12]: GETGLOBAL"
    private static final Pattern INSTRUCTION_LINE_PATTERN = Pattern.compile(
            "^\\s*(\\d+)\\s*\\[line\\s*(\\d+)\\]:"
    );

    // 匹配无源行号的指令行，例如 "   0 [line ----]: GETGLOBAL" 或旧格式
    private static final Pattern INSTRUCTION_NO_LINE_PATTERN = Pattern.compile(
            "^\\s*(\\d+)\\s*\\[line\\s*----\\]:"
    );

    private BytecodeSyncUtils() {
    }

    /**
     * 从字节码单行文本中解析出对应的 Lua 源码行号
     * @param bytecodeLine 字节码反汇编单行文本
     * @return 源码行号（1-based），若未找到或该行非有效指令行则返回 -1
     */
    public static int extractSourceLineFromBytecode(String bytecodeLine) {
        if (bytecodeLine == null || bytecodeLine.isEmpty()) {
            return -1;
        }
        Matcher matcher = INSTRUCTION_LINE_PATTERN.matcher(bytecodeLine);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    /**
     * 从字节码单行文本中解析出对应的指令 PC 索引
     * @param bytecodeLine 字节码单行文本
     * @return 指令 PC 索引（0-based），若非指令行则返回 -1
     */
    public static int extractPcFromBytecode(String bytecodeLine) {
        if (bytecodeLine == null || bytecodeLine.isEmpty()) {
            return -1;
        }
        Matcher matcher = INSTRUCTION_LINE_PATTERN.matcher(bytecodeLine);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        Matcher noLineMatcher = INSTRUCTION_NO_LINE_PATTERN.matcher(bytecodeLine);
        if (noLineMatcher.find()) {
            try {
                return Integer.parseInt(noLineMatcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    /**
     * 在完整的字节码反汇编文本中，根据 Lua 源码行号检索其对应的首个字节码段落（行号）
     * 若未找到完全相等的源码行，则就近匹配小于等于该源码行的最近前序指令行
     * @param bytecodeText 字节码全文
     * @param sourceLine 目标 Lua 源码行号（1-based）
     * @return 匹配的字节码段落索引（0-based paragraph index），未找到返回 -1
     */
    public static int findBytecodeParagraphForSourceLine(String bytecodeText, int sourceLine) {
        if (bytecodeText == null || bytecodeText.isEmpty() || sourceLine <= 0) {
            return -1;
        }

        String[] lines = bytecodeText.split("\n", -1);
        int exactMatch = -1;
        int closestBefore = -1;
        int closestDiff = Integer.MAX_VALUE;

        for (int i = 0; i < lines.length; i++) {
            int line = extractSourceLineFromBytecode(lines[i]);
            if (line > 0) {
                if (line == sourceLine) {
                    exactMatch = i;
                    break; // 优先返回首条匹配指令
                } else if (line < sourceLine) {
                    int diff = sourceLine - line;
                    if (diff < closestDiff) {
                        closestDiff = diff;
                        closestBefore = i;
                    }
                }
            }
        }

        if (exactMatch >= 0) {
            return exactMatch;
        }
        return closestBefore;
    }

    /**
     * 查找指定源码行在字节码文本中对应的所有指令段落索引
     * @param bytecodeText 字节码全文
     * @param sourceLine 目标 Lua 源码行号（1-based）
     * @return 匹配的指令段落索引列表（0-based）
     */
    public static List<Integer> findAllBytecodeParagraphsForSourceLine(String bytecodeText, int sourceLine) {
        if (bytecodeText == null || bytecodeText.isEmpty() || sourceLine <= 0) {
            return Collections.emptyList();
        }

        List<Integer> result = new ArrayList<>();
        String[] lines = bytecodeText.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            int line = extractSourceLineFromBytecode(lines[i]);
            if (line == sourceLine) {
                result.add(i);
            }
        }
        return result;
    }
}
