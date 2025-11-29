package com.github.relua.ast;

import java.util.ArrayList;
import java.util.List;

public class Assign extends Statement {
    public final List<Expression> left;
    public final List<Expression> right;
    
public Assign(String name, Expression right, SourcePos pos) {
        super(NodeType.ASSIGN, pos);
        this.left = new ArrayList<>();
        this.left.add(new Name(name, pos));
        this.right = new ArrayList<>();
        this.right.add(right);
    }

    public Assign(Expression left, Expression right, SourcePos pos) {
        super(NodeType.ASSIGN, pos);
        this.left = new ArrayList<>();
        this.left.add(left);
        this.right = new ArrayList<>();
        this.right.add(right);
    }
    
    public Assign(List<Expression> left, List<Expression> right, SourcePos pos) {
        super(NodeType.ASSIGN, pos);
        this.left = left;
        this.right = right;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}