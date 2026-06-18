package com.github.relua.decompiler.ssa;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.github.relua.decompiler.Decompiler;
import com.github.relua.model.LuacFile;
import com.github.relua.parser.LuacParser;

class SsaXiaomiFixtureTest {
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
                .getPipeline().getSsaExpressionAnalysis(chunk.getFunction());
        assertNotNull(analysis,
                "missing SSA expression analysis for " + chunk.getFunction());
        if (!chunk.getInstructions().isEmpty()) {
            assertFalse(analysis.getAnalyzedInstructionCount() == 0,
                    "no instructions analyzed for " + chunk.getFunction());
        }
        for (com.github.relua.model.Chunk subChunk : chunk.getSubChunks()) {
            assertSsaExpressionAnalysisExists(decompiler, subChunk);
        }
    }
}
