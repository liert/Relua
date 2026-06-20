package com.github.relua.decompiler.pipeline;

import com.github.relua.model.LuacFile;
import com.github.relua.model.Chunk;
import com.github.relua.decompiler.LuaCodeGenerator;
import com.github.relua.decompiler.Decompiler;
import com.github.relua.decompiler.CodeGeneratorContext;
import com.github.relua.decompiler.InstructionHandler;
import com.github.relua.decompiler.BasicBlock;
import com.github.relua.decompiler.ssa.SsaFunction;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * 反编译引擎核心流线管理类，负责组织与调用各个反编译阶段
 */
public class DecompilerEngine {
    private final List<PipelineDebugListener> listeners = new ArrayList<>();

    public void addDebugListener(PipelineDebugListener listener) {
        listeners.add(listener);
    }

    public List<PipelineDebugListener> getDebugListeners() {
        return listeners;
    }

    /**
     * 执行完整的反编译流程
     * @param luacFile 解析后的 LuacFile 对象
     * @param showBytecode 是否仅输出字节码指令
     * @return 统一反编译结果
     */
    public DecompilerResult decompile(LuacFile luacFile, boolean showBytecode) {
        if (luacFile == null || luacFile.getMainChunk() == null) {
            throw new IllegalArgumentException("Invalid LuacFile object");
        }

        DecompilerResult result = new DecompilerResult();
        result.setLuacFile(luacFile);

        Chunk mainChunk = luacFile.getMainChunk();
        long startTime = System.currentTimeMillis();

        if (showBytecode) {
            Decompiler decompiler = new Decompiler();
            String bytecode = decompiler.generateBytecode(mainChunk);
            result.setDecompiledCode(bytecode);
            result.putTiming("bytecode_generation", System.currentTimeMillis() - startTime);
        } else {
            LuaCodeGenerator codeGenerator = new LuaCodeGenerator(mainChunk);
            
            // 注册所有 debug 监听器
            for (PipelineDebugListener listener : listeners) {
                codeGenerator.addDebugListener(listener);
            }

            // 执行生成
            String rawCode = codeGenerator.generate(mainChunk);
            result.putTiming("decompilation_pipeline", System.currentTimeMillis() - startTime);

            // 清理 chunk 标记并解析 line range
            processChunkMarkers(rawCode, result);

                        // 获取中间分析状态（CFG，AST，SSA）
            populateIntermediateStates(codeGenerator, result);
            result.setLuaCodeGenerator(codeGenerator);
        }

        return result;
    }

    private void processChunkMarkers(String rawCode, DecompilerResult result) {
        StringBuilder cleanCode = new StringBuilder();
        String[] lines = rawCode.split("\\r?\\n", -1);
        
        Map<String, Integer> startLines = new HashMap<>();
        int cleanLineCount = 0;
        
        for (String line : lines) {
            if (line.contains("--[=[RELUA_CHUNK_START:")) {
                int startIdx = line.indexOf("--[=[RELUA_CHUNK_START:") + "--[=[RELUA_CHUNK_START:".length();
                int endIdx = line.indexOf("]=]", startIdx);
                if (startIdx != -1 && endIdx != -1) {
                    String chunkName = line.substring(startIdx, endIdx);
                    startLines.put(chunkName, Math.max(1, cleanLineCount));
                }
            } else if (line.contains("--[=[RELUA_CHUNK_END:")) {
                int startIdx = line.indexOf("--[=[RELUA_CHUNK_END:") + "--[=[RELUA_CHUNK_END:".length();
                int endIdx = line.indexOf("]=]", startIdx);
                if (startIdx != -1 && endIdx != -1) {
                    String chunkName = line.substring(startIdx, endIdx);
                    int startLine = startLines.getOrDefault(chunkName, 1);
                    result.putChunkLineRange(chunkName, new com.github.relua.model.LineRange(startLine, cleanLineCount + 1));
                }
            } else {
                cleanCode.append(line).append("\n");
                cleanLineCount++;
            }
        }
        
        if (cleanCode.length() > 0) {
            cleanCode.setLength(cleanCode.length() - 1);
        }
        
        result.setDecompiledCode(cleanCode.toString());
    }

    private void populateIntermediateStates(LuaCodeGenerator generator, DecompilerResult result) {
        for (CodeGeneratorContext ctx : generator.getContexts()) {
            Chunk chunk = ctx.getChunk();
            String name = chunk.getFunction();
            result.putChunkAst(name, ctx.getAstBlock());

            if (ctx.getAstBlock() != null) {
                List<String> semanticErrors = SemanticValidator.validate(ctx.getAstBlock());
                for (String err : semanticErrors) {
                    String logMsg = "[Semantic Error][" + name + "] " + err;
                    com.github.relua.log.Logger.warning(logMsg);
                    result.addLog(logMsg);
                }
            }

            InstructionHandler handler = generator.getInstructionHandler(name);
            if (handler != null) {
                List<BasicBlock> basicBlocks = handler.getBasicBlocks(chunk);
                result.putChunkCfg(name, basicBlocks);

                SsaFunction ssa = handler.getPipeline().getSsaFunction(name);
                if (ssa != null) {
                    result.putChunkSsaFunction(name, ssa);
                }
            }
        }
    }
}
