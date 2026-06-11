package com.github.relua.util;

import java.rmi.registry.Registry;

import com.github.relua.ast.AstNode;
import com.github.relua.ast.Expression;
import com.github.relua.ast.Name;
import com.github.relua.ast.SourcePos;
import com.github.relua.ast.StringConst;
import com.github.relua.ast.TableConstructor;
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
        if (register == null) {
            return "";
        }
        if (register.getValue() instanceof Expression) {
            return register.getName();
        }
        if ((register.getFromType() == FromType.CONSTANT || register.getFromType() == FromType.GLOBAL) && register.getValue() != null) {
            return register.getValue().toString();
        }
        return register.getName();
    }

    public static String transformRX(Chunk chunk, Register register, int index) {
        if (index < 256) {
            return transformRegister(register.getRegisterEntity(index));
        } else {
            Object val = chunk.getConstant(index - 256).getValue();
            return  "\"" + (val != null ? val.toString() : "") + "\"";
        }
    }

    public static Expression transformToAstNode(RegisterEntity register, int instructionIndex) {
        if (register == null) {
            return new Name("", new SourcePos(instructionIndex, -1));
        }
        if (register.getValue() instanceof Expression) {
            if (register.getValue() instanceof TableConstructor
                    && ((TableConstructor) register.getValue()).isEmpty()) {
                return new Name(register.getName(), new SourcePos(instructionIndex, -1));
            }
            return (Expression) register.getValue();
        }
        switch (register.getType()) {
            case STRING:
                if (register.getFromType() == FromType.CONSTANT && register.getValue() != null && !register.getName().equals(register.getValue().toString())) {
                    return new StringConst(transformRegister(register), new SourcePos(instructionIndex, -1));
                }
                return new Name(transformRegister(register), new SourcePos(instructionIndex, -1));
            default:
                return new Name(transformRegister(register), new SourcePos(instructionIndex, -1));
        }
    }
}
