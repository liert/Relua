package com.github.relua.util;

import java.util.Map;

import com.github.relua.decompiler.BasicBlock;
import com.github.relua.model.FromType;
import com.github.relua.model.Register;
import com.github.relua.model.Register.RegisterEntity;
import com.github.relua.model.ValueType;

public class RegisterUtils {
/**
     * 合并前驱块的输出状态
     * 
     * @param block 当前块
     * @return 合并后的寄存器状态
     */
    public static Register mergePredecessors(BasicBlock block) {
        Register merged = new Register();

        // 如果没有前驱，返回空状态
        if (block.getPredecessors().isEmpty()) {
            return merged;
        }

        // 获取第一个前驱的输出状态作为初始值
        BasicBlock firstPredecessor = block.getPredecessors().get(0);
        merged = new Register(firstPredecessor.getOutputState());

        // 合并其他前驱的输出状态
        for (int i = 1; i < block.getPredecessors().size(); i++) {
            BasicBlock predecessor = block.getPredecessors().get(i);
            merged = mergeRegisterStates(merged, predecessor.getOutputState());
        }

        return merged;
    }

    /**
     * 合并两个寄存器状态（PHI合并）
     * 
     * @param state1 第一个状态
     * @param state2 第二个状态
     * @return 合并后的状态
     */
    public static Register mergeRegisterStates(Register state1, Register state2) {
        Register merged = new Register(state1);

        // 遍历state2的所有寄存器实体
        for (Map.Entry<Integer, RegisterEntity> entry : state2.getAllRegisterEntities().entrySet()) {
            int index = entry.getKey();
            RegisterEntity entity2 = entry.getValue();
            RegisterEntity entity1 = merged.getRegisterEntity(index);

            // 如果两个寄存器实体类型相同且值相同，保持不变
            if (entity1.getType() == entity2.getType() &&
                    entity1.getValue() != null &&
                    entity1.getValue().equals(entity2.getValue())) {
                continue;
            }

            // 规则1：修复跨 Block 数据流分析中的符号丢失。
            // 如果其中一个是未知类型，或者值是默认的寄存器名称 "Rxx"，而另一个是已知的具体符号，则优先保留具体符号。
            if (isDefaultOrUnknown(entity1) && !isDefaultOrUnknown(entity2)) {
                // 规则2（关键）：若 entity1 来源是 REGISTER/UNKNOWN（函数调用/变量结果），
                // 而 entity2 来源是 CONSTANT（字面量），则不应用常量覆盖函数结果。
                // 这修复了 Xiaomi 混淆 Lua 中 FORLOOP 用作条件跳转时，
                // fallthrough 路径的常量（如数字 3）错误覆盖主路径的函数调用结果问题。
                boolean entity1IsRegisterResult = (entity1 == null
                        || entity1.getType() == ValueType.UNKNOWN
                        || entity1.getFromType() == FromType.REGISTER
                        || entity1.getFromType() == FromType.UNKNOWN);
                boolean entity2IsConstant = (entity2.getFromType() == FromType.CONSTANT);
                if (entity1IsRegisterResult && entity2IsConstant) {
                    // 保留 entity1（UNKNOWN/REGISTER 来源），不被 CONSTANT 来源覆盖
                    // 但如果 entity1 本身是空/nil，则仍然使用 entity2
                    if (entity1 != null && entity1.getValue() != null
                            && !entity1.getValue().toString().equals("nil")) {
                        // entity1 有实际值，保持不变
                    } else {
                        merged.setRegisterEntity(index, entity2.getValue(), entity2.getType(), entity2.getFromType());
                    }
                } else {
                    merged.setRegisterEntity(index, entity2.getValue(), entity2.getType(), entity2.getFromType());
                }
            } else if (!isDefaultOrUnknown(entity1) && isDefaultOrUnknown(entity2)) {
                // 保持 state1 的具体符号，无需修改
            } else if (!isDefaultOrUnknown(entity1) && !isDefaultOrUnknown(entity2)) {
                // 两者都是具体符号，但值不同：
                // 优先保留非 CONSTANT 来源（函数调用/变量引用）
                boolean e1IsConst = (entity1.getFromType() == FromType.CONSTANT);
                boolean e2IsConst = (entity2.getFromType() == FromType.CONSTANT);
                if (e1IsConst && !e2IsConst) {
                    // entity2 是非常量来源，优先保留
                    merged.setRegisterEntity(index, entity2.getValue(), entity2.getType(), entity2.getFromType());
                } else if (!e1IsConst && e2IsConst) {
                    // entity1 是非常量来源，保持不变
                } else {
                    // 否则，标记为UNKNOWN类型，等待后续指令进一步确定
                    merged.setRegisterEntity(index, "R" + index, ValueType.UNKNOWN, FromType.UNKNOWN);
                }
            } else {
                // 两者都是默认/未知，标记为UNKNOWN
                merged.setRegisterEntity(index, "R" + index, ValueType.UNKNOWN, FromType.UNKNOWN);
            }
        }

        return merged;
    }

    private static boolean isDefaultOrUnknown(RegisterEntity entity) {
        if (entity == null || entity.getType() == ValueType.UNKNOWN) {
            return true;
        }
        Object val = entity.getValue();
        if (val == null) {
            return true;
        }
        String s = val.toString();
        if (s.equals("nil") || s.matches("R\\d+")) {
            return true;
        }
        // 来源为 UNKNOWN 的也视为默认（PHI 合并后的占位符）
        if (entity.getFromType() == FromType.UNKNOWN) {
            return true;
        }
        return false;
    }
}
