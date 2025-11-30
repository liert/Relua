package com.github.relua.gui.utils;

import com.github.relua.gui.views.GraphVisualizationView;
import com.github.relua.model.LuacFile;
import com.github.relua.ast.AstNode;
import com.github.relua.ast.AstVisitor;
import com.github.relua.decompiler.CodeGeneratorContext;
import com.github.relua.decompiler.InstructionHandler;

/**
 * AST到图形的转换器，用于将Relua的AST转换为JavaFX Canvas的图形表示
 */
public class ASTGraphConverter {
    // 图形可视化视图
    private final GraphVisualizationView graphView;
    
    /**
     * 构造函数
     * @param graphView 图形可视化视图
     */
    public ASTGraphConverter(GraphVisualizationView graphView) {
        this.graphView = graphView;
    }
    
    /**
     * 将AST转换为图形
     * @param astNode AST节点或LuacFile对象
     */
    public void convertToGraph(Object astNode) {
        // 清空现有图形
        graphView.clearGraph();
        
        if (astNode != null) {
            if (astNode instanceof LuacFile) {
                // 处理LuacFile对象，生成AST节点
                processLuacFile((LuacFile) astNode);
            } else if (astNode instanceof AstNode) {
                // 直接处理AST节点
                processAstNode((AstNode) astNode);
            } else {
                // 简化实现，仅添加基本节点
                int rootIndex = graphView.addNode("AST Root");
                int child1Index = graphView.addNode("Child 1");
                int child2Index = graphView.addNode("Child 2");
                int child3Index = graphView.addNode("Child 3");
                
                graphView.addEdge(rootIndex, child1Index);
                graphView.addEdge(rootIndex, child2Index);
                graphView.addEdge(child2Index, child3Index);
                
                graphView.applyLayout();
            }
        }
    }
    
    /**
     * 处理LuacFile对象，生成AST节点并转换为图形
     * @param luacFile LuacFile对象
     */
    private void processLuacFile(LuacFile luacFile) {
        try {
            // 创建代码生成上下文
            CodeGeneratorContext codeGenContext = new CodeGeneratorContext();
            
            // 创建指令处理器
            InstructionHandler instructionHandler = new InstructionHandler(codeGenContext);
            
            // 处理主代码块，生成AST节点
            instructionHandler.process(luacFile.getMainChunk());
            AstNode astRoot = instructionHandler.generateASTFromChunk(luacFile.getMainChunk());
            
            // 处理AST节点，生成图形
            processAstNode(astRoot);
        } catch (Exception e) {
            // 如果生成AST失败，显示错误信息
            int errorIndex = graphView.addNode("Error generating AST: " + e.getMessage());
            graphView.applyLayout();
            e.printStackTrace();
        }
    }
    
