package com.github.relua.ast;

public class GotoStatement extends Statement {
    public final String label;
    private String forAssignLeft;
    private String forAssignRight;
    private String forCondition;
    
    public GotoStatement(String label, SourcePos pos) {
        super(NodeType.GOTO, pos);
        this.label = label;
    }
    
    public void setForLoopAssign(String left, String right, String condition) {
        this.forAssignLeft = left;
        this.forAssignRight = right;
        this.forCondition = condition;
    }
    
    public boolean isForLoop() {
        return this.forAssignLeft != null;
    }
    
    public String getForAssignLeft() {
        return this.forAssignLeft;
    }
    
    public String getForAssignRight() {
        return this.forAssignRight;
    }
    
    public String getForCondition() {
        return this.forCondition;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visit(this);
    }
}