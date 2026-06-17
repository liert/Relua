package com.github.relua.util;

import java.util.Map;

import com.github.relua.decompiler.BasicBlock;
import com.github.relua.model.FromType;
import com.github.relua.model.Register;
import com.github.relua.model.Register.RegisterEntity;
import com.github.relua.model.ValueType;

public class RegisterUtils {
    public static Register mergePredecessors(BasicBlock block) {
        return mergePredecessors(block, "");
    }

    public static Register mergePredecessors(BasicBlock block, String varPrefix) {
        Register merged = new Register();
        merged.setVarPrefix(varPrefix);

        // 如果没有前驱，返回空状态
        if (block.getPredecessors().isEmpty()) {
            return merged;
        }

        // 获取第一个前驱的输出状态作为初始值
        BasicBlock firstPredecessor = block.getPredecessors().get(0);
        merged = new Register(firstPredecessor.getOutputState());
        merged.setVarPrefix(varPrefix);

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

            // 处理未初始化状态的合并
            if (isUninitialized(entity1) && !isUninitialized(entity2)) {
                merged.setRegisterEntity(index, entity2.getValue(), entity2.getType(), entity2.getFromType());
                if (entity2.getCustomName() != null) {
                    merged.getRegisterEntity(index).setCustomName(entity2.getCustomName());
                }
            } else if (!isUninitialized(entity1) && isUninitialized(entity2)) {
                // 保持 entity1 的具体符号，无需修改
            } else if (isUninitialized(entity1) && isUninitialized(entity2)) {
                // 两者都是未初始化，无需修改
            } else {
                // 两者都已初始化但值或类型不同（真正的数据流分支合并差异，值在运行时是动态的）
                // 必须标记为 UNKNOWN 并且值为 "R" + index，避免被常量错误覆盖
                String varName = entity1.getCustomName() != null ? entity1.getCustomName() : "R" + index;
                merged.setRegisterEntity(index, varName, ValueType.UNKNOWN, FromType.UNKNOWN);
                if (entity1.getCustomName() != null) {
                    merged.getRegisterEntity(index).setCustomName(entity1.getCustomName());
                }
            }
        }

        return merged;
    }

    private static boolean isUninitialized(RegisterEntity entity) {
        return entity == null || 
               entity.getType() == ValueType.NIL || 
               entity.getFromType() == FromType.NIL || 
               (entity.getValue() != null && entity.getValue().toString().equals("nil"));
    }
}
