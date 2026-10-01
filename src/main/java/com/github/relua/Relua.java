package com.github.relua;

import com.github.relua.model.LuacFile;
import com.github.relua.parser.LuacParser;

import javafx.application.Application;

import com.github.relua.debug.DecompilerDebugger;
import com.github.relua.decompiler.Decompiler;
import com.github.relua.gui.ReluaGUI;
import com.github.relua.log.Logger;
import com.github.relua.log.LogConfig;
import com.github.relua.log.LogLevel;
import com.github.relua.util.BytecodeFormatter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Relua主程序入口：默认启动GUI，指定 --nogui 进入命令行模式
 */
public class Relua {
    private static final String VERSION = "1.0.0";

    /**
     * 主函数
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 初始化日志系统
        initLogger(LogLevel.WARNING, Boolean.getBoolean("relua.logToConsole"), false, "logs/relua.log");

        boolean cliMode = false;
        for (String arg : args) {
            switch (arg) {
                case "-v":
                case "--version":
                    showVersion();
                    return;
                case "-h":
                case "--help":
                    showHelp();
                    return;
                case "-l":
                case "--log":
                    // TODO: 后续调整日志系统
                    continue;
                case "--nogui":
                    cliMode = true;
            }
        }

        if (cliMode) {
            runCli(args);
        } else {
            runGui(args);
        }
    }

    /**
     * GUI模式（默认）：启动图形界面并打开命令行指定的文件/文件夹
     * @param args 除去 --nogui 后的命令行参数
     */
    private static void runGui(String[] args) {
        List<String> inputFiles = new ArrayList<>();
        for (String arg : args) {
            switch (arg) {
                case "-l":
                case "--log":
                    System.err.println("Option " + arg + " is only available in command-line mode, please add --nogui");
                    System.exit(1);
                    return;
                default:
                    if (arg.startsWith("-")) {
                        System.err.println("Unknown option: " + arg);
                        showHelp();
                        System.exit(1);
                    } else {
                        inputFiles.add(arg);
                    }
                    break;
            }
        }

        try {
            Application.launch(ReluaGUI.class, inputFiles.toArray(new String[0]));
        } catch (Throwable t) {
            System.err.println("Failed to start GUI: " + t.getMessage());
            System.err.println("If JavaFX is unavailable, use --nogui for command-line mode");
            System.exit(1);
        }
    }

