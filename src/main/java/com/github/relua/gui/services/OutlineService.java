package com.github.relua.gui.services;

import com.github.relua.gui.model.OutlineItem;
import com.github.relua.gui.model.OutlineItem.ItemType;
import com.github.relua.model.Chunk;
import com.github.relua.model.LineRange;
import com.github.relua.model.LocalVar;
import com.github.relua.model.LuacFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 结构大纲解析与符号树构建服务
 * 能够从 LuacFile 嵌套代码块模型、反编译 LineRange 映射表以及 Lua 源码正则表达式中智能提取完整符号树
 */
public class OutlineService {

    // 匹配常规 Lua 函数定义: function name(...) 或 local function name(...)
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
            "^\\s*(?:(local)\\s+)?function\\s+([a-zA-Z_0-9.:]+)\\s*\\(([^)]*)\\)"
    );

    // 匹配表达式/表字段函数赋值: table.foo = function(...)
    private static final Pattern ASSIGN_FUNCTION_PATTERN = Pattern.compile(
            "^\\s*([a-zA-Z_0-9.:]+)\\s*=\\s*function\\s*\\(([^)]*)\\)"
    );

    /**
     * 为当前代码编辑器构建完整符号大纲树
     * @param luaSource Lua 源码全文 (可为 null)
     * @param luacFile 字节码文件模型 (可为 null)
     * @param chunkLineRanges 反编译代码块行号映射表 (可为 null)
     * @return 根大纲节点 (若无符号则返回空根节点)
     */
    public OutlineItem buildOutline(String luaSource, LuacFile luacFile, Map<String, LineRange> chunkLineRanges) {
        if (luacFile != null && luacFile.getMainChunk() != null) {
            return buildOutlineFromLuac(luacFile, chunkLineRanges, luaSource);
        } else if (luaSource != null && !luaSource.trim().isEmpty()) {
            return buildOutlineFromSource(luaSource);
        } else {
            return new OutlineItem("根代码块", "()", ItemType.MAIN_CHUNK, 1, 1);
        }
    }

    private OutlineItem buildOutlineFromLuac(LuacFile luacFile, Map<String, LineRange> chunkLineRanges, String luaSource) {
        Chunk mainChunk = luacFile.getMainChunk();
        LineRange mainRange = (chunkLineRanges != null) ? chunkLineRanges.get("main") : null;

        int startLine = (mainRange != null) ? mainRange.startLine : Math.max(1, mainChunk.getLineDefined());
        int endLine = (mainRange != null) ? mainRange.endLine : Math.max(startLine, mainChunk.getLastLineDefined());

        OutlineItem root = new OutlineItem("main", "()", ItemType.MAIN_CHUNK, startLine, endLine);
        Set<Integer> recordedLines = new HashSet<>();
        recordedLines.add(startLine);

        // 递归解析子 Chunk
        if (mainChunk.getSubChunks() != null) {
            for (int i = 0; i < mainChunk.getSubChunks().size(); i++) {
                Chunk sub = mainChunk.getSubChunks().get(i);
                OutlineItem child = buildChunkOutlineItem(sub, chunkLineRanges, i + 1, recordedLines);
                if (child != null) {
                    root.addChild(child);
                }
            }
        }

        // 若有源码，可从源码中补充在字节码层面未单独建 Chunk 的表字段定义
        if (luaSource != null && !luaSource.isEmpty()) {
            List<OutlineItem> sourceItems = parseFunctionsFromSource(luaSource);
            for (OutlineItem item : sourceItems) {
                if (!recordedLines.contains(item.getStartLine())) {
                    root.addChild(item);
                    recordedLines.add(item.getStartLine());
                }
            }
        }

        return root;
    }

    private OutlineItem buildChunkOutlineItem(Chunk chunk, Map<String, LineRange> chunkLineRanges, int index, Set<Integer> recordedLines) {
        if (chunk == null) return null;

        String funcName = chunk.getFunction();
        if (funcName == null || funcName.trim().isEmpty()) {
            funcName = "func_" + index;
        }

        LineRange range = (chunkLineRanges != null) ? chunkLineRanges.get(funcName) : null;
        int startLine = (range != null) ? range.startLine : Math.max(1, chunk.getLineDefined());
        int endLine = (range != null) ? range.endLine : Math.max(startLine, chunk.getLastLineDefined());

        recordedLines.add(startLine);

        // 提取参数签名
        String signature = buildSignature(chunk);

        // 确定类型
        ItemType type = ItemType.FUNCTION;
        if (funcName.contains(":") || funcName.contains(".")) {
            type = ItemType.METHOD;
        } else if (funcName.startsWith("local_")) {
            type = ItemType.LOCAL_FUNCTION;
        }

        OutlineItem item = new OutlineItem(funcName, signature, type, startLine, endLine);

        // 递归处理嵌套子函数
        if (chunk.getSubChunks() != null) {
            for (int i = 0; i < chunk.getSubChunks().size(); i++) {
                Chunk nested = chunk.getSubChunks().get(i);
                OutlineItem nestedItem = buildChunkOutlineItem(nested, chunkLineRanges, i + 1, recordedLines);
                if (nestedItem != null) {
                    item.addChild(nestedItem);
                }
            }
        }

        return item;
    }

    private String buildSignature(Chunk chunk) {
        int numParams = chunk.getNumParams();
        if (numParams <= 0) {
            return "()";
        }

        List<String> paramNames = new ArrayList<>();
        List<LocalVar> locals = chunk.getLocalVars();
        if (locals != null && locals.size() >= numParams) {
            for (int i = 0; i < numParams; i++) {
                LocalVar v = locals.get(i);
                if (v != null && v.getName() != null && !v.getName().isEmpty()) {
                    paramNames.add(v.getName());
                } else {
                    paramNames.add("p" + (i + 1));
                }
            }
        } else {
            for (int i = 0; i < numParams; i++) {
                paramNames.add("p" + (i + 1));
            }
        }

        return "(" + String.join(", ", paramNames) + ")";
    }

    /**
     * 针对纯文本模式（无 LuacFile 调试数据时）通过正则词法解析提取函数
     */
    public OutlineItem buildOutlineFromSource(String luaSource) {
        String[] lines = luaSource.split("\r?\n", -1);
        OutlineItem root = new OutlineItem("根代码块", "()", ItemType.MAIN_CHUNK, 1, Math.max(1, lines.length));

        List<OutlineItem> items = parseFunctionsFromSource(luaSource);
        for (OutlineItem item : items) {
            root.addChild(item);
        }

        return root;
    }

    private List<OutlineItem> parseFunctionsFromSource(String luaSource) {
        if (luaSource == null || luaSource.isEmpty()) {
            return Collections.emptyList();
        }

        List<OutlineItem> results = new ArrayList<>();
        String[] lines = luaSource.split("\r?\n", -1);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNum = i + 1;

            Matcher m1 = FUNCTION_PATTERN.matcher(line);
            if (m1.find()) {
                boolean isLocal = m1.group(1) != null;
                String name = m1.group(2);
                String params = m1.group(3) != null ? m1.group(3).trim() : "";

                ItemType type = ItemType.FUNCTION;
                if (isLocal) {
                    type = ItemType.LOCAL_FUNCTION;
                } else if (name.contains(":") || name.contains(".")) {
                    type = ItemType.METHOD;
                }

                results.add(new OutlineItem(name, "(" + params + ")", type, lineNum, lineNum));
                continue;
            }

            Matcher m2 = ASSIGN_FUNCTION_PATTERN.matcher(line);
            if (m2.find()) {
                String name = m2.group(1);
                String params = m2.group(2) != null ? m2.group(2).trim() : "";
                ItemType type = (name.contains(":") || name.contains(".")) ? ItemType.METHOD : ItemType.FUNCTION;
                results.add(new OutlineItem(name, "(" + params + ")", type, lineNum, lineNum));
            }
        }

        return results;
    }
}
