package com.github.relua.decompiler.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.relua.ast.*;

public class StructureRestorer {

    public void restructure(Block block) {
        if (block == null || block.statements == null) {
            return;
        }

        // 1. 递归重构嵌套的 Block
        for (Statement stmt : block.statements) {
            restructureNestedBlocks(stmt);
        }

        // 2. 重组当前 Block 层的条件分支与消解 goto
        eliminateDanglingGotos(block);
    }

    private void restructureNestedBlocks(Statement statement) {
        if (statement instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) statement;
            for (Block nested : ifStmt.blocks) {
                restructure(nested);
            }
            restructure(ifStmt.elseBlock);
        } else if (statement instanceof FunctionDeclaration) {
            restructure(((FunctionDeclaration) statement).func.body);
        } else if (statement instanceof WhileStatement) {
            restructure(((WhileStatement) statement).body);
        } else if (statement instanceof RepeatStatement) {
            restructure(((RepeatStatement) statement).body);
        } else if (statement instanceof ForNumeric) {
            restructure(((ForNumeric) statement).body);
        } else if (statement instanceof ForIn) {
            restructure(((ForIn) statement).body);
        }
    }

    /**
     * 识别 `if not cond then goto Label ... statements ... ::Label::` 模式并重构
     */
    private void eliminateDanglingGotos(Block block) {
        List<Statement> stmts = block.statements;
        boolean changed = true;

        while (changed) {
            changed = false;
            for (int i = 0; i < stmts.size(); i++) {
                Statement stmt = stmts.get(i);
                
                // 寻找满足 `if [not] cond then goto Label` 的 IfStatement
                if (isGotoIfStatement(stmt)) {
                    IfStatement ifStmt = (IfStatement) stmt;
                    GotoStatement gotoStmt = (GotoStatement) ifStmt.blocks.get(0).statements.get(0);
                    String labelName = gotoStmt.label;

                    // 寻找匹配的 LabelStatement
                    int labelIndex = findLabelIndex(stmts, i + 1, labelName);
                    if (labelIndex != -1) {
                        // 检查在这期间是否有其它 goto 引用该 Label
                        if (!hasOtherGotosTo(stmts, labelName, i)) {
                            // 提取 [i + 1, labelIndex - 1] 之间的语句作为新的 if body
                            List<Statement> bodyStmts = new ArrayList<>();
                            for (int k = i + 1; k < labelIndex; k++) {
                                bodyStmts.add(stmts.get(k));
                            }

                            // 对原条件取反
                            Expression originalCond = ifStmt.conditions.get(0);
                            Expression newCond = negateCondition(originalCond);

                            Block newBody = new Block(new SourcePos(i + 1, -1));
                            newBody.statements.addAll(bodyStmts);

                            // 重写 IfStatement
                            List<Expression> newConds = new ArrayList<>();
                            newConds.add(newCond);
                            List<Block> newBlocks = new ArrayList<>();
                            newBlocks.add(newBody);
                            
                            IfStatement newIf = new IfStatement(newConds, newBlocks, null, ifStmt.pos);

                            // 用新 IfStatement 替换原 IfStatement，并清除后面的语句和 Label
                            stmts.set(i, newIf);
                            
                            // 从后往前移除已被提取的语句以及 LabelStatement
                            for (int k = labelIndex; k > i; k--) {
                                stmts.remove(k);
                            }

                            changed = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean isGotoIfStatement(Statement stmt) {
        if (!(stmt instanceof IfStatement)) {
            return false;
        }
        IfStatement ifStmt = (IfStatement) stmt;
        // 必须仅有一个条件分支，且无 else 分支
        if (ifStmt.conditions.size() != 1 || ifStmt.blocks.size() != 1 || ifStmt.elseBlock != null) {
            return false;
        }
        Block body = ifStmt.blocks.get(0);
        // then 块里仅能有一条 goto 语句
        if (body.statements.size() != 1) {
            return false;
        }
        return body.statements.get(0) instanceof GotoStatement;
    }

    private int findLabelIndex(List<Statement> stmts, int start, String labelName) {
        for (int i = start; i < stmts.size(); i++) {
            Statement stmt = stmts.get(i);
            if (stmt instanceof LabelStatement && ((LabelStatement) stmt).label.equals(labelName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 检查除语句外是否还有其它 goto 跳转到指定 label
     */
    private boolean hasOtherGotosTo(List<Statement> stmts, String labelName, int excludeIfIndex) {
        int gotoCount = 0;
        for (int i = 0; i < stmts.size(); i++) {
            if (i == excludeIfIndex) continue;
            gotoCount += countGotosTo(stmts.get(i), labelName);
        }
        return gotoCount > 0;
    }

    private int countGotosTo(AstNode node, String labelName) {
        if (node == null) return 0;
        int count = 0;

        if (node instanceof GotoStatement) {
            if (((GotoStatement) node).label.equals(labelName)) {
                return 1;
            }
        } else if (node instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) node;
            for (Block block : ifStmt.blocks) {
                count += countGotosTo(block, labelName);
            }
            count += countGotosTo(ifStmt.elseBlock, labelName);
        } else if (node instanceof WhileStatement) {
            count += countGotosTo(((WhileStatement) node).body, labelName);
        } else if (node instanceof RepeatStatement) {
            count += countGotosTo(((RepeatStatement) node).body, labelName);
        } else if (node instanceof ForNumeric) {
            count += countGotosTo(((ForNumeric) node).body, labelName);
        } else if (node instanceof ForIn) {
            count += countGotosTo(((ForIn) node).body, labelName);
        } else if (node instanceof Block) {
            for (Statement stmt : ((Block) node).statements) {
                count += countGotosTo(stmt, labelName);
            }
        } else if (node instanceof FunctionDeclaration) {
            count += countGotosTo(((FunctionDeclaration) node).func.body, labelName);
        } else if (node instanceof FunctionLiteral) {
            count += countGotosTo(((FunctionLiteral) node).body, labelName);
        }

        return count;
    }

    /**
     * 对条件表达式取反
     */
    private Expression negateCondition(Expression expr) {
        if (expr instanceof UnaryOp && ((UnaryOp) expr).op.equals("not")) {
            return ((UnaryOp) expr).expr;
        }
        
        // 比较运算的直接取反优化 (如 == 改为 ~=)
        if (expr instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) expr;
            String negatedOp = getNegatedOperator(binary.op);
            if (negatedOp != null) {
                return new BinaryOp(negatedOp, binary.left, binary.right, binary.pos);
            }
        }

        return new UnaryOp("not", expr, expr.pos);
    }

    private String getNegatedOperator(String op) {
        switch (op) {
            case "==": return "~=";
            case "~=": return "==";
            case "<":  return ">=";
            case ">":  return "<=";
            case "<=": return ">";
            case ">=": return "<";
            default:   return null;
        }
    }
}
