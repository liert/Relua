package com.github.relua.decompiler.ssa;

import java.util.HashMap;
import java.util.Map;

import com.github.relua.ast.TableConstructor;
import com.github.relua.decompiler.CodeGeneratorContext;
import com.github.relua.decompiler.DecompilerPipeline;
import com.github.relua.model.Chunk;
import com.github.relua.model.Constant;
import com.github.relua.model.FromType;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;
import com.github.relua.model.UpValue;
import com.github.relua.model.ValueType;
import com.github.relua.util.RegisterNamePolicy;

/**
 * Compatibility view over SSA for code that still asks "what is in Rn at pc?".
 * It does not run a linear transfer function; every entity is derived from the
 * SSA value used or defined by the current instruction.
 */
public final class SsaRegisterSnapshot {
    private final DecompilerPipeline pipeline;
    private final Chunk chunk;
    private final int pc;
    private final String varPrefix;
    private final Map<Integer, Entity> overrides = new HashMap<>();

    public SsaRegisterSnapshot(DecompilerPipeline pipeline, Chunk chunk, int pc, String varPrefix) {
        this.pipeline = pipeline;
        this.chunk = chunk;
        this.pc = pc;
        this.varPrefix = varPrefix != null ? varPrefix : "";
    }

    public String getVarPrefix() {
        return varPrefix;
    }

    public Entity getRegisterEntity(int register) {
        Entity override = overrides.get(register);
        if (override != null) {
            return override;
        }
        SsaValue value = null;
        SsaInstruction instruction = safeInstruction(pc);
        if (instruction != null) {
            value = instruction.getFirstUseForRegister(register);
            if (value == null) {
                value = instruction.getFirstDefForRegister(register);
            }
        }
        if (value == null) {
            value = latestDefinitionBefore(register);
        }
        return entityFor(register, value);
    }

    public void setRegisterEntity(int register, Object value, ValueType type, FromType fromType) {
        overrides.put(register, new Entity(register, value, type, fromType, nameFor(register, null)));
    }

    public void setRegisterEntity(int register, Object value, ValueType type) {
        setRegisterEntity(register, value, type, FromType.UNKNOWN);
    }

    private Entity entityFor(int register, SsaValue value) {
        if (value == null) {
            return new Entity(register, nameFor(register, null), ValueType.UNKNOWN, FromType.UNKNOWN, nameFor(register, null));
        }

        SsaExpressionAnalysis analysis = pipeline.requireSsaExpressionAnalysis(chunk.getFunction());
        SsaValueSummary summary = analysis.resolveCopySummary(value);
        if (summary == null) {
            summary = analysis.getSummary(value);
        }
        SsaValue resolved = summary != null && summary.getValue() != null ? summary.getValue() : value;
        Object data = nameFor(register, resolved);
        ValueType type = ValueType.UNKNOWN;
        FromType from = FromType.UNKNOWN;

        if (summary != null) {
            switch (summary.getKind()) {
                case PARAMETER:
                    data = RegisterNamePolicy.parameterName(resolved.getRegister());
                    type = ValueType.OBJECT;
                    from = FromType.GLOBAL;
                    break;
                case CONSTANT:
                    data = summary.getConstantValue();
                    type = valueTypeForConstant(data);
                    from = data == null ? FromType.NIL : FromType.CONSTANT;
                    break;
                case GLOBAL:
                    data = globalName(definingInstruction(resolved));
                    type = ValueType.GLOBAL;
                    from = FromType.GLOBAL;
                    break;
                case UPVALUE:
                    UpValue upvalue = upvalueFor(definingInstruction(resolved));
                    if (upvalue != null) {
                        data = upvalue.getValue() != null ? upvalue.getValue() : upvalue.getName();
                        type = upvalue.getType();
                        from = upvalue.getFromType();
                    } else {
                        type = ValueType.UPVALUE;
                        from = FromType.UPVALUE;
                    }
                    break;
                case TABLE_NEW:
                    data = new TableConstructor(new java.util.ArrayList<>(),
                            new com.github.relua.ast.SourcePos(pc, -1));
                    type = ValueType.TABLE;
                    from = FromType.CONSTANT;
                    break;
                case CLOSURE:
                    data = closureName(definingInstruction(resolved));
                    type = ValueType.FUNCTION;
                    from = FromType.GLOBAL;
                    break;
                case TABLE_READ:
                    type = ValueType.TABLE;
                    from = FromType.REGISTER;
                    break;
                case CALL_RESULT:
                    String callName = callResultName(definingInstruction(resolved), resolved);
                    if (callName != null) {
                        data = callName;
                        type = ValueType.OBJECT;
                        from = FromType.GLOBAL;
                        break;
                    }
                    break;
                case VARARG:
                case ARITHMETIC:
                case UNARY:
                case CONCAT:
                case COPY:
                case PHI:
                case UNKNOWN:
                default:
                    break;
            }
        }
        return new Entity(register, data, type, from, nameFor(register, resolved));
    }

