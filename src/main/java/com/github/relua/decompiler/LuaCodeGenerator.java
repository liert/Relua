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
import com.github.relua.ast.BinaryOp;
import com.github.relua.ast.Expression;
import com.github.relua.ast.ExpressionStatement;
import com.github.relua.ast.ForIn;
import com.github.relua.ast.ForNumeric;
import com.github.relua.ast.FunctionDeclaration;
import com.github.relua.ast.FunctionLiteral;
import com.github.relua.ast.FunctionCall;
import com.github.relua.ast.GlobalAssign;
import com.github.relua.ast.IfStatement;
import com.github.relua.ast.IndexExpr;
import com.github.relua.ast.LocalAssign;
import com.github.relua.ast.MemberExpr;
import com.github.relua.ast.MultiVal;
import com.github.relua.ast.Name;
import com.github.relua.ast.RepeatStatement;
import com.github.relua.ast.ReturnStatement;
import com.github.relua.ast.Statement;
import com.github.relua.ast.StringConst;
import com.github.relua.ast.TableConstructor;
import com.github.relua.ast.TableField;
import com.github.relua.ast.UnaryOp;
import com.github.relua.ast.WhileStatement;
import com.github.relua.log.Logger;
import com.github.relua.model.Chunk;
import com.github.relua.model.CodeLine;
import com.github.relua.model.FromType;
import com.github.relua.model.Register;
import com.github.relua.model.ValueType;
import com.github.relua.decompiler.ssa.SsaAstNameResolver;
import com.github.relua.util.RegisterNamePolicy;

/**
 * Lua代码生成器，作为总控类，负责协调各个代码生成器
 */
public class LuaCodeGenerator {
    // private InstructionHandler instructionHandler;
    private AstCodeEmitter astCodeEmitter;
    private List<CodeGeneratorContext> contexts = new ArrayList<>();
    private Map<String, InstructionHandler> handlers = new HashMap<>();
    private final SsaAstNameResolver ssaNameResolver = new SsaAstNameResolver();

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
            String parameterName = RegisterNamePolicy.parameterName(i);
            entity.setCustomName(parameterName);
            entity.setValue(parameterName);
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
                String parameterName = RegisterNamePolicy.parameterName(i);
                entity.setCustomName(parameterName);
                entity.setValue(parameterName);
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
                    InstructionHandler ctxHandler = handlers.get(ctxChunk.getFunction());
                    new AstCleanupPass().cleanup(ctx.getAstBlock(), ctx, java.util.Collections.emptySet(),
                            java.util.Collections.emptySet(), ctxHandler != null ? ctxHandler.getPipeline() : null);
                    
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

