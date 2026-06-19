package com.github.relua.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.relua.util.RegisterNamePolicy;

/**
 * @deprecated Linear register state is kept only as a compatibility layer while
 *             SSA owns value identity and expression recovery. New code should
 *             prefer SsaFunction/SsaValue/SsaExpressionAnalysis.
 */
@Deprecated
public class Register {
    // 使用Map存储所有寄存器，支持任意数量的寄存器
    private Map<Integer, RegisterEntity> registers = new HashMap<>();
    
    // 命名变量的前缀，如 chunk_、module_ 或空串
    private String varPrefix = "";

    // 控制流相关字段
    public boolean jump = false;
    public int ifDepth = 0; // if嵌套深度
    public int[] jumpTargets; // 存储每个if块的跳转目标
    public boolean[] hasElse; // 标记每个if块是否有else

    public Register() {
        jumpTargets = new int[10]; // 支持最多10层嵌套
        hasElse = new boolean[10];
    }

    /**
     * 复制构造函数，用于复制寄存器状态
     */
    public Register(Register other) {
        this();
        this.varPrefix = other.varPrefix;
        for (Map.Entry<Integer, RegisterEntity> entry : other.registers.entrySet()) {
            RegisterEntity original = entry.getValue();
            RegisterEntity copy = new RegisterEntity(original.getIndex(), original.getValue(), original.getType(), original.getFromType());
            copy.setNamePrefix(original.getNamePrefix());
            copy.setCustomName(original.getCustomName());
            this.registers.put(original.getIndex(), copy);
        }
        this.jump = other.jump;
        this.ifDepth = other.ifDepth;
        System.arraycopy(other.jumpTargets, 0, this.jumpTargets, 0, other.jumpTargets.length);
        System.arraycopy(other.hasElse, 0, this.hasElse, 0, other.hasElse.length);
    }

    public String getVarPrefix() {
        return varPrefix;
    }

    public void setVarPrefix(String varPrefix) {
        this.varPrefix = varPrefix;
        for (RegisterEntity entity : registers.values()) {
            entity.setNamePrefix(varPrefix);
        }
    }

    public RegisterEntity getRegisterEntity(String index) {
        try {
            return getRegisterEntity(Integer.parseInt(index));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid register index: " + index);
        }
    }

    public List<RegisterEntity> getRegisterEntities() {
        return new ArrayList<>(registers.values());
    }

    /**
     * 移动寄存器值
     */
    public void move(int desIndex, int srcIndex) {
        RegisterEntity desRegisterEntity = getRegisterEntity(desIndex);
        RegisterEntity srcRegisterEntity = getRegisterEntity(srcIndex);
        desRegisterEntity.setValue(srcRegisterEntity.getValue());
        desRegisterEntity.setType(srcRegisterEntity.getType());
        desRegisterEntity.setFromType(srcRegisterEntity.getFromType());
    }

    /**
     * 移动寄存器值
     */
    public void move(RegisterEntity des, RegisterEntity src) {
        des.setValue(src.getValue());
        des.setType(src.getType());
    }

    /**
     * 获取寄存器实体，如果不存在则创建
     */
    public RegisterEntity getRegisterEntity(int index) {
        return registers.computeIfAbsent(index, i -> {
            RegisterEntity entity = new RegisterEntity(i, "nil", ValueType.NIL, FromType.NIL);
            entity.setNamePrefix(varPrefix);
            return entity;
        });
    }

    /**
     * 设置寄存器值
     */
    public void setRegisterEntity(int index, Object value, ValueType type, FromType fromType) {
        RegisterEntity entity = getRegisterEntity(index);
        entity.setValue(value);
        entity.setType(type);
        entity.setFromType(fromType);
        entity.setNamePrefix(varPrefix);
    }

    public void setRegisterEntity(int index, Object value, ValueType type) {
        RegisterEntity entity = getRegisterEntity(index);
        entity.setValue(value);
        entity.setType(type);
        entity.setNamePrefix(varPrefix);
    }

    /**
     * 获取当前寄存器数量
     * 
     * @return 寄存器数量
     */
    public int getRegisterCount() {
        return registers.size();
    }

    /**
     * 获取所有寄存器实体
     */
    public Map<Integer, RegisterEntity> getAllRegisterEntities() {
        return registers;
    }

