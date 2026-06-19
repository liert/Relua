package com.github.relua.util;

import com.github.relua.model.Chunk;
import com.github.relua.model.Register;

public final class RegisterNamePolicy {

    private RegisterNamePolicy() {
    }

    public static String physicalRegisterName(int register) {
        return "R" + register;
    }

    public static String parameterName(int parameterIndex) {
        return "a" + parameterIndex;
    }

    public static String prefixedRegisterName(String prefix, int register) {
        return (prefix != null ? prefix : "") + physicalRegisterName(register);
    }

    public static boolean isTemporaryRegisterName(String name) {
        return isTemporaryRegisterName(name, null, null);
    }

    public static boolean isTemporaryRegisterName(String name, Chunk chunk, Register registerState) {
        if (name == null) {
            return false;
        }
        String prefix = registerState != null ? registerState.getVarPrefix() : "";
        if (!prefix.isEmpty() && !name.startsWith(prefix)) {
            if (name.startsWith("chunk_") || name.startsWith("module_")) {
                // Keep compatibility with standard prefixes
            } else if (!name.startsWith("R")) {
                return false;
            }
        }

        int rIndex = name.indexOf('R');
        if (rIndex == -1) {
            return false;
        }

        String prefixPart = name.substring(0, rIndex);
        if (!prefixPart.isEmpty() && !prefixPart.equals("chunk_") && !prefixPart.equals("module_")
                && (registerState == null || !prefixPart.equals(registerState.getVarPrefix()))) {
            return false;
        }

        String rest = name.substring(rIndex + 1);
        int index = 0;
        while (index < rest.length() && Character.isDigit(rest.charAt(index))) {
            index++;
        }
        if (index == 0) {
            return false;
        }

        String regNumStr = rest.substring(0, index);
        int regNum;
        try {
            regNum = Integer.parseInt(regNumStr);
        } catch (NumberFormatException e) {
            return false;
        }

        if (chunk != null && regNum >= chunk.getMaxStackSize()) {
            return false;
        }

        if (index < rest.length()) {
            if (rest.charAt(index) != '_') {
                return false;
            }
            int vIndex = index + 1;
            while (vIndex < rest.length() && Character.isDigit(rest.charAt(vIndex))) {
                vIndex++;
            }
            if (vIndex != rest.length()) {
                return false;
            }
        }

        // Logical check: not a parameter and not a custom-named register
        if (chunk != null && regNum < chunk.getNumParams()) {
            return false;
        }
        if (registerState != null) {
            Register.RegisterEntity entity = registerState.getRegisterEntity(regNum);
            if (entity != null && entity.getCustomName() != null) {
                return false;
            }
        }

        return true;
    }

    public static boolean isPhysicalRegisterName(String name) {
        return isPhysicalRegisterName(name, null, null);
    }

    public static boolean isPhysicalRegisterName(String name, Chunk chunk, Register registerState) {
        if (name == null) {
            return false;
        }
        if (!name.startsWith("R")) {
            return false;
        }
        String rest = name.substring(1);
        int index = 0;
        while (index < rest.length() && Character.isDigit(rest.charAt(index))) {
            index++;
        }
        if (index == 0) {
            return false;
        }

        String regNumStr = rest.substring(0, index);
        int regNum;
        try {
            regNum = Integer.parseInt(regNumStr);
        } catch (NumberFormatException e) {
            return false;
        }

        if (chunk != null && regNum >= chunk.getMaxStackSize()) {
            return false;
        }

        if (index < rest.length()) {
            if (rest.charAt(index) != '_') {
                return false;
            }
            int vIndex = index + 1;
            while (vIndex < rest.length() && Character.isDigit(rest.charAt(vIndex))) {
                vIndex++;
            }
            if (vIndex != rest.length()) {
                return false;
            }
        }

        if (chunk != null && regNum < chunk.getNumParams()) {
            return false;
        }
        if (registerState != null) {
            Register.RegisterEntity entity = registerState.getRegisterEntity(regNum);
            if (entity != null && entity.getCustomName() != null) {
                return false;
            }
        }

        return true;
    }

    public static int temporaryRegisterIndex(String name) {
        if (name == null) {
            return -1;
        }
        int rIndex = name.indexOf('R');
        if (rIndex == -1) {
            return -1;
        }

        String prefixPart = name.substring(0, rIndex);
        if (!prefixPart.isEmpty() && !prefixPart.equals("chunk_") && !prefixPart.equals("module_")) {
            return -1;
        }

        String rest = name.substring(rIndex + 1);
        int index = 0;
        while (index < rest.length() && Character.isDigit(rest.charAt(index))) {
            index++;
        }
        if (index == 0) {
            return -1;
        }

        String regNumStr = rest.substring(0, index);
        try {
            return Integer.parseInt(regNumStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