    /**
     * 处理AST节点，生成图形
     * @param astNode AST节点
     */
    private void processAstNode(AstNode astNode) {
        // 添加根节点
        int rootIndex = graphView.addNode(astNode.type.name());
        
        // 使用访问者模式遍历子节点
        astNode.accept(new AstVisitor<Void>() {
            @Override
            public Void visit(com.github.relua.ast.Block node) {
                // 添加Block节点的子节点
                for (com.github.relua.ast.Statement child : node.statements) {
                    int childIndex = graphView.addNode(child.type.name());
                    graphView.addEdge(rootIndex, childIndex);
                    // 递归处理子节点
                    child.accept(this);
                }
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.Assign node) {
                // 添加Assign节点的子节点
                for (com.github.relua.ast.Expression child : node.left) {
                    int childIndex = graphView.addNode(child.type.name());
                    graphView.addEdge(rootIndex, childIndex);
                    // 递归处理子节点
                    child.accept(this);
                }
                for (com.github.relua.ast.Expression child : node.right) {
                    int childIndex = graphView.addNode(child.type.name());
                    graphView.addEdge(rootIndex, childIndex);
                    // 递归处理子节点
                    child.accept(this);
                }
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.LocalAssign node) {
                // 添加LocalAssign节点的子节点
                for (com.github.relua.ast.Expression child : node.right) {
                    int childIndex = graphView.addNode(child.type.name());
                    graphView.addEdge(rootIndex, childIndex);
                    // 递归处理子节点
                    child.accept(this);
                }
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.IfStatement node) {
                // 添加IfStatement节点的子节点
                for (com.github.relua.ast.Expression child : node.conditions) {
                    int childIndex = graphView.addNode(child.type.name());
                    graphView.addEdge(rootIndex, childIndex);
                    // 递归处理子节点
                    child.accept(this);
                }
                for (com.github.relua.ast.Block child : node.blocks) {
                    int childIndex = graphView.addNode(child.type.name());
                    graphView.addEdge(rootIndex, childIndex);
                    // 递归处理子节点
                    child.accept(this);
                }
                if (node.elseBlock != null) {
                    int childIndex = graphView.addNode(node.elseBlock.type.name());
                    graphView.addEdge(rootIndex, childIndex);
                    // 递归处理子节点
                    node.elseBlock.accept(this);
                }
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.WhileStatement node) {
                // 添加WhileStatement节点的子节点
                int condIndex = graphView.addNode(node.condition.type.name());
                graphView.addEdge(rootIndex, condIndex);
                // 递归处理条件节点
                node.condition.accept(this);
                
                int blockIndex = graphView.addNode(node.body.type.name());
                graphView.addEdge(rootIndex, blockIndex);
                // 递归处理块节点
                node.body.accept(this);
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.FunctionCall node) {
                // 添加FunctionCall节点的子节点
                int calleeIndex = graphView.addNode(node.callee.type.name());
                graphView.addEdge(rootIndex, calleeIndex);
                // 递归处理调用者节点
                node.callee.accept(this);
                
                for (com.github.relua.ast.Expression child : node.args) {
                    int childIndex = graphView.addNode(child.type.name());
                    graphView.addEdge(rootIndex, childIndex);
                    // 递归处理参数节点
                    child.accept(this);
                }
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.BinaryOp node) {
                // 添加BinaryOp节点的子节点
                int leftIndex = graphView.addNode(node.left.type.name());
                graphView.addEdge(rootIndex, leftIndex);
                // 递归处理左操作数节点
                node.left.accept(this);
                
                int rightIndex = graphView.addNode(node.right.type.name());
                graphView.addEdge(rootIndex, rightIndex);
                // 递归处理右操作数节点
                node.right.accept(this);
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.UnaryOp node) {
                // 添加UnaryOp节点的子节点
                int exprIndex = graphView.addNode(node.expr.type.name());
                graphView.addEdge(rootIndex, exprIndex);
                // 递归处理表达式节点
                node.expr.accept(this);
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.ReturnStatement node) {
                // 添加ReturnStatement节点的子节点
                for (com.github.relua.ast.Expression child : node.values) {
                    int childIndex = graphView.addNode(child.type.name());
                    graphView.addEdge(rootIndex, childIndex);
                    // 递归处理返回值节点
                    child.accept(this);
                }
                return null;
            }
            
            // 其他visit方法的默认实现
            @Override
            public Void visit(com.github.relua.ast.ExpressionStatement node) {
                // 添加ExpressionStatement节点的子节点
                int exprIndex = graphView.addNode(node.expression.type.name());
                graphView.addEdge(rootIndex, exprIndex);
                // 递归处理表达式节点
                node.expression.accept(this);
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.FunctionDeclaration node) {
                // 添加FunctionDeclaration节点的子节点
                int blockIndex = graphView.addNode(node.func.type.name());
                graphView.addEdge(rootIndex, blockIndex);
                // 递归处理函数体节点
                node.func.accept(this);
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.TableConstructor node) {
                // TableField类不是公共的，无法从外部包访问，所以只处理TableConstructor节点本身，不处理其字段
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.IndexExpr node) {
                // 添加IndexExpr节点的子节点
                int baseIndex = graphView.addNode(node.table.type.name());
                graphView.addEdge(rootIndex, baseIndex);
                // 递归处理基节点
                node.table.accept(this);
                
                int indexIndex = graphView.addNode(node.index.type.name());
                graphView.addEdge(rootIndex, indexIndex);
                // 递归处理索引节点
                node.index.accept(this);
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.FunctionLiteral node) {
                // 添加FunctionLiteral节点的子节点
                int blockIndex = graphView.addNode(node.body.type.name());
                graphView.addEdge(rootIndex, blockIndex);
                // 递归处理函数体节点
                node.body.accept(this);
                return null;
            }
            
            // 对于其他节点类型，只添加节点，不处理子节点
            @Override
            public Void visit(com.github.relua.ast.NilConst node) {
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.BooleanConst node) {
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.NumberConst node) {
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.StringConst node) {
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.Name node) {
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.MemberExpr node) {
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.RepeatStatement node) {
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.ForNumeric node) {
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.ForIn node) {
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.BreakStatement node) {
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.GotoStatement node) {
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.LabelStatement node) {
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.Vararg node) {
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.MultiVal node) {
                return null;
            }
            
            @Override
            public Void visit(com.github.relua.ast.GlobalAssign node) {
                return null;
            }
        });
        
        // 应用布局
        graphView.applyLayout();
    }
}