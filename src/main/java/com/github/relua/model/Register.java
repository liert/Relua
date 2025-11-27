package com.github.relua.model;

public class Register {
    public RegisterEntity R0 = new RegisterEntity(0, 0);
    public RegisterEntity R1 = new RegisterEntity(1, 0);
    public RegisterEntity R2 = new RegisterEntity(2, 0);
    public RegisterEntity R3 = new RegisterEntity(3, 0);
    public RegisterEntity R4 = new RegisterEntity(4, 0);
    public RegisterEntity R5 = new RegisterEntity(5, 0);
    public RegisterEntity R6 = new RegisterEntity(6, 0);
    public RegisterEntity R7 = new RegisterEntity(7, 0);
    public RegisterEntity R8 = new RegisterEntity(8, 0);
    public boolean jump = false;

    public RegisterEntity getRegisterEntity(String index) {
        try {
            return getRegisterEntity(Integer.parseInt(index));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid register index: " + index);
        }
    }

    public void move(int desIndex, int srcIndex) {
        RegisterEntity desRegisterEntity = getRegisterEntity(desIndex);
        RegisterEntity srcRegisterEntity = getRegisterEntity(srcIndex);
        desRegisterEntity.setValue(srcRegisterEntity.getValue());
        desRegisterEntity.setType(srcRegisterEntity.getType());
    }

    public void move(RegisterEntity des, RegisterEntity src) {
        des.setValue(src.getValue());
        des.setType(src.getType());
    }

    public RegisterEntity getRegisterEntity(int index) {
        switch (index) {
            case 0:
                return R0;
            case 1:
                return R1;
            case 2:
                return R2;
            case 3:
                return R3;
            case 4:
                return R4;
            case 5:
                return R5;
            case 6:
                return R6;
            case 7:
                return R7;
            case 8:
                return R8;
            default:
                throw new IllegalArgumentException("Invalid register index: " + index);
        }
    }

    public static class RegisterEntity {
        private int index;
        private Object value = "nil";
        private ValueType type = ValueType.NIL;

        public RegisterEntity(int index, int value) {
            this.index = index;
            this.value = value;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return "R" + index;
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

        public String toString() {
            return "RegisterEntity{index=" + index + ", value=" + value + ", type=" + type + "}";
        }
    }
}
