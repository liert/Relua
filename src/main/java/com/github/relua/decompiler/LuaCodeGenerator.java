package com.github.relua.decompiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.relua.ast.Assign;
import com.github.relua.ast.AstPrinter;
import com.github.relua.ast.Block;
import com.github.relua.ast.Expression;
import com.github.relua.ast.FunctionDeclaration;
import com.github.relua.ast.FunctionLiteral;
import com.github.relua.ast.GlobalAssign;
import com.github.relua.ast.IndexExpr;
import com.github.relua.ast.MemberExpr;
import com.github.relua.ast.Name;
import com.github.relua.ast.Statement;
import com.github.relua.ast.StringConst;
import com.github.relua.log.Logger;
import com.github.relua.model.Chunk;
import com.github.relua.model.CodeLine;
import com.github.relua.model.FromType;
import com.github.relua.model.Register;
import com.github.relua.model.ValueType;
import com.github.relua.util.RegisterNamePolicy;

/**
 * Lua代码生成器，作为总控类，负责协调各个代码生成器
 */
public class LuaCodeGenerator {
    // private InstructionHandler instructionHandler;
    private AstCodeEmitter astCodeEmitter;
    private List<CodeGeneratorContext> contexts = new ArrayList<>();
    private Map<String, InstructionHandler> handlers = new HashMap<>();

    /**
     * 构造函数
     * 
     * @param codeGenContext 代码生成上下文
     */
    public LuaCodeGenerator(Chunk chunk) {
        this.astCodeEmitter = new AstCodeEmitter();

        // 初始化所有代码块上下文
        initializeContexts(chunk);
    }

    /**
     * 初始化代码块的上下文
     * 
     * @param chunk
     * @param register
     * @return
     */
    private void initializeContexts(Chunk chunk) {
        Register register = new Register();
        
        String prefix = "";
        if ("main".equals(chunk.getFunction())) {
            prefix = hasModuleCall(chunk) ? "module_" : "chunk_";
        }
        register.setVarPrefix(prefix);

        for (int i = 0; i < chunk.getNumParams(); i++) {
            Register.RegisterEntity entity = register.getRegisterEntity(i);
            entity.setCustomName("a" + i);
            entity.setValue("a" + i);
            entity.setType(ValueType.OBJECT);
            entity.setFromType(FromType.GLOBAL);
        }
        CodeGeneratorContext context = new CodeGeneratorContext(chunk, register);
        contexts.add(context);
        handlers.put(chunk.getFunction(), new InstructionHandler(this, context));
        for (Chunk subChunk : chunk.getSubChunks()) {
            initializeContexts(subChunk);
        }
    }

