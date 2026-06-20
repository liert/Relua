package com.github.relua.ast;

import java.util.ArrayList;
import java.util.List;

public class AstCloner implements AstVisitor<AstNode> {

    @SuppressWarnings("unchecked")
    public <T extends AstNode> T clone(T node) {
        if (node == null) {
            return null;
        }
        return (T) node.accept(this);
    }

    public List<Expression> cloneExpressions(List<Expression> expressions) {
        if (expressions == null) {
            return null;
        }
        List<Expression> cloned = new ArrayList<>();
        for (Expression expr : expressions) {
            cloned.add(clone(expr));
        }
        return cloned;
    }

    public List<Statement> cloneStatements(List<Statement> statements) {
        if (statements == null) {
            return null;
        }
        List<Statement> cloned = new ArrayList<>();
        for (Statement stmt : statements) {
            cloned.add(clone(stmt));
        }
        return cloned;
    }

    @Override
    public AstNode visit(Block node) {
        Block cloned = new Block(node.pos);
        cloned.statements.addAll(cloneStatements(node.statements));
        return cloned;
    }

    @Override
    public AstNode visit(Assign node) {
        List<Expression> left = new ArrayList<>();
        for (Expression expr : node.left) {
            left.add(clone(expr));
        }
        return new Assign(left, cloneExpressions(node.right), node.pos);
    }

    @Override
    public AstNode visit(LocalAssign node) {
        return new LocalAssign(new ArrayList<>(node.names), cloneExpressions(node.right), node.pos);
    }

    @Override
    public AstNode visit(GlobalAssign node) {
        return new GlobalAssign(new ArrayList<>(node.names), cloneExpressions(node.right), node.pos);
    }

    @Override
    public AstNode visit(IfStatement node) {
        List<Block> blocks = new ArrayList<>();
        for (Block b : node.blocks) {
            blocks.add(clone(b));
        }
        return new IfStatement(cloneExpressions(node.conditions), blocks, clone(node.elseBlock), node.pos);
    }

    @Override
    public AstNode visit(WhileStatement node) {
        return new WhileStatement(clone(node.condition), clone(node.body), node.pos);
    }

    @Override
    public AstNode visit(RepeatStatement node) {
        return new RepeatStatement(clone(node.body), clone(node.condition), node.pos);
    }

    @Override
    public AstNode visit(ForNumeric node) {
        return new ForNumeric(node.name, clone(node.start), clone(node.end), clone(node.step), clone(node.body), node.pos);
    }

    @Override
    public AstNode visit(ForIn node) {
        return new ForIn(new ArrayList<>(node.names), cloneExpressions(node.iterators), clone(node.body), node.pos);
    }

    @Override
    public AstNode visit(FunctionDeclaration node) {
        return new FunctionDeclaration(node.name, clone(node.func), node.isLocal, node.pos);
    }

    @Override
    public AstNode visit(ReturnStatement node) {
        return new ReturnStatement(cloneExpressions(node.values), node.pos);
    }

    @Override
    public AstNode visit(ExpressionStatement node) {
        return new ExpressionStatement(clone(node.expression), node.pos);
    }

    @Override
    public AstNode visit(BreakStatement node) {
        return new BreakStatement(node.pos);
    }

    @Override
    public AstNode visit(GotoStatement node) {
        return new GotoStatement(node.label, node.pos);
    }

    @Override
    public AstNode visit(LabelStatement node) {
        return new LabelStatement(node.label, node.pos);
    }

    @Override
    public AstNode visit(NilConst node) {
        return new NilConst(node.pos);
    }

    @Override
    public AstNode visit(BooleanConst node) {
        return new BooleanConst(node.value, node.pos);
    }

    @Override
    public AstNode visit(NumberConst node) {
        return new NumberConst(node.value, node.pos);
    }

    @Override
    public AstNode visit(StringConst node) {
        return new StringConst(node.value, node.pos);
    }

    @Override
    public AstNode visit(Name node) {
        return new Name(node.name, node.pos);
    }

    @Override
    public AstNode visit(IndexExpr node) {
        return new IndexExpr(clone(node.table), clone(node.index), node.pos);
    }

    @Override
    public AstNode visit(MemberExpr node) {
        return new MemberExpr(clone(node.table), node.member, node.pos);
    }

    @Override
    public AstNode visit(FunctionCall node) {
        return new FunctionCall(clone(node.callee), cloneExpressions(node.args), node.isMethodCall, node.returns, node.pos);
    }

    @Override
    public AstNode visit(FunctionLiteral node) {
        FunctionLiteral cloned = new FunctionLiteral(new ArrayList<>(node.params), node.vararg, clone(node.body), node.pos);
        cloned.setChunk(node.getChunk());
        return cloned;
    }

    @Override
    public AstNode visit(TableConstructor node) {
        List<TableField> fields = new ArrayList<>();
        for (TableField field : node.fields) {
            fields.add(new TableField(clone(field.key), clone(field.value)));
        }
        return new TableConstructor(fields, node.pos);
    }

    @Override
    public AstNode visit(UnaryOp node) {
        return new UnaryOp(node.op, clone(node.expr), node.pos);
    }

    @Override
    public AstNode visit(BinaryOp node) {
        return new BinaryOp(node.op, clone(node.left), clone(node.right), node.pos);
    }

    @Override
    public AstNode visit(Vararg node) {
        return new Vararg(node.pos);
    }

    @Override
    public AstNode visit(MultiVal node) {
        return new MultiVal(cloneExpressions(node.values), node.pos);
    }
}
