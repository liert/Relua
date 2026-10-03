package com.github.relua.decompiler;

import org.junit.jupiter.api.Test;
import java.io.File;
import com.github.relua.model.LuacFile;
import com.github.relua.parser.LuacParser;

/**
 * Decompiler 测试用例
 */
class DecompilerTest {

    @Test
    void testDecompileTraffic() throws Exception {
        com.github.relua.log.LogConfig cfg = new com.github.relua.log.LogConfig();
        cfg.setLogLevel(com.github.relua.log.LogLevel.DEBUG);
        cfg.setFileOutput(true);
        cfg.setLogFilePath("target/debug.log");
        com.github.relua.log.Logger.init(cfg);
        File file = new File("src/test/resources/xiaomi/traffic.lua");
        if (file.exists()) {
            LuacParser parser = new LuacParser();
            LuacFile luacFile = parser.parse(file.getAbsolutePath());
            Decompiler decompiler = new Decompiler();
            String code = decompiler.decompile(luacFile);

            org.junit.jupiter.api.Assertions.assertTrue(
                code.contains("if chunk_R2[a0] and chunk_R2[a0].nickname ~= \"\" then"),
                "Should recover compound 'and' condition for hostname nickname check"
            );
            org.junit.jupiter.api.Assertions.assertTrue(
                code.contains("return R1_5 == \"\" and a0 or R1_5"),
                "Should recover ternary short-circuit expression for fallback to a0"
            );
        }
    }

    @Test
    void testDecompileAllResources() throws Exception {
        String[] files = {
            "src/test/resources/tplink/dispatcher.lua",
            "src/test/resources/tplink/version.lua",
            "src/test/resources/xiaomi/aeslua.lua",
            "src/test/resources/xiaomi/cgi.lua",
            "src/test/resources/xiaomi/http.lua",
            "src/test/resources/xiaomi/sha1.lua",
            "src/test/resources/xiaomi/traffic.lua",
            "src/test/resources/xiaomi/xqdatacenter.lua",
            "src/test/resources/xiaomi/XQSecureUtil.lua",
            "src/test/resources/xiaomi/xqsystem.lua"
        };
        LuacParser parser = new LuacParser();
        for (String filePath : files) {
            File f = new File(filePath);
            if (!f.exists()) {
                continue;
            }
            LuacFile luac = parser.parse(f.getAbsolutePath());
            Decompiler decompiler = new Decompiler();
            String code = decompiler.decompile(luac);
            org.junit.jupiter.api.Assertions.assertNotNull(code, "Decompiled code should not be null for " + filePath);
            org.junit.jupiter.api.Assertions.assertFalse(code.isEmpty(), "Decompiled code should not be empty for " + filePath);
        }
    }
}
