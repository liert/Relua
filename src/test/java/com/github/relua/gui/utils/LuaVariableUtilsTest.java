package com.github.relua.gui.utils;

import com.github.relua.gui.utils.LuaVariableUtils.SymbolMatch;
import com.github.relua.gui.utils.LuaVariableUtils.SymbolRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lua 变量与符号词法分析工具类单元测试
 */
public class LuaVariableUtilsTest {

    @Test
    @DisplayName("验证合法与非法 Lua 标识符判断")
    public void testIsValidIdentifier() {
        assertTrue(LuaVariableUtils.isValidIdentifier("x"));
        assertTrue(LuaVariableUtils.isValidIdentifier("_var"));
        assertTrue(LuaVariableUtils.isValidIdentifier("myVar123"));
        assertTrue(LuaVariableUtils.isValidIdentifier("GLOBAL_CONFIG_99"));

        assertFalse(LuaVariableUtils.isValidIdentifier(null));
        assertFalse(LuaVariableUtils.isValidIdentifier(""));
        assertFalse(LuaVariableUtils.isValidIdentifier("   "));
        assertFalse(LuaVariableUtils.isValidIdentifier("123abc"));
        assertFalse(LuaVariableUtils.isValidIdentifier("foo-bar"));
        assertFalse(LuaVariableUtils.isValidIdentifier("foo.bar"));
        assertFalse(LuaVariableUtils.isValidIdentifier("foo bar"));
    }

    @Test
    @DisplayName("验证 Lua 保留关键字检测")
    public void testIsKeyword() {
        assertTrue(LuaVariableUtils.isKeyword("local"));
        assertTrue(LuaVariableUtils.isKeyword("function"));
        assertTrue(LuaVariableUtils.isKeyword("end"));
        assertTrue(LuaVariableUtils.isKeyword("if"));
        assertTrue(LuaVariableUtils.isKeyword("then"));
        assertTrue(LuaVariableUtils.isKeyword("nil"));
        assertTrue(LuaVariableUtils.isKeyword("true"));
        assertTrue(LuaVariableUtils.isKeyword("false"));
        assertTrue(LuaVariableUtils.isKeyword("LOCAL")); // 大小写均判定为关键字

        assertFalse(LuaVariableUtils.isKeyword("locals"));
        assertFalse(LuaVariableUtils.isKeyword("myFunction"));
        assertFalse(LuaVariableUtils.isKeyword("_end"));
        assertFalse(LuaVariableUtils.isKeyword("x"));
        assertFalse(LuaVariableUtils.isKeyword(null));
    }

    @Test
    @DisplayName("验证变量重命名输入校验规则")
    public void testValidateVariableName() {
        // 空值校验
        assertNotNull(LuaVariableUtils.validateVariableName("foo", null));
        assertNotNull(LuaVariableUtils.validateVariableName("foo", ""));
        assertNotNull(LuaVariableUtils.validateVariableName("foo", "   "));

        // 空格校验
        assertNotNull(LuaVariableUtils.validateVariableName("foo", "new Name"));
        assertNotNull(LuaVariableUtils.validateVariableName("foo", "new\tName"));

        // 非法字符/数字开头校验
        assertNotNull(LuaVariableUtils.validateVariableName("foo", "123var"));
        assertNotNull(LuaVariableUtils.validateVariableName("foo", "new-var"));
        assertNotNull(LuaVariableUtils.validateVariableName("foo", "new.var"));

        // 保留关键字校验
        assertNotNull(LuaVariableUtils.validateVariableName("foo", "local"));
        assertNotNull(LuaVariableUtils.validateVariableName("foo", "while"));

        // 与原名相同校验
        assertNotNull(LuaVariableUtils.validateVariableName("foo", "foo"));

        // 合法输入
        assertNull(LuaVariableUtils.validateVariableName("foo", "bar"));
        assertNull(LuaVariableUtils.validateVariableName("foo", "_tempVar_1"));
    }

    @Test
    @DisplayName("验证光标位置精准提取变量符号")
    public void testGetSymbolAt() {
        String code = "local myVar = 100\nprint(myVar)";

        // 光标在 "myVar" 的起始处 (index 6)
        SymbolRange rangeStart = LuaVariableUtils.getSymbolAt(code, 6);
        assertNotNull(rangeStart);
        assertEquals("myVar", rangeStart.symbol);
        assertEquals(6, rangeStart.start);
        assertEquals(11, rangeStart.end);

        // 光标在 "myVar" 中间 (index 8)
        SymbolRange rangeMid = LuaVariableUtils.getSymbolAt(code, 8);
        assertNotNull(rangeMid);
        assertEquals("myVar", rangeMid.symbol);

        // 光标在 "myVar" 末尾 (index 11)
        SymbolRange rangeEnd = LuaVariableUtils.getSymbolAt(code, 11);
        assertNotNull(rangeEnd);
        assertEquals("myVar", rangeEnd.symbol);

        // 光标在关键字 "local" 上 -> 应当返回 null，不触发重命名
        SymbolRange rangeKw = LuaVariableUtils.getSymbolAt(code, 2);
        assertNull(rangeKw);

        // 光标在空白或等号上 -> 返回 null
        assertNull(LuaVariableUtils.getSymbolAt(code, 5));
        assertNull(LuaVariableUtils.getSymbolAt(code, 12));
    }

    @Test
    @DisplayName("验证注释与字符串字面量内的变量名被正确排除")
    public void testCommentsAndStringsExcluded() {
        String code = "local target = 1\n" +
                "-- this is target in comment\n" +
                "local msg = \"hello target world\"\n" +
                "local str2 = 'single target quote'\n" +
                "local str3 = [[long target literal]]\n" +
                "print(target)";

        // 光标落在注释中的 target 上 -> 应当识别为在忽略区间内，返回 null
        int commentPos = code.indexOf("-- this is target") + 12;
        assertNull(LuaVariableUtils.getSymbolAt(code, commentPos));

        // 光标落在双引号字符串中的 target 上 -> 返回 null
        int strPos = code.indexOf("\"hello target") + 8;
        assertNull(LuaVariableUtils.getSymbolAt(code, strPos));

        // 全文查找 target 的出现位置
        List<SymbolMatch> matches = LuaVariableUtils.findVariableOccurrences(code, "target", 6);
        // 应当只匹配到第 1 行的 "local target = 1" 和第 6 行的 "print(target)"，其余注释和字符串中均排除
        assertEquals(2, matches.size());

        // 第一个匹配项包含光标 (index 6)，isCurrent 应为 true
        assertTrue(matches.get(0).isCurrent);
        // 第二个匹配项不包含光标，isCurrent 应为 false
        assertFalse(matches.get(1).isCurrent);
    }

    @Test
    @DisplayName("验证倒序批量替换不破坏字符偏移并完美更新变量")
    public void testReverseBatchRename() {
        String code = "local val = 1\nif val > 0 then\n    val = val + 1\nend\nreturn val";
        List<SymbolMatch> matches = LuaVariableUtils.findVariableOccurrences(code, "val", -1);
        assertEquals(5, matches.size());

        String newName = "counter";
        StringBuilder sb = new StringBuilder(code);
        // 倒序替换
        for (int i = matches.size() - 1; i >= 0; i--) {
            SymbolMatch m = matches.get(i);
            sb.replace(m.start, m.end, newName);
        }

        String expected = "local counter = 1\nif counter > 0 then\n    counter = counter + 1\nend\nreturn counter";
        assertEquals(expected, sb.toString());
    }
}