    private String callResultName(SsaInstruction instruction, SsaValue result) {
        if (instruction == null || instruction.getInstruction().getOpcode() != Opcode.CALL) {
            return null;
        }
        Instruction raw = instruction.getInstruction();
        SsaValue callee = instruction.getFirstUseForRegister(raw.getA());
        if (isGlobalNamed(callee, "require")) {
            SsaValue arg = instruction.getFirstUseForRegister(raw.getA() + 1);
            Object module = constantValue(arg);
            if (module != null && !module.toString().isEmpty()) {
                return module.toString().replace(".", "_");
            }
        }
        ExpressionName constructor = constructorName(callee);
        if (constructor != null) {
            return constructor.name + "Obj";
        }
        return null;
    }

    private boolean isGlobalNamed(SsaValue value, String expected) {
        SsaInstruction def = definingInstruction(value);
        return expected.equals(globalName(def));
    }

    private Object constantValue(SsaValue value) {
        if (value == null) {
            return null;
        }
        SsaValueSummary summary = pipeline.requireSsaExpressionAnalysis(chunk.getFunction()).resolveCopySummary(value);
        return summary != null && summary.getKind() == SsaValueKind.CONSTANT ? summary.getConstantValue() : null;
    }

    private ExpressionName constructorName(SsaValue callee) {
        SsaInstruction def = definingInstruction(callee);
        if (def == null || def.getInstruction().getOpcode() != Opcode.GETTABLE) {
            return null;
        }
        Object key = rkConstant(def.getInstruction().getC());
        if (!"new".equals(key)) {
            return null;
        }
        SsaValue tableValue = def.getFirstUseForRegister(def.getInstruction().getB());
        SsaRegisterSnapshot tableSnapshot = new SsaRegisterSnapshot(pipeline, chunk, def.getPc(), varPrefix);
        Entity table = tableSnapshot.entityFor(tableValue != null ? tableValue.getRegister() : -1, tableValue);
        String base = table.getValue() != null ? table.getValue().toString() : table.getName();
        if (base == null || base.isEmpty() || RegisterNamePolicy.isTemporaryRegisterName(base)) {
            return null;
        }
        return new ExpressionName(base.replace(".", "_").replace(":", "_"));
    }

    private Object rkConstant(int rk) {
        if (rk < 256) {
            return null;
        }
        Constant constant = chunk.getConstant(rk - 256);
        return constant != null ? constant.getValue() : null;
    }

    private SsaInstruction safeInstruction(int instructionPc) {
        SsaFunction function = pipeline.getSsaFunction(chunk.getFunction());
        return function != null ? function.getInstruction(instructionPc) : null;
    }

    private SsaInstruction definingInstruction(SsaValue value) {
        SsaFunction function = pipeline.getSsaFunction(chunk.getFunction());
        return function != null ? function.getDefiningInstruction(value) : null;
    }

