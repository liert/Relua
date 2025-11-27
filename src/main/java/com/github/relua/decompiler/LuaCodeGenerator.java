package com.github.relua.decompiler;

import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Constant;

import java.util.List;

/**
 * Lua代码生成器
 */
public class LuaCodeGenerator {
    private static final String INDENT = "    "; // 4个空格缩进
    
    /**
     * 生成Lua代码
     * @param chunk 代码块
     * @return 生成的Lua代码
     */
    public String generate(Chunk chunk) {
        StringBuilder sb = new StringBuilder();
        
        // 生成代码块头部信息
        generateChunkHeader(chunk, sb);
        
        // 生成指令代码
        generateInstructions(chunk, sb, 0);
        
        // 生成子代码块
        generateSubChunks(chunk, sb, 0);
        
        return sb.toString();
    }
    
    /**
     * 生成代码块头部信息
     * @param chunk 代码块
     * @param sb 字符串构建器
     */
    private void generateChunkHeader(Chunk chunk, StringBuilder sb) {
        // 对于主代码块，不需要特殊头部
        // 对于子代码块，会在CLOSURE指令处理时生成函数定义
    }
    
    /**
     * 生成指令代码
     * @param chunk 代码块
     * @param sb 字符串构建器
     * @param indentLevel 缩进级别
     */
    private void generateInstructions(Chunk chunk, StringBuilder sb, int indentLevel) {
        List<Instruction> instructions = chunk.getInstructions();
        List<Constant> constants = chunk.getConstants();
        
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            
            // 添加缩进
            for (int j = 0; j < indentLevel; j++) {
                sb.append(INDENT);
            }
            
            // 生成指令对应的Lua代码
            generateInstructionCode(chunk, instruction, i, sb);
            sb.append("\n");
        }
    }
    
    /**
     * 生成单个指令的Lua代码
     * @param chunk 代码块
     * @param instruction 指令
     * @param index 指令索引
     * @param sb 字符串构建器
     */
    private void generateInstructionCode(Chunk chunk, Instruction instruction, int index, StringBuilder sb) {
        switch (instruction.getOpcode()) {
            case LOADK:
                // 加载常量到寄存器
                int a = instruction.getA();
                int bx = instruction.getBx();
                Constant constant = chunk.getConstant(bx);
                if (constant != null) {
                    sb.append(String.format("R%d = %s", a, constant.toString()));
                } else {
                    sb.append(String.format("R%d = nil -- Unknown constant index %d", a, bx));
                }
                break;
            case LOADBOOL:
                // 加载布尔值到寄存器
                a = instruction.getA();
                boolean boolValue = instruction.getB() != 0;
                sb.append(String.format("R%d = %b", a, boolValue));
                break;
            case LOADNIL:
                // 加载nil到寄存器
                a = instruction.getA();
                int b = instruction.getB();
                for (int i = a; i <= b; i++) {
                    if (i > a) sb.append("; ");
                    sb.append(String.format("R%d = nil", i));
                }
                break;
            case MOVE:
                // 寄存器间数据移动
                a = instruction.getA();
                b = instruction.getB();
                sb.append(String.format("R%d = R%d", a, b));
                break;
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
            case POW:
                // 算术运算
                generateArithmeticCode(chunk, instruction, sb);
                break;
            case UNM:
            case NOT:
            case LEN:
                // 一元运算
                generateUnaryCode(chunk, instruction, sb);
                break;
            case GETGLOBAL:
                // 获取全局变量
                a = instruction.getA();
                bx = instruction.getBx();
                constant = chunk.getConstant(bx);
                if (constant != null) {
                    sb.append(String.format("R%d = %s", a, constant.getValue()));
                } else {
                    sb.append(String.format("R%d = nil -- Unknown global variable index %d", a, bx));
                }
                break;
            case SETGLOBAL:
                // 设置全局变量
                a = instruction.getA();
                bx = instruction.getBx();
                constant = chunk.getConstant(bx);
                if (constant != null) {
                    sb.append(String.format("%s = R%d", constant.getValue(), a));
                } else {
                    sb.append(String.format("-- Unknown global variable index %d = R%d", bx, a));
                }
                break;
            case CALL:
                // 函数调用
                a = instruction.getA();
                b = instruction.getB();
                int c = instruction.getC();
                sb.append(String.format("R%d = R%d(...) -- CALL: a=%d, b=%d, c=%d", a, a, a, b, c));
                break;
            case RETURN:
                // 返回
                a = instruction.getA();
                b = instruction.getB();
                if (b == 0) {
                    sb.append("return");
                } else if (b == 1) {
                    sb.append(String.format("return R%d", a));
                } else {
                    sb.append("return ");
                    for (int i = a; i < a + b; i++) {
                        if (i > a) sb.append(", ");
                        sb.append(String.format("R%d", i));
                    }
                }
                break;
            default:
                // 其他指令，暂时输出指令信息
                sb.append(String.format("-- %s: A=%d, B=%d, C=%d, Bx=%d, sBx=%d",
                        instruction.getOpcode().name(),
                        instruction.getA(),
                        instruction.getB(),
                        instruction.getC(),
                        instruction.getBx(),
                        instruction.getSBx()));
                break;
        }
    }
    
    /**
     * 生成算术运算代码
     * @param chunk 代码块
     * @param instruction 指令
     * @param sb 字符串构建器
     */
    private void generateArithmeticCode(Chunk chunk, Instruction instruction, StringBuilder sb) {
        int a = instruction.getA();
        int b = instruction.getB();
        int c = instruction.getC();
        String op = getArithmeticOperator(instruction.getOpcode());
        
        sb.append(String.format("R%d = R%d %s R%d", a, b, op, c));
    }
    
    /**
     * 生成一元运算代码
     * @param chunk 代码块
     * @param instruction 指令
     * @param sb 字符串构建器
     */
    private void generateUnaryCode(Chunk chunk, Instruction instruction, StringBuilder sb) {
        int a = instruction.getA();
        int b = instruction.getB();
        String op = getUnaryOperator(instruction.getOpcode());
        
        sb.append(String.format("R%d = %s R%d", a, op, b));
    }
    
    /**
     * 获取算术运算符
     * @param opcode 操作码
     * @return 运算符字符串
     */
    private String getArithmeticOperator(Instruction.Opcode opcode) {
        switch (opcode) {
            case ADD: return "+";
            case SUB: return "-";
            case MUL: return "*";
            case DIV: return "/";
            case MOD: return "%";
            case POW: return "^";
            default: return "?";
        }
    }
    
    /**
     * 获取一元运算符
     * @param opcode 操作码
     * @return 运算符字符串
     */
    private String getUnaryOperator(Instruction.Opcode opcode) {
        switch (opcode) {
            case UNM: return "-";
            case NOT: return "not ";
            case LEN: return "#";
            default: return "?";
        }
    }
    
    /**
     * 生成子代码块
     * @param chunk 代码块
     * @param sb 字符串构建器
     * @param indentLevel 缩进级别
     */
    private void generateSubChunks(Chunk chunk, StringBuilder sb, int indentLevel) {
        List<Chunk> subChunks = chunk.getSubChunks();
        
        if (!subChunks.isEmpty()) {
            sb.append("\n");
            for (int j = 0; j < indentLevel; j++) {
                sb.append(INDENT);
            }
            sb.append("-- Sub-chunks (functions):\n");
            
            for (int i = 0; i < subChunks.size(); i++) {
                Chunk subChunk = subChunks.get(i);
                
                for (int j = 0; j < indentLevel; j++) {
                    sb.append(INDENT);
                }
                sb.append(String.format("-- Sub-chunk %d:\n", i + 1));
                
                generateChunkHeader(subChunk, sb);
                generateInstructions(subChunk, sb, indentLevel + 1);
                generateSubChunks(subChunk, sb, indentLevel + 1);
            }
        }
    }
}