package com.github.relua.decompiler;

import com.github.relua.model.LuacFile;
import com.github.relua.parser.LuacParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import com.github.relua.log.Logger;
import com.github.relua.log.LogConfig;
import com.github.relua.log.LogLevel;
import com.github.relua.decompiler.analysis.DataFlowAnalyzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.relua.ast.*;
import com.github.relua.model.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Decompiler测试用例（小米 xqdatacenter.lua 反编译）
 */
class DecompilerTest {

    @BeforeAll
    static void setUpAll() {
        LogConfig config = new LogConfig();
        config.setLogLevel(LogLevel.WARNING);
        config.setConsoleOutput(false);
        config.setFileOutput(false);
        Logger.init(config);
    }

    @Test
    void testDecompileXqdatacenter() throws IOException {
        // 小米固件 Fate Lua 字节码
        String filePath = "src/test/resources/xiaomi/xqdatacenter.lua";
        File file = new File(filePath);
        assertTrue(file.exists(), "xqdatacenter.lua file does not exist: " + file.getAbsolutePath());

        // 创建解析器和反编译器
        LuacParser parser = new LuacParser();
        Decompiler decompiler = new Decompiler();

        // 解析
        LuacFile luacFile = parser.parse(filePath);
        assertNotNull(luacFile, "Failed to parse xqdatacenter.lua");

        // 反编译
        com.github.relua.debug.DecompilerDebugger debugger = new com.github.relua.debug.DecompilerDebugger("target/debug_xqdatacenter", "xqdatacenter.lua");
        com.github.relua.debug.DecompilerDebugger.set(debugger);

        String luaCode = decompiler.decompile(luacFile);
        assertNotNull(luaCode, "Failed to decompile");
        assertFalse(luaCode.isEmpty(), "Decompiled code is empty");

        com.github.relua.debug.DecompilerDebugger.clear();

        // 写入输出文件
        try (PrintWriter writer = new PrintWriter(new FileWriter("target/xqdatacenter_decompiled.lua"))) {
            writer.print(luaCode);
        }

        // 验证 peephole 优化
        assertTrue(luaCode.contains("return false"), "Should contain direct 'return false'");
        assertTrue(luaCode.contains("return true"), "Should contain direct 'return true'");

        // 打印 tunnelRequest 函数内容用于验证
        System.out.println("=== tunnelRequest 函数 ===");
        String[] lines = luaCode.split("\n");
        boolean inTunnel = false;
        int count = 0;
        for (String line : lines) {
            if (line.contains("function tunnelRequest")) {
                inTunnel = true;
                count = 0;
            }
            if (inTunnel) {
                System.out.println(line);
                count++;
                if (count > 30) break;
            }
        }
    }

    @Test
    void testDecompileCgi() throws IOException {
        String filePath = "src/test/resources/xiaomi/cgi.lua";
        File file = new File(filePath);
        assertTrue(file.exists(), "cgi.lua file does not exist: " + file.getAbsolutePath());

        LuacParser parser = new LuacParser();
        Decompiler decompiler = new Decompiler();

        LuacFile luacFile = parser.parse(filePath);
        assertNotNull(luacFile, "Failed to parse cgi.lua");

        com.github.relua.debug.DecompilerDebugger debugger = new com.github.relua.debug.DecompilerDebugger("target/debug_cgi", "cgi.lua");
        com.github.relua.debug.DecompilerDebugger.set(debugger);

        String luaCode = decompiler.decompile(luacFile);
        assertNotNull(luaCode, "Failed to decompile");
        assertFalse(luaCode.contains(":close:close"), "SELF call should not duplicate method name");
        assertFalse(luaCode.contains(":read:read"), "SELF call should not duplicate method name");
        assertFalse(luaCode.contains("256 * R2 + 127"), "Stopped execute status must use the waitExec status code result");
        assertFalse(luaCode.contains("return a0 + 127"), "Stopped execute status must not use the command argument");
        assertFalse(luaCode.contains("a1_2:close"), "SELF object should resolve to the captured file handle");
        assertFalse(luaCode.contains("module_R"), "SSA-first lowering should preserve recoverable global names");
        assertTrue(luaCode.contains("exectime = os.clock()"), "GETGLOBAL/GETTABLE should preserve global member access");
        assertTrue(luaCode.contains("require(\"luci.ltn12\")"), "GETGLOBAL should preserve require calls");
        assertTrue(luaCode.contains("xiaoqiang_common_XQFunction.waitExec"), "GETTABLE should preserve table member calls");
        assertTrue(luaCode.contains("a0:close()"), "SELF close call should preserve the captured file handle");
        assertTrue(luaCode.contains("a0:read("), "SELF read call should preserve the captured file handle");
        assertTrue(luaCode.contains("256 * R2_2 + 127") || luaCode.contains("256 * R2_2") && luaCode.contains("+ 127"),
                "Stopped execute status should preserve the waitExec status code result");

        com.github.relua.debug.DecompilerDebugger.clear();

        try (PrintWriter writer = new PrintWriter(new FileWriter("target/cgi_decompiled.lua"))) {
            writer.print(luaCode);
        }
    }

