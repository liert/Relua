package com.github.relua.debug;

import com.github.relua.ast.AstNode;
import com.github.relua.ast.AstPrinter;
import com.github.relua.ast.Block;
import com.github.relua.log.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 反编译器调试器，负责输出反编译各阶段的中间产物与汇总日志
 */
public class DecompilerDebugger {
    private static final ThreadLocal<DecompilerDebugger> INSTANCE = new ThreadLocal<>();

    private final String debugDir;
    private final String inputFileName;
    private final List<StageInfo> stages = new ArrayList<>();
    private final long startTime;
    private int stageCounter = 0;

    public static class StageInfo {
        public final int index;
        public final String name;
        public final long start;
        public long end;
        public String status = "SUCCESS";
        public String errorMsg = "";

        public StageInfo(int index, String name, long start) {
            this.index = index;
            this.name = name;
            this.start = start;
        }

        public long getDuration() {
            return end - start;
        }
    }

    public DecompilerDebugger(String debugDir, String inputFileName) {
        this.debugDir = debugDir;
        this.inputFileName = inputFileName;
        this.startTime = System.currentTimeMillis();

        // 确保调试输出目录存在
        File dir = new File(debugDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static void set(DecompilerDebugger debugger) {
        INSTANCE.set(debugger);
    }

    public static DecompilerDebugger get() {
        return INSTANCE.get();
    }

    public static void clear() {
        INSTANCE.remove();
    }

    public static boolean isEnabled() {
        return INSTANCE.get() != null;
    }

    public static void dump(String stageName, Object data) {
        DecompilerDebugger debugger = get();
        if (debugger != null) {
            debugger.writeStage(stageName, data);
        }
    }

    public synchronized void writeStage(String stageName, Object data) {
        long now = System.currentTimeMillis();
        stageCounter++;
        StageInfo info = new StageInfo(stageCounter, stageName, now);
        stages.add(info);

        // 清理文件名中的非法字符
        String safeStageName = stageName.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
        String fileName = String.format("%02d_%s", stageCounter, safeStageName);
        String suffix = ".txt";
        if (data instanceof Block || data instanceof AstNode) {
            suffix = ".lua";
        }

        File file = new File(debugDir, fileName + suffix);
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            if (data instanceof Block || data instanceof AstNode) {
                AstPrinter printer = new AstPrinter();
                String printed = ((AstNode) data).accept(printer);
                writer.print(printed);
            } else if (data != null) {
                writer.print(data.toString());
            } else {
                writer.print("Empty result");
            }
            info.end = System.currentTimeMillis();
        } catch (Exception e) {
            info.end = System.currentTimeMillis();
            info.status = "ERROR";
            info.errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            Logger.error("Failed to write debug stage " + stageName + ": " + e.getMessage());
        }
    }

    public synchronized void recordStageManual(String stageName, long start, long end, String status, String errorMsg) {
        stageCounter++;
        StageInfo info = new StageInfo(stageCounter, stageName, start);
        info.end = end;
        info.status = status;
        info.errorMsg = errorMsg;
        stages.add(info);
    }

    public synchronized void writeSummaryLog() {
        File file = new File(debugDir, "decompile.log");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("================================================================================");
            writer.println("Decompilation Summary Log");
            writer.println("Input File: " + inputFileName);
            writer.println("Start Time: " + sdf.format(new Date(startTime)));
            writer.println("================================================================================");
            writer.printf("%-3s | %-35s | %-12s | %-12s | %-8s | %s\n",
                    "No.", "Stage Name", "Start Offset", "Duration(ms)", "Status", "Details/Errors");
            writer.println("--------------------------------------------------------------------------------");
            for (StageInfo info : stages) {
                writer.printf("%02d  | %-35s | %-12s | %-12d | %-8s | %s\n",
                        info.index,
                        info.name,
                        String.format("+%dms", info.start - startTime),
                        info.getDuration(),
                        info.status,
                        info.errorMsg
                );
            }
            writer.println("================================================================================");
            writer.println("Total Duration: " + (System.currentTimeMillis() - startTime) + " ms");
            writer.println("================================================================================");
        } catch (IOException e) {
            Logger.error("Failed to write summary log: " + e.getMessage());
        }
    }
}
