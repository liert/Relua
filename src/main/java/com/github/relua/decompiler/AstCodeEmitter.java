package com.github.relua.decompiler;

import com.github.relua.ast.AstNode;
import com.github.relua.ast.AstPrinter;
import com.github.relua.ast.Block;
import com.github.relua.ast.Expression;
import com.github.relua.ast.ExpressionStatement;
import com.github.relua.ast.Statement;
import com.github.relua.model.Chunk;
import com.github.relua.log.Logger;

/**
 * AST代码生成器，负责将AST转换为Lua代码
 */
public class AstCodeEmitter {
    /**
     * 生成AST代码
     * 
     * @param chunk   代码块
     * @param context 代码生成上下文
     * @param handler 指令处理器
     */
    public void emitAst(Chunk chunk, CodeGeneratorContext context, InstructionHandler handler) {
        emitAst(chunk, context, handler, java.util.Collections.emptySet());
    }

    public void emitAst(Chunk chunk, CodeGeneratorContext context, InstructionHandler handler,
            java.util.Set<String> parentDeclared) {
        java.util.Set<String> upvalueNames = new java.util.HashSet<>();
        java.util.Set<Integer> usedUpvalueIndices = new java.util.HashSet<>();
        if (chunk.getInstructions() != null) {
            for (com.github.relua.model.Instruction ins : chunk.getInstructions()) {
                if (ins != null) {
                    if (ins.getOpcode() == com.github.relua.model.Opcode.GETUPVAL
                            || ins.getOpcode() == com.github.relua.model.Opcode.SETUPVAL) {
                        usedUpvalueIndices.add(ins.getB());
                    }
                }
            }
        }
        for (int idx : usedUpvalueIndices) {
            com.github.relua.model.UpValue uv = context.getUpvalue(idx);
            if (uv != null) {
                String uvName = getResolvedUpvalueName(uv, idx);
                if (uvName != null) {
                    upvalueNames.add(uvName);
                }
            }
        }
        Logger.debug("[DEBUG] Chunk " + chunk.getFunction() + " usedUpvalueIndices: " + usedUpvalueIndices
                + " -> upvalueNames: " + upvalueNames);
        emitAst(chunk, context, handler, parentDeclared, upvalueNames);
    }

    public void emitAst(Chunk chunk, CodeGeneratorContext context, InstructionHandler handler,
            java.util.Set<String> parentDeclared, java.util.Set<String> upvalueNames) {
        // 获取指令处理器生成的AST
        AstNode ast = handler.generateASTFromChunk(chunk);

        if (com.github.relua.debug.DecompilerDebugger.isEnabled()) {
            com.github.relua.debug.DecompilerDebugger.dump("ast_constructed_" + chunk.getFunction(), ast);
        }

        // 如果生成的是Block，直接设置为上下文的astBlock
        if (ast instanceof Block) {
            Block block = (Block) ast;
            java.util.Set<String> declared = new AstCleanupPass().cleanup(block, context, parentDeclared, upvalueNames);
            context.setDeclaredVariables(declared);
            context.setAstBlock(block);
        } else {
            // 否则，创建一个新的Block，并将AST添加到其中
            Block block = new Block(null);
            if (ast instanceof Statement) {
                block.statements.add((Statement) ast);
            } else if (ast instanceof Expression) {
                Expression expr = (Expression) ast;
                block.statements.add(new ExpressionStatement(expr, expr.pos));
            }
            java.util.Set<String> declared = new AstCleanupPass().cleanup(block, context, parentDeclared, upvalueNames);
            context.setDeclaredVariables(declared);
            context.setAstBlock(block);
        }
    }

    private String getResolvedUpvalueName(com.github.relua.model.UpValue upvalue, int b) {
        if (upvalue != null && (upvalue.getFromType() == com.github.relua.model.FromType.CONSTANT || upvalue.getFromType() == com.github.relua.model.FromType.GLOBAL) && upvalue.getValue() != null) {
            Object val = upvalue.getValue();
            if (upvalue.getFromType() == com.github.relua.model.FromType.GLOBAL) {
                String globalName = val.toString();
                if (!globalName.matches("^(chunk_|module_)?R\\d+$")) {
                    if (globalName.contains(".")) {
                        return globalName.split("\\.")[0];
                    }
                    return globalName;
                }
            }
        }
        return (upvalue != null) ? upvalue.getName() : ("upvalue_" + b);
    }
}
