package com.github.relua.decompiler;

import com.github.relua.ast.AstNode;
import com.github.relua.ast.AstPrinter;
import com.github.relua.model.Chunk;

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

    public void emitAst(Chunk chunk, CodeGeneratorContext context, InstructionHandler handler, java.util.Set<String> parentDeclared) {
        java.util.Set<String> upvalueNames = new java.util.HashSet<>();
        java.util.Set<Integer> usedUpvalueIndices = new java.util.HashSet<>();
        if (chunk.getInstructions() != null) {
            for (com.github.relua.model.Instruction ins : chunk.getInstructions()) {
                if (ins != null) {
                    if (ins.getOpcode() == com.github.relua.model.Opcode.GETUPVAL || ins.getOpcode() == com.github.relua.model.Opcode.SETUPVAL) {
                        usedUpvalueIndices.add(ins.getB());
                    }
                }
            }
        }
        for (int idx : usedUpvalueIndices) {
            com.github.relua.model.UpValue uv = context.getUpvalue(idx);
            if (uv != null && uv.getName() != null) {
                upvalueNames.add(uv.getName());
            }
        }
        System.out.println("[DEBUG] Chunk " + chunk.getFunction() + " usedUpvalueIndices: " + usedUpvalueIndices + " -> upvalueNames: " + upvalueNames);
        emitAst(chunk, context, handler, parentDeclared, upvalueNames);
    }

    public void emitAst(Chunk chunk, CodeGeneratorContext context, InstructionHandler handler, java.util.Set<String> parentDeclared, java.util.Set<String> upvalueNames) {
        // 获取指令处理器生成的AST
        AstNode ast = handler.generateASTFromChunk(chunk);

        // 如果生成的是Block，直接设置为上下文的astBlock
        if (ast instanceof com.github.relua.ast.Block) {
            com.github.relua.ast.Block block = (com.github.relua.ast.Block) ast;
            java.util.Set<String> declared = new AstCleanupPass().cleanup(block, parentDeclared, upvalueNames);
            context.setDeclaredVariables(declared);
            context.setAstBlock(block);
        } else {
            // 否则，创建一个新的Block，并将AST添加到其中
            com.github.relua.ast.Block block = new com.github.relua.ast.Block(null);
            if (ast instanceof com.github.relua.ast.Statement) {
                block.statements.add((com.github.relua.ast.Statement) ast);
            } else if (ast instanceof com.github.relua.ast.Expression) {
                com.github.relua.ast.Expression expr = (com.github.relua.ast.Expression) ast;
                block.statements.add(new com.github.relua.ast.ExpressionStatement(expr, expr.pos));
            }
            java.util.Set<String> declared = new AstCleanupPass().cleanup(block, parentDeclared, upvalueNames);
            context.setDeclaredVariables(declared);
            context.setAstBlock(block);
        }
    }
}
