package com.github.relua.ast;

import java.util.ArrayList;
import java.util.List;

public class IfStatement extends Statement {
    public final List<Expression> conditions;
    public final List<Block> blocks;
    public final Block elseBlock;
    
    public IfStatement(List<Expression> conds, List<Block> blocks, Block elseBlock, SourcePos pos) {
        super(NodeType.IF, pos);
        this.conditions = conds;
        this.blocks = blocks;
        this.elseBlock = elseBlock;
    }

    public IfStatement(Expression conds, Block blocks, Block elseBlock, SourcePos pos) {
        super(NodeType.IF, pos);
        this.conditions = new ArrayList<Expression>();
        this.conditions.add(conds);
        this.blocks = new ArrayList<Block>();
        this.blocks.add(blocks);
        this.elseBlock = elseBlock;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}