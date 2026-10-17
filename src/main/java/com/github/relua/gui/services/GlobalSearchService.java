package com.github.relua.gui.services;

import com.github.relua.decompiler.Decompiler;
import com.github.relua.model.Chunk;
import com.github.relua.model.Constant;
import com.github.relua.model.LuacFile;
import com.github.relua.parser.LuacParser;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 全局项目字符串与全文检索服务，支持在打开的标签页（内存即时文本）与工作区文件目录中进行多维检索
 * 提供区分大小写、全词匹配、正则表达式、文件类型通配符过滤以及异步流式结果反馈
 */
public class GlobalSearchService {

    // 默认检索忽略的目录名称
    private static final Set<String> IGNORED_DIR_NAMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            ".git", ".svn", ".relua", ".idea", ".vscode", "target", "build", "bin", "node_modules", ".gradle"
    )));

    /**
     * 搜索配置选项模型
     */
    public static class SearchOptions {
        private final String query;
        private final boolean caseSensitive;
        private final boolean wholeWord;
        private final boolean regex;
        private final String fileMask;
        private final boolean openTabsOnly;

        public SearchOptions(String query, boolean caseSensitive, boolean wholeWord, boolean regex, String fileMask, boolean openTabsOnly) {
            this.query = query != null ? query : "";
            this.caseSensitive = caseSensitive;
            this.wholeWord = wholeWord;
            this.regex = regex;
            this.fileMask = (fileMask != null && !fileMask.trim().isEmpty()) ? fileMask.trim() : "*.lua, *.luac, *.*";
            this.openTabsOnly = openTabsOnly;
        }

        public String getQuery() {
            return query;
        }

        public boolean isCaseSensitive() {
            return caseSensitive;
        }

        public boolean isWholeWord() {
            return wholeWord;
        }

        public boolean isRegex() {
            return regex;
        }

        public String getFileMask() {
            return fileMask;
        }

        public boolean isOpenTabsOnly() {
            return openTabsOnly;
        }
    }

    /**
     * 单个匹配项结果模型
     */
    public static class SearchResultItem {
        private final File file;
        private final String displayPath;
        private final int lineNumber;     // 1-based 行号
        private final int startColumn;    // 0-based 起始列偏移
        private final int endColumn;      // 0-based 结束列偏移
        private final String lineContent;  // 匹配行完整内容
        private final String matchedText;  // 匹配文本

        public SearchResultItem(File file, String displayPath, int lineNumber, int startColumn, int endColumn, String lineContent, String matchedText) {
            this.file = file;
            this.displayPath = displayPath != null ? displayPath : (file != null ? file.getName() : "");
            this.lineNumber = lineNumber;
            this.startColumn = startColumn;
            this.endColumn = endColumn;
            this.lineContent = lineContent != null ? lineContent : "";
            this.matchedText = matchedText != null ? matchedText : "";
        }

        public File getFile() {
            return file;
        }

        public String getDisplayPath() {
            return displayPath;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public int getStartColumn() {
            return startColumn;
        }

        public int getEndColumn() {
            return endColumn;
        }

        public String getLineContent() {
            return lineContent;
        }

        public String getMatchedText() {
            return matchedText;
        }

        @Override
        public String toString() {
            return displayPath + ":" + lineNumber + " [" + matchedText + "] " + lineContent.trim();
        }
    }

    /**
     * 全局搜索结果汇总报告
     */
    public static class SearchResultSummary {
        private final List<SearchResultItem> items;
        private final int totalMatches;
        private final int matchedFilesCount;
        private final int scannedFilesCount;
        private final long durationMs;

        public SearchResultSummary(List<SearchResultItem> items, int totalMatches, int matchedFilesCount, int scannedFilesCount, long durationMs) {
            this.items = items;
            this.totalMatches = totalMatches;
            this.matchedFilesCount = matchedFilesCount;
            this.scannedFilesCount = scannedFilesCount;
            this.durationMs = durationMs;
        }

        public List<SearchResultItem> getItems() {
            return items;
        }

        public int getTotalMatches() {
            return totalMatches;
        }

        public int getMatchedFilesCount() {
            return matchedFilesCount;
        }

        public int getScannedFilesCount() {
            return scannedFilesCount;
        }

        public long getDurationMs() {
            return durationMs;
        }
    }

    /**
     * 构建编译搜索正则表达式
     * @param query 搜索关键词
     * @param caseSensitive 是否区分大小写
     * @param wholeWord 是否匹配全词
     * @param isRegex 是否使用正则表达式
     * @return 编译好的 Pattern，若输入为空或语法非法则返回 null
     */
    public static Pattern buildSearchPattern(String query, boolean caseSensitive, boolean wholeWord, boolean isRegex) {
        if (query == null || query.isEmpty()) {
            return null;
        }

        String patternStr;
        if (isRegex) {
            patternStr = wholeWord ? "\\b(?:" + query + ")\\b" : query;
        } else {
            String escaped = Pattern.quote(query);
            patternStr = wholeWord ? "\\b(?:" + escaped + ")\\b" : escaped;
        }

        int flags = 0;
        if (!caseSensitive) {
            flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }

        try {
            return Pattern.compile(patternStr, flags);
        } catch (PatternSyntaxException e) {
            return null;
        }
    }

    /**
     * 检查文件名是否匹配指定的文件掩码 (如 "*.lua, *.luac")
     * @param filename 文件名
     * @param fileMask 掩码字符串
     * @return 是否匹配
     */
    public static boolean matchesFileMask(String filename, String fileMask) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        if (fileMask == null || fileMask.trim().isEmpty() || fileMask.contains("*.*") || fileMask.equals("*")) {
            return true;
        }

        String[] parts = fileMask.split("[,;\\s]+");
        String lowerFilename = filename.toLowerCase();

        for (String part : parts) {
            String p = part.trim().toLowerCase();
            if (p.isEmpty()) continue;
            if (p.equals("*") || p.equals("*.*")) {
                return true;
            }

            // 将通配符 glob 转为正则
            StringBuilder regex = new StringBuilder("^");
            for (int i = 0; i < p.length(); i++) {
                char c = p.charAt(i);
                if (c == '*') {
                    regex.append(".*");
                } else if (c == '?') {
                    regex.append(".");
                } else if ("\\.[]{}()+-^$|".indexOf(c) != -1) {
                    regex.append("\\").append(c);
                } else {
                    regex.append(c);
                }
            }
            regex.append("$");

            if (lowerFilename.matches(regex.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在文本内容中按行检索所有匹配项
     * @param file 目标文件
     * @param displayPath 显示相对路径
     * @param text 文本全文
     * @param pattern 搜索正则
     * @return 匹配结果列表
     */
    public static List<SearchResultItem> searchInText(File file, String displayPath, String text, Pattern pattern) {
        if (text == null || text.isEmpty() || pattern == null) {
            return Collections.emptyList();
        }

        List<SearchResultItem> results = new ArrayList<>();
        String[] lines = text.split("\r?\n", -1);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1; // 1-based
            Matcher matcher = pattern.matcher(line);

            while (matcher.find()) {
                results.add(new SearchResultItem(
                        file,
                        displayPath,
                        lineNumber,
                        matcher.start(),
                        matcher.end(),
                        line,
                        matcher.group()
                ));
            }
        }

        return results;
    }

    /**
     * 创建后台异步搜索任务，支持进度更新、实时匹配回调与安全中断
     * @param rootFolder 工作区根目录 (可为 null)
     * @param openTabContents 当前打开标签页的实时内存文本 (File -> text)
     * @param options 搜索选项
     * @param onBatchMatchFound 批次匹配项到达回调 (将在 JavaFX 主线程触发)
     * @return 可执行/可取消的 Task
     */
    public Task<SearchResultSummary> createSearchTask(
            File rootFolder,
            Map<File, String> openTabContents,
            SearchOptions options,
            Consumer<List<SearchResultItem>> onBatchMatchFound) {

        return new Task<SearchResultSummary>() {
            @Override
            protected SearchResultSummary call() throws Exception {
                long startTime = System.currentTimeMillis();
                Pattern pattern = buildSearchPattern(options.getQuery(), options.isCaseSensitive(), options.isWholeWord(), options.isRegex());
                if (pattern == null) {
                    return new SearchResultSummary(Collections.emptyList(), 0, 0, 0, 0);
                }

                updateMessage("准备检索...");
                List<SearchResultItem> allResults = new ArrayList<>();
                Set<File> processedFiles = new HashSet<>();
                Set<File> matchedFiles = new HashSet<>();
                int scannedCount = 0;

                List<SearchResultItem> batchBuffer = new ArrayList<>();
                long lastFlushTime = System.currentTimeMillis();

                // 1. 优先扫描所有已打开的标签页（具备最新内存编辑状态）
                if (openTabContents != null) {
                    for (Map.Entry<File, String> entry : openTabContents.entrySet()) {
                        if (isCancelled()) return null;
                        File file = entry.getKey();
                        if (file == null) continue;

                        if (!matchesFileMask(file.getName(), options.getFileMask())) {
                            continue;
                        }

                        processedFiles.add(file.getAbsoluteFile());
                        scannedCount++;
                        updateMessage("正在扫描打开的文件: " + file.getName());

                        String relPath = getRelativePath(rootFolder, file);
                        List<SearchResultItem> matches = searchInText(file, relPath, entry.getValue(), pattern);
                        if (!matches.isEmpty()) {
                            matchedFiles.add(file);
                            allResults.addAll(matches);
                            batchBuffer.addAll(matches);
                        }

                        if (System.currentTimeMillis() - lastFlushTime > 100 && !batchBuffer.isEmpty()) {
                            flushBatch(batchBuffer, onBatchMatchFound);
                            lastFlushTime = System.currentTimeMillis();
                        }
                    }
                }

                // 2. 如果不仅限于打开的标签，则递归扫描工程目录中未打开的文件
                if (!options.isOpenTabsOnly() && rootFolder != null && rootFolder.exists() && rootFolder.isDirectory()) {
                    List<File> diskFiles = new ArrayList<>();
                    collectFilesRecursively(rootFolder, diskFiles, options.getFileMask(), this::isCancelled);

                    int totalDiskFiles = diskFiles.size();
                    for (int i = 0; i < totalDiskFiles; i++) {
                        if (isCancelled()) return null;
                        File file = diskFiles.get(i);
                        if (processedFiles.contains(file.getAbsoluteFile())) {
                            continue; // 已在标签页中扫描过
                        }

                        scannedCount++;
                        updateProgress(i + 1, totalDiskFiles);
                        updateMessage("正在扫描: " + file.getName());

                        String content = readFileContentOrDecompile(file);
                        if (content != null && !content.isEmpty()) {
                            String relPath = getRelativePath(rootFolder, file);
                            List<SearchResultItem> matches = searchInText(file, relPath, content, pattern);
                            if (!matches.isEmpty()) {
                                matchedFiles.add(file);
                                allResults.addAll(matches);
                                batchBuffer.addAll(matches);
                            }
                        }

                        if (System.currentTimeMillis() - lastFlushTime > 100 && !batchBuffer.isEmpty()) {
                            flushBatch(batchBuffer, onBatchMatchFound);
                            lastFlushTime = System.currentTimeMillis();
                        }
                    }
                }

                if (!batchBuffer.isEmpty()) {
                    flushBatch(batchBuffer, onBatchMatchFound);
                }

                long duration = System.currentTimeMillis() - startTime;
                return new SearchResultSummary(allResults, allResults.size(), matchedFiles.size(), scannedCount, duration);
            }
        };
    }

    private void flushBatch(List<SearchResultItem> buffer, Consumer<List<SearchResultItem>> consumer) {
        if (consumer == null || buffer.isEmpty()) {
            buffer.clear();
            return;
        }
        List<SearchResultItem> snapshot = new ArrayList<>(buffer);
        buffer.clear();
        Platform.runLater(() -> consumer.accept(snapshot));
    }

    private void collectFilesRecursively(File dir, List<File> result, String fileMask, java.util.function.BooleanSupplier isCancelled) {
        if (dir == null || !dir.isDirectory() || isCancelled.getAsBoolean()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (isCancelled.getAsBoolean()) return;
            if (f.isDirectory()) {
                if (!IGNORED_DIR_NAMES.contains(f.getName())) {
                    collectFilesRecursively(f, result, fileMask, isCancelled);
                }
            } else if (f.isFile()) {
                if (matchesFileMask(f.getName(), fileMask)) {
                    result.add(f);
                }
            }
        }
    }

    private String readFileContentOrDecompile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }

        // 判断是否为 Luac 二进制文件
        boolean isLuac = file.getName().toLowerCase().endsWith(".luac");
        if (!isLuac) {
            // 快速嗅探文件头前 4 字节
            try (java.io.InputStream in = new java.io.FileInputStream(file)) {
                byte[] header = new byte[4];
                int read = in.read(header);
                if (read == 4 && header[0] == 0x1B && header[1] == 'L' && header[2] == 'u' && header[3] == 'a') {
                    isLuac = true;
                }
            } catch (Exception ignored) {
            }
        }

        if (isLuac) {
            try {
                LuacParser parser = new LuacParser();
                LuacFile luacFile = parser.parse(file.getAbsolutePath());
                if (luacFile != null) {
                    Decompiler decompiler = new Decompiler();
                    return decompiler.decompile(luacFile, false);
                }
            } catch (Exception e) {
                // 反编译失败时，提取常量字符串拼接作为备用文本
                return extractStringsFromLuac(file);
            }
        }

        // 普通文本文件读取
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            try {
                return new String(Files.readAllBytes(file.toPath()), StandardCharsets.ISO_8859_1);
            } catch (IOException ignored) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    private String extractStringsFromLuac(File file) {
        try {
            LuacParser parser = new LuacParser();
            LuacFile luacFile = parser.parse(file.getAbsolutePath());
            if (luacFile != null && luacFile.getMainChunk() != null) {
                StringBuilder sb = new StringBuilder();
                collectChunkStrings(luacFile.getMainChunk(), sb);
                return sb.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void collectChunkStrings(Chunk chunk, StringBuilder sb) {
        if (chunk == null) return;
        if (chunk.getConstants() != null) {
            for (Constant c : chunk.getConstants()) {
                if (c != null && c.getType() == com.github.relua.model.ValueType.STRING && c.getValue() != null) {
                    sb.append(c.getValue()).append("\n");
                }
            }
        }
        if (chunk.getSubChunks() != null) {
            for (Chunk sub : chunk.getSubChunks()) {
                collectChunkStrings(sub, sb);
            }
        }
    }

    private String getRelativePath(File rootFolder, File file) {
        if (rootFolder == null || file == null) {
            return file != null ? file.getName() : "";
        }
        try {
            String rootPath = rootFolder.getCanonicalPath();
            String filePath = file.getCanonicalPath();
            if (filePath.startsWith(rootPath)) {
                String rel = filePath.substring(rootPath.length());
                if (rel.startsWith(File.separator)) {
                    rel = rel.substring(File.separator.length());
                }
                return rel;
            }
        } catch (Exception ignored) {
        }
        return file.getName();
    }
}
