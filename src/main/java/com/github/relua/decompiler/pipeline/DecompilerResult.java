package com.github.relua.decompiler.pipeline;

import com.github.relua.model.LuacFile;
import com.github.relua.model.LineRange;
import com.github.relua.decompiler.BasicBlock;
import com.github.relua.decompiler.LuaCodeGenerator;
import com.github.relua.decompiler.ssa.SsaFunction;
import com.github.relua.ast.Block;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 反编译结果容器类，存储反编译主流程中的所有阶段产物
 */
public class DecompilerResult {
    private String decompiledCode;
    private LuacFile luacFile;
    private LuaCodeGenerator luaCodeGenerator;
    private final Map<String, List<BasicBlock>> chunkCfgs = new HashMap<>();
    private final Map<String, Block> chunkAsts = new HashMap<>();
    private final Map<String, SsaFunction> chunkSsaFunctions = new HashMap<>();
    private final Map<String, LineRange> chunkLineRanges = new HashMap<>();
    private final List<String> logs = new ArrayList<>();
    private final Map<String, Long> timings = new HashMap<>();

    public LuaCodeGenerator getLuaCodeGenerator() {
        return luaCodeGenerator;
    }

    public void setLuaCodeGenerator(LuaCodeGenerator luaCodeGenerator) {
        this.luaCodeGenerator = luaCodeGenerator;
    }

    public String getDecompiledCode() {
        return decompiledCode;
    }

    public void setDecompiledCode(String decompiledCode) {
        this.decompiledCode = decompiledCode;
    }

    public LuacFile getLuacFile() {
        return luacFile;
    }

    public void setLuacFile(LuacFile luacFile) {
        this.luacFile = luacFile;
    }

    public Map<String, List<BasicBlock>> getChunkCfgs() {
        return chunkCfgs;
    }

    public void putChunkCfg(String functionName, List<BasicBlock> cfg) {
        this.chunkCfgs.put(functionName, cfg);
    }

    public Map<String, Block> getChunkAsts() {
        return chunkAsts;
    }

    public void putChunkAst(String functionName, Block ast) {
        this.chunkAsts.put(functionName, ast);
    }

    public Map<String, SsaFunction> getChunkSsaFunctions() {
        return chunkSsaFunctions;
    }

    public void putChunkSsaFunction(String functionName, SsaFunction ssaFunction) {
        this.chunkSsaFunctions.put(functionName, ssaFunction);
    }

    public Map<String, LineRange> getChunkLineRanges() {
        return chunkLineRanges;
    }

    public void putChunkLineRange(String chunkName, LineRange lineRange) {
        if (chunkName != null && lineRange != null) {
            this.chunkLineRanges.put(chunkName, lineRange);
        }
    }

    public void putAllChunkLineRanges(Map<String, LineRange> lineRanges) {
        if (lineRanges != null) {
            this.chunkLineRanges.putAll(lineRanges);
        }
    }

    public List<String> getLogs() {
        return logs;
    }

    public void addLog(String log) {
        this.logs.add(log);
    }

    public Map<String, Long> getTimings() {
        return timings;
    }

    public void putTiming(String phase, long durationMs) {
        this.timings.put(phase, durationMs);
    }
}
