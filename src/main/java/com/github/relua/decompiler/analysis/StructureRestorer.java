package com.github.relua.decompiler.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

        // 2.5 重构数值 for 循环 (FORPREP/FORLOOP 拓扑还原)
        restoreForNumericLoops(block);

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
        
        // 保存原始 stmts 快照，用于构建 body（避免被后续修改干扰）
        List<Statement> originalStmts = new ArrayList<>(stmts);
        
        // 一次性收集所有候选
        List<ForInCandidate> candidates = collectForInCandidates(stmts);
        
        // 按 span 从小到大排序（先处理内层循环）
        candidates.sort((a, b) -> Integer.compare(a.span, b.span));
        
        // 确定每个共享 backwardGoto 的最外层消费者（span 最大者）
        Map<Integer, Integer> lastSharingSpan = new HashMap<>();
        for (ForInCandidate c : candidates) {
            int prev = lastSharingSpan.getOrDefault(c.backwardGotoIndex, -1);
            if (c.span > prev) {
                lastSharingSpan.put(c.backwardGotoIndex, c.span);
            }
        }
        
        // 跟踪已移除的原始索引，用于计算偏移
        List<Integer> allRemovedOriginalIndices = new ArrayList<>();
        
        for (ForInCandidate c : candidates) {
            // 计算偏移
            int iterOffset = 0, gotoOffset = 0, startOffset = 0, backGOffset = 0;
            for (int removedOrig : allRemovedOriginalIndices) {
                if (removedOrig < c.iteratorIndex) iterOffset--;
                if (removedOrig < c.initialGotoIndex) gotoOffset--;
                if (removedOrig < c.startLabelIndex) startOffset--;
                if (removedOrig < c.backwardGotoIndex) backGOffset--;
            }
            
            int adjIter = c.iteratorIndex + iterOffset;
            int adjGoto = c.initialGotoIndex + gotoOffset;
            int adjStart = c.startLabelIndex + startOffset;
            int adjBackG = c.backwardGotoIndex + backGOffset;
            
            // 验证索引有效
            if (adjIter < 0 || adjIter >= stmts.size()) continue;
            Statement iterStmt = stmts.get(adjIter);
            if (!isForInIteratorAssign(iterStmt)) continue;
            if (adjGoto < 0 || adjGoto >= stmts.size() || !(stmts.get(adjGoto) instanceof GotoStatement)) continue;
            if (adjStart < 0 || adjStart >= stmts.size() || !(stmts.get(adjStart) instanceof LabelStatement)) continue;
            
            LabelStatement startLabel = (LabelStatement) stmts.get(adjStart);
            
            // backwardGoto 可能已被移除
            GotoStatement backwardGoto = null;
            if (adjBackG >= 0 && adjBackG < stmts.size() && stmts.get(adjBackG) instanceof GotoStatement) {
                backwardGoto = (GotoStatement) stmts.get(adjBackG);
            }
            if (backwardGoto == null) continue;
            
            int regA = getFirstRegisterIndex(iterStmt);
            if (regA == -1) continue;
            
            // 判断是否为共享 backwardGoto 的最外层消费者
            boolean isLastSharing = (c.span == lastSharingSpan.getOrDefault(c.backwardGotoIndex, -1));
            
            // 判断 backwardGoto 是否属于当前循环
            boolean backwardGotoBelongs = backwardGoto.label.equals(startLabel.label);
            
            // 从原始 stmts 快照构建循环体（包含所有语句及 backwardGoto，
            // 让 restructure 在 body 内部处理嵌套 for-in 的模式匹配）
            Block bodyBlock = new Block(new SourcePos(c.startLabelIndex + 1, -1));
            for (int k = c.startLabelIndex + 1; k <= c.backwardGotoIndex; k++) {
                bodyBlock.statements.add(originalStmts.get(k));
            }
            
            // 清理尾部悬空 GotoStatement
            stripTrailingDanglingGotos(bodyBlock);
            
            // 重命名循环变量
            int lhsCount = getLhsCount(iterStmt);
            String kName, vName;
            if (lhsCount == 4) {
                kName = "R" + (regA + 2);
                vName = "R" + (regA + 3);
            } else {
                kName = "R" + (regA + 3);
                vName = "R" + (regA + 4);
            }
            renameVariable(bodyBlock, kName, "k");
            renameVariable(bodyBlock, vName, "v");
            
            List<Expression> iterators = getIteratorExpressions(iterStmt);
            List<String> names = new ArrayList<>();
            names.add("k");
            names.add("v");
            
            ForIn forIn = new ForIn(names, iterators, bodyBlock, iterStmt.pos);
            
            // 替换迭代器赋值语句
            stmts.set(adjIter, forIn);
            
            // 从父块移除结构元素（使用原始索引）
            Set<Integer> toRemoveOrigIndices = new HashSet<>();
            toRemoveOrigIndices.add(c.initialGotoIndex);
            toRemoveOrigIndices.add(c.startLabelIndex);
            
            // 移除 startLabel+1 到 backwardGoto-1 范围内所有尚未移除的语句
            // （这些代码已复制到 ForIn body，不能留在 parent）
            for (int k = c.startLabelIndex + 1; k < c.backwardGotoIndex; k++) {
                boolean alreadyRemoved = false;
                for (int removedOrig : allRemovedOriginalIndices) {
                    if (removedOrig == k) { alreadyRemoved = true; break; }
                }
                if (!alreadyRemoved) {
                    toRemoveOrigIndices.add(k);
                }
            }
            
            // backwardGoto 移除策略：
            // 只有最外层共享者（或唯一拥有者）才从 parent 移除 backwardGoto
            if (isLastSharing) {
                toRemoveOrigIndices.add(c.backwardGotoIndex);
            }
            
            // 按调整后的索引降序移除
            List<int[]> removePairs = new ArrayList<>();
            for (int origIdx : toRemoveOrigIndices) {
                int adjIdx = origIdx;
                for (int removedOrig : allRemovedOriginalIndices) {
                    if (removedOrig < origIdx) adjIdx--;
                }
                removePairs.add(new int[]{adjIdx, origIdx});
            }
            removePairs.sort((a2, b2) -> Integer.compare(b2[0], a2[0]));
            
            for (int[] pair : removePairs) {
                int adjIdx = pair[0];
                int origIdx = pair[1];
                if (adjIdx >= 0 && adjIdx < stmts.size()) {
                    stmts.remove(adjIdx);
                    allRemovedOriginalIndices.add(origIdx);
                }
            }
            
            // 递归重构循环体
            restructure(bodyBlock);
        }
        
        // 第二轮：跨层搜索 — 当当前块有 backward goto + label 但缺少 iterator 时，
        // 从嵌套 if-body 中提取 iterator 到当前块
        restoreForInLoopsCrossLevel(block);
    }
    
    /**
     * 跨层 for-in 恢复：在当前块中找到未匹配的 backward goto，
     * 搜索嵌套 if-body 中对应的 iterator + forward goto，提取到当前块后构建 ForIn。
     */
    private void restoreForInLoopsCrossLevel(Block block) {
        if (block == null || block.statements == null) return;
        List<Statement> stmts = block.statements;
        
        // 收集当前块中所有 backward goto 目标 label
        Set<String> backwardGotoLabels = new HashSet<>();
        for (Statement s : stmts) {
            if (s instanceof GotoStatement) {
                backwardGotoLabels.add(((GotoStatement) s).label);
            }
        }
        
        // 对于每个 backward goto 的 label，在嵌套块中搜索 iterator + forward goto
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < stmts.size(); i++) {
                if (!(stmts.get(i) instanceof GotoStatement)) continue;
                GotoStatement backGoto = (GotoStatement) stmts.get(i);
                
                // 搜索嵌套块中的 iterator + forwardGoto 模式
                // forwardGoto 的目标 label 应在当前块中且位于 backwardGoto 附近
                ForInExtract extract = null;
                int sourceIfIndex = -1;
                for (int j = i - 1; j >= 0; j--) {
                    if (stmts.get(j) instanceof IfStatement) {
                        extract = searchForInInIf((IfStatement) stmts.get(j));
                        if (extract != null) {
                            sourceIfIndex = j;
                            break;
                        }
                    }
                }
                if (extract == null) continue;
                
                // 构建 ForIn 循环体：收集 sourceIf 之后到 backwardGoto 之间的语句
                Block bodyBlock = new Block(new SourcePos(sourceIfIndex + 1, -1));
                for (int k = sourceIfIndex + 1; k < i; k++) {
                    bodyBlock.statements.add(stmts.get(k));
                }
                
                stripTrailingDanglingGotos(bodyBlock);
                
                // 重命名循环变量
                int regA = getFirstRegisterIndex(extract.iterator);
                if (regA == -1) continue;
                int lhsCount = getLhsCount(extract.iterator);
                String kName, vName;
                if (lhsCount == 4) {
                    kName = "R" + (regA + 2);
                    vName = "R" + (regA + 3);
                } else {
                    kName = "R" + (regA + 3);
                    vName = "R" + (regA + 4);
                }
                renameVariable(bodyBlock, kName, "k");
                renameVariable(bodyBlock, vName, "v");
                
                List<Expression> iterators = getIteratorExpressions(extract.iterator);
                List<String> names = new ArrayList<>();
                names.add("k");
                names.add("v");
                
                ForIn forIn = new ForIn(names, iterators, bodyBlock, extract.iterator.pos);
                
                // 用 ForIn 替换 sourceIf 位置
                stmts.set(sourceIfIndex, forIn);
                
                // 移除 sourceIf+1 到 backwardGoto 之间的所有语句
                for (int k = i; k > sourceIfIndex; k--) {
                    stmts.remove(k);
                }
                
                restructure(bodyBlock);
                changed = true;
                break;
            }
        }
    }
    
    /**
     * 递归搜索 IfStatement 的所有 block 中的 iterator + forwardGoto 模式。
     * 找到后提取 iterator 并从嵌套块中移除。
     */
    private ForInExtract searchForInInIf(IfStatement ifStmt) {
        for (Block nested : ifStmt.blocks) {
            ForInExtract result = searchForInInBlock(nested);
            if (result != null) return result;
        }
        if (ifStmt.elseBlock != null) {
            ForInExtract result = searchForInInBlock(ifStmt.elseBlock);
            if (result != null) return result;
        }
        return null;
    }
    
    /**
     * 在块中搜索 iterator + forwardGoto 模式（通常在块的末尾）。
     */
    private ForInExtract searchForInInBlock(Block block) {
        if (block == null || block.statements == null) return null;
        List<Statement> stmts = block.statements;
        
        // 从末尾向前搜索：找 GotoStatement 前面紧跟 iterator
        for (int i = stmts.size() - 1; i >= 1; i--) {
            if (stmts.get(i) instanceof GotoStatement && isForInIteratorAssign(stmts.get(i - 1))) {
                ForInExtract extract = new ForInExtract();
                extract.iterator = stmts.get(i - 1);
                extract.forwardGoto = (GotoStatement) stmts.get(i);
                // 从块中移除这两个语句
                stmts.remove(i);
                stmts.remove(i - 1);
                return extract;
            }
        }
        
        // 递归搜索嵌套的 IfStatement
        for (int i = stmts.size() - 1; i >= 0; i--) {
            if (stmts.get(i) instanceof IfStatement) {
                ForInExtract result = searchForInInIf((IfStatement) stmts.get(i));
                if (result != null) return result;
            }
        }
        return null;
    }
    
    private static class ForInExtract {
        Statement iterator;
        GotoStatement forwardGoto;
    }
    
    /**
     * 收集当前块中所有潜在的 for-in 循环候选
     */
    private List<ForInCandidate> collectForInCandidates(List<Statement> stmts) {
        List<ForInCandidate> candidates = new ArrayList<>();
        
        for (int i = 0; i < stmts.size(); i++) {
            Statement stmt = stmts.get(i);
            if (!isForInIteratorAssign(stmt)) continue;
            
            int regA = getFirstRegisterIndex(stmt);
            if (regA == -1) continue;
            
            // 向后搜索最近的 GotoStatement + LabelStatement 模式
            int gotoIndex = -1;
            int maxSearch = Math.min(i + 10, stmts.size() - 2);
            for (int j = i + 1; j <= maxSearch; j++) {
                if (stmts.get(j) instanceof GotoStatement
                        && j + 1 < stmts.size()
                        && stmts.get(j + 1) instanceof LabelStatement) {
                    gotoIndex = j;
                    break;
                }
            }
            if (gotoIndex == -1) {
                continue;
            }
            
            LabelStatement startLabel = (LabelStatement) stmts.get(gotoIndex + 1);
            String labelStart = startLabel.label;
            
            // 寻找该循环的 backwardGoto：
            // 目标精确匹配 labelStart 的 GotoStatement（确保属于当前循环）
            int backwardGotoIndex = -1;
            for (int k = gotoIndex + 2; k < stmts.size(); k++) {
                if (stmts.get(k) instanceof GotoStatement) {
                    GotoStatement gs = (GotoStatement) stmts.get(k);
                    if (gs.label.equals(labelStart)) {
                        backwardGotoIndex = k;
                        break;
                    }
                }
            }
            if (backwardGotoIndex == -1) {
                continue;
            }
            
            ForInCandidate c = new ForInCandidate();
            c.iteratorIndex = i;
            c.initialGotoIndex = gotoIndex;
            c.startLabelIndex = gotoIndex + 1;
            c.backwardGotoIndex = backwardGotoIndex;
            c.span = backwardGotoIndex - i;
            candidates.add(c);
        }
        return candidates;
    }
    
    /**
     * 从标签名中提取编号（如 "L58" -> 58）
     */
    private int parseLabelNumber(String label) {
        if (label != null && label.startsWith("L")) {
            try {
                return Integer.parseInt(label.substring(1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
    
    /**
     * 清理块尾部悬空的 GotoStatement（指向块外部的标签）
     */
    private void stripTrailingDanglingGotos(Block block) {
        if (block == null || block.statements.isEmpty()) return;
        while (!block.statements.isEmpty()) {
            Statement last = block.statements.get(block.statements.size() - 1);
            if (last instanceof GotoStatement) {
                String label = ((GotoStatement) last).label;
                if (!containsLabel(block.statements, label)) {
                    block.statements.remove(block.statements.size() - 1);
                    continue;
                }
            }
            break;
        }
    }
    
    /**
     * 检查语句列表中是否包含指定名称的 LabelStatement
     */
    private boolean containsLabel(List<Statement> stmts, String labelName) {
        for (Statement stmt : stmts) {
            if (stmt instanceof LabelStatement && ((LabelStatement) stmt).label.equals(labelName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * for-in 循环候选的数据结构
     */
    private static class ForInCandidate {
        int iteratorIndex;
        int initialGotoIndex;
        int startLabelIndex;
        int backwardGotoIndex;
        int span;
    }

    private boolean isForInIteratorAssign(Statement stmt) {
        if (stmt instanceof Assign) {
            Assign assign = (Assign) stmt;
            if (assign.left.size() == 3) {
                return areContinuousRegisters(assign.left.get(0), assign.left.get(1), assign.left.get(2));
            }
            // TFORLOOP 的 CALL c=4 产生 4 个返回值 (f, s, var, control)
            if (assign.left.size() == 4) {
                return areContinuousRegisters(assign.left.get(0), assign.left.get(1), assign.left.get(2))
                        && assign.left.get(3) instanceof Name
                        && ((Name) assign.left.get(3)).name.matches("R\\d+")
                        && Integer.parseInt(((Name) assign.left.get(3)).name.substring(1))
                            == Integer.parseInt(((Name) assign.left.get(2)).name.substring(1)) + 1;
            }
        } else if (stmt instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) stmt;
            if (local.names.size() == 3) {
                return areContinuousRegisterNames(local.names.get(0), local.names.get(1), local.names.get(2));
            }
            if (local.names.size() == 4) {
                return areContinuousRegisterNames(local.names.get(0), local.names.get(1), local.names.get(2))
                        && local.names.get(3).matches("R\\d+")
                        && Integer.parseInt(local.names.get(3).substring(1))
                            == Integer.parseInt(local.names.get(2).substring(1)) + 1;
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

    private int getLhsCount(Statement stmt) {
        if (stmt instanceof Assign) {
            return ((Assign) stmt).left.size();
        } else if (stmt instanceof LocalAssign) {
            return ((LocalAssign) stmt).names.size();
        }
        return 0;
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

    private void restoreForNumericLoops(Block block) {
        if (block == null || block.statements == null) {
            return;
        }
        List<Statement> stmts = block.statements;
        boolean changed = true;
        
        while (changed) {
            changed = false;
            for (int i = 0; i < stmts.size(); i++) {
                if (i + 4 >= stmts.size()) {
                    break;
                }
                
                Statement s1 = stmts.get(i);
                Statement s2 = stmts.get(i + 1);
                Statement s3 = stmts.get(i + 2);
                
                int r1 = getAssignRegisterIndex(s1);
                int r2 = getAssignRegisterIndex(s2);
                int r3 = getAssignRegisterIndex(s3);
                
                if (r1 != -1 && r2 == r1 + 1 && r3 == r1 + 2) {
                    Statement s4 = stmts.get(i + 3);
                    if (s4 instanceof GotoStatement) {
                        String labelEnd = ((GotoStatement) s4).label;
                        
                        Statement s5 = stmts.get(i + 4);
                        if (s5 instanceof LabelStatement) {
                            String labelStart = ((LabelStatement) s5).label;
                            
                            int endLabelIdx = findLabelIndex(stmts, i + 5, labelEnd);
                            if (endLabelIdx != -1) {
                                GotoStatement backGoto = null;
                                int backGotoIdx = -1;
                                if (endLabelIdx > 0 && stmts.get(endLabelIdx - 1) instanceof GotoStatement) {
                                    backGoto = (GotoStatement) stmts.get(endLabelIdx - 1);
                                    backGotoIdx = endLabelIdx - 1;
                                } else if (endLabelIdx + 1 < stmts.size() && stmts.get(endLabelIdx + 1) instanceof GotoStatement) {
                                    backGoto = (GotoStatement) stmts.get(endLabelIdx + 1);
                                    backGotoIdx = endLabelIdx + 1;
                                }
                                
                                if (backGoto != null && backGoto.label.equals(labelStart)) {
                                    Expression startExpr = getAssignRightExpr(s1);
                                    Expression limitExpr = getAssignRightExpr(s2);
                                    Expression stepExpr = getAssignRightExpr(s3);
                                    
                                    if (isNumberOne(stepExpr)) {
                                        stepExpr = null;
                                    }
                                    
                                    String varName = "R" + (r1 + 3);
                                    
                                    int bodyEnd = (backGotoIdx < endLabelIdx) ? endLabelIdx - 1 : endLabelIdx;
                                    Block bodyBlock = new Block(new SourcePos(i + 5, -1));
                                    for (int k = i + 5; k < bodyEnd; k++) {
                                        bodyBlock.statements.add(stmts.get(k));
                                    }
                                    
                                    stripTrailingDanglingGotos(bodyBlock);
                                    
                                    ForNumeric forNum = new ForNumeric(varName, startExpr, limitExpr, stepExpr, bodyBlock, s1.pos);
                                    
                                    stmts.set(i, forNum);
                                    
                                    int removeLimit = Math.max(endLabelIdx, backGotoIdx);
                                    for (int k = removeLimit; k > i; k--) {
                                        stmts.remove(k);
                                    }
                                    
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

    private int getAssignRegisterIndex(Statement stmt) {
        if (stmt instanceof Assign) {
            Assign assign = (Assign) stmt;
            if (assign.left.size() == 1 && assign.left.get(0) instanceof Name) {
                String name = ((Name) assign.left.get(0)).name;
                if (name.matches("R\\d+")) {
                    return Integer.parseInt(name.substring(1));
                }
            }
        } else if (stmt instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) stmt;
            if (local.names.size() == 1) {
                String name = local.names.get(0);
                if (name.matches("R\\d+")) {
                    return Integer.parseInt(name.substring(1));
                }
            }
        }
        return -1;
    }

    private Expression getAssignRightExpr(Statement stmt) {
        if (stmt instanceof Assign) {
            Assign assign = (Assign) stmt;
            if (assign.right.size() == 1) {
                return assign.right.get(0);
            }
        } else if (stmt instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) stmt;
            if (local.right.size() == 1) {
                return local.right.get(0);
            }
        }
        return null;
    }

    private boolean isNumberOne(Expression expr) {
        if (expr instanceof NumberConst) {
            NumberConst num = (NumberConst) expr;
            return num.value == 1.0;
        }
        return false;
    }
}