        // 第二遍：把匿名闭包作为表达式内联，并过滤掉对应的临时寄存器赋值。
        List<Statement> finalStatements = new ArrayList<>();
        for (int i = 0; i < rewritten.size(); i++) {
            Statement statement = rewritten.get(i);
            ClosureBinding binding = asTemporaryClosureBinding(statement, contextByFunction);
            if (binding != null && inlineSingleUseClosureBinding(rewritten, i, binding, contextByFunction,
                    emittedFunctions)) {
                continue;
            }
            if (isTemporaryRegisterClosureAssign(statement, emittedFunctions)) {
                continue;
            }
            finalStatements.add(rewriteAnonymousClosureUses(statement, contextByFunction, emittedFunctions));
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

    private ClosureBinding asTemporaryClosureBinding(Statement statement,
            Map<String, CodeGeneratorContext> contextByFunction) {
        if (!(statement instanceof Assign)) {
            return null;
        }
        Assign assign = (Assign) statement;
        if (assign.left.size() != 1 || assign.right.size() != 1) {
            return null;
        }
        Expression left = assign.left.get(0);
        Expression right = assign.right.get(0);
        if (!(left instanceof Name) || !(right instanceof Name)) {
            return null;
        }
        String targetName = ((Name) left).name;
        String closureName = ((Name) right).name;
        if (!RegisterNamePolicy.isTemporaryRegisterName(targetName) || !contextByFunction.containsKey(closureName)) {
            return null;
        }
        return new ClosureBinding(targetName, closureName);
    }

    private boolean inlineSingleUseClosureBinding(List<Statement> statements, int bindingIndex, ClosureBinding binding,
            Map<String, CodeGeneratorContext> contextByFunction, Set<String> emittedFunctions) {
        for (int i = bindingIndex + 1; i < statements.size(); i++) {
            Statement statement = statements.get(i);
            if (definesName(statement, binding.targetName)) {
                return false;
            }
            int reads = countNameReads(statement, binding.targetName);
            if (reads == 0) {
                if (isControlBoundary(statement)) {
                    return false;
                }
                continue;
            }
            if (reads != 1) {
                return false;
            }
            FunctionLiteral literal = createAnonymousFunctionLiteral(contextByFunction.get(binding.closureName));
            statements.set(i, replaceNameRead(statement, binding.targetName, literal));
            emittedFunctions.add(binding.closureName);
            return true;
        }
        return false;
    }

    private Statement rewriteAnonymousClosureUses(Statement statement,
            Map<String, CodeGeneratorContext> contextByFunction, Set<String> emittedFunctions) {
        if (asTemporaryClosureBinding(statement, contextByFunction) != null) {
            return statement;
        }
        if (statement instanceof Assign) {
            Assign assign = (Assign) statement;
            List<Expression> right = rewriteClosureExpressions(assign.right, contextByFunction, emittedFunctions);
            return new Assign(assign.left, right, assign.pos);
        }
        if (statement instanceof GlobalAssign) {
            GlobalAssign assign = (GlobalAssign) statement;
            return new GlobalAssign(assign.names, rewriteClosureExpressions(assign.right, contextByFunction,
                    emittedFunctions), assign.pos);
        }
        if (statement instanceof LocalAssign) {
            LocalAssign assign = (LocalAssign) statement;
            return new LocalAssign(assign.names, rewriteClosureExpressions(assign.right, contextByFunction,
                    emittedFunctions), assign.pos);
        }
        if (statement instanceof ExpressionStatement) {
            ExpressionStatement expr = (ExpressionStatement) statement;
            return new ExpressionStatement(rewriteClosureExpression(expr.expression, contextByFunction,
                    emittedFunctions), expr.pos);
        }
        if (statement instanceof ReturnStatement) {
            ReturnStatement ret = (ReturnStatement) statement;
            return new ReturnStatement(rewriteClosureExpressions(ret.values, contextByFunction, emittedFunctions),
                    ret.pos);
        }
        if (statement instanceof IfStatement) {
            IfStatement ifs = (IfStatement) statement;
            List<Expression> conditions = rewriteClosureExpressions(ifs.conditions, contextByFunction,
                    emittedFunctions);
            for (Block child : ifs.blocks) {
                inlineClosureDeclarations(child, contextByFunction, emittedFunctions);
            }
            if (ifs.elseBlock != null) {
                inlineClosureDeclarations(ifs.elseBlock, contextByFunction, emittedFunctions);
            }
            return new IfStatement(conditions, ifs.blocks, ifs.elseBlock, ifs.pos);
        }
        if (statement instanceof WhileStatement) {
            WhileStatement loop = (WhileStatement) statement;
            inlineClosureDeclarations(loop.body, contextByFunction, emittedFunctions);
            return new WhileStatement(rewriteClosureExpression(loop.condition, contextByFunction, emittedFunctions),
                    loop.body, loop.pos);
        }
        if (statement instanceof RepeatStatement) {
            RepeatStatement loop = (RepeatStatement) statement;
            inlineClosureDeclarations(loop.body, contextByFunction, emittedFunctions);
            return new RepeatStatement(loop.body, rewriteClosureExpression(loop.condition, contextByFunction,
                    emittedFunctions), loop.pos);
        }
        if (statement instanceof ForIn) {
            ForIn loop = (ForIn) statement;
            inlineClosureDeclarations(loop.body, contextByFunction, emittedFunctions);
            return new ForIn(loop.names, rewriteClosureExpressions(loop.iterators, contextByFunction,
                    emittedFunctions), loop.body, loop.pos);
        }
        if (statement instanceof ForNumeric) {
            ForNumeric loop = (ForNumeric) statement;
            inlineClosureDeclarations(loop.body, contextByFunction, emittedFunctions);
            return new ForNumeric(loop.name, rewriteClosureExpression(loop.start, contextByFunction, emittedFunctions),
                    rewriteClosureExpression(loop.end, contextByFunction, emittedFunctions),
                    rewriteClosureExpression(loop.step, contextByFunction, emittedFunctions), loop.body, loop.pos);
        }
        if (statement instanceof FunctionDeclaration) {
            FunctionDeclaration declaration = (FunctionDeclaration) statement;
            inlineClosureDeclarations(declaration.func.body, contextByFunction, emittedFunctions);
        }
        return statement;
    }

    private List<Expression> rewriteClosureExpressions(List<Expression> expressions,
            Map<String, CodeGeneratorContext> contextByFunction, Set<String> emittedFunctions) {
        List<Expression> rewritten = new ArrayList<>();
        for (Expression expression : expressions) {
            rewritten.add(rewriteClosureExpression(expression, contextByFunction, emittedFunctions));
        }
        return rewritten;
    }

    private Expression rewriteClosureExpression(Expression expression,
            Map<String, CodeGeneratorContext> contextByFunction, Set<String> emittedFunctions) {
        if (expression instanceof Name) {
            String name = ((Name) expression).name;
            if (contextByFunction.containsKey(name)) {
                emittedFunctions.add(name);
                return createAnonymousFunctionLiteral(contextByFunction.get(name));
            }
            return expression;
        }
        if (expression instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) expression;
            return new BinaryOp(binary.op, rewriteClosureExpression(binary.left, contextByFunction, emittedFunctions),
                    rewriteClosureExpression(binary.right, contextByFunction, emittedFunctions), binary.pos);
        }
        if (expression instanceof UnaryOp) {
            UnaryOp unary = (UnaryOp) expression;
            return new UnaryOp(unary.op, rewriteClosureExpression(unary.expr, contextByFunction, emittedFunctions),
                    unary.pos);
        }
        if (expression instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) expression;
            return new FunctionCall(rewriteClosureExpression(call.callee, contextByFunction, emittedFunctions),
                    rewriteClosureExpressions(call.args, contextByFunction, emittedFunctions), call.isMethodCall,
                    call.returns, call.pos);
        }
        if (expression instanceof IndexExpr) {
            IndexExpr index = (IndexExpr) expression;
            return new IndexExpr(rewriteClosureExpression(index.table, contextByFunction, emittedFunctions),
                    rewriteClosureExpression(index.index, contextByFunction, emittedFunctions), index.pos);
        }
        if (expression instanceof MemberExpr) {
            MemberExpr member = (MemberExpr) expression;
            return new MemberExpr(rewriteClosureExpression(member.table, contextByFunction, emittedFunctions),
                    member.member, member.pos);
        }
        if (expression instanceof TableConstructor) {
            TableConstructor table = (TableConstructor) expression;
            List<TableField> fields = new ArrayList<>();
            for (TableField field : table.fields) {
                fields.add(new TableField(
                        field.key == null ? null : rewriteClosureExpression(field.key, contextByFunction,
                                emittedFunctions),
                        rewriteClosureExpression(field.value, contextByFunction, emittedFunctions)));
            }
            return new TableConstructor(fields, table.pos);
        }
        if (expression instanceof MultiVal) {
            MultiVal multi = (MultiVal) expression;
            return new MultiVal(rewriteClosureExpressions(multi.values, contextByFunction, emittedFunctions),
                    multi.pos);
        }
        return expression;
    }

    private boolean definesName(Statement statement, String name) {
        if (statement instanceof Assign) {
            Assign assign = (Assign) statement;
            for (Expression left : assign.left) {
                if (left instanceof Name && name.equals(((Name) left).name)) {
                    return true;
                }
            }
        }
        if (statement instanceof LocalAssign) {
            LocalAssign assign = (LocalAssign) statement;
            return assign.names.contains(name);
        }
        return false;
    }

    private int countNameReads(Statement statement, String name) {
        if (statement instanceof Assign) {
            Assign assign = (Assign) statement;
            return countNameReads(assign.right, name);
        }
        if (statement instanceof GlobalAssign) {
            return countNameReads(((GlobalAssign) statement).right, name);
        }
        if (statement instanceof LocalAssign) {
            return countNameReads(((LocalAssign) statement).right, name);
        }
        if (statement instanceof ExpressionStatement) {
            return countNameReads(((ExpressionStatement) statement).expression, name);
        }
        if (statement instanceof ReturnStatement) {
            return countNameReads(((ReturnStatement) statement).values, name);
        }
        return 0;
    }

    private int countNameReads(List<Expression> expressions, String name) {
        int count = 0;
        for (Expression expression : expressions) {
            count += countNameReads(expression, name);
        }
        return count;
    }

    private int countNameReads(Expression expression, String name) {
        if (expression instanceof Name) {
            return name.equals(((Name) expression).name) ? 1 : 0;
        }
        if (expression instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) expression;
            return countNameReads(binary.left, name) + countNameReads(binary.right, name);
        }
        if (expression instanceof UnaryOp) {
            return countNameReads(((UnaryOp) expression).expr, name);
        }
        if (expression instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) expression;
            return countNameReads(call.callee, name) + countNameReads(call.args, name);
        }
        if (expression instanceof IndexExpr) {
            IndexExpr index = (IndexExpr) expression;
            return countNameReads(index.table, name) + countNameReads(index.index, name);
        }
        if (expression instanceof MemberExpr) {
            return countNameReads(((MemberExpr) expression).table, name);
        }
        if (expression instanceof TableConstructor) {
            int count = 0;
            for (TableField field : ((TableConstructor) expression).fields) {
                count += field.key == null ? 0 : countNameReads(field.key, name);
                count += countNameReads(field.value, name);
            }
            return count;
        }
        if (expression instanceof MultiVal) {
            return countNameReads(((MultiVal) expression).values, name);
        }
        return 0;
    }

    private Statement replaceNameRead(Statement statement, String name, Expression replacement) {
        if (statement instanceof Assign) {
            Assign assign = (Assign) statement;
            return new Assign(assign.left, replaceNameReads(assign.right, name, replacement), assign.pos);
        }
        if (statement instanceof GlobalAssign) {
            GlobalAssign assign = (GlobalAssign) statement;
            return new GlobalAssign(assign.names, replaceNameReads(assign.right, name, replacement), assign.pos);
        }
        if (statement instanceof LocalAssign) {
            LocalAssign assign = (LocalAssign) statement;
            return new LocalAssign(assign.names, replaceNameReads(assign.right, name, replacement), assign.pos);
        }
        if (statement instanceof ExpressionStatement) {
            ExpressionStatement expr = (ExpressionStatement) statement;
            return new ExpressionStatement(replaceNameRead(expr.expression, name, replacement), expr.pos);
        }
        if (statement instanceof ReturnStatement) {
            ReturnStatement ret = (ReturnStatement) statement;
            return new ReturnStatement(replaceNameReads(ret.values, name, replacement), ret.pos);
        }
        return statement;
    }

    private List<Expression> replaceNameReads(List<Expression> expressions, String name, Expression replacement) {
        List<Expression> rewritten = new ArrayList<>();
        for (Expression expression : expressions) {
            rewritten.add(replaceNameRead(expression, name, replacement));
        }
        return rewritten;
    }

    private Expression replaceNameRead(Expression expression, String name, Expression replacement) {
        if (expression instanceof Name) {
            return name.equals(((Name) expression).name) ? replacement : expression;
        }
        if (expression instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) expression;
            return new BinaryOp(binary.op, replaceNameRead(binary.left, name, replacement),
                    replaceNameRead(binary.right, name, replacement), binary.pos);
        }
        if (expression instanceof UnaryOp) {
            UnaryOp unary = (UnaryOp) expression;
            return new UnaryOp(unary.op, replaceNameRead(unary.expr, name, replacement), unary.pos);
        }
        if (expression instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) expression;
            return new FunctionCall(replaceNameRead(call.callee, name, replacement),
                    replaceNameReads(call.args, name, replacement), call.isMethodCall, call.returns, call.pos);
        }
        if (expression instanceof IndexExpr) {
            IndexExpr index = (IndexExpr) expression;
            return new IndexExpr(replaceNameRead(index.table, name, replacement),
                    replaceNameRead(index.index, name, replacement), index.pos);
        }
        if (expression instanceof MemberExpr) {
            MemberExpr member = (MemberExpr) expression;
            return new MemberExpr(replaceNameRead(member.table, name, replacement), member.member, member.pos);
        }
        if (expression instanceof TableConstructor) {
            TableConstructor table = (TableConstructor) expression;
            List<TableField> fields = new ArrayList<>();
            for (TableField field : table.fields) {
                fields.add(new TableField(field.key == null ? null : replaceNameRead(field.key, name, replacement),
                        replaceNameRead(field.value, name, replacement)));
            }
            return new TableConstructor(fields, table.pos);
        }
        if (expression instanceof MultiVal) {
            MultiVal multi = (MultiVal) expression;
            return new MultiVal(replaceNameReads(multi.values, name, replacement), multi.pos);
        }
        return expression;
    }

    private boolean isControlBoundary(Statement statement) {
        return statement instanceof IfStatement || statement instanceof WhileStatement
                || statement instanceof RepeatStatement || statement instanceof ForIn || statement instanceof ForNumeric
                || statement instanceof FunctionDeclaration;
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
        List<String> params = ssaNameResolver.parameterNames(functionChunk.getNumParams());
        FunctionLiteral literal = new FunctionLiteral(params, functionChunk.getIsVararg() != 0, context.getAstBlock(), null);
        literal.setChunk(functionChunk);
        return literal;
    }

    private FunctionLiteral createAnonymousFunctionLiteral(CodeGeneratorContext context) {
        Chunk functionChunk = context.getChunk();
        List<String> params = ssaNameResolver.parameterNames(functionChunk.getNumParams());
        return new FunctionLiteral(params, functionChunk.getIsVararg() != 0, context.getAstBlock(), null);
    }

    private static class ClosureBinding {
        final String targetName;
        final String closureName;

        ClosureBinding(String targetName, String closureName) {
            this.targetName = targetName;
            this.closureName = closureName;
        }
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
