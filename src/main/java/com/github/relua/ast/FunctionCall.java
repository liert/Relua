package com.github.relua.ast;

import java.util.List;

public class FunctionCall extends Expression {
    public final Expression callee;
    public final List<Expression> args;
    public final boolean isMethodCall;
    public final List<Expression> returns;
    public boolean multiReturn;
    
    public FunctionCall(Expression callee, List<Expression> args, boolean isMethod, SourcePos pos) {
        this(callee, args, isMethod, null, pos);
    }
    
    public FunctionCall(Expression callee, List<Expression> args, boolean isMethod, List<Expression> returns, SourcePos pos) {
        super(NodeType.FUNC_CALL, pos);
        this.callee = callee;
        this.args = args;
        this.isMethodCall = isMethod;
        this.returns = returns;
        this.multiReturn = false;
    }
    
    public void setMultiReturn(boolean multiReturn) {
        this.multiReturn = multiReturn;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}