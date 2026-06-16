package com.github.relua;

import com.github.relua.model.LuacFile;
import com.github.relua.parser.LuacParser;
import com.github.relua.decompiler.Decompiler;
import com.github.relua.log.Logger;
import com.github.relua.log.LogConfig;
import com.github.relua.log.LogLevel;

import java.io.FileWriter;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Relua主程序入口
 */
public class Relua {
    private static final String VERSION = "1.0.0";
    
    /**
     * 主函数
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 初始化日志系统（默认WARNING级别，不输出到控制台或文件）
        initLogger(LogLevel.WARNING, Boolean.getBoolean("relua.logToConsole"), false, "logs/relua.log");
        
        if (args.length == 0) {
            showHelp();
            return;
        }
        
        java.util.List<String> inputFiles = new java.util.ArrayList<>();
        String outputFile = null;
        boolean showVersion = false;
        boolean showHelp = false;
        boolean showBytecode = false;
        boolean debugMode = false;
        LogLevel logLevel = LogLevel.WARNING;
        boolean logToConsole = Boolean.getBoolean("relua.logToConsole");
        boolean fileOutput = false;
        String logFilePath = "logs/relua.log";
        boolean debugOutput = Boolean.getBoolean("relua.debugOutput");
        
        // 解析命令行参数
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-v":
                case "--version":
                    showVersion = true;
                    break;
                case "-h":
                case "--help":
                    showHelp = true;
                    break;
                case "-o":
                case "--output":
                    if (i + 1 < args.length) {
                        outputFile = args[++i];
                    } else {
                        Logger.error("Missing output file path");
                        System.exit(1);
                    }
                    break;
                case "-b":
                case "--bytecode":
                    showBytecode = true;
                    break;
                case "--debug":
                    debugMode = true;
                    break;
                case "-l":
                case "--log":
                    if (i + 1 < args.length) {
                        String levelStr = args[++i];
                        logLevel = LogLevel.fromString(levelStr);
                        logToConsole = true; // 指定了日志级别，默认开启控制台输出
                        if (logLevel == LogLevel.DEBUG || logLevel == LogLevel.INFO) {
                            debugOutput = true; // DEBUG/INFO 级别自动开启 debugOutput
                        }
                    } else {
                        Logger.error("Missing log level");
                        System.exit(1);
                    }
                    break;
                case "-f":
                case "--log-file":
                    if (i + 1 < args.length) {
                        logFilePath = args[++i];
                        fileOutput = true;
                        logToConsole = false; // 指定了日志文件，默认关闭控制台日志输出
                    } else {
                        Logger.error("Missing log file path");
                        System.exit(1);
                    }
                    break;
                default:
                    if (arg.startsWith("-")) {
                        Logger.error("Unknown option: " + arg);
                        showHelp();
                        System.exit(1);
                    } else {
                        inputFiles.add(arg);
                    }
                    break;
            }
        }
        
        // 处理命令行选项
        if (showVersion) {
            showVersion();
            return;
        }
        
        if (showHelp) {
            showHelp();
            return;
        }
        
        if (inputFiles.isEmpty()) {
            Logger.error("No input file specified");
            showHelp();
            System.exit(1);
        }

        if (outputFile != null && inputFiles.size() > 1) {
            Logger.error("Cannot specify --output (-o) when decompiling multiple files");
            System.exit(1);
        }

        // 应用解析后的日志配置与调试输出配置
        System.setProperty("relua.debugOutput", String.valueOf(debugOutput || debugMode));
        initLogger(logLevel, logToConsole, fileOutput, logFilePath);
        
        boolean hasError = false;
        for (String inputFile : inputFiles) {
            com.github.relua.debug.DecompilerDebugger debugger = null;
            if (debugMode) {
                try {
                    java.io.File file = new java.io.File(inputFile);
                    String baseName = file.getName();
                    int dotIndex = baseName.lastIndexOf('.');
                    String nameWithoutExt = dotIndex > 0 ? baseName.substring(0, dotIndex) : baseName;
                    String debugDir = "debug_output/" + nameWithoutExt;
                    
                    debugger = new com.github.relua.debug.DecompilerDebugger(debugDir, inputFile);
                    com.github.relua.debug.DecompilerDebugger.set(debugger);
                } catch (Exception e) {
                    Logger.error("Failed to initialize debug output directory for " + inputFile + ": " + e.getMessage());
                }
            }
            
            long fileStartTime = System.currentTimeMillis();
            try {
                final String finalInputFile = inputFile;
                final boolean finalShowBytecode = showBytecode;
                // 执行反编译
                String luaCode = runWithCapturedStdout(() -> decompileFile(finalInputFile, finalShowBytecode));
                
                // 输出结果
                if (outputFile != null) {
                    writeToFile(outputFile, luaCode);
                    Logger.info("Successfully decompiled " + inputFile + " to " + outputFile);
                } else {
                    System.out.println(luaCode); // 直接输出反编译结果，不经过日志
                }
            } catch (Exception e) {
                System.err.println("Error decompiling " + inputFile + ": " + e.getMessage());
                hasError = true;
                if (debugger != null) {
                    debugger.recordStageManual("decompilation_failed", fileStartTime, System.currentTimeMillis(), "ERROR", e.getMessage());
                }
            } finally {
                if (debugger != null) {
                    try {
                        debugger.writeSummaryLog();
                    } catch (Exception e) {
                        Logger.error("Failed to write summary log for " + inputFile + ": " + e.getMessage());
                    }
                    com.github.relua.debug.DecompilerDebugger.clear();
                }
            }
        }

        if (hasError && inputFiles.size() == 1) {
            System.exit(1);
        }
    }
    
    /**
     * 初始化日志系统
     */
    private static void initLogger(LogLevel level, boolean logToConsole, boolean fileOutput, String logFilePath) {
        LogConfig config = new LogConfig();
        config.setLogLevel(level);
        config.setConsoleOutput(logToConsole);
        config.setFileOutput(fileOutput);
        config.setLogFilePath(logFilePath);
        config.setMaxFileSize(10 * 1024 * 1024); // 10MB
        config.setMaxBackupFiles(5);
        Logger.init(config);
    }

