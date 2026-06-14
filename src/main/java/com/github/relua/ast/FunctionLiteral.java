package com.github.relua.ast;

import java.util.List;
import com.github.relua.model.Chunk;

public class FunctionLiteral extends Expression {
    public final List<String> params;
    public final boolean vararg;
    public final Block body;
    private Chunk chunk;
    
    public FunctionLiteral(List<String> params, boolean vararg, Block body, SourcePos pos) {
        super(NodeType.FUNC_LIT, pos);
        this.params = params;
        this.vararg = vararg;
        this.body = body;
    }
    
    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }
    
    public Chunk getChunk() {
        return chunk;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}