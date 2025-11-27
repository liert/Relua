package com.github.relua;

import com.github.relua.model.LuacFile;
import com.github.relua.parser.LuacParser;
import com.github.relua.decompiler.Decompiler;

import java.io.FileWriter;
import java.io.IOException;
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
        if (args.length == 0) {
            showHelp();
            return;
        }
        
        String inputFile = null;
        String outputFile = null;
        boolean showVersion = false;
        boolean showHelp = false;
        
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
                        System.err.println("Error: Missing output file path");
                        System.exit(1);
                    }
                    break;
                default:
                    if (arg.startsWith("-")) {
                        System.err.println("Error: Unknown option: " + arg);
                        showHelp();
                        System.exit(1);
                    } else if (inputFile == null) {
                        inputFile = arg;
                    } else {
                        System.err.println("Error: Multiple input files specified");
                        showHelp();
                        System.exit(1);
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
        
        if (inputFile == null) {
            System.err.println("Error: No input file specified");
            showHelp();
            System.exit(1);
        }
        
        try {
            // 执行反编译
            String luaCode = decompileFile(inputFile);
            
            // 输出结果
            if (outputFile != null) {
                writeToFile(outputFile, luaCode);
                System.out.println("Successfully decompiled " + inputFile + " to " + outputFile);
            } else {
                System.out.println(luaCode);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * 反编译Luac文件
     * @param inputFile 输入Luac文件路径
     * @return 反编译后的Lua代码
     * @throws IOException IO异常
     */
    private static String decompileFile(String inputFile) throws IOException {
        // 创建解析器和反编译器
        LuacParser parser = new LuacParser();
        Decompiler decompiler = new Decompiler();
        
        // 解析Luac文件
        System.out.println("Parsing " + inputFile + "...");
        LuacFile luacFile = parser.parse(inputFile);
        
        // 反编译
        System.out.println("Decompiling...");
        return decompiler.decompile(luacFile);
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
        System.out.println("Usage: relua [OPTIONS] INPUT_FILE");
        System.out.println("Decompile Lua bytecode files to readable Lua code");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -o, --output FILE    Write output to FILE instead of stdout");
        System.out.println("  -v, --version        Show version information");
        System.out.println("  -h, --help           Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  relua file.luac              Decompile file.luac to stdout");
        System.out.println("  relua -o file.lua file.luac  Decompile file.luac to file.lua");
    }
}