    /**
     * 命令行模式（--nogui）
     * @param args 除去 --nogui 后的命令行参数
     */
    private static void runCli(String[] args) {
        if (args.length == 0) {
            showHelp();
            return;
        }

        List<String> inputFiles = new ArrayList<>();
        String outputFile = null;
        boolean showBytecode = false;
        boolean showPseudocode = false;
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
                case "--nogui":
                    break;
                case "-h":
                case "--help":
                    showHelp();
                    return;
                case "-v":
                case "--version":
                    showVersion();
                    return;
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
                case "-p":
                case "--pseudocode":
                    showPseudocode = true;
                    break;
                case "-a":
                case "--all":
                    showBytecode = true;
                    showPseudocode = true;
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
        
        // 默认行为：若未显式指定输出指令，也未指定输出伪代码，则默认输出伪代码
        if (!showBytecode && !showPseudocode) {
            showPseudocode = true;
        }

        // 处理命令行选项
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
            DecompilerDebugger debugger = null;
            if (debugMode) {
                try {
                    File file = new File(inputFile);
                    String baseName = file.getName();
                    int dotIndex = baseName.lastIndexOf('.');
                    String nameWithoutExt = dotIndex > 0 ? baseName.substring(0, dotIndex) : baseName;
                    String debugDir = "debug_output/" + nameWithoutExt;
                    
                    debugger = new DecompilerDebugger(debugDir, inputFile);
                    DecompilerDebugger.set(debugger);
                } catch (Exception e) {
                    Logger.error("Failed to initialize debug output directory for " + inputFile + ": " + e.getMessage());
                }
            }
            
            long fileStartTime = System.currentTimeMillis();
            try {
                final String finalInputFile = inputFile;
                final boolean finalShowBytecode = showBytecode;
                final boolean finalShowPseudocode = showPseudocode;
                // 执行反编译
                String resultOutput = runWithCapturedStdout(() -> decompileFile(finalInputFile, finalShowBytecode, finalShowPseudocode));
                
                // 输出结果
                if (outputFile != null) {
                    writeToFile(outputFile, resultOutput);
                    Logger.info("Successfully processed " + inputFile + " to " + outputFile);
                } else {
                    System.out.println(resultOutput); // 直接输出结果，不经过日志
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
                    DecompilerDebugger.clear();
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

    private static class LoggerPrintStream extends OutputStream {
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
     * 反编译Luac文件并根据选项返回字节码指令、伪代码或两者
     * @param inputFile 输入Luac文件路径
     * @param showBytecode 是否输出字节码指令
     * @param showPseudocode 是否输出反编译伪代码
     * @return 格式化后的输出内容
     * @throws IOException IO异常
     */
    private static String decompileFile(String inputFile, boolean showBytecode, boolean showPseudocode) throws IOException {
        // 创建解析器和反编译器
        LuacParser parser = new LuacParser();
        Decompiler decompiler = new Decompiler();
        
        // 解析Luac文件
        Logger.info("Parsing " + inputFile + "...");
        LuacFile luacFile = parser.parse(inputFile);

        if (luacFile == null || luacFile.getMainChunk() == null) {
            throw new IOException("Failed to parse Luac file or main chunk is null: " + inputFile);
        }
        
        if (DecompilerDebugger.isEnabled()) {
            DecompilerDebugger.dump("bytecode_parsed", decompiler.generateBytecode(luacFile.getMainChunk()));
        }

        String bytecodeStr = null;
        if (showBytecode) {
            bytecodeStr = BytecodeFormatter.formatLuacFile(luacFile);
        }

        String pseudocodeStr = null;
        if (showPseudocode) {
            // 反编译
            Logger.info("Decompiling...");
            pseudocodeStr = decompiler.decompile(luacFile, false);
        }

        if (showBytecode && showPseudocode) {
            StringBuilder sb = new StringBuilder();
            sb.append(bytecodeStr != null ? bytecodeStr : "");
            if (bytecodeStr != null && !bytecodeStr.endsWith("\n")) {
                sb.append("\n");
            }
            sb.append("\n");
            sb.append("; ====================================================================\n");
            sb.append("; Relua 反编译伪代码\n");
            sb.append("; ====================================================================\n\n");
            sb.append(pseudocodeStr != null ? pseudocodeStr : "");
            return sb.toString();
        } else if (showBytecode) {
            return bytecodeStr != null ? bytecodeStr : "";
        } else {
            return pseudocodeStr != null ? pseudocodeStr : "";
        }
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
    }
    
    /**
     * 显示帮助信息
     */
    private static void showHelp() {
        System.out.println("Usage: relua [OPTIONS] [INPUT_FILES...]");
        System.out.println("Decompile Lua bytecode files to readable Lua code");
        System.out.println();
        System.out.println("GUI mode is the default; pass --nogui to decompile on the command line.");
        System.out.println();
        System.out.println("Output Selection Options:");
        System.out.println("  -p, --pseudocode     Output decompiled Lua pseudocode (default)");
        System.out.println("  -b, --bytecode       Output bytecode instructions / disassembly");
        System.out.println("  -a, --all            Output both bytecode instructions and pseudocode");
        System.out.println();
        System.out.println("General Options:");
        System.out.println("  --nogui              Run in command-line mode instead of launching the GUI");
        System.out.println("  -o, --output FILE    Write output to FILE instead of stdout (only for single input file)");
        System.out.println("  --debug              Enable intermediate stages dumping for debugging");
        System.out.println("  -l, --log LEVEL      Set log level (DEBUG, INFO, WARNING, ERROR, CRITICAL)");
        System.out.println("  -f, --log-file FILE  Write logs to FILE");
        System.out.println("  -v, --version        Show version information");
        System.out.println("  -h, --help           Show this help message");
        System.out.println();
    }
}