    private boolean hasModuleCall(Chunk mainChunk) {
        if (mainChunk == null || mainChunk.getConstants() == null) {
            return false;
        }
        for (com.github.relua.model.Constant c : mainChunk.getConstants()) {
            Object val = c.getValue();
            if (val != null) {
                String s = val.toString();
                if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
                    s = s.substring(1, s.length() - 1);
                }
                if ("module".equals(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 生成Lua代码
     * 
     * @param chunk 代码块
     * @return 生成的Lua代码
     */
    public String generate(Chunk chunk, Register register) {
        return generate(chunk, register, java.util.Collections.emptySet());
    }

    public String generate(Chunk chunk, Register register, Set<String> parentDeclared) {
        // 创建代码生成上下文
        Logger.debug("当前处理的Chunk函数名: " + chunk.getFunction());
        InstructionHandler handler = handlers.get(chunk.getFunction());
        CodeGeneratorContext context = handler.getContext();

        // System.out.println("=== 开始处理Chunk ===");
        // System.out.println("Chunk信息: lineDefined=" + chunk.getLineDefined() + ", lastLineDefined="
        //         + chunk.getLastLineDefined() + ", numParams=" + chunk.getNumParams() + ", isVararg="
        //         + chunk.getIsVararg() + ", maxStackSize=" + chunk.getMaxStackSize());

        // 先让指令处理器处理代码块，建立控制流和变量映射
        handler.process(chunk);

        // 生成代码块头部信息
        if (chunk.getFunction().equals("main")) {
            // System.out.println("生成代码块头部信息...");
            generateChunkHeader(chunk, context);
        }

        // 生成指令代码（使用AST）
        // System.out.println("生成AST代码...");
        astCodeEmitter.emitAst(chunk, context, handler, parentDeclared);

        // 关闭所有未结束的控制流结构
        // System.out.println("关闭所有未结束的控制流结构...");
        context.closeAllControlFlow();

        // System.out.println("=== Chunk处理完成 ===");

        for (Chunk subChunk : chunk.getSubChunks()) {
            Register temp = new Register();
            for (int i = 0; i < subChunk.getNumParams(); i++) {
                Register.RegisterEntity entity = temp.getRegisterEntity(i);
                entity.setCustomName("a" + i);
                entity.setValue("a" + i);
                entity.setType(ValueType.OBJECT);
                entity.setFromType(FromType.GLOBAL);
            }
            // 嵌套子闭包，合并当前的 parentDeclared 和本层 context 中新声明的变量，传给子闭包
            Set<String> mergedParentDeclared = new HashSet<>(parentDeclared);
            mergedParentDeclared.addAll(context.getDeclaredVariables());
            generate(subChunk, temp, mergedParentDeclared);
        }

        if (chunk.getFunction().equals("main")) {
            Map<String, CodeGeneratorContext> contextByFunction = new HashMap<>();
            for (CodeGeneratorContext ctx : contexts) {
                contextByFunction.put(ctx.getChunk().getFunction(), ctx);
            }

            Set<String> emittedFunctions = new HashSet<>();
            for (CodeGeneratorContext ctx : contexts) {
                inlineClosureDeclarations(ctx.getAstBlock(), contextByFunction, emittedFunctions);
            }

            // 为main函数创建一个主Block
            Block mainBlock = new Block(null);
            
            // 收集所有拉平输出的全局函数声明，并将它们放在 main 之前
            List<Statement> globalDecls = new ArrayList<>();
            for (CodeGeneratorContext ctx : contexts) {
                Chunk ctxChunk = ctx.getChunk();
                String funcName = ctxChunk.getFunction();
                
                if (funcName.equals("main")) {
                    continue;
                } else if (emittedFunctions.contains(funcName)) {
                    continue;
                } else {
                    // 对于被拉平输出的全局函数，因为它们已经不嵌套在父闭包内部，不需要防 shadowed，重新 cleanup
                    new AstCleanupPass().cleanup(ctx.getAstBlock(), ctx, java.util.Collections.emptySet());
                    
                    FunctionLiteral funcLit = createFunctionLiteral(ctx);
                    
                    // 创建FunctionDeclaration
                    FunctionDeclaration funcDecl = new FunctionDeclaration(funcName, funcLit, false, null);
                    globalDecls.add(funcDecl);
                }
            }
            
            // 优先添加拉平输出的全局函数声明
            mainBlock.statements.addAll(globalDecls);
            
            // 接着添加main函数的内容
            for (CodeGeneratorContext ctx : contexts) {
                if (ctx.getChunk().getFunction().equals("main")) {
                    mainBlock.statements.addAll(ctx.getAstBlock().statements);
                }
            }

            if (com.github.relua.debug.DecompilerDebugger.isEnabled()) {
                com.github.relua.debug.DecompilerDebugger.dump("closures_inlined_main", mainBlock);
            }
            
            // 使用AstPrinter生成代码
            AstPrinter printer = new AstPrinter();
            String finalCode = mainBlock.accept(printer);
            if (com.github.relua.debug.DecompilerDebugger.isEnabled()) {
                com.github.relua.debug.DecompilerDebugger.dump("final_code_main", finalCode);
            }
            return finalCode;
        }
        return "";
    }

    private void inlineClosureDeclarations(Block block, Map<String, CodeGeneratorContext> contextByFunction,
            Set<String> emittedFunctions) {
        if (block == null || block.statements == null) {
            return;
        }

        // 第一遍：重写闭包声明，并收集所有已被内联的函数名
        List<Statement> rewritten = new ArrayList<>();
        for (Statement statement : block.statements) {
            FunctionDeclaration functionDeclaration = asClosureDeclaration(statement, contextByFunction);
            if (functionDeclaration != null) {
                emittedFunctions.add(extractClosureName(statement));
                inlineClosureDeclarations(functionDeclaration.func.body, contextByFunction, emittedFunctions);
                rewritten.add(functionDeclaration);
            } else {
                rewritten.add(statement);
            }
        }

        // 第二遍：过滤掉对已被内联的闭包的多余临时寄存器赋值
        List<Statement> finalStatements = new ArrayList<>();
        for (Statement statement : rewritten) {
            if (isTemporaryRegisterClosureAssign(statement, emittedFunctions)) {
                continue;
            }
            finalStatements.add(statement);
        }

        block.statements.clear();
        block.statements.addAll(finalStatements);
    }

    private boolean isTemporaryRegisterClosureAssign(Statement statement, Set<String> emittedFunctions) {
        if (statement instanceof Assign) {
            Assign assign = (Assign) statement;
            if (assign.left.size() == 1 && assign.right.size() == 1) {
                Expression left = assign.left.get(0);
                Expression right = assign.right.get(0);
                if (left instanceof Name && right instanceof Name) {
                    String leftName = ((Name) left).name;
                    String rightName = ((Name) right).name;
                    if (RegisterNamePolicy.isTemporaryRegisterName(leftName) && emittedFunctions.contains(rightName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private FunctionDeclaration asClosureDeclaration(Statement statement,
            Map<String, CodeGeneratorContext> contextByFunction) {
        String targetName = extractClosureTarget(statement);
        String closureName = extractClosureName(statement);
        if (RegisterNamePolicy.isTemporaryRegisterName(targetName) || !contextByFunction.containsKey(closureName)) {
            return null;
        }

        CodeGeneratorContext closureContext = contextByFunction.get(closureName);
        FunctionLiteral literal = createFunctionLiteral(closureContext);
        return new FunctionDeclaration(targetName, literal, false, statement.pos);
    }

    private String extractClosureName(Statement statement) {
        if (statement instanceof Assign) {
            Assign assign = (Assign) statement;
            if (assign.right.size() == 1 && assign.right.get(0) instanceof Name) {
                return ((Name) assign.right.get(0)).name;
            }
        }
        if (statement instanceof GlobalAssign) {
            GlobalAssign assign = (GlobalAssign) statement;
            if (assign.right.size() == 1 && assign.right.get(0) instanceof Name) {
                return ((Name) assign.right.get(0)).name;
            }
        }
        return "";
    }

    private String extractClosureTarget(Statement statement) {
        if (statement instanceof Assign) {
            Assign assign = (Assign) statement;
            if (assign.left.size() == 1) {
                return expressionToFunctionName(assign.left.get(0));
            }
        }
        if (statement instanceof GlobalAssign) {
            GlobalAssign assign = (GlobalAssign) statement;
            if (assign.names.size() == 1) {
                return assign.names.get(0);
            }
        }
        return "";
    }

    private String expressionToFunctionName(Expression expression) {
        if (expression instanceof Name) {
            return ((Name) expression).name;
        }
        if (expression instanceof IndexExpr) {
            IndexExpr indexExpr = (IndexExpr) expression;
            String tableName = expressionToFunctionName(indexExpr.table);
            if (!tableName.isEmpty() && indexExpr.index instanceof StringConst) {
                String key = ((StringConst) indexExpr.index).value;
                if (key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                    return tableName + "." + key;
                }
            }
        }
        if (expression instanceof MemberExpr) {
            MemberExpr memberExpr = (MemberExpr) expression;
            String tableName = expressionToFunctionName(memberExpr.table);
            if (!tableName.isEmpty()) {
                return tableName + "." + memberExpr.member;
            }
        }
        return "";
    }

    private FunctionLiteral createFunctionLiteral(CodeGeneratorContext context) {
        Chunk functionChunk = context.getChunk();
        List<String> params = new ArrayList<>();
        for (int i = 0; i < functionChunk.getNumParams(); i++) {
            params.add(com.github.relua.util.TransformUtils.transformRegister(context.getRegister().getRegisterEntity(i)));
        }
        FunctionLiteral literal = new FunctionLiteral(params, functionChunk.getIsVararg() != 0, context.getAstBlock(), null);
        literal.setChunk(functionChunk);
        return literal;
    }

    /**
     * 生成Lua代码
     * 
     * @param chunk 代码块
     * @return 生成的Lua代码
     */
    // public String generate(Chunk chunk, CodeGeneratorContext context) {
    // // 创建代码生成上下文
    // this.instructionHandler = new InstructionHandler(context);

    // System.out.println("=== 开始处理Chunk ===");
    // System.out.println("Chunk信息: lineDefined=" + chunk.getLineDefined() + ",
    // lastLineDefined="
    // + chunk.getLastLineDefined() + ", numParams=" + chunk.getNumParams() + ",
    // isVararg="
    // + chunk.getIsVararg() + ", maxStackSize=" + chunk.getMaxStackSize());

    // // 先让指令处理器处理代码块，建立控制流和变量映射
    // instructionHandler.process(chunk);

    // // 生成代码块头部信息
    // if (chunk.getFunction().equals("main")) {
    // System.out.println("生成代码块头部信息...");
    // generateChunkHeader(chunk, context);
    // }

    // // 生成指令代码（使用AST）
    // System.out.println("生成AST代码...");
    // astCodeEmitter.emitAst(chunk, context, instructionHandler);

    // // 关闭所有未结束的控制流结构
    // // System.out.println("关闭所有未结束的控制流结构...");
    // context.closeAllControlFlow();

    // System.out.println("=== Chunk处理完成 ===");

    // contexts.add(context);
    // return "";
    // }

    /**
     * 生成代码块头部信息
     * 
     * @param chunk   代码块
     * @param context 代码生成上下文
     */
    private void generateChunkHeader(Chunk chunk, CodeGeneratorContext context) {
        // 对于主代码块，添加一些元信息注释
        context.addCodeLine("-- Decompiled Lua code", CodeLine.CodeType.COMMENT);
        context.addCodeLine("-- Generated by Relua", CodeLine.CodeType.COMMENT);
        context.addEmptyLine();
    }

    public InstructionHandler getInstructionHandler(String function) {
        return handlers.get(function);
    }
}
