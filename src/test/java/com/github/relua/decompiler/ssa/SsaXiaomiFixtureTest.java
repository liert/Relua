package com.github.relua.decompiler.ssa;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.github.relua.decompiler.Decompiler;
import com.github.relua.model.LuacFile;
import com.github.relua.parser.LuacParser;

class SsaXiaomiFixtureTest {
    private static final Pattern SELF_COPY_TEMPORARY = Pattern
            .compile("(?m)^\\s*(?:local\\s+)?(R\\d+)\\s*=\\s*\\1\\s*$");

    @Test
    void verifiesSsaForAllXiaomiFixturesDuringDecompilation() throws Exception {
        File dir = new File("src/test/resources/xiaomi");
        File[] fixtures = dir.listFiles((parent, name) -> name.endsWith(".lua"));
        assertNotNull(fixtures, "xiaomi fixture directory must be readable");
        assertFalse(fixtures.length == 0, "xiaomi fixture directory must contain lua bytecode fixtures");

        LuacParser parser = new LuacParser();
        for (File fixture : fixtures) {
            LuacFile luacFile = parser.parse(fixture.getPath());
            assertNotNull(luacFile, "failed to parse " + fixture.getName());

            String lua = new Decompiler().decompile(luacFile);
            assertNotNull(lua, "failed to decompile " + fixture.getName());
            assertFalse(lua.isEmpty(), "empty decompilation for " + fixture.getName());
            assertFalse(lua.contains("nil:"), "SSA loop values must not lower to nil method calls in "
                    + fixture.getName());
            assertFalse(lua.contains("tostring(nil)"), "SSA loop values must not lower to nil tostring calls in "
                    + fixture.getName());
            assertFalse(lua.contains("a0:byte)"), "SSA call results must not lower back to method references in "
                    + fixture.getName());
            assertFalse(lua.contains("[nil]"), "unused SSA table reads must not leave nil-index artifacts in "
                    + fixture.getName());
            assertFalse(SELF_COPY_TEMPORARY.matcher(lua).find(),
                    "SSA use-def cleanup must delete self-copy temporaries in " + fixture.getName());
            if ("xqdatacenter.lua".equals(fixture.getName())) {
                assertFalse(lua.contains("R4 = \"/userdisk/upload.tmp\""),
                        "SSA def-use cleanup must delete substituted upload path temporaries in "
                                + fixture.getName());
                assertFalse(lua.contains("R5 = \"\\r\\n\"\n                R6 = \"\\n\"\n                R3 = R4:gsub"),
                        "SSA/AST cleanup must delete constants whose call operands were substituted in "
                                + fixture.getName());
            }
            if ("http.lua".equals(fixture.getName())) {
                assertFalse(lua.contains("function main_26_0("),
                        "anonymous closure callbacks must not be emitted as top-level helper functions in "
                                + fixture.getName());
                assertFalse(lua.contains("function main_28_0("),
                        "anonymous closure callbacks must not be emitted as top-level helper functions in "
                                + fixture.getName());
                assertFalse(lua.contains("function main_28_1("),
                        "anonymous closure callbacks must not be emitted as top-level helper functions in "
                                + fixture.getName());
                assertFalse(lua.contains("gsub(\"[\\\"%z\\001-\\031]\", main_"),
                        "anonymous closure callbacks must be lowered as function expressions in "
                                + fixture.getName());
            }
        }
    }

    @Test
    void buildsExpressionSummariesForAllXiaomiChunks() throws Exception {
        File dir = new File("src/test/resources/xiaomi");
        File[] fixtures = dir.listFiles((parent, name) -> name.endsWith(".lua"));
        assertNotNull(fixtures, "xiaomi fixture directory must be readable");

        LuacParser parser = new LuacParser();
        for (File fixture : fixtures) {
            LuacFile luacFile = parser.parse(fixture.getPath());
            assertNotNull(luacFile, "failed to parse " + fixture.getName());

            Decompiler decompiler = new Decompiler();
            String lua = decompiler.decompile(luacFile);
            assertFalse(lua.isEmpty(), "empty decompilation for " + fixture.getName());
            assertSsaExpressionAnalysisExists(decompiler, luacFile.getMainChunk());
        }
    }

    private void assertSsaExpressionAnalysisExists(Decompiler decompiler, com.github.relua.model.Chunk chunk) {
        SsaExpressionAnalysis analysis = decompiler.getCodeGenerator().getInstructionHandler(chunk.getFunction())
                .getPipeline().requireSsaExpressionAnalysis(chunk.getFunction());
        assertNotNull(analysis,
                "missing SSA expression analysis for " + chunk.getFunction());
        for (int pc = 0; pc < chunk.getInstructions().size(); pc++) {
            assertNotNull(decompiler.getCodeGenerator().getInstructionHandler(chunk.getFunction()).getPipeline()
                    .requireSsaInstruction(chunk.getFunction(), pc),
                    "missing SSA instruction for " + chunk.getFunction() + " pc=" + pc);
        }
        if (!chunk.getInstructions().isEmpty()) {
            assertFalse(analysis.getAnalyzedInstructionCount() == 0,
                    "no instructions analyzed for " + chunk.getFunction());
        }
        for (com.github.relua.model.Chunk subChunk : chunk.getSubChunks()) {
            assertSsaExpressionAnalysisExists(decompiler, subChunk);
        }
    }
}
