package com.github.relua.gui.services;

import com.github.relua.decompiler.Decompiler;
import com.github.relua.gui.model.OutlineItem;
import com.github.relua.gui.workspace.FileMetadata;
import com.github.relua.gui.workspace.ProjectWorkspaceManager;
import com.github.relua.model.LineRange;
import com.github.relua.model.LuacFile;
import com.github.relua.parser.LuacParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** 文件函数索引；读取、反编译和函数提取不写入源文件或工作区。 */
public class FileFunctionService {
    public List<OutlineItem> load(File file) throws IOException {
        FileMetadata snapshot = ProjectWorkspaceManager.getInstance().getValidSourceSnapshot(file);
        if (snapshot != null) {
            return build(snapshot.getEditedText(), null, snapshot.getChunkLineRanges());
        }
        LuacFile luacFile;
        String source;
        Map<String, LineRange> ranges = Collections.emptyMap();
        try {
            com.github.relua.parser.ChunkParser.setParsingContext(file.getName(), "文件树函数大纲分析");
            luacFile = new LuacParser().parse(file.getAbsolutePath());
        } catch (IOException parseError) {
            if (file.length() > 10 * 1024 * 1024) throw parseError;
            byte[] bytes = Files.readAllBytes(file.toPath());
            for (byte value : bytes) {
                if (value == 0) throw parseError;
            }
            source = new String(bytes, StandardCharsets.UTF_8);
            return build(restoredSource(file, source), null, ranges);
        } finally {
            com.github.relua.parser.ChunkParser.clearParsingContext();
        }
        Decompiler decompiler = new Decompiler();
        source = decompiler.decompile(luacFile, false);
        ranges = decompiler.getChunkLineRanges();
        return build(restoredSource(file, source), luacFile, ranges);
    }

    private String restoredSource(File file, String source) {
        FileMetadata metadata = ProjectWorkspaceManager.getInstance().getValidFileMetadata(file);
        return metadata != null && metadata.getEditedText() != null
                ? metadata.getEditedText() : source;
    }

    public List<OutlineItem> build(String source, LuacFile luacFile, Map<String, LineRange> ranges) {
        OutlineItem root = new OutlineService().buildOutline(source, luacFile, ranges);
        List<OutlineItem> functions = new ArrayList<>();
        collect(root, functions);
        // 显示当前源码中的名称与参数，保留字节码提取的范围和匿名函数。
        Map<Integer, OutlineItem> sourceFunctions = new java.util.HashMap<>();
        if (source != null) {
            for (OutlineItem function : new OutlineService().buildOutlineFromSource(source).getChildren()) {
                sourceFunctions.put(function.getStartLine(), function);
            }
        }
        for (int i = 0; i < functions.size(); i++) {
            OutlineItem original = functions.get(i);
            OutlineItem current = sourceFunctions.get(original.getStartLine());
            int endLine = original.getEndLine();
            if (endLine <= original.getStartLine() && ranges != null) {
                for (LineRange r : ranges.values()) {
                    if (r != null && r.startLine == original.getStartLine() && r.endLine >= r.startLine) {
                        endLine = r.endLine;
                        break;
                    }
                }
            }
            if (current != null) {
                functions.set(i, new OutlineItem(current.getName(), current.getSignature(),
                        current.getType(), original.getStartLine(), endLine));
            } else if (endLine != original.getEndLine()) {
                functions.set(i, new OutlineItem(original.getName(), original.getSignature(),
                        original.getType(), original.getStartLine(), endLine));
            }
        }
        functions.sort(Comparator.comparingInt(OutlineItem::getStartLine));
        return functions;
    }

    private void collect(OutlineItem parent, List<OutlineItem> functions) {
        for (OutlineItem child : parent.getChildren()) {
            if (child.getType() != OutlineItem.ItemType.VARIABLE
                    && child.getType() != OutlineItem.ItemType.MAIN_CHUNK) functions.add(child);
            collect(child, functions);
        }
    }
}
