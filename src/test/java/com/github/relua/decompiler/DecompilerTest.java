package com.github.relua.decompiler;

import com.github.relua.model.LuacFile;
import com.github.relua.parser.LuacParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import com.github.relua.log.Logger;
import com.github.relua.log.LogConfig;
import com.github.relua.log.LogLevel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

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
        String luaCode = decompiler.decompile(luacFile);
        assertNotNull(luaCode, "Failed to decompile");
        assertFalse(luaCode.isEmpty(), "Decompiled code is empty");

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
    void testDecompileHttp() throws IOException {
        String filePath = "src/test/resources/xiaomi/http.lua";
        File file = new File(filePath);
        assertTrue(file.exists(), "http.lua file does not exist: " + file.getAbsolutePath());

        LuacParser parser = new LuacParser();
        Decompiler decompiler = new Decompiler();

        LuacFile luacFile = parser.parse(filePath);
        assertNotNull(luacFile, "Failed to parse http.lua");

        String luaCode = decompiler.decompile(luacFile);
        assertNotNull(luaCode, "Failed to decompile");

        try (PrintWriter writer = new PrintWriter(new FileWriter("target/http_decompiled.lua"))) {
            writer.print(luaCode);
        }
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
            chunk.addInstruction(new Instruction(0, (4 << 6) | (4 << 23), Opcode.LOADNIL)); // A=4, B=4
            chunk.addInstruction(new Instruction(1, (4 << 6) | (2 << 23), Opcode.RETURN));  // A=4, B=2

            Register register = new Register();
            CodeGeneratorContext context = new CodeGeneratorContext(chunk, register);
            // Mock a pipeline and converter
            LuaCodeGenerator generator = new LuaCodeGenerator(chunk);
            InstructionHandler handler = new InstructionHandler(generator, context);
            DecompilerPipeline pipeline = new DecompilerPipeline(generator, handler);
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
            chunk.addInstruction(new Instruction(0, (4 << 6) | (4 << 23), Opcode.LOADNIL)); // A=4, B=4
            chunk.addInstruction(new Instruction(1, (4 << 6) | (2 << 23), Opcode.RETURN));  // A=4, B=2

            Register register = new Register();
            CodeGeneratorContext context = new CodeGeneratorContext(chunk, register);
            context.addLabelPC(0); // 设置 assign 为 LabelPC
            
            LuaCodeGenerator generator = new LuaCodeGenerator(chunk);
            InstructionHandler handler = new InstructionHandler(generator, context);
            DecompilerPipeline pipeline = new DecompilerPipeline(generator, handler);
            InstructionToASTConverter converter = new InstructionToASTConverter(chunk, pipeline);

            boolean optimized = converter.tryOptimizeAssignReturn(block, ret, 1);
            assertFalse(optimized, "Should not optimize if assign is LabelPC");
            assertEquals(1, block.statements.size());
            assertTrue(block.statements.get(0) instanceof Assign);
        }
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
}