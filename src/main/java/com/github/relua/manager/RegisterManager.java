package com.github.relua.manager;

import java.util.Map;

import com.github.relua.model.Register;

import java.util.HashMap;

public class RegisterManager {
    private Map<Integer, Register> registers = new HashMap<>();
    private int currentRegisterIndex = 0;

    public void addRegister(int index, Register register) {
        registers.put(index, register);
        currentRegisterIndex = index;
    }

    public Register getCurrentRegister() {
        return registers.get(currentRegisterIndex);
    }

    public Map<Integer, Register> getRegisters() {
        return registers;
    }
}
