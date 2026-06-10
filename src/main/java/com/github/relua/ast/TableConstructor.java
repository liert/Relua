package com.github.relua.ast;

import java.util.List;

public class TableConstructor extends Expression {
    public final List<TableField> fields;
    
    public TableConstructor(List<TableField> fields, SourcePos pos) {
        super(NodeType.TABLE_CONSTR, pos);
        this.fields = fields;
    }

    public void addArrayField(Expression value) {
        this.fields.add(new TableField(null, value));
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}

class TableField {
    public final Expression key;
    public final Expression value;
    
    public TableField(Expression key, Expression value) {
        this.key = key;
        this.value = value;
    }
}