    @Test
    void testDecompileHttp() throws IOException {
        String filePath = "src/test/resources/xiaomi/http.lua";
        File file = new File(filePath);
        assertTrue(file.exists(), "http.lua file does not exist: " + file.getAbsolutePath());

        LuacParser parser = new LuacParser();
        Decompiler decompiler = new Decompiler();

        LuacFile luacFile = parser.parse(filePath);
        assertNotNull(luacFile, "Failed to parse http.lua");

        com.github.relua.debug.DecompilerDebugger debugger = new com.github.relua.debug.DecompilerDebugger("target/debug_http", "http.lua");
        com.github.relua.debug.DecompilerDebugger.set(debugger);

        String luaCode = decompiler.decompile(luacFile);
        assertNotNull(luaCode, "Failed to decompile");

        com.github.relua.debug.DecompilerDebugger.clear();

        try (PrintWriter writer = new PrintWriter(new FileWriter("target/http_decompiled.lua"))) {
            writer.print(luaCode);
        }

        // 验证 writeJsonNoLog 中没有重复的 write(string.format("\"%s\"", ...))
        int count = 0;
        int idx = 0;
        String pattern = "write(string.format(\"\\\"%s\\\"\"";
        while ((idx = luaCode.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        assertEquals(1, count, "write(string.format) in writeJsonNoLog should only be emitted once (no double-emission)");
    }

    @Test
    void testDecompileXQSecureUtil() throws IOException {
        String filePath = "src/test/resources/xiaomi/XQSecureUtil.lua";
        File file = new File(filePath);
        assertTrue(file.exists(), "XQSecureUtil.lua file does not exist: " + file.getAbsolutePath());

        LuacParser parser = new LuacParser();
        Decompiler decompiler = new Decompiler();

        LuacFile luacFile = parser.parse(filePath);
        assertNotNull(luacFile, "Failed to parse XQSecureUtil.lua");

        String luaCode = decompiler.decompile(luacFile);
        assertNotNull(luaCode, "Failed to decompile");

        try (PrintWriter writer = new PrintWriter(new FileWriter("target/XQSecureUtil_decompiled.lua"))) {
            writer.print(luaCode);
        }

        // 验证 peephole 优化
        // 1. 验证 LOADNIL 折叠：xssCheck 中应该包含 return nil，且不能有 R4 = nil 后紧跟 return R4 的冗余
        assertTrue(luaCode.contains("return nil"), "Should contain direct 'return nil'");
        assertFalse(luaCode.contains("R4 = nil\n    return R4"), "Should not contain redundant 'R4 = nil; return R4'");

        // 2. 验证 LOADBOOL 折叠
        assertTrue(luaCode.contains("return false"), "Should contain direct 'return false'");
        assertTrue(luaCode.contains("return true"), "Should contain direct 'return true'");

        // 3. 验证 LOADK 折叠（如 return "" 等）
        assertTrue(luaCode.contains("return \"\""), "Should contain direct 'return \"\"'");
    }

    @Test
    void testDecompileSha1() throws IOException {
        String filePath = "src/test/resources/xiaomi/sha1.lua";
        File file = new File(filePath);
        assertTrue(file.exists(), "sha1.lua file does not exist: " + file.getAbsolutePath());

        LuacParser parser = new LuacParser();
        Decompiler decompiler = new Decompiler();

        LuacFile luacFile = parser.parse(filePath);
        assertNotNull(luacFile, "Failed to parse sha1.lua");

        // Save raw bytecode disassembly
        String bytecode = decompiler.decompile(luacFile, true);
        try (PrintWriter writer = new PrintWriter(new FileWriter("target/sha1_bytecode.txt"))) {
            writer.print(bytecode);
        }

        String luaCode = decompiler.decompile(luacFile);
        assertNotNull(luaCode, "Failed to decompile");

        try (PrintWriter writer = new PrintWriter(new FileWriter("target/sha1_decompiled.lua"))) {
            writer.print(luaCode);
        }

        assertFalse(luaCode.contains("R3 = R2 + 3 .."), "Index arithmetic must not be merged into concat");
        assertFalse(luaCode.contains("R2 + 3 .."), "Arithmetic index expression must not be absorbed by concat");
        assertFalse(luaCode.matches("(?s).*if R2(?:_\\d+)? ~= \"string\" then\\s*end\\s*local R2(?:_\\d+)?\\s*_G\\.assert\\(R2(?:_\\d+)?\\).*"),
                "sha1 type assert must not be emitted as an empty if plus unresolved boolean register");
        assertFalse(luaCode.matches("(?s).*if # a0 >= 2147483647 then\\s*end\\s*local R2(?:_\\d+)?\\s*_G\\.assert\\(R2(?:_\\d+)?\\).*"),
                "sha1 length assert must not be emitted as an empty if plus unresolved boolean register");
        assertTrue(luaCode.matches("(?s).*local R2(?:_\\d+)? = R2(?:_\\d+)? == \"string\"\\s*_G\\.assert\\(R2(?:_\\d+)?\\).*"),
                "sha1 type check should recover the comparison boolean passed to assert");
        assertTrue(luaCode.matches("(?s).*local R2(?:_\\d+)? = # a0 < 2147483647\\s*_G\\.assert\\(R2(?:_\\d+)?\\).*"),
                "sha1 length check should recover the comparison boolean passed to assert");
        assertFalse(luaCode.contains("end(function"),
                "sha1 helper closures should be materialized before calls instead of nested as IIFEs");
        assertFalse(luaCode.matches("(?s).*function\\([^\\n]*\\).*?end\\s*\\([^)]*\\).*"),
                "sha1 helper closures should not be emitted as immediately invoked function expressions");
        assertTrue(luaCode.matches("(?s).*local R3(?:_\\d+)? = R3(?:_\\d+)? == \"string\"\\s*_G\\.assert\\(R3(?:_\\d+)?, \"key passed to hmac_sha1 should be a string\"\\).*"),
                "hmac_sha1 key type assert should recover the comparison boolean passed to assert");
        assertTrue(luaCode.matches("(?s).*local R3(?:_\\d+)? = R3(?:_\\d+)? == \"string\"\\s*_G\\.assert\\(R3(?:_\\d+)?, \"text passed to hmac_sha1 should be a string\"\\).*"),
                "hmac_sha1 text type assert should recover the comparison boolean passed to assert");
        assertFalse(luaCode.matches("(?s).*function hmac_sha1\\(a0, a1\\).*a0_\\d+.*end.*"),
                "hmac_sha1 should not reference unmaterialized SSA parameter versions");
        assertTrue(luaCode.matches("(?s).*R3(?:_\\d+)? = R2(?:_\\d+)? \\+ 3.*")
                        || luaCode.matches("(?s).*if a0\\[R2(?:_\\d+)? \\+ 3\\] then.*"),
                "asHEX must preserve or safely inline the R2 + 3 index definition");
        if (luaCode.matches("(?s).*if a0\\[R3(?:_\\d+)?\\] then.*")) {
            int definitionIndex = indexOfPattern(luaCode, "R3(?:_\\d+)? = R2(?:_\\d+)? \\+ 3");
            int useIndex = indexOfPattern(luaCode, "if a0\\[R3(?:_\\d+)?\\] then");
            assertTrue(definitionIndex >= 0 && definitionIndex < useIndex,
                    "R3 must be assigned before it is used as a0 index");
        }
    }

    @Test
    void testPeepholeOptimizationEdgeCases() {
        // --- 模式 B 正例 1：R4 = nil; return nil -> return nil ---
        {
            Assign assign = new Assign(
                    Collections.singletonList(new Name("R4", new SourcePos(0, 1))),
                    Collections.singletonList(new NilConst(new SourcePos(0, 1))),
                    new SourcePos(0, 1)
            );
            ReturnStatement ret = new ReturnStatement(
                    Collections.singletonList(new NilConst(new SourcePos(1, 2))),
                    new SourcePos(1, 2)
            );
            List<Instruction> insts = new ArrayList<>();
            insts.add(new Instruction(0, (4 << 6) | (4 << 23), Opcode.LOADNIL)); // A=4, B=4
            insts.add(new Instruction(1, (4 << 6) | (2 << 23), Opcode.RETURN));  // A=4, B=2
            verifyPeephole(assign, ret, insts, null, 1, NilConst.class);
        }

        // --- 模式 B 正例 2：R1 = true; return true -> return true ---
        {
            Assign assign = new Assign(
                    Collections.singletonList(new Name("R1", new SourcePos(0, 1))),
                    Collections.singletonList(new BooleanConst(true, new SourcePos(0, 1))),
                    new SourcePos(0, 1)
            );
            ReturnStatement ret = new ReturnStatement(
                    Collections.singletonList(new BooleanConst(true, new SourcePos(1, 2))),
                    new SourcePos(1, 2)
            );
            List<Instruction> insts = new ArrayList<>();
            insts.add(new Instruction(0, (1 << 6) | (1 << 23) | (0 << 14), Opcode.LOADBOOL)); // A=1, B=1, C=0
            insts.add(new Instruction(1, (1 << 6) | (2 << 23), Opcode.RETURN));               // A=1, B=2
            verifyPeephole(assign, ret, insts, null, 1, BooleanConst.class);
        }

        // --- 模式 B 正例 3：R1 = false; return false -> return false ---
        {
            Assign assign = new Assign(
                    Collections.singletonList(new Name("R1", new SourcePos(0, 1))),
                    Collections.singletonList(new BooleanConst(false, new SourcePos(0, 1))),
                    new SourcePos(0, 1)
            );
            ReturnStatement ret = new ReturnStatement(
                    Collections.singletonList(new BooleanConst(false, new SourcePos(1, 2))),
                    new SourcePos(1, 2)
            );
            List<Instruction> insts = new ArrayList<>();
            insts.add(new Instruction(0, (1 << 6) | (0 << 23) | (0 << 14), Opcode.LOADBOOL)); // A=1, B=0, C=0
            insts.add(new Instruction(1, (1 << 6) | (2 << 23), Opcode.RETURN));               // A=1, B=2
            verifyPeephole(assign, ret, insts, null, 1, BooleanConst.class);
        }

        // --- 模式 B 正例 4：R1 = "abc"; return "abc" -> return "abc" ---
        {
            Assign assign = new Assign(
                    Collections.singletonList(new Name("R1", new SourcePos(0, 1))),
                    Collections.singletonList(new StringConst("abc", new SourcePos(0, 1))),
                    new SourcePos(0, 1)
            );
            ReturnStatement ret = new ReturnStatement(
                    Collections.singletonList(new StringConst("abc", new SourcePos(1, 2))),
                    new SourcePos(1, 2)
            );
            List<Instruction> insts = new ArrayList<>();
            insts.add(new Instruction(0, (1 << 6) | (0 << 14), Opcode.LOADK)); // A=1, Bx=0
            insts.add(new Instruction(1, (1 << 6) | (2 << 23), Opcode.RETURN)); // A=1, B=2
            List<Constant> constants = new ArrayList<>();
            constants.add(Constant.string("abc"));
            verifyPeephole(assign, ret, insts, constants, 1, StringConst.class);
        }

        // --- 模式 B 正例 5：R1 = 700; return 700 -> return 700 ---
        {
            Assign assign = new Assign(
                    Collections.singletonList(new Name("R1", new SourcePos(0, 1))),
                    Collections.singletonList(new NumberConst(700.0, new SourcePos(0, 1))),
                    new SourcePos(0, 1)
            );
            ReturnStatement ret = new ReturnStatement(
                    Collections.singletonList(new NumberConst(700.0, new SourcePos(1, 2))),
                    new SourcePos(1, 2)
            );
            List<Instruction> insts = new ArrayList<>();
            insts.add(new Instruction(0, (1 << 6) | (0 << 14), Opcode.LOADK)); // A=1, Bx=0
            insts.add(new Instruction(1, (1 << 6) | (2 << 23), Opcode.RETURN)); // A=1, B=2
            List<Constant> constants = new ArrayList<>();
            constants.add(Constant.number(700.0));
            verifyPeephole(assign, ret, insts, constants, 1, NumberConst.class);
        }

        // --- 负例 1：local name = nil; return nil (非临时寄存器不折叠) ---
        {
            Assign assign = new Assign(
                    Collections.singletonList(new Name("name", new SourcePos(0, 1))),
                    Collections.singletonList(new NilConst(new SourcePos(0, 1))),
                    new SourcePos(0, 1)
            );
            ReturnStatement ret = new ReturnStatement(
                    Collections.singletonList(new NilConst(new SourcePos(1, 2))),
                    new SourcePos(1, 2)
            );
            List<Instruction> insts = new ArrayList<>();
            insts.add(new Instruction(0, (4 << 6) | (4 << 23), Opcode.LOADNIL)); // A=4, B=4
            insts.add(new Instruction(1, (4 << 6) | (2 << 23), Opcode.RETURN));  // A=4, B=2
            verifyPeephole(assign, ret, insts, null, 2, null);
        }

        // --- 负例 2：R4 = nil; return false (常量语义不一致不折叠) ---
        {
            Assign assign = new Assign(
                    Collections.singletonList(new Name("R4", new SourcePos(0, 1))),
                    Collections.singletonList(new NilConst(new SourcePos(0, 1))),
                    new SourcePos(0, 1)
            );
            ReturnStatement ret = new ReturnStatement(
                    Collections.singletonList(new BooleanConst(false, new SourcePos(1, 2))),
                    new SourcePos(1, 2)
            );
            List<Instruction> insts = new ArrayList<>();
            insts.add(new Instruction(0, (4 << 6) | (4 << 23), Opcode.LOADNIL)); // A=4, B=4
            insts.add(new Instruction(1, (4 << 6) | (2 << 23), Opcode.RETURN));  // A=4, B=2
            verifyPeephole(assign, ret, insts, null, 2, null);
        }

        // --- 负例 3：R4 = nil; return nil，但 returnInst.A != 4 (核心防线不折叠) ---
        {
            Assign assign = new Assign(
                    Collections.singletonList(new Name("R4", new SourcePos(0, 1))),
                    Collections.singletonList(new NilConst(new SourcePos(0, 1))),
                    new SourcePos(0, 1)
            );
            ReturnStatement ret = new ReturnStatement(
                    Collections.singletonList(new NilConst(new SourcePos(1, 2))),
                    new SourcePos(1, 2)
            );
            List<Instruction> insts = new ArrayList<>();
            insts.add(new Instruction(0, (4 << 6) | (4 << 23), Opcode.LOADNIL)); // A=4, B=4
            insts.add(new Instruction(1, (5 << 6) | (2 << 23), Opcode.RETURN));  // A=5 (而不是 4), B=2
            verifyPeephole(assign, ret, insts, null, 2, null);
        }

        // --- 负例 4：R4 = nil; return nil，但 assign 是 LabelPC (控制流跳入不折叠) ---
        {
            Assign assign = new Assign(
                    Collections.singletonList(new Name("R4", new SourcePos(0, 1))),
                    Collections.singletonList(new NilConst(new SourcePos(0, 1))),
                    new SourcePos(0, 1)
            );
            ReturnStatement ret = new ReturnStatement(
                    Collections.singletonList(new NilConst(new SourcePos(1, 2))),
                    new SourcePos(1, 2)
            );
            List<Instruction> insts = new ArrayList<>();
            insts.add(new Instruction(0, (4 << 6) | (4 << 23), Opcode.LOADNIL)); // A=4, B=4
            insts.add(new Instruction(1, (4 << 6) | (2 << 23), Opcode.RETURN));  // A=4, B=2
            
            Block block = new Block(null);
            block.statements.add(assign);
            block.statements.add(ret);

            Chunk chunk = new Chunk();
            for (Instruction inst : insts) {
                chunk.addInstruction(inst);
            }
            Register register = new Register();
            CodeGeneratorContext context = new CodeGeneratorContext(chunk, register);
            context.addLabelPC(0); // 设置 assign 为 LabelPC

            new AstCleanupPass().optimizeReturnPatterns(block, context, Collections.emptySet());
            assertEquals(2, block.statements.size(), "Should not optimize if assign is LabelPC");
        }
    }

    @Test
    void testGenerationPeepholeOptimization() {
        // --- 目标：验证在生成阶段直接移除 Assign 并折叠为 ReturnStatement ---
        {
            Assign assign = new Assign(
                    Collections.singletonList(new Name("R4", new SourcePos(0, 1))),
                    Collections.singletonList(new NilConst(new SourcePos(0, 1))),
                    new SourcePos(0, 1)
            );
            ReturnStatement ret = new ReturnStatement(
                    Collections.singletonList(new NilConst(new SourcePos(1, 2))),
                    new SourcePos(1, 2)
            );
            Block block = new Block(null);
            block.statements.add(assign);

            Chunk chunk = new Chunk();
            chunk.setFunction("peephole_constant_return");
            chunk.addInstruction(new Instruction(0, (4 << 6) | (4 << 23), Opcode.LOADNIL)); // A=4, B=4
            chunk.addInstruction(new Instruction(1, (4 << 6) | (2 << 23), Opcode.RETURN));  // A=4, B=2

            Register register = new Register();
            CodeGeneratorContext context = new CodeGeneratorContext(chunk, register);
            // Mock a pipeline and converter
            LuaCodeGenerator generator = new LuaCodeGenerator(chunk);
            InstructionHandler handler = new InstructionHandler(generator, context);
            DecompilerPipeline pipeline = new DecompilerPipeline(generator, handler);
            pipeline.processChunk(chunk);
            InstructionToASTConverter converter = new InstructionToASTConverter(chunk, pipeline);

            // 运行生成阶段优化
            boolean optimized = converter.tryOptimizeAssignReturn(block, ret, 1);
            assertTrue(optimized, "Generation peephole should succeed for LOADNIL + RETURN");
            assertEquals(1, block.statements.size(), "Assign should be removed and Return added");
            Statement resultStmt = block.statements.get(0);
            assertTrue(resultStmt instanceof ReturnStatement);
            ReturnStatement resultRet = (ReturnStatement) resultStmt;
            assertEquals(1, resultRet.values.size());
            assertTrue(resultRet.values.get(0) instanceof NilConst);
        }

        // --- 负例：如果 assign 是 LabelPC，不应该被折叠 ---
        {
            Assign assign = new Assign(
                    Collections.singletonList(new Name("R4", new SourcePos(0, 1))),
                    Collections.singletonList(new NilConst(new SourcePos(0, 1))),
                    new SourcePos(0, 1)
            );
            ReturnStatement ret = new ReturnStatement(
                    Collections.singletonList(new NilConst(new SourcePos(1, 2))),
                    new SourcePos(1, 2)
            );
            Block block = new Block(null);
            block.statements.add(assign);

            Chunk chunk = new Chunk();
            chunk.setFunction("peephole_label_return");
            chunk.addInstruction(new Instruction(0, (4 << 6) | (4 << 23), Opcode.LOADNIL)); // A=4, B=4
            chunk.addInstruction(new Instruction(1, (4 << 6) | (2 << 23), Opcode.RETURN));  // A=4, B=2

            Register register = new Register();
            CodeGeneratorContext context = new CodeGeneratorContext(chunk, register);
            context.addLabelPC(0); // 设置 assign 为 LabelPC
            
            LuaCodeGenerator generator = new LuaCodeGenerator(chunk);
            InstructionHandler handler = new InstructionHandler(generator, context);
            DecompilerPipeline pipeline = new DecompilerPipeline(generator, handler);
            pipeline.processChunk(chunk);
            InstructionToASTConverter converter = new InstructionToASTConverter(chunk, pipeline);

            boolean optimized = converter.tryOptimizeAssignReturn(block, ret, 1);
            assertFalse(optimized, "Should not optimize if assign is LabelPC");
            assertEquals(1, block.statements.size());
            assertTrue(block.statements.get(0) instanceof Assign);
        }
    }

    @Test
    void testDataFlowDoesNotMergeRegisterLifetimesAcrossIfCondition() {
        SourcePos pos = new SourcePos(0, 0);
        Block body = new Block(pos);

        body.statements.add(new Assign("R3",
                new BinaryOp("+", new Name("R2", pos), new NumberConst(3, pos), pos),
                pos));
        body.statements.add(new Assign("R3",
                new IndexExpr(new Name("a0", pos), new Name("R3", pos), pos),
                pos));

        Block thenBlock = new Block(pos);
        thenBlock.statements.add(new Assign("R3", new StringConst("1", pos), pos));
        Block elseBlock = new Block(pos);
        elseBlock.statements.add(new Assign("R3", new StringConst("0", pos), pos));
        body.statements.add(new IfStatement(
                new Name("R3", pos),
                thenBlock,
                elseBlock,
                pos));

        body.statements.add(new Assign("R4", new StringConst("1", pos), pos));
        body.statements.add(new Assign("R5", new StringConst("0", pos), pos));
        body.statements.add(new Assign("R6", new StringConst("1", pos), pos));
        body.statements.add(new Assign("R3",
                new BinaryOp("..",
                        new BinaryOp("..",
                                new BinaryOp("..", new Name("R3", pos), new Name("R4", pos), pos),
                                new Name("R5", pos),
                                pos),
                        new Name("R6", pos),
                        pos),
                pos));

        Block root = new Block(pos);
        root.statements.add(new Assign("R3", new UnaryOp("#", new Name("a0", pos), pos), pos));
        root.statements.add(new WhileStatement(
                new BinaryOp("<", new Name("R2", pos), new Name("R3", pos), pos),
                body,
                pos));

        new DataFlowAnalyzer().optimize(root);

        String lua = root.accept(new AstPrinter());
        assertFalse(lua.contains("R2 + 3 .."), "Index arithmetic must not be absorbed into concat: \n" + lua);
        assertFalse(lua.contains("R3 = R2 + 3 .."), "R3 index lifetime must not merge with concat key lifetime: \n" + lua);
        assertTrue(lua.contains("if a0[R2 + 3] then")
                        || lua.contains("R3 = a0[R2 + 3]")
                        || lua.contains("R3 = R2 + 3"),
                "The index definition must be preserved or safely inlined into the condition: \n" + lua);
        if (lua.contains("if a0[R3] then")) {
            assertTrue(lua.indexOf("R3 = R2 + 3") < lua.indexOf("if a0[R3] then"),
                    "If R3 is used as an index, its index definition must still be live: \n" + lua);
        }
    }

    @Test
    void testAstPrinterParenthesizesConcatOperandsWhenNeeded() {
        SourcePos pos = new SourcePos(0, 0);
        Expression expr = new BinaryOp("..",
                new BinaryOp("..",
                        new BinaryOp("+", new Name("R2", pos), new NumberConst(3, pos), pos),
                        new Name("R4", pos),
                        pos),
                new Name("R5", pos),
                pos);

        String lua = expr.accept(new AstPrinter());
        assertEquals("((R2 + 3) .. R4) .. R5", lua);
    }

    @Test
    void testAstPrinterAlignsNestedFunctionDeclarationEnd() {
        SourcePos pos = new SourcePos(0, 0);

        Block handlerBody = new Block(pos);
        handlerBody.statements.add(new ReturnStatement(Collections.emptyList(), pos));
        FunctionDeclaration handler = new FunctionDeclaration("a0.filehandler",
                new FunctionLiteral(Collections.emptyList(), false, handlerBody, pos),
                false,
                pos);

        Block initBody = new Block(pos);
        initBody.statements.add(new Assign("R4", new Name("main_0_0", pos), pos));
        initBody.statements.add(handler);
        initBody.statements.add(new Assign("R4", new TableConstructor(Collections.emptyList(), pos), pos));
        FunctionDeclaration init = new FunctionDeclaration("Request.__init__",
                new FunctionLiteral(Collections.singletonList("a0"), false, initBody, pos),
                false,
                pos);

        String lua = init.accept(new AstPrinter());

        assertEquals(String.join("\n",
                "function Request.__init__(a0)",
                "    R4 = main_0_0",
                "    function a0.filehandler()",
                "        return",
                "    end",
                "    R4 = {}",
                "end"), lua);
    }

    @Test
    void testAstPrinterFormatsAnonymousFunctionExpressionIndentation() {
        SourcePos pos = new SourcePos(0, 0);
        Block body = new Block(pos);
        body.statements.add(new ReturnStatement(Collections.emptyList(), pos));

        Block root = new Block(pos);
        root.statements.add(new Assign("R1",
                new FunctionLiteral(Collections.emptyList(), false, body, pos),
                pos));
        root.statements.add(new Assign("R2", new NumberConst(1, pos), pos));

        String lua = root.accept(new AstPrinter());

        assertEquals(String.join("\n",
                "R1 = function()",
                "    return",
                "end",
                "R2 = 1",
                ""), lua);
    }

    @Test
    void testDataFlowDeletesOnlyPureDeadTemporaryAssignments() {
        SourcePos pos = new SourcePos(0, 0);
        Block root = new Block(pos);

        root.statements.add(new Assign("R7",
                new BinaryOp("+", new NumberConst(1, pos), new NumberConst(2, pos), pos),
                pos));
        root.statements.add(new Assign("R8",
                new FunctionCall(new Name("work", pos), Collections.emptyList(), false, pos),
                pos));
        root.statements.add(new Assign("R9",
                new IndexExpr(new Name("tbl", pos), new StringConst("field", pos), pos),
                pos));

        new DataFlowAnalyzer().optimize(root);

        String lua = root.accept(new AstPrinter());
        assertFalse(lua.contains("R7 ="), "Pure dead temporary assignment should be removed: \n" + lua);
        assertTrue(lua.contains("R8 = work()"), "FunctionCall RHS must be preserved: \n" + lua);
        assertTrue(lua.contains("R9 = tbl.field"), "IndexExpr RHS is not assumed pure: \n" + lua);
    }

    @Test
    void testDataFlowKeepsNestedAssignmentThatMayReachOuterUse() {
        SourcePos pos = new SourcePos(0, 0);
        Block thenBlock = new Block(pos);
        thenBlock.statements.add(new Assign("R1", new NumberConst(1, pos), pos));

        Block root = new Block(pos);
        root.statements.add(new IfStatement(new Name("cond", pos), thenBlock, null, pos));
        root.statements.add(new ExpressionStatement(
                new FunctionCall(new Name("sink", pos), Collections.singletonList(new Name("R1", pos)), false, pos),
                pos));

        new DataFlowAnalyzer().optimize(root);

        String lua = root.accept(new AstPrinter());
        assertTrue(lua.contains("R1 = 1"), "Nested definition may reach code after the branch: \n" + lua);
        assertTrue(lua.contains("sink(R1)"), "Outer use must remain tied to the temporary: \n" + lua);
    }

    @Test
    void testDataFlowUsesSsaDefinitionPcForCapturedRegisterProtection() {
        Block root = new Block(new SourcePos(0, 0));
        root.statements.add(new Assign("module_R2", new StringConst("string", new SourcePos(4, 0)),
                new SourcePos(4, 0)));
        root.statements.add(new Assign("module_R2", new StringConst("table", new SourcePos(8, 0)),
                new SourcePos(8, 0)));

        Set<String> upvalues = new HashSet<>();
        upvalues.add("module_R2");
        Map<String, Set<Integer>> protectedDefs = new HashMap<>();
        protectedDefs.put("module_R2", new HashSet<>(Collections.singletonList(8)));

        new DataFlowAnalyzer().optimize(root, Collections.emptySet(), upvalues, protectedDefs);

        String lua = root.accept(new AstPrinter());
        assertFalse(lua.contains("module_R2 = \"string\""),
                "Earlier SSA version should not be protected by a later closure capture: \n" + lua);
        assertTrue(lua.contains("module_R2 = \"table\""),
                "Captured reaching definition must remain protected: \n" + lua);
    }

    @Test
    void testDataFlowDeletesDeadDefinitionKilledBeforeLaterLabel() {
        SourcePos pos = new SourcePos(0, 0);
        Block root = new Block(pos);
        root.statements.add(new Assign("R4", new Name("main_0_0", pos), pos));
        root.statements.add(new Assign(new IndexExpr(new Name("a0", pos), new StringConst("filehandler", pos), pos),
                new Name("main_0_0", pos),
                pos));
        root.statements.add(new Assign("R4", new TableConstructor(Collections.emptyList(), pos), pos));
        root.statements.add(new LabelStatement("L14", pos));
        root.statements.add(new Assign(new IndexExpr(new Name("R4", pos), new StringConst("env", pos), pos),
                new Name("a1", pos),
                pos));

        new DataFlowAnalyzer().optimize(root);

        String lua = root.accept(new AstPrinter());
        assertFalse(lua.contains("R4 = main_0_0"),
                "Dead definition killed before the later label should be deleted: \n" + lua);
        assertTrue(lua.contains("R4 = {}"), "Later live R4 definition must remain: \n" + lua);
    }

    @Test
    void testDataFlowDeletesTemporariesAfterSsaOperandSubstitution() {
        SourcePos pos = new SourcePos(0, 0);
        Block root = new Block(pos);
        root.statements.add(new Assign("R4", new StringConst("/userdisk/upload.tmp", pos), pos));
        root.statements.add(new Assign("R3",
                new FunctionCall(new MemberExpr(new Name("io", pos), "open", pos),
                        java.util.Arrays.asList(new StringConst("/userdisk/upload.tmp", pos), new StringConst("w", pos)),
                        false,
                        pos),
                pos));
        root.statements.add(new Assign("R0", new Name("R3", pos), pos));
        root.statements.add(new Assign("R4", new MemberExpr(new Name("a0", pos), "file", pos), pos));
        root.statements.add(new Assign("R4", new Name("R4", pos), pos));
        root.statements.add(new Assign("R3",
                new FunctionCall(new MemberExpr(new Name("string", pos), "gsub", pos),
                        java.util.Arrays.asList(new Name("R4", pos), new StringConst("+", pos), new StringConst(" ", pos)),
                        false,
                        pos),
                pos));

        new DataFlowAnalyzer().optimize(root);

        String lua = root.accept(new AstPrinter());
        assertFalse(lua.contains("R4 = \"/userdisk/upload.tmp\""),
                "Dead LOADK temporary already substituted into call args should be deleted: \n" + lua);
        assertFalse(lua.contains("R4 = R4"), "Self-copy temporary should be deleted: \n" + lua);
        assertTrue(lua.contains("io.open(\"/userdisk/upload.tmp\", \"w\")"),
                "Call with substituted constant must remain: \n" + lua);
    }

    @Test
    void testDataFlowDeletesNestedTemporariesKilledBeforeBlockExit() {
        SourcePos pos = new SourcePos(0, 0);
        Block thenBlock = new Block(pos);
        thenBlock.statements.add(new Assign("R4", new StringConst("/userdisk/upload.tmp", pos), pos));
        thenBlock.statements.add(new Assign("R3",
                new FunctionCall(new MemberExpr(new Name("io", pos), "open", pos),
                        java.util.Arrays.asList(new StringConst("/userdisk/upload.tmp", pos), new StringConst("w", pos)),
                        false,
                        pos),
                pos));
        thenBlock.statements.add(new Assign("R0", new Name("R3", pos), pos));
        thenBlock.statements.add(new Assign("R4", new MemberExpr(new Name("a0", pos), "file", pos), pos));

        Block root = new Block(pos);
        root.statements.add(new IfStatement(new Name("cond", pos), thenBlock, null, pos));

        new DataFlowAnalyzer().optimize(root);

        String lua = root.accept(new AstPrinter());
        assertFalse(lua.contains("R4 = \"/userdisk/upload.tmp\""),
                "Nested dead temporary killed before block exit should be deleted: \n" + lua);
        assertTrue(lua.contains("R4 = a0.file"), "Later reaching definition must remain: \n" + lua);
    }

    @Test
    void testDataFlowKeepsDeadLookingDefinitionWhenLabelPrecedesKill() {
        SourcePos pos = new SourcePos(0, 0);
        Block root = new Block(pos);
        root.statements.add(new Assign("R4", new Name("main_0_0", pos), pos));
        root.statements.add(new LabelStatement("L14", pos));
        root.statements.add(new Assign("R4", new TableConstructor(Collections.emptyList(), pos), pos));

        new DataFlowAnalyzer().optimize(root);

        String lua = root.accept(new AstPrinter());
        assertTrue(lua.contains("R4 = main_0_0"),
                "A label before the killing definition means control flow may reach the old definition: \n" + lua);
    }

    private void verifyPeephole(Statement assign, Statement ret, List<Instruction> instructions, List<Constant> constants, int expectedSize, Class<? extends Expression> expectedValueClass) {
        Block block = new Block(null);
        block.statements.add(assign);
        block.statements.add(ret);

        Chunk chunk = new Chunk();
        for (Instruction inst : instructions) {
            chunk.addInstruction(inst);
        }
        if (constants != null) {
            for (Constant c : constants) {
                chunk.getConstants().add(c);
            }
        }

        Register register = new Register();
        CodeGeneratorContext context = new CodeGeneratorContext(chunk, register);

        new AstCleanupPass().optimizeReturnPatterns(block, context, Collections.emptySet());

        assertEquals(expectedSize, block.statements.size());
        if (expectedSize == 1) {
            Statement resultStmt = block.statements.get(0);
            assertTrue(resultStmt instanceof ReturnStatement);
            ReturnStatement resultRet = (ReturnStatement) resultStmt;
            assertEquals(1, resultRet.values.size());
            assertTrue(expectedValueClass.isInstance(resultRet.values.get(0)));
        }
    }

    private int indexOfPattern(String lua, String regex) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(lua);
        return matcher.find() ? matcher.start() : -1;
    }
}
