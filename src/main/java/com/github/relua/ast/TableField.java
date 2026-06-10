package com.github.relua.ast;

public class TableField {
    public final Expression key;
    public final Expression value;
    
    public TableField(Expression key, Expression value) {
        this.key = key;
        this.value = value;
    }
}
