package com.github.relua.model;

/**
 * Lua常量模型
 */
public class Constant {
    private ValueType type;
    private Object value;

    /**
     * 构造函数
     * @param type 常量类型
     * @param value 常量值
     */
    public Constant(ValueType type, Object value) {
        this.type = type;
        this.value = value;
    }

    /**
     * 创建nil常量
     * @return nil常量
     */
    public static Constant nil() {
        return new Constant(ValueType.NIL, null);
    }

    /**
     * 创建布尔常量
     * @param value 布尔值
     * @return 布尔常量
     */
    public static Constant booleanConstant(boolean value) {
        return new Constant(ValueType.BOOLEAN, value);
    }

    /**
     * 创建数字常量
     * @param value 数字值
     * @return 数字常量
     */
    public static Constant number(double value) {
        return new Constant(ValueType.NUMBER, value);
    }

    /**
     * 创建字符串常量
     * @param value 字符串值
     * @return 字符串常量
     */
    public static Constant string(String value) {
        return new Constant(ValueType.STRING, value);
    }

    /**
     * 获取常量类型
     * @return 常量类型
     */
    public ValueType getType() {
        return type;
    }

    /**
     * 获取常量值
     * @return 常量值
     */
    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        switch (type) {
            case NIL:
                return "nil";
            case BOOLEAN:
                return Boolean.toString((Boolean) value);
            case NUMBER:
                double num = (Double) value;
                if (num == (long) num) {
                    return Long.toString((long) num);
                }
                return Double.toString(num);
            case STRING:
                return "\"" + value + "\"";
            default:
                return "unknown";
        }
    }
}