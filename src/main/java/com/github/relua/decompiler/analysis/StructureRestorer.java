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

        // 2. 重构泛型 for 循环 (FORGPREP/FORGLOOP 拓扑还原)
        restoreForInLoops(block);

        // 3. 重构结构化的 if-else 分支
        eliminateStructuredIfElse(block);

        // 4. 重组当前 Block 层的条件分支与消解其余简单的 goto
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

    private void restoreForInLoops(Block block) {
        if (block == null || block.statements == null) {
            return;
        }
        List<Statement> stmts = block.statements;
        boolean changed = true;
        
        while (changed) {
            changed = false;
            for (int i = 0; i < stmts.size(); i++) {
                Statement stmt = stmts.get(i);
                
                // 1. 寻找 3 个连续的迭代内部寄存器的赋值语句
                if (isForInIteratorAssign(stmt)) {
                    int regA = getFirstRegisterIndex(stmt);
                    if (regA == -1) continue;
                    
                    // 2. 语句 i+1 必须是 GotoStatement
                    if (i + 1 < stmts.size() && stmts.get(i + 1) instanceof GotoStatement) {
                        GotoStatement prepGoto = (GotoStatement) stmts.get(i + 1);
                        String labelEnd = prepGoto.label;
                        
                        // 3. 语句 i+2 必须是 LabelStatement，代表 Label_Start
                        if (i + 2 < stmts.size() && stmts.get(i + 2) instanceof LabelStatement) {
                            LabelStatement startLabel = (LabelStatement) stmts.get(i + 2);
                            String labelStart = startLabel.label;
                            
                            // 4. 寻找 Label_End
                            int labelEndIndex = findLabelIndex(stmts, i + 3, labelEnd);
                            if (labelEndIndex != -1 && labelEndIndex + 1 < stmts.size() 
                                    && stmts.get(labelEndIndex + 1) instanceof GotoStatement) {
                                GotoStatement loopGoto = (GotoStatement) stmts.get(labelEndIndex + 1);
                                
                                // 5. 循环尾的 GotoStatement 必须跳回 Label_Start
                                if (loopGoto.label.equals(labelStart)) {
                                    
                                    // 匹配成功！
                                    List<Expression> iterators = getIteratorExpressions(stmt);
                                    
                                    // 提取循环体语句
                                    List<Statement> bodyStmts = new ArrayList<>();
                                    for (int k = i + 3; k < labelEndIndex; k++) {
                                        bodyStmts.add(stmts.get(k));
                                    }
                                    
                                    Block bodyBlock = new Block(new SourcePos(i + 3, -1));
                                    bodyBlock.statements.addAll(bodyStmts);
                                    
                                    // 重命名循环变量 (A+3, A+4 -> k, v) 并隐藏其内部寄存器
                                    String kName = "R" + (regA + 3);
                                    String vName = "R" + (regA + 4);
                                    renameVariable(bodyBlock, kName, "k");
                                    renameVariable(bodyBlock, vName, "v");
                                    
                                    List<String> names = new ArrayList<>();
                                    names.add("k");
                                    names.add("v");
                                    
                                    ForIn forIn = new ForIn(names, iterators, bodyBlock, stmt.pos);
                                    
                                    stmts.set(i, forIn);
                                    
                                    // 从后往前移出多余的跳转和 Label 语句
                                    for (int k = labelEndIndex + 1; k > i; k--) {
                                        stmts.remove(k);
                                    }
                                    
                                    // 递归重构循环体
                                    restructure(bodyBlock);
                                    
                                    changed = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isForInIteratorAssign(Statement stmt) {
        if (stmt instanceof Assign) {
            Assign assign = (Assign) stmt;
            if (assign.left.size() == 3) {
                return areContinuousRegisters(assign.left.get(0), assign.left.get(1), assign.left.get(2));
            }
        } else if (stmt instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) stmt;
            if (local.names.size() == 3) {
                return areContinuousRegisterNames(local.names.get(0), local.names.get(1), local.names.get(2));
            }
        }
        return false;
    }

    private boolean areContinuousRegisters(Expression e1, Expression e2, Expression e3) {
        if (e1 instanceof Name && e2 instanceof Name && e3 instanceof Name) {
            return areContinuousRegisterNames(((Name) e1).name, ((Name) e2).name, ((Name) e3).name);
        }
        return false;
    }

    private boolean areContinuousRegisterNames(String n1, String n2, String n3) {
        if (n1.matches("R\\d+") && n2.matches("R\\d+") && n3.matches("R\\d+")) {
            int r1 = Integer.parseInt(n1.substring(1));
            int r2 = Integer.parseInt(n2.substring(1));
            int r3 = Integer.parseInt(n3.substring(1));
            return r2 == r1 + 1 && r3 == r1 + 2;
        }
        return false;
    }

    private int getFirstRegisterIndex(Statement stmt) {
        String name = null;
        if (stmt instanceof Assign) {
            Assign assign = (Assign) stmt;
            if (!assign.left.isEmpty() && assign.left.get(0) instanceof Name) {
                name = ((Name) assign.left.get(0)).name;
            }
        } else if (stmt instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) stmt;
            if (!local.names.isEmpty()) {
                name = local.names.get(0);
            }
        }
        if (name != null && name.matches("R\\d+")) {
            return Integer.parseInt(name.substring(1));
        }
        return -1;
    }

    private List<Expression> getIteratorExpressions(Statement stmt) {
        if (stmt instanceof Assign) {
            return ((Assign) stmt).right;
        } else if (stmt instanceof LocalAssign) {
            return ((LocalAssign) stmt).right;
        }
        return new ArrayList<>();
    }

    private void renameVariable(AstNode node, String oldName, String newName) {
        if (node == null) return;
        if (node instanceof Name) {
            Name nameNode = (Name) node;
            if (nameNode.name.equals(oldName)) {
                nameNode.name = newName;
            }
        } else if (node instanceof Assign) {
            Assign assign = (Assign) node;
            for (Expression left : assign.left) {
                renameVariable(left, oldName, newName);
            }
            for (Expression right : assign.right) {
                renameVariable(right, oldName, newName);
            }
        } else if (node instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) node;
            for (int i = 0; i < local.names.size(); i++) {
                if (local.names.get(i).equals(oldName)) {
                    local.names.set(i, newName);
                }
            }
            for (Expression right : local.right) {
                renameVariable(right, oldName, newName);
            }
        } else if (node instanceof GlobalAssign) {
            GlobalAssign glob = (GlobalAssign) node;
            for (int i = 0; i < glob.names.size(); i++) {
                if (glob.names.get(i).equals(oldName)) {
                    glob.names.set(i, newName);
                }
            }
            for (Expression right : glob.right) {
                renameVariable(right, oldName, newName);
            }
        } else if (node instanceof ExpressionStatement) {
            renameVariable(((ExpressionStatement) node).expression, oldName, newName);
        } else if (node instanceof ReturnStatement) {
            for (Expression val : ((ReturnStatement) node).values) {
                renameVariable(val, oldName, newName);
            }
        } else if (node instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) node;
            for (Expression cond : ifStmt.conditions) {
                renameVariable(cond, oldName, newName);
            }
            for (Block nested : ifStmt.blocks) {
                renameVariable(nested, oldName, newName);
            }
            renameVariable(ifStmt.elseBlock, oldName, newName);
        } else if (node instanceof WhileStatement) {
            WhileStatement wh = (WhileStatement) node;
            renameVariable(wh.condition, oldName, newName);
            renameVariable(wh.body, oldName, newName);
        } else if (node instanceof RepeatStatement) {
            RepeatStatement rep = (RepeatStatement) node;
            renameVariable(rep.condition, oldName, newName);
            renameVariable(rep.body, oldName, newName);
        } else if (node instanceof ForNumeric) {
            ForNumeric fn = (ForNumeric) node;
            renameVariable(fn.start, oldName, newName);
            renameVariable(fn.end, oldName, newName);
            renameVariable(fn.step, oldName, newName);
            renameVariable(fn.body, oldName, newName);
        } else if (node instanceof ForIn) {
            ForIn fi = (ForIn) node;
            for (int i = 0; i < fi.names.size(); i++) {
                if (fi.names.get(i).equals(oldName)) {
                    fi.names.set(i, newName);
                }
            }
            for (Expression exp : fi.iterators) {
                renameVariable(exp, oldName, newName);
            }
            renameVariable(fi.body, oldName, newName);
        } else if (node instanceof FunctionDeclaration) {
            renameVariable(((FunctionDeclaration) node).func, oldName, newName);
        } else if (node instanceof Block) {
            for (Statement stmt : ((Block) node).statements) {
                renameVariable(stmt, oldName, newName);
            }
        } else if (node instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) node;
            renameVariable(binary.left, oldName, newName);
            renameVariable(binary.right, oldName, newName);
        } else if (node instanceof UnaryOp) {
            renameVariable(((UnaryOp) node).expr, oldName, newName);
        } else if (node instanceof IndexExpr) {
            IndexExpr idx = (IndexExpr) node;
            renameVariable(idx.table, oldName, newName);
            renameVariable(idx.index, oldName, newName);
        } else if (node instanceof MemberExpr) {
            renameVariable(((MemberExpr) node).table, oldName, newName);
        } else if (node instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) node;
            renameVariable(call.callee, oldName, newName);
            for (Expression arg : call.args) {
                renameVariable(arg, oldName, newName);
            }
        } else if (node instanceof TableConstructor) {
            TableConstructor tc = (TableConstructor) node;
            if (tc.fields != null) {
                for (TableField field : tc.fields) {
                    renameVariable(field.key, oldName, newName);
                    renameVariable(field.value, oldName, newName);
                }
            }
        } else if (node instanceof FunctionLiteral) {
            renameVariable(((FunctionLiteral) node).body, oldName, newName);
        }
    }

    private void eliminateStructuredIfElse(Block block) {
        if (block == null || block.statements == null) {
            return;
        }
        List<Statement> stmts = block.statements;
        boolean changed = true;
        
        while (changed) {
            changed = false;
            for (int i = 0; i < stmts.size(); i++) {
                Statement stmt = stmts.get(i);
                
                if (isGotoIfStatement(stmt)) {
                    IfStatement ifStmt = (IfStatement) stmt;
                    GotoStatement gotoElse = (GotoStatement) ifStmt.blocks.get(0).statements.get(0);
                    String labelElse = gotoElse.label;
                    
                    // 寻找匹配的 LabelStatement labelElse
                    int labelElseIndex = findLabelIndex(stmts, i + 1, labelElse);
                    if (labelElseIndex != -1) {
                        // 检查在 labelElseIndex 之前，紧接着的那条语句是否是 GotoStatement (Label_End)
                        if (labelElseIndex > 0 && stmts.get(labelElseIndex - 1) instanceof GotoStatement) {
                            GotoStatement gotoEnd = (GotoStatement) stmts.get(labelElseIndex - 1);
                            String labelEnd = gotoEnd.label;
                            
                            // 寻找 Label_End
                            int labelEndIndex = findLabelIndex(stmts, labelElseIndex + 1, labelEnd);
                            if (labelEndIndex != -1) {
                                // 检查安全性：没有其它地方跳转到 labelElse 和 labelEnd
                                if (!hasOtherGotosTo(stmts, labelElse, i) && !hasOtherGotosTo(stmts, labelEnd, labelElseIndex - 1)) {
                                    
                                    // 提取 then body
                                    List<Statement> thenStmts = new ArrayList<>();
                                    for (int m = i + 1; m < labelElseIndex - 1; m++) {
                                        thenStmts.add(stmts.get(m));
                                    }
                                    Block thenBlock = new Block(new SourcePos(i + 1, -1));
                                    thenBlock.statements.addAll(thenStmts);
                                    
                                    // 提取 else body
                                    List<Statement> elseStmts = new ArrayList<>();
                                    for (int m = labelElseIndex + 1; m < labelEndIndex; m++) {
                                        elseStmts.add(stmts.get(m));
                                    }
                                    Block elseBlock = new Block(new SourcePos(labelElseIndex + 1, -1));
                                    elseBlock.statements.addAll(elseStmts);
                                    
                                    // 对条件取反
                                    Expression originalCond = ifStmt.conditions.get(0);
                                    Expression newCond = negateCondition(originalCond);
                                    
                                    // 重构 IfStatement
                                    IfStatement newIf = new IfStatement(newCond, thenBlock, elseBlock, ifStmt.pos);
                                    
                                    stmts.set(i, newIf);
                                    
                                    // 从后往前移出旧语句段
                                    for (int m = labelEndIndex; m > i; m--) {
                                        stmts.remove(m);
                                    }
                                    
                                    // 递归重构 thenBlock 和 elseBlock
                                    restructure(thenBlock);
                                    restructure(elseBlock);
                                    
                                    changed = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
