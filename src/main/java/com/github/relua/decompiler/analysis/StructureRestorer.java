package com.github.relua.decompiler.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.relua.ast.*;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;

public class StructureRestorer {
    private Chunk chunk;

    public StructureRestorer() {
    }

    public StructureRestorer(Chunk chunk) {
        this.chunk = chunk;
    }

    public void restructure(Block block) {
        if (block == null || block.statements == null) {
            return;
        }

        // 0. 合并由 LOADBOOL 引起的跨 Block 控制流
        mergeLoadBoolControlFlow(block);

        // 1. 递归重构嵌套的 Block
        for (Statement stmt : block.statements) {
            restructureNestedBlocks(stmt);
        }

        // 2. 重构泛型 for 循环 (FORGPREP/FORGLOOP 拓扑还原)
        restoreForInLoops(block);

        // 2.5 重构数值 for 循环 (FORPREP/FORLOOP 拓扑还原)
        restoreForNumericLoops(block);

        // 2.6 重构 while 循环 (GOTO 循环还原)。必须在数值 for 之后执行，
        // 否则数值 for body 里的单分支 if/goto 会被提前折成 while，破坏 FORLOOP 拓扑。
        restoreWhileLoops(block);

        // 3. 重构结构化的 if-else 分支
        eliminateInvertedIfElse(block);
        eliminateSingleGotoIfElse(block);
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
            Chunk subChunk = ((FunctionDeclaration) statement).func.getChunk();
            new StructureRestorer(subChunk).restructure(((FunctionDeclaration) statement).func.body);
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
            int numVars = 2;
            if (backwardGoto != null && backwardGoto.pos != null && backwardGoto.pos.pc > 0 && chunk != null) {
                Instruction tforInstruction = chunk.getInstruction(backwardGoto.pos.pc - 1);
                if (tforInstruction != null && tforInstruction.getOpcode() == Opcode.TFORLOOP) {
                    numVars = tforInstruction.getC();
                }
            }

            List<Expression> iterators = getIteratorExpressions(iterStmt);
            List<String> names = new ArrayList<>();
            addForInLoopVariables(bodyBlock, names, regA, lhsCount, numVars);
            
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
                int numVars = 2;
                if (backGoto != null && backGoto.pos != null && backGoto.pos.pc > 0 && chunk != null) {
                    Instruction tforInstruction = chunk.getInstruction(backGoto.pos.pc - 1);
                    if (tforInstruction != null && tforInstruction.getOpcode() == Opcode.TFORLOOP) {
                        numVars = tforInstruction.getC();
                    }
                }

                List<Expression> iterators = getIteratorExpressions(extract.iterator);
                List<String> names = new ArrayList<>();
                addForInLoopVariables(bodyBlock, names, regA, lhsCount, numVars);
                
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

    private int getRegisterIndex(String name) {
        if (name == null) {
            return -1;
        }
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("^(?:chunk_|module_)?R(\\d+)$");
        java.util.regex.Matcher m = p.matcher(name);
        if (m.matches()) {
            return Integer.parseInt(m.group(1));
        }
        return -1;
    }

    private void addForInLoopVariables(Block bodyBlock, List<String> names, int baseRegister, int lhsCount, int numVars) {
        int firstValueRegister = firstForInValueRegister(baseRegister, lhsCount);
        if (numVars == 1) {
            renameVariable(bodyBlock, scopedPhysicalRegisterName(firstValueRegister), "v");
            names.add("v");
            return;
        }

        renameVariable(bodyBlock, scopedPhysicalRegisterName(firstValueRegister), "k");
        renameVariable(bodyBlock, scopedPhysicalRegisterName(firstValueRegister + 1), "v");
        names.add("k");
        names.add("v");
    }

    private int firstForInValueRegister(int baseRegister, int lhsCount) {
        return lhsCount == 4 ? baseRegister + 2 : baseRegister + 3;
    }

    private String scopedPhysicalRegisterName(int register) {
        return registerNamePrefix() + physicalRegisterName(register);
    }

    private String registerNamePrefix() {
        if (chunk != null && "main".equals(chunk.getFunction())) {
            return isModuleScenario() ? "module_" : "chunk_";
        }
        return "";
    }

    private String physicalRegisterName(int register) {
        return "R" + register;
    }

    private boolean isForInIteratorAssign(Statement stmt) {
        if (stmt instanceof Assign) {
            Assign assign = (Assign) stmt;
            if (assign.left.size() == 3) {
                return areContinuousRegisters(assign.left.get(0), assign.left.get(1), assign.left.get(2));
            }
            // TFORLOOP 的 CALL c=4 产生 4 个返回值 (f, s, var, control)
            if (assign.left.size() == 4) {
                int r2 = getRegisterIndex(((Name) assign.left.get(2)).name);
                int r3 = getRegisterIndex(((Name) assign.left.get(3)).name);
                return areContinuousRegisters(assign.left.get(0), assign.left.get(1), assign.left.get(2))
                        && r3 != -1
                        && r2 != -1
                        && r3 == r2 + 1;
            }
        } else if (stmt instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) stmt;
            if (local.names.size() == 3) {
                return areContinuousRegisterNames(local.names.get(0), local.names.get(1), local.names.get(2));
            }
            if (local.names.size() == 4) {
                int r2 = getRegisterIndex(local.names.get(2));
                int r3 = getRegisterIndex(local.names.get(3));
                return areContinuousRegisterNames(local.names.get(0), local.names.get(1), local.names.get(2))
                        && r3 != -1
                        && r2 != -1
                        && r3 == r2 + 1;
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
        int r1 = getRegisterIndex(n1);
        int r2 = getRegisterIndex(n2);
        int r3 = getRegisterIndex(n3);
        if (r1 != -1 && r2 != -1 && r3 != -1) {
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
        return getRegisterIndex(name);
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
                                    
                                    String varName = physicalRegisterName(r1 + 3);
                                    
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
                return getRegisterIndex(((Name) assign.left.get(0)).name);
            }
        } else if (stmt instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) stmt;
            if (local.names.size() == 1) {
                return getRegisterIndex(local.names.get(0));
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

    private void eliminateSingleGotoIfElse(Block block) {
        if (block == null || block.statements == null) {
            return;
        }
        List<Statement> stmts = block.statements;
        boolean changed = true;

        while (changed) {
            changed = false;
            for (int i = 0; i < stmts.size(); i++) {
                Statement stmt = stmts.get(i);
                if (stmt instanceof IfStatement) {
                    IfStatement ifStmt = (IfStatement) stmt;
                    if (ifStmt.conditions.size() == 1 && ifStmt.blocks.size() == 1 && ifStmt.elseBlock == null) {
                        Block body = ifStmt.blocks.get(0);
                        if (body.statements.isEmpty()) {
                            continue;
                        }
                        Statement last = body.statements.get(body.statements.size() - 1);
                        if (last instanceof GotoStatement) {
                            GotoStatement gotoEnd = (GotoStatement) last;
                            String labelEnd = gotoEnd.label;

                            int labelEndIndex = findLabelIndex(stmts, i + 1, labelEnd);
                            if (labelEndIndex != -1) {
                                boolean hasOther = hasOtherGotosTo(stmts, labelEnd, i);
                                if (!hasOther) {
                                    // 提取 then block 语句 (除去最后的 gotoEnd)
                                    List<Statement> thenStmts = new ArrayList<>();
                                    for (int k = 0; k < body.statements.size() - 1; k++) {
                                        thenStmts.add(body.statements.get(k));
                                    }

                                    // 提取 else block 语句 [i + 1, labelEndIndex - 1]
                                    List<Statement> elseStmts = new ArrayList<>();
                                    for (int m = i + 1; m < labelEndIndex; m++) {
                                        elseStmts.add(stmts.get(m));
                                    }

                                    Block thenBlock = new Block(body.pos);
                                    thenBlock.statements.addAll(thenStmts);
                                    Block elseBlock = new Block(new SourcePos(i + 1, -1));
                                    elseBlock.statements.addAll(elseStmts);

                                    IfStatement newIf = new IfStatement(ifStmt.conditions.get(0), thenBlock, elseBlock, ifStmt.pos);
                                    stmts.set(i, newIf);

                                    // 从后往前移除已提取的语句和 LabelStatement
                                    for (int m = labelEndIndex; m > i; m--) {
                                        stmts.remove(m);
                                    }

                                    // 递归重组 then 和 else 块
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

    private void eliminateInvertedIfElse(Block block) {
        if (block == null || block.statements == null) {
            return;
        }
        List<Statement> stmts = block.statements;
        boolean changed = true;

        while (changed) {
            changed = false;
            for (int i = 0; i < stmts.size(); i++) {
                Statement stmt = stmts.get(i);
                if (stmt instanceof IfStatement) {
                    IfStatement ifStmt = (IfStatement) stmt;
                    if (ifStmt.conditions.size() == 1 && ifStmt.blocks.size() == 1 && ifStmt.elseBlock == null) {
                        Block body = ifStmt.blocks.get(0);
                        if (body.statements.isEmpty()) {
                            continue;
                        }
                        // 确保 then 块的最后一条语句不是 GotoStatement (若是，则由 eliminateSingleGotoIfElse 处理)
                        Statement thenLast = body.statements.get(body.statements.size() - 1);
                        if (thenLast instanceof GotoStatement) {
                            continue;
                        }

                        // 从 i + 1 往后寻找合适结尾的 GotoStatement (即下一个语句是相应的 LabelStatement)
                        for (int j = i + 1; j < stmts.size() - 1; j++) {
                            Statement targetStmt = stmts.get(j);
                            if (targetStmt instanceof GotoStatement) {
                                GotoStatement gotoEnd = (GotoStatement) targetStmt;
                                String labelEnd = gotoEnd.label;

                                Statement nextStmt = stmts.get(j + 1);
                                if (nextStmt instanceof LabelStatement && ((LabelStatement) nextStmt).label.equals(labelEnd)) {
                                    int labelEndIndex = j + 1;
                                    boolean hasOther = hasOtherGotosToExcept(stmts, labelEnd, j);
                                    if (!hasOther) {
                                        // 提取 then block 语句 (原 ifStmt 的 body)
                                        Block thenBlock = body;

                                        // 提取 else block 语句 [i + 1, j - 1]
                                        List<Statement> elseStmts = new ArrayList<>();
                                        for (int m = i + 1; m < j; m++) {
                                            elseStmts.add(stmts.get(m));
                                        }
                                        Block elseBlock = new Block(new SourcePos(i + 1, -1));
                                        elseBlock.statements.addAll(elseStmts);

                                        IfStatement newIf = new IfStatement(ifStmt.conditions.get(0), thenBlock, elseBlock, ifStmt.pos);
                                        stmts.set(i, newIf);

                                        // 从后往前移除已提取 of 语句和 LabelStatement
                                        for (int m = labelEndIndex; m > i; m--) {
                                            stmts.remove(m);
                                        }

                                        // 递归重组 then 和 else 块
                                        restructure(thenBlock);
                                        restructure(elseBlock);

                                        changed = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (changed) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean hasOtherGotosToExcept(List<Statement> stmts, String labelName, int exceptIndex) {
        for (int i = 0; i < stmts.size(); i++) {
            if (i == exceptIndex) continue;
            Statement stmt = stmts.get(i);
            if (stmt instanceof GotoStatement && ((GotoStatement) stmt).label.equals(labelName)) {
                return true;
            }
            if (stmt instanceof IfStatement) {
                IfStatement ifStmt = (IfStatement) stmt;
                for (Block b : ifStmt.blocks) {
                    if (hasOtherGotosToExcept(b.statements, labelName, -1)) {
                        return true;
                    }
                }
                if (ifStmt.elseBlock != null) {
                    if (hasOtherGotosToExcept(ifStmt.elseBlock.statements, labelName, -1)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void mergeLoadBoolControlFlow(Block block) {
        if (block == null || block.statements == null) {
            return;
        }
        List<Statement> stmts = block.statements;
        boolean changed = true;

        while (changed) {
            changed = false;
            for (int i = 0; i < stmts.size(); i++) {
                Statement stmt = stmts.get(i);
                if (stmt instanceof IfStatement) {
                    IfStatement ifStmt = (IfStatement) stmt;
                    
                    if (i + 2 < stmts.size()) {
                        Statement next1 = stmts.get(i + 1);
                        Statement next2 = stmts.get(i + 2);
                        if (next1 instanceof LabelStatement && isAssignTrue(next2)) {
                            LabelStatement labelStmt = (LabelStatement) next1;
                            Assign assignTrue = (Assign) next2;
                            Expression targetVar = assignTrue.left.get(0);
                            
                            if (hasAssignFalse(ifStmt.elseBlock, targetVar)) {
                                if (hasGotoTarget(ifStmt.blocks.get(0), labelStmt.label)) {
                                    replaceGotoWithAssignTrue(ifStmt.blocks.get(0), labelStmt.label, targetVar, assignTrue.pos);
                                    
                                    stmts.remove(i + 2);
                                    stmts.remove(i + 1);
                                    
                                    changed = true;
                                    break;
                                } else {
                                    int innerIfIdx = getTrailingIfStatementIndex(ifStmt.blocks.get(0));
                                    if (innerIfIdx != -1) {
                                        IfStatement innerIf = (IfStatement) ifStmt.blocks.get(0).statements.get(innerIfIdx);
                                        if (innerIf.blocks.size() == 1 && innerIf.elseBlock == null 
                                                && hasAssignFalse(innerIf.blocks.get(0), targetVar)) {
                                            
                                            Block innerElse = new Block(assignTrue.pos);
                                            innerElse.statements.add(assignTrue);
                                            
                                            IfStatement newInnerIf = new IfStatement(innerIf.conditions, innerIf.blocks, innerElse, innerIf.pos);
                                            ifStmt.blocks.get(0).statements.set(innerIfIdx, newInnerIf);
                                            
                                            stmts.remove(i + 2);
                                            stmts.remove(i + 1);
                                            
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
    }

    private int getTrailingIfStatementIndex(Block block) {
        if (block == null || block.statements.isEmpty()) return -1;
        for (int i = block.statements.size() - 1; i >= 0; i--) {
            Statement s = block.statements.get(i);
            if (!(s instanceof LabelStatement)) {
                if (s instanceof IfStatement) {
                    return i;
                }
                break;
            }
        }
        return -1;
    }

    private boolean isAssignTrue(Statement stmt) {
        if (!(stmt instanceof Assign)) return false;
        Assign assign = (Assign) stmt;
        if (assign.left.size() != 1 || assign.right.size() != 1) return false;
        Expression val = assign.right.get(0);
        return val instanceof BooleanConst && ((BooleanConst) val).value;
    }

    private boolean hasAssignFalse(Block block, Expression targetVar) {
        if (block == null || block.statements == null) return false;
        for (Statement stmt : block.statements) {
            if (stmt instanceof Assign) {
                Assign assign = (Assign) stmt;
                if (assign.left.size() == 1 && assign.right.size() == 1) {
                    Expression var = assign.left.get(0);
                    Expression val = assign.right.get(0);
                    if (isSameVariable(var, targetVar) && val instanceof BooleanConst && !((BooleanConst) val).value) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isSameVariable(Expression e1, Expression e2) {
        if (e1 instanceof Name && e2 instanceof Name) {
            return ((Name) e1).name.equals(((Name) e2).name);
        }
        return false;
    }

    private boolean hasGotoTarget(AstNode node, String labelName) {
        if (node == null) return false;
        if (node instanceof GotoStatement) {
            return ((GotoStatement) node).label.equals(labelName);
        } else if (node instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) node;
            for (Block block : ifStmt.blocks) {
                if (hasGotoTarget(block, labelName)) return true;
            }
            return hasGotoTarget(ifStmt.elseBlock, labelName);
        } else if (node instanceof Block) {
            for (Statement stmt : ((Block) node).statements) {
                if (hasGotoTarget(stmt, labelName)) return true;
            }
        }
        return false;
    }

    private void replaceGotoWithAssignTrue(AstNode node, String labelName, Expression targetVar, SourcePos pos) {
        if (node == null) return;
        if (node instanceof Block) {
            Block block = (Block) node;
            for (int i = 0; i < block.statements.size(); i++) {
                Statement stmt = block.statements.get(i);
                if (stmt instanceof GotoStatement && ((GotoStatement) stmt).label.equals(labelName)) {
                    block.statements.set(i, new Assign(targetVar, new BooleanConst(true, pos), pos));
                } else {
                    replaceGotoWithAssignTrue(stmt, labelName, targetVar, pos);
                }
            }
        } else if (node instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) node;
            for (Block block : ifStmt.blocks) {
                replaceGotoWithAssignTrue(block, labelName, targetVar, pos);
            }
            replaceGotoWithAssignTrue(ifStmt.elseBlock, labelName, targetVar, pos);
        }
    }

    private void restoreWhileLoops(Block block) {
        if (block == null || block.statements == null) {
            return;
        }
        List<Statement> stmts = block.statements;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < stmts.size(); i++) {
                Statement stmt = stmts.get(i);
                if (stmt instanceof LabelStatement) {
                    LabelStatement labelStmt = (LabelStatement) stmt;
                    String labelName = labelStmt.label;

                    // Find a GotoStatement targeting this labelName
                    int gotoIdx = -1;
                    for (int j = i + 1; j < stmts.size(); j++) {
                        if (stmts.get(j) instanceof GotoStatement) {
                            GotoStatement gs = (GotoStatement) stmts.get(j);
                            if (gs.label.equals(labelName)) {
                                gotoIdx = j;
                                break;
                            }
                        }
                    }

                    if (gotoIdx != -1) {
                        // Check if there is an IfStatement between i and gotoIdx
                        int ifIdx = -1;
                        for (int j = i + 1; j < gotoIdx; j++) {
                            if (stmts.get(j) instanceof IfStatement) {
                                ifIdx = j;
                                break;
                            }
                        }

                        if (ifIdx != -1) {
                            IfStatement ifStmt = (IfStatement) stmts.get(ifIdx);
                            // Verify standard shape of loop condition ifStmt:
                            // 1 condition, 1 block, no else
                            if (ifStmt.conditions.size() == 1 && ifStmt.blocks.size() == 1 && ifStmt.elseBlock == null) {
                                // Find any preparation statements between label (i) and ifStmt (ifIdx)
                                List<Statement> prepStmts = new ArrayList<>();
                                for (int j = i + 1; j < ifIdx; j++) {
                                    prepStmts.add(stmts.get(j));
                                }

                                // We must make sure prepStmts only contain assignments
                                boolean prepsValid = true;
                                for (Statement prep : prepStmts) {
                                    if (!(prep instanceof Assign) && !(prep instanceof LocalAssign)) {
                                        prepsValid = false;
                                        break;
                                    }
                                }

                                if (prepsValid) {
                                    // Check if there are other gotos targeting this label
                                    if (!hasOtherGotosTo(stmts, labelName, gotoIdx)) {
                                        // Let's perform inlining on the condition
                                        Expression cond = ifStmt.conditions.get(0);
                                        Set<Statement> inlinedPreps = new HashSet<>();
                                        for (int j = prepStmts.size() - 1; j >= 0; j--) {
                                            Statement prep = prepStmts.get(j);
                                            String varName = null;
                                            Expression rightExpr = null;
                                            if (prep instanceof LocalAssign) {
                                                LocalAssign la = (LocalAssign) prep;
                                                if (la.names.size() == 1 && la.right.size() == 1) {
                                                    varName = la.names.get(0);
                                                    rightExpr = la.right.get(0);
                                                }
                                            } else if (prep instanceof Assign) {
                                                Assign ass = (Assign) prep;
                                                if (ass.left.size() == 1 && ass.right.size() == 1 && ass.left.get(0) instanceof Name) {
                                                    varName = ((Name) ass.left.get(0)).name;
                                                    rightExpr = ass.right.get(0);
                                                }
                                            }

                                        if (varName != null && rightExpr != null) {
                                                // Check if varName is referenced in cond
                                                if (containsVar(cond, varName)) {
                                                    // 安全检查：如果该变量在循环体内部被重新赋值（循环携带变量），
                                                    // 则不能将其初始值内联到条件表达式中，否则会破坏循环语义
                                                    boolean assignedInLoopBody = false;
                                                    // 检查 ifStmt then-block
                                                    for (Statement s : ifStmt.blocks.get(0).statements) {
                                                        if (isAssignedInStatement(s, varName)) {
                                                            assignedInLoopBody = true;
                                                            break;
                                                        }
                                                    }
                                                    // 检查 ifStmt 之后到 gotoIdx 之间的语句（循环体尾部）
                                                    if (!assignedInLoopBody) {
                                                        for (int k = ifIdx + 1; k < gotoIdx; k++) {
                                                            if (isAssignedInStatement(stmts.get(k), varName)) {
                                                                assignedInLoopBody = true;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    if (!assignedInLoopBody) {
                                                        cond = replaceVariable(cond, varName, rightExpr);
                                                        inlinedPreps.add(prep);
                                                    }
                                                }
                                            }
                                        }

                                        // Build the loop body:
                                        // 1. Statements in ifStmt then-block
                                        // 2. Statements in stmts after ifStmt (from ifIdx + 1 to gotoIdx - 1)
                                        Block loopBody = new Block(ifStmt.blocks.get(0).pos);
                                        loopBody.statements.addAll(ifStmt.blocks.get(0).statements);
                                        for (int j = ifIdx + 1; j < gotoIdx; j++) {
                                            loopBody.statements.add(stmts.get(j));
                                        }

                                        WhileStatement whileStmt = new WhileStatement(cond, loopBody, labelStmt.pos);

                                        // Let's build the list of replacement statements
                                        List<Statement> replacement = new ArrayList<>();
                                        for (Statement prep : prepStmts) {
                                            if (!inlinedPreps.contains(prep)) {
                                                replacement.add(prep);
                                            }
                                        }
                                        replacement.add(whileStmt);

                                        // Remove everything from index i to gotoIdx
                                        // and insert replacement at index i
                                        for (int k = gotoIdx; k >= i; k--) {
                                            stmts.remove(k);
                                        }
                                        stmts.addAll(i, replacement);

                                        // Recursively restructure the loop body
                                        restructure(loopBody);

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

    /**
     * 检查语句中（含嵌套块）是否对指定变量执行了赋值（写入）操作。
     * 不进入 FunctionDeclaration/FunctionLiteral（独立作用域）。
     */
    private boolean isAssignedInStatement(Statement stmt, String varName) {
        if (stmt == null) return false;
        if (stmt instanceof Assign) {
            Assign assign = (Assign) stmt;
            for (Expression left : assign.left) {
                if (left instanceof Name && ((Name) left).name.equals(varName)) return true;
            }
        } else if (stmt instanceof LocalAssign) {
            LocalAssign local = (LocalAssign) stmt;
            if (local.names.contains(varName)) return true;
        } else if (stmt instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) stmt;
            for (Block b : ifStmt.blocks) {
                if (b != null) {
                    for (Statement s : b.statements) {
                        if (isAssignedInStatement(s, varName)) return true;
                    }
                }
            }
            if (ifStmt.elseBlock != null) {
                for (Statement s : ifStmt.elseBlock.statements) {
                    if (isAssignedInStatement(s, varName)) return true;
                }
            }
        } else if (stmt instanceof WhileStatement) {
            Block body = ((WhileStatement) stmt).body;
            if (body != null) for (Statement s : body.statements) if (isAssignedInStatement(s, varName)) return true;
        } else if (stmt instanceof RepeatStatement) {
            Block body = ((RepeatStatement) stmt).body;
            if (body != null) for (Statement s : body.statements) if (isAssignedInStatement(s, varName)) return true;
        } else if (stmt instanceof ForNumeric) {
            Block body = ((ForNumeric) stmt).body;
            if (body != null) for (Statement s : body.statements) if (isAssignedInStatement(s, varName)) return true;
        } else if (stmt instanceof ForIn) {
            Block body = ((ForIn) stmt).body;
            if (body != null) for (Statement s : body.statements) if (isAssignedInStatement(s, varName)) return true;
        }
        return false;
    }

    private boolean containsVar(Expression expr, String varName) {
        if (expr == null) return false;
        if (expr instanceof Name) {
            return ((Name) expr).name.equals(varName);
        }
        if (expr instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) expr;
            return containsVar(binary.left, varName) || containsVar(binary.right, varName);
        }
        if (expr instanceof UnaryOp) {
            return containsVar(((UnaryOp) expr).expr, varName);
        }
        if (expr instanceof IndexExpr) {
            IndexExpr idx = (IndexExpr) expr;
            return containsVar(idx.table, varName) || containsVar(idx.index, varName);
        }
        if (expr instanceof MemberExpr) {
            return containsVar(((MemberExpr) expr).table, varName);
        }
        if (expr instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) expr;
            if (containsVar(call.callee, varName)) return true;
            for (Expression arg : call.args) {
                if (containsVar(arg, varName)) return true;
            }
        }
        return false;
    }

    private Expression replaceVariable(Expression expr, String varName, Expression replacement) {
        if (expr == null) return null;
        if (expr instanceof Name) {
            Name nameNode = (Name) expr;
            if (nameNode.name.equals(varName)) {
                return replacement;
            }
            return expr;
        }
        if (expr instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) expr;
            Expression newLeft = replaceVariable(binary.left, varName, replacement);
            Expression newRight = replaceVariable(binary.right, varName, replacement);
            if (newLeft != binary.left || newRight != binary.right) {
                return new BinaryOp(binary.op, newLeft, newRight, binary.pos);
            }
            return binary;
        }
        if (expr instanceof UnaryOp) {
            UnaryOp unary = (UnaryOp) expr;
            Expression newExpr = replaceVariable(unary.expr, varName, replacement);
            if (newExpr != unary.expr) {
                return new UnaryOp(unary.op, newExpr, unary.pos);
            }
            return unary;
        }
        if (expr instanceof IndexExpr) {
            IndexExpr idx = (IndexExpr) expr;
            Expression newTable = replaceVariable(idx.table, varName, replacement);
            Expression newIndex = replaceVariable(idx.index, varName, replacement);
            if (newTable != idx.table || newIndex != idx.index) {
                return new IndexExpr(newTable, newIndex, idx.pos);
            }
            return idx;
        }
        if (expr instanceof MemberExpr) {
            MemberExpr member = (MemberExpr) expr;
            Expression newTable = replaceVariable(member.table, varName, replacement);
            if (newTable != member.table) {
                return new MemberExpr(newTable, member.member, member.pos);
            }
            return member;
        }
        if (expr instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) expr;
            Expression newCallee = replaceVariable(call.callee, varName, replacement);
            List<Expression> newArgs = new ArrayList<>(call.args);
            boolean argsChanged = false;
            for (int i = 0; i < newArgs.size(); i++) {
                Expression newArg = replaceVariable(newArgs.get(i), varName, replacement);
                if (newArg != newArgs.get(i)) {
                    newArgs.set(i, newArg);
                    argsChanged = true;
                }
            }
            if (newCallee != call.callee || argsChanged) {
                return new FunctionCall(newCallee, newArgs, call.isMethodCall, call.returns, call.pos);
            }
            return call;
        }
        return expr;
    }

    private boolean isModuleScenario() {
        if (chunk == null) {
            return false;
        }
        if ("main".equals(chunk.getFunction())) {
            return hasModuleCall(chunk);
        }
        return false;
    }

    private boolean hasModuleCall(Chunk mainChunk) {
        if (mainChunk == null || mainChunk.getConstants() == null) {
            return false;
        }
        for (com.github.relua.model.Constant c : mainChunk.getConstants()) {
            Object val = c.getValue();
            if (val != null) {
                String s = val.toString();
                if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
                    s = s.substring(1, s.length() - 1);
                }
                if ("module".equals(s)) {
                    return true;
                }
            }
        }
        return false;
    }
}
