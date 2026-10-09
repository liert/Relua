package com.github.relua.gui.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lua 变量与符号词法分析工具类，提供标识符校验、光标取词、作用域符号检索与字符串/注释排除
 */
public final class LuaVariableUtils {

    /**
     * Lua 保留关键字集合
     */
    public static final Set<String> LUA_KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "and", "break", "do", "else", "elseif", "end", "false", "for", "function",
            "goto", "if", "in", "local", "nil", "not", "or", "repeat", "return", "then",
            "true", "until", "while"
    )));

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    // 匹配长注释、单行注释、双引号字符串、单引号字符串、长括号字符串
    private static final Pattern IGNORED_LITERALS_PATTERN = Pattern.compile(
            "(?s:--\\[\\[.*?\\]\\])|(--[^\\r\\n]*)|(\"([^\"\\\\\\r\\n]|\\\\.)*\")|('([^'\\\\\\r\\n]|\\\\.)*')|(?s:\\[\\[.*?\\]\\])"
    );

    private LuaVariableUtils() {
    }

    /**
     * 符号范围记录类，记录文本中某个符号的具体区间
     */
    public static class SymbolRange {
        public final String symbol;
        public final int start;
        public final int end;

        public SymbolRange(String symbol, int start, int end) {
            this.symbol = symbol;
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return symbol + " [" + start + ", " + end + "]";
        }
    }

    /**
     * 符号出现匹配项，记录是否属于当前光标所在的符号
     */
    public static class SymbolMatch {
        public final int start;
        public final int end;
        public final boolean isCurrent;

        public SymbolMatch(int start, int end, boolean isCurrent) {
            this.start = start;
            this.end = end;
            this.isCurrent = isCurrent;
        }

        @Override
        public String toString() {
            return "[" + start + ", " + end + "]" + (isCurrent ? " (current)" : "");
        }
    }

    /**
     * 判断字符是否属于合法的标识符组成字符（字母、数字、下划线）
     * @param c 字符
     * @return 是否为标识符字符
     */
    public static boolean isIdentifierChar(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    /**
     * 判断字符串是否为合法的 Lua 标识符格式（以字母或下划线开头，由字母、数字和下划线组成）
     * @param name 待检查名称
     * @return 是否为合法标识符
     */
    public static boolean isValidIdentifier(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return IDENTIFIER_PATTERN.matcher(name).matches();
    }

    /**
     * 判断名称是否属于 Lua 关键字
     * @param name 待检查名称
     * @return 是否为保留关键字
     */
    public static boolean isKeyword(String name) {
        if (name == null) {
            return false;
        }
        return LUA_KEYWORDS.contains(name.toLowerCase());
    }

    /**
     * 校验新变量名称的合法性，若校验不通过则返回具体的错误原因，通过则返回 null
     * @param oldName 原变量名
     * @param newName 新变量名
     * @return 错误提示信息，通过返回 null
     */
    public static String validateVariableName(String oldName, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            return "变量名称不能为空";
        }
        String trimmed = newName.trim();
        if (trimmed.contains(" ") || trimmed.contains("\t")) {
            return "变量名称不能包含空格或制表符";
        }
        if (!isValidIdentifier(trimmed)) {
            return "变量名称必须以字母或下划线开头，且只能包含字母、数字和下划线";
        }
        if (isKeyword(trimmed)) {
            return "变量名称不能是 Lua 保留关键字 (" + trimmed + ")";
        }
        if (oldName != null && oldName.equals(trimmed)) {
            return "新变量名不能与当前变量名相同";
        }
        return null;
    }

    /**
     * 根据光标偏移量提取光标所在位置的有效变量符号
     * @param text 完整代码文本
     * @param caretOffset 当前光标字符偏移
     * @return 识别出的符号范围，若光标不在有效标识符上或属于关键字则返回 null
     */
    public static SymbolRange getSymbolAt(String text, int caretOffset) {
        if (text == null || text.isEmpty() || caretOffset < 0 || caretOffset > text.length()) {
            return null;
        }

        int len = text.length();
        int pos = caretOffset;

        // 如果光标在末尾或者在非标识符字符上，检查光标左侧的一个字符
        if (pos == len || (!isIdentifierChar(text.charAt(pos)) && pos > 0 && isIdentifierChar(text.charAt(pos - 1)))) {
            pos--;
        }

        if (pos < 0 || pos >= len || !isIdentifierChar(text.charAt(pos))) {
            return null;
        }

        // 向左扫描标识符起始点
        int start = pos;
        while (start > 0 && isIdentifierChar(text.charAt(start - 1))) {
            start--;
        }

        // 向右扫描标识符终点
        int end = pos;
        while (end < len && isIdentifierChar(text.charAt(end))) {
            end++;
        }

        // 标识符首字符不能为数字
        if (start < end && Character.isDigit(text.charAt(start))) {
            return null;
        }

        String word = text.substring(start, end);
        if (isKeyword(word)) {
            return null;
        }

        // 检查光标是否落在注释或字符串内部
        if (isInIgnoredLiteral(text, start, end)) {
            return null;
        }

        return new SymbolRange(word, start, end);
    }

    /**
     * 判断指定区间是否落在注释或字符串字面量内部
     */
    public static boolean isInIgnoredLiteral(String text, int start, int end) {
        List<int[]> ignoredRanges = getIgnoredLiteralRanges(text);
        for (int[] range : ignoredRanges) {
            // 如果区间有重叠或被包含在注释/字符串内部
            if (start >= range[0] && end <= range[1]) {
                return true;
            }
        }
        return false;
    }

    /**
     * 收集文本中所有属于注释或字符串字面量的区间
     * @param text 文本
     * @return 区间列表，每个元素为 [startOffset, endOffset]
     */
    public static List<int[]> getIgnoredLiteralRanges(String text) {
        List<int[]> ranges = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return ranges;
        }
        Matcher matcher = IGNORED_LITERALS_PATTERN.matcher(text);
        while (matcher.find()) {
            ranges.add(new int[]{matcher.start(), matcher.end()});
        }
        return ranges;
    }

    /**
     * 检索指定变量名称在全文中的所有有效出现位置，自动排除字符串与注释内部的同名文本
     * @param text 完整代码文本
     * @param variableName 变量名
     * @param currentCaret 当前光标偏移（用于标记 isCurrent）
     * @return 出现位置列表
     */
    public static List<SymbolMatch> findVariableOccurrences(String text, String variableName, int currentCaret) {
        List<SymbolMatch> matches = new ArrayList<>();
        if (text == null || variableName == null || variableName.isEmpty()) {
            return matches;
        }

        List<int[]> ignoredRanges = getIgnoredLiteralRanges(text);
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(variableName) + "\\b");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            // 检查是否落在被忽略的注释或字符串内
            boolean ignored = false;
            for (int[] range : ignoredRanges) {
                if (start >= range[0] && end <= range[1]) {
                    ignored = true;
                    break;
                }
            }

            if (!ignored) {
                boolean isCurrent = (currentCaret >= start && currentCaret <= end);
                matches.add(new SymbolMatch(start, end, isCurrent));
            }
        }

        return matches;
    }
}
