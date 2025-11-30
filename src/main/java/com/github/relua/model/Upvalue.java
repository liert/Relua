package com.github.relua.model;

/**
 * 上值(Upvalue)类，用于表示Lua闭包中的上值
 */
public class Upvalue {
    private int index; // 上值索引
    private String name; // 上值名称
    private Object value; // 上值的值
    private ValueType type; // 上值的类型
    private FromType fromType; // 上值的来源类型

    /**
     * 构造函数
     * 
     * @param index 上值索引
     * @param name 上值名称
     * @param value 上值的值
     * @param type 上值的类型
     * @param fromType 上值的来源类型
     */
    public Upvalue(int index, String name, Object value, ValueType type, FromType fromType) {
        this.index = index;
        this.name = name;
        this.value = value;
        this.type = type;
        this.fromType = fromType;
    }

    /**
     * 构造函数
     * 
     * @param index 上值索引
     * @param name 上值名称
     */
    public Upvalue(int index, String name) {
        this(index, name, null, ValueType.NIL, FromType.NIL);
    }

    // getter和setter方法
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public String toString() {
        return "Upvalue{" +
                "index=" + index +
                ", name='" + name + '\'' +
                ", value=" + value +
                ", type=" + type +
                ", fromType=" + fromType +
                '}';
    }
}