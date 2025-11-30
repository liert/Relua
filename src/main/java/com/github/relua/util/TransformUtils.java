package com.github.relua.util;

import java.rmi.registry.Registry;

import com.github.relua.model.Chunk;
import com.github.relua.model.FromType;
import com.github.relua.model.Register;
import com.github.relua.model.Register.RegisterEntity;

public class TransformUtils {
    /**
     * 转变寄存器显示的内容
     * 
     */
    public static String transformRegister(RegisterEntity register) {
        if (register.getFromType() == FromType.CONSTANT || register.getFromType() == FromType.GLOBAL) {
            return register.getValue().toString();
        }
        return register.getName();
    }

    public static String transformRX(Chunk chunk, Register register, int index) {
        if (index < 256) {
            return transformRegister(register.getRegisterEntity(index));
        } else {
            return  "\"" + chunk.getConstant(index - 256).getValue().toString() + "\"";
        }
    }
}
