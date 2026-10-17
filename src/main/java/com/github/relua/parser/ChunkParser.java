package com.github.relua.parser;

import com.github.relua.log.Logger;
import com.github.relua.model.Chunk;
import com.github.relua.model.Constant;
import com.github.relua.model.Instruction;
import com.github.relua.model.LocalVar;

import java.io.EOFException;
import java.io.IOException;

/**
 * 代码块解析器
 */
public class ChunkParser {
    private BinaryReader reader;
    private BytecodeFormatProfile formatProfile;

    /**
     * 构造函数
     * @param reader 二进制读取器
     */
    public ChunkParser(BinaryReader reader) {
        this(reader, new StandardLua51FormatProfile());
    }

    public ChunkParser(BinaryReader reader, BytecodeFormatProfile formatProfile) {
        this.reader = reader;
        this.formatProfile = formatProfile == null ? new StandardLua51FormatProfile() : formatProfile;
    }

    private static final java.util.concurrent.atomic.AtomicInteger PARSE_CALL_COUNT = new java.util.concurrent.atomic.AtomicInteger(0);
    private static final ThreadLocal<java.util.concurrent.atomic.AtomicInteger> THREAD_PARSE_CALL_COUNT =
            ThreadLocal.withInitial(java.util.concurrent.atomic.AtomicInteger::new);
    private static final ThreadLocal<String> CURRENT_PARSING_FILE = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_TRIGGER_REASON = new ThreadLocal<>();

    public static int getParseCallCount() {
        return PARSE_CALL_COUNT.get();
    }

    public static void resetParseCallCount() {
        PARSE_CALL_COUNT.set(0);
    }

    public static int getThreadParseCallCount() {
        return THREAD_PARSE_CALL_COUNT.get().get();
    }

    public static void resetThreadParseCallCount() {
        THREAD_PARSE_CALL_COUNT.get().set(0);
    }

    public static String getCurrentParsingFile() {
        return CURRENT_PARSING_FILE.get();
    }

    public static String getCurrentTriggerReason() {
        return CURRENT_TRIGGER_REASON.get();
    }

    public static void setParsingContext(String file, String reason) {
        CURRENT_PARSING_FILE.set(file);
        CURRENT_TRIGGER_REASON.set(reason);
    }

    public static void clearParsingContext() {
        CURRENT_PARSING_FILE.remove();
        CURRENT_TRIGGER_REASON.remove();
    }

    /**
     * 支持 try-with-resources 的解析上下文作用域
     */
    public static class ParsingScope implements AutoCloseable {
        private final String prevFile;
        private final String prevReason;

        public ParsingScope(String file, String reason) {
            this.prevFile = CURRENT_PARSING_FILE.get();
            this.prevReason = CURRENT_TRIGGER_REASON.get();
            CURRENT_PARSING_FILE.set(file);
            CURRENT_TRIGGER_REASON.set(reason);
        }

        @Override
        public void close() {
            if (prevFile != null) {
                CURRENT_PARSING_FILE.set(prevFile);
            } else {
                CURRENT_PARSING_FILE.remove();
            }
            if (prevReason != null) {
                CURRENT_TRIGGER_REASON.set(prevReason);
            } else {
                CURRENT_TRIGGER_REASON.remove();
            }
        }
    }

    /**
     * 解析代码块
     * @return 解析后的代码块
     * @throws IOException IO异常
     */
    public Chunk parse(String function) throws IOException {
        Chunk chunk = new Chunk();
        chunk.setFunction(function);
        PARSE_CALL_COUNT.incrementAndGet();
        THREAD_PARSE_CALL_COUNT.get().incrementAndGet();
        
        formatProfile.parseChunkHeader(reader, chunk);
        String ctxFile = CURRENT_PARSING_FILE.get();
        String ctxReason = CURRENT_TRIGGER_REASON.get();
        String logPrefix = (ctxFile != null ? "[" + ctxFile + (ctxReason != null ? " (" + ctxReason + ")" : "") + "] " : "");
        Logger.info(logPrefix + String.format("源文件索引: %d, 起始行号: %d, 结束行号: %d", chunk.getSource(), chunk.getLineDefined(), chunk.getLastLineDefined()));
        Logger.info(logPrefix + String.format("上值数量: %d, 固定参数数量: %d, 是否可变参数: %s, 最大栈大小: %d",
                chunk.getNup(), chunk.getNumParams(), chunk.getIsVararg() == 1 ? "是" : "否", chunk.getMaxStackSize()));
        
        // 解析指令列表
        parseInstructions(chunk);
        
        // 解析常量表
        parseConstants(chunk);
        
        // 解析子代码块
        parseSubChunks(chunk);
        
        // 解析行号表
        parseLineNumbers(chunk);

        // 解析局部变量表
        parseLocalVars(chunk);

        // 解析Upvalue名称表
        parseUpvalueNames();

        return chunk;
    }

