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
        // 获取指令处理器生成的AST
        AstNode ast = handler.generateASTFromChunk(chunk);

        // 如果生成的是Block，直接设置为上下文的astBlock
        if (ast instanceof com.github.relua.ast.Block) {
            com.github.relua.ast.Block block = (com.github.relua.ast.Block) ast;
            new AstCleanupPass().cleanup(block);
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
            new AstCleanupPass().cleanup(block);
            context.setAstBlock(block);
        }
    }
}