    private SsaValue latestDefinitionBefore(int register) {
        SsaFunction function = pipeline.getSsaFunction(chunk.getFunction());
        if (function == null) {
            return null;
        }
        SsaValue latest = null;
        int latestPc = Integer.MIN_VALUE;
        for (SsaBlock block : function.getBlocks()) {
            for (SsaPhi phi : block.getPhis()) {
                int blockStart = block.getBasicBlock().getStartIndex();
                if (phi.getRegister() == register && blockStart <= pc && blockStart > latestPc) {
                    latest = phi.getTarget();
                    latestPc = blockStart;
                }
            }
            for (SsaInstruction instruction : block.getInstructions()) {
                if (instruction.getPc() >= pc || instruction.getPc() < latestPc) {
                    continue;
                }
                SsaValue def = instruction.getFirstDefForRegister(register);
                if (def != null) {
                    latest = def;
                    latestPc = instruction.getPc();
                }
            }
        }
        return latest;
    }

    private UpValue upvalueFor(SsaInstruction instruction) {
        if (instruction == null || instruction.getInstruction().getOpcode() != Opcode.GETUPVAL) {
            return null;
        }
        return pipeline.getContext().getUpvalue(instruction.getInstruction().getB());
    }

    private String globalName(SsaInstruction instruction) {
        if (instruction == null) {
            return "";
        }
        Constant constant = chunk.getConstant(instruction.getInstruction().getBx());
        String name = constant != null && constant.getValue() != null ? constant.getValue().toString() : "";
        if (name.length() >= 2 && name.startsWith("\"") && name.endsWith("\"")) {
            name = name.substring(1, name.length() - 1);
        }
        if (RegisterNamePolicy.isPhysicalRegisterName(name)) {
            name = (isModuleScenario() ? "module_" : "global_") + name;
        }
        return name;
    }

    private String closureName(SsaInstruction instruction) {
        if (instruction == null) {
            return "";
        }
        Instruction raw = instruction.getInstruction();
        return chunk.getFunction() + "_" + raw.getBx();
    }

    private String nameFor(int register, SsaValue value) {
        int actual = value != null ? value.getRegister() : register;
        if (actual >= 0 && actual < chunk.getNumParams()) {
            return RegisterNamePolicy.parameterName(actual);
        }
        return RegisterNamePolicy.prefixedRegisterName(varPrefix, actual);
    }

    private ValueType valueTypeForConstant(Object value) {
        if (value == null) {
            return ValueType.NIL;
        }
        if (value instanceof String) {
            return ValueType.STRING;
        }
        if (value instanceof Number) {
            return ValueType.NUMBER;
        }
        if (value instanceof Boolean) {
            return ValueType.BOOLEAN;
        }
        return ValueType.OBJECT;
    }

    private boolean isModuleScenario() {
        CodeGeneratorContext mainContext = pipeline.getContext("main");
        if (mainContext == null || mainContext.getChunk() == null || mainContext.getChunk().getConstants() == null) {
            return false;
        }
        for (Constant c : mainContext.getChunk().getConstants()) {
            Object val = c.getValue();
            if (val != null) {
                String s = val.toString();
                if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
                    s = s.substring(1, s.length() - 1);
                }
                if ("module".equals(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static final class Entity {
        private final int index;
        private Object value;
        private ValueType type;
        private FromType fromType;
        private final String name;

        private Entity(int index, Object value, ValueType type, FromType fromType, String name) {
            this.index = index;
            this.value = value;
            this.type = type != null ? type : ValueType.UNKNOWN;
            this.fromType = fromType != null ? fromType : FromType.UNKNOWN;
            this.name = name;
        }

        public int getIndex() {
            return index;
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
            this.type = type != null ? type : ValueType.UNKNOWN;
        }

        public FromType getFromType() {
            return fromType;
        }

        public void setFromType(FromType fromType) {
            this.fromType = fromType != null ? fromType : FromType.UNKNOWN;
        }

        public String getName() {
            return name;
        }
    }

    private static final class ExpressionName {
        private final String name;

        private ExpressionName(String name) {
            this.name = name;
        }
    }
}