    /**
     * 解析指令列表
     * @param chunk 代码块
     * @throws IOException IO异常
     */
    private void parseInstructions(Chunk chunk) throws IOException {
        try {
            // 读取指令数量，使用4字节int
            int instructionCount = reader.readInt();
            
            for (int i = 0; i < instructionCount; i++) {
                // 读取指令，使用4字节int
                int code = reader.readInt();
                Instruction instruction = formatProfile.decodeInstruction(i, code);
                chunk.addInstruction(instruction);
            }
        } catch (EOFException e) {
            // 处理文件末尾异常，可能是解析逻辑与实际文件格式不匹配
            System.err.println("Warning: Reached end of file while parsing instructions. This may be due to incompatible Luac file format.");
        }
    }

    /**
     * 解析常量表
     * @param chunk 代码块
     * @throws IOException IO异常
     */
    private void parseConstants(Chunk chunk) throws IOException {
        try {
            int constantCount = reader.readInt();
            
            for (int i = 0; i < constantCount; i++) {
                try {
                    byte type = reader.readByte();
                    Constant constant = formatProfile.parseConstant(type, reader);
                    chunk.addConstant(constant);
                } catch (EOFException e) {
                    System.err.println("Warning: Reached end of file while parsing constant " + i + ".");
                    break;
                }
            }
        } catch (EOFException e) {
            System.err.println("Warning: Reached end of file while parsing constant count.");
        }
    }

    /**
     * 解析子代码块
     * @param chunk 代码块
     * @throws IOException IO异常
     */
    private void parseSubChunks(Chunk chunk) throws IOException {
        try {
            int subChunkCount = reader.readInt();
            
            for (int i = 0; i < subChunkCount; i++) {
                try {
                    String ctxFile = CURRENT_PARSING_FILE.get();
                    String ctxReason = CURRENT_TRIGGER_REASON.get();
                    String logPrefix = (ctxFile != null ? "[" + ctxFile + (ctxReason != null ? " (" + ctxReason + ")" : "") + "] " : "");
                    Logger.info(logPrefix + String.format("子代码块 [%d]", i));
                    Chunk subChunk = parse(chunk.getFunction() + "_" + i);
                    chunk.addSubChunk(subChunk);
                } catch (EOFException e) {
                    System.err.println("Warning: Reached end of file while parsing sub-chunk " + i + ".");
                    break;
                }
            }
        } catch (EOFException e) {
            System.err.println("Warning: Reached end of file while parsing sub-chunk count.");
        }
    }

    /**
     * 解析局部变量表
     * @param chunk 代码块
     * @throws IOException IO异常
     */
    private void parseLocalVars(Chunk chunk) throws IOException {
        try {
            int localVarCount = reader.readInt();
            
            for (int i = 0; i < localVarCount; i++) {
                try {
                    String name = formatProfile.readString(reader);
                    int startPC = reader.readInt();
                    int endPC = reader.readInt();
                    
                    LocalVar localVar = new LocalVar(name, startPC, endPC);
                    chunk.addLocalVar(localVar);
                } catch (EOFException e) {
                    System.err.println("Warning: Reached end of file while parsing local variable " + i + ".");
                    break;
                }
            }
        } catch (EOFException e) {
            System.err.println("Warning: Reached end of file while parsing local variable count.");
        }
    }

    /**
     * 解析行号表
     * @param chunk 代码块
     * @throws IOException IO异常
     */
    private void parseLineNumbers(Chunk chunk) throws IOException {
        try {
            int lineNumberCount = reader.readInt();
            
            for (int i = 0; i < lineNumberCount; i++) {
                try {
                    int lineNumber = reader.readInt();
                    chunk.addLineNumber(lineNumber);
                } catch (EOFException e) {
                    System.err.println("Warning: Reached end of file while parsing line number " + i + ".");
                    break;
                }
            }
        } catch (EOFException e) {
            System.err.println("Warning: Reached end of file while parsing line number count.");
        }
    }

    private void parseUpvalueNames() throws IOException {
        try {
            int upvalueNameCount = reader.readInt();
            for (int i = 0; i < upvalueNameCount; i++) {
                try {
                    formatProfile.readString(reader);
                } catch (EOFException e) {
                    System.err.println("Warning: Reached end of file while parsing upvalue name " + i + ".");
                    break;
                }
            }
        } catch (EOFException e) {
            System.err.println("Warning: Reached end of file while parsing upvalue name count.");
        }
    }
}