    private static String runWithCapturedStdout(DecompileAction action) throws Exception {
        PrintStream originalOut = System.out;
        if (Boolean.getBoolean("relua.debugOutput")) {
            try {
                System.setOut(new PrintStream(new LoggerPrintStream(originalOut)));
                return action.run();
            } finally {
                System.setOut(originalOut);
            }
        }

        try {
            System.setOut(new PrintStream(new ByteArrayOutputStream()));
            return action.run();
        } finally {
            System.setOut(originalOut);
        }
    }

    private static class LoggerPrintStream extends java.io.OutputStream {
        private final StringBuilder buffer = new StringBuilder();
        private final PrintStream originalOut;

        public LoggerPrintStream(PrintStream originalOut) {
            this.originalOut = originalOut;
        }

        @Override
        public void write(int b) {
            if (b == '\n') {
                flushLine();
            } else if (b != '\r') {
                buffer.append((char) b);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) {
            for (int i = 0; i < len; i++) {
                write(b[off + i]);
            }
        }

        private void flushLine() {
            if (buffer.length() > 0) {
                String line = buffer.toString();
                buffer.setLength(0);
                if (isCalledFromLogger()) {
                    originalOut.println(line);
                } else {
                    Logger.debug(line);
                }
            }
        }

        private boolean isCalledFromLogger() {
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                if (element.getClassName().startsWith("com.github.relua.log.")) {
                    return true;
                }
            }
            return false;
        }
    }

    @FunctionalInterface
    private interface DecompileAction {
        String run() throws Exception;
    }
    
    /**
     * 反编译Luac文件
     * @param inputFile 输入Luac文件路径
     * @param showBytecode 是否显示字节码
     * @return 反编译后的Lua代码
     * @throws IOException IO异常
     */
    private static String decompileFile(String inputFile, boolean showBytecode) throws IOException {
        // 创建解析器和反编译器
        LuacParser parser = new LuacParser();
        Decompiler decompiler = new Decompiler();
        
        // 解析Luac文件
        Logger.info("Parsing " + inputFile + "...");
        LuacFile luacFile = parser.parse(inputFile);
        
        if (com.github.relua.debug.DecompilerDebugger.isEnabled() && luacFile != null && luacFile.getMainChunk() != null) {
            com.github.relua.debug.DecompilerDebugger.dump("bytecode_parsed", decompiler.generateBytecode(luacFile.getMainChunk()));
        }

        // 反编译
        Logger.info("Decompiling...");
        return decompiler.decompile(luacFile, showBytecode);
    }
    
    /**
     * 将内容写入文件
     * @param filePath 文件路径
     * @param content 内容
     * @throws IOException IO异常
     */
    private static void writeToFile(String filePath, String content) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.print(content);
        }
    }
    
    /**
     * 显示版本信息
     */
    private static void showVersion() {
        System.out.println("Relua " + VERSION);
        System.out.println("A Lua bytecode decompiler for OpenWRT");
    }
    
    /**
     * 显示帮助信息
     */
    private static void showHelp() {
        System.out.println("Usage: relua [OPTIONS] INPUT_FILES...");
        System.out.println("Decompile Lua bytecode files to readable Lua code");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -o, --output FILE    Write output to FILE instead of stdout (only for single input file)");
        System.out.println("  -b, --bytecode       Show bytecode instructions in output");
        System.out.println("  --debug              Enable intermediate stages dumping for debugging");
        System.out.println("  -l, --log LEVEL      Set log level (DEBUG, INFO, WARNING, ERROR, CRITICAL)");
        System.out.println("  -f, --log-file FILE  Write logs to FILE");
        System.out.println("  -v, --version        Show version information");
        System.out.println("  -h, --help           Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  relua file.luac              Decompile file.luac to stdout");
        System.out.println("  relua -o file.lua file.luac  Decompile file.luac to file.lua");
        System.out.println("  relua --debug file.luac      Decompile with debug stages dumped to debug_output/file/");
        System.out.println("  relua -b file.luac           Show bytecode instructions");
        System.out.println("  relua -l DEBUG file.luac     Decompile file.luac with DEBUG logs");
        System.out.println("  relua -l DEBUG -f my.log file.luac   Decompile with DEBUG logs redirected to my.log");
    }
}
