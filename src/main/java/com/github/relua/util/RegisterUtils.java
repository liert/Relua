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

            // 规则：修复跨 Block 数据流分析中的符号丢失。
            // 如果其中一个是未知类型，或者值是默认的寄存器名称 "Rxx"，而另一个是已知的具体符号，则优先保留具体符号。
            if (isDefaultOrUnknown(entity1) && !isDefaultOrUnknown(entity2)) {
                merged.setRegisterEntity(index, entity2.getValue(), entity2.getType(), entity2.getFromType());
            } else if (!isDefaultOrUnknown(entity1) && isDefaultOrUnknown(entity2)) {
                // 保持 state1 的具体符号，无需修改
            } else {
                // 否则，标记为UNKNOWN类型，等待后续指令进一步确定
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
        return s.equals("nil") || s.matches("R\\d+");
    }
}