    /**
     * 复制寄存器状态到另一个寄存器对象
     */
    public void copyTo(Register target) {
        target.registers.clear();
        for (Map.Entry<Integer, RegisterEntity> entry : registers.entrySet()) {
            RegisterEntity original = entry.getValue();
            RegisterEntity copy = new RegisterEntity(original.getIndex(), original.getValue(), original.getType(), original.getFromType());
            copy.setNamePrefix(original.getNamePrefix());
            copy.setCustomName(original.getCustomName());
            target.registers.put(original.getIndex(), copy);
        }
    }

    /**
     * 合并另一个寄存器对象的状态
     */
    public void merge(Register other) {
        for (Map.Entry<Integer, RegisterEntity> entry : other.registers.entrySet()) {
            int index = entry.getKey();
            RegisterEntity otherEntity = entry.getValue();
            RegisterEntity thisEntity = getRegisterEntity(index);
            
            // 总是更新当前寄存器状态，确保所有寄存器实体都被正确合并
            thisEntity.setValue(otherEntity.getValue());
            thisEntity.setType(otherEntity.getType());
            if (otherEntity.getCustomName() != null) {
                thisEntity.setCustomName(otherEntity.getCustomName());
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Register{");
        for (Map.Entry<Integer, RegisterEntity> entry : registers.entrySet()) {
            sb.append(entry.getValue().toString()).append(", ");
        }
        if (sb.length() > 9) {
            sb.setLength(sb.length() - 2); // 移除最后一个逗号和空格
        }
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * 比较两个寄存器状态是否相等
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Register other = (Register) obj;
        
        // 比较寄存器实体
        if (!registers.equals(other.registers)) {
            return false;
        }
        
        // 比较控制流相关字段
        if (jump != other.jump) {
            return false;
        }
        if (ifDepth != other.ifDepth) {
            return false;
        }
        if (!Arrays.equals(jumpTargets, other.jumpTargets)) {
            return false;
        }
        if (!Arrays.equals(hasElse, other.hasElse)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 生成哈希码
     */
    @Override
    public int hashCode() {
        int result = registers.hashCode();
        result = 31 * result + (jump ? 1 : 0);
        result = 31 * result + ifDepth;
        result = 31 * result + java.util.Arrays.hashCode(jumpTargets);
        result = 31 * result + java.util.Arrays.hashCode(hasElse);
        return result;
    }

    /**
     * @deprecated Part of the legacy linear register-state compatibility layer.
     *             Prefer SSA summaries and SSA value names for new recovery code.
     */
    @Deprecated
    public static class RegisterEntity {
        private int index;
        private Object value = "nil";
        private ValueType type = ValueType.NIL;
        private FromType fromType = FromType.NIL;
        private String namePrefix = "";
        private String customName = null;

        /**
         * 构造函数，支持不同类型的初始值
         */
        public RegisterEntity(int index, Object value, ValueType type, FromType fromType) {
            this.index = index;
            this.value = value;
            this.type = type;
            this.fromType = fromType;
        }

        public String getCustomName() {
            return customName;
        }

        public void setCustomName(String customName) {
            this.customName = customName;
        }

        public String getNamePrefix() {
            return namePrefix;
        }

        public void setNamePrefix(String namePrefix) {
            this.namePrefix = namePrefix != null ? namePrefix : "";
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            if (customName != null) {
                return namePrefix + customName;
            }
            return RegisterNamePolicy.prefixedRegisterName(namePrefix, index);
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public ValueType getType() {
            return type;
        }

        public void setType(ValueType type) {
            this.type = type;
        }

        public FromType getFromType() {
            return fromType;
        }

        public void setFromType(FromType fromType) {
            this.fromType = fromType;
        }

        /**
         * 创建当前实体的副本
         */
        public RegisterEntity copy() {
            RegisterEntity copy = new RegisterEntity(index, value, type, fromType);
            copy.setNamePrefix(namePrefix);
            copy.setCustomName(customName);
            return copy;
        }

        public String toString() {
            return getName() + " = " + value + " | type=" + type + " | from=" + fromType;
        }
        
        /**
         * 比较两个寄存器实体是否相等
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            RegisterEntity other = (RegisterEntity) obj;
            
            if (index != other.index) {
                return false;
            }
            if (type != other.type) {
                return false;
            }
            if (customName == null) {
                if (other.customName != null) {
                    return false;
                }
            } else if (!customName.equals(other.customName)) {
                return false;
            }
            if (value == null) {
                return other.value == null;
            } else {
                return value.equals(other.value);
            }
        }
        
        /**
         * 生成哈希码
         */
        @Override
        public int hashCode() {
            int result = index;
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (value != null ? value.hashCode() : 0);
            result = 31 * result + (customName != null ? customName.hashCode() : 0);
            return result;
        }
    }
}
