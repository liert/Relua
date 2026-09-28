package com.github.relua.decompiler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.github.relua.ast.AstNode;
import com.github.relua.ast.AstPrinter;
import com.github.relua.ast.Block;
import com.github.relua.ast.Expression;
import com.github.relua.ast.ExpressionStatement;
import com.github.relua.ast.Statement;
import com.github.relua.debug.DecompilerDebugger;
import com.github.relua.decompiler.pipeline.PipelineDebugListener;
import com.github.relua.model.Chunk;
import com.github.relua.model.FromType;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;
import com.github.relua.model.UpValue;
import com.github.relua.log.Logger;
import com.github.relua.util.RegisterNamePolicy;

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
        emitAst(chunk, context, handler, Collections.emptySet());
    }

    public void emitAst(Chunk chunk, CodeGeneratorContext context, InstructionHandler handler, Set<String> parentDeclared) {
        Set<String> upvalueNames = new HashSet<>();
        Set<Integer> usedUpvalueIndices = new HashSet<>();
        if (chunk.getInstructions() != null) {
            for (Instruction ins : chunk.getInstructions()) {
                if (ins != null) {
                    if (ins.getOpcode() == Opcode.GETUPVAL || ins.getOpcode() == Opcode.SETUPVAL) {
                        usedUpvalueIndices.add(ins.getB());
                    }
                }
            }
        }
        for (int idx : usedUpvalueIndices) {
            UpValue uv = context.getUpvalue(idx);
            if (uv != null) {
                String uvName = getResolvedUpvalueName(uv, idx);
                if (uvName != null) {
                    upvalueNames.add(uvName);
                }
            }
        }
        Logger.debug("[DEBUG] Chunk " + chunk.getFunction() + " usedUpvalueIndices: " + usedUpvalueIndices + " -> upvalueNames: " + upvalueNames);
        emitAst(chunk, context, handler, parentDeclared, upvalueNames);
    }

    public void emitAst(Chunk chunk, CodeGeneratorContext context, InstructionHandler handler, Set<String> parentDeclared, Set<String> upvalueNames) {
        // 获取指令处理器生成的AST
        AstNode ast = handler.generateASTFromChunk(chunk);

        for (PipelineDebugListener listener : handler.getPipeline().getGenerator().getDebugListeners()) {
            listener.onAstConstructed(chunk, ast);
        }

        // 如果生成的是Block，直接设置为上下文的astBlock
        if (ast instanceof Block) {
            Block block = (Block) ast;
            Set<String> declared = new AstCleanupPass().cleanup(block, context, parentDeclared, upvalueNames, handler.getPipeline());
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
            Set<String> declared = new AstCleanupPass().cleanup(block, context, parentDeclared, upvalueNames, handler.getPipeline());
            context.setDeclaredVariables(declared);
            context.setAstBlock(block);
        }
    }

    private String getResolvedUpvalueName(UpValue upvalue, int b) {
        if (upvalue != null
                && (upvalue.getFromType() == FromType.CONSTANT || upvalue.getFromType() == FromType.GLOBAL)
                && upvalue.getValue() != null) {
            Object val = upvalue.getValue();
            if (upvalue.getFromType() == FromType.GLOBAL) {
                String globalName = val.toString();
                if (!RegisterNamePolicy.isTemporaryRegisterName(globalName)) {
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
