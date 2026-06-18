package com.github.relua.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RegisterNamePolicy {
    private static final Pattern TEMPORARY_REGISTER = Pattern.compile("^(?:chunk_|module_)?R(\\d+)$");

    private RegisterNamePolicy() {
    }

    public static String physicalRegisterName(int register) {
        return "R" + register;
    }

    public static String prefixedRegisterName(String prefix, int register) {
        return (prefix != null ? prefix : "") + physicalRegisterName(register);
    }

    public static boolean isTemporaryRegisterName(String name) {
        return name != null && TEMPORARY_REGISTER.matcher(name).matches();
    }

    public static int temporaryRegisterIndex(String name) {
        if (name == null) {
            return -1;
        }
        Matcher matcher = TEMPORARY_REGISTER.matcher(name);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : -1;
    }
}
