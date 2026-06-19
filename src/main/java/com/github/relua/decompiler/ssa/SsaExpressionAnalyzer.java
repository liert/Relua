package com.github.relua.decompiler.ssa;

import java.util.LinkedHashMap;
import java.util.Map;

import com.github.relua.model.Chunk;
import com.github.relua.model.Constant;
import com.github.relua.model.Instruction;
import com.github.relua.model.Opcode;

public final class SsaExpressionAnalyzer {
    public SsaExpressionAnalysis analyze(SsaFunction function) {
        Map<SsaValue, SsaValueSummary> summaries = new LinkedHashMap<>();
        Chunk chunk = function.getChunk();
        int analyzedInstructions = 0;
        int effectfulInstructions = 0;

        for (SsaBlock block : function.getBlocks()) {
            for (SsaPhi phi : block.getPhis()) {
                SsaValueSummary summary = summaryFor(summaries, phi.getTarget());
                summary.setKind(SsaValueKind.PHI);
            }
            for (SsaInstruction instruction : block.getInstructions()) {
                analyzedInstructions++;
                if (summarizeInstruction(chunk, instruction, summaries)) {
                    effectfulInstructions++;
                }
            }
        }

        return new SsaExpressionAnalysis(function, summaries, analyzedInstructions, effectfulInstructions);
    }

    private boolean summarizeInstruction(Chunk chunk, SsaInstruction ssaInstruction,
            Map<SsaValue, SsaValueSummary> summaries) {
        Instruction instruction = ssaInstruction.getInstruction();
        Opcode opcode = instruction.getOpcode();
        boolean effectful = false;
        for (SsaValue use : ssaInstruction.getUses()) {
            summaryFor(summaries, use);
        }

        switch (opcode) {
            case MOVE:
                if (!ssaInstruction.getDefs().isEmpty()) {
                    SsaValueSummary summary = summaryFor(summaries, ssaInstruction.getDefs().get(0));
                    summary.setKind(SsaValueKind.COPY);
                    if (!ssaInstruction.getUses().isEmpty()) {
                        summary.setCopySource(ssaInstruction.getUses().get(0));
                    }
                }
                break;
            case LOADK:
                if (!ssaInstruction.getDefs().isEmpty()) {
                    SsaValueSummary summary = summaryFor(summaries, ssaInstruction.getDefs().get(0));
                    summary.setKind(SsaValueKind.CONSTANT);
                    Constant constant = chunk.getConstant(instruction.getBx());
                    if (constant != null) {
                        summary.setConstantValue(constant.getValue());
                    }
                }
                break;
            case LOADBOOL:
                constantDef(summaries, ssaInstruction, instruction.getB() != 0);
                break;
            case LOADNIL:
                for (SsaValue def : ssaInstruction.getDefs()) {
                    SsaValueSummary summary = summaryFor(summaries, def);
                    summary.setKind(SsaValueKind.CONSTANT);
                    summary.setConstantValue(null);
                }
                break;
            case GETGLOBAL:
                defKind(summaries, ssaInstruction, SsaValueKind.GLOBAL, SsaEffect.READ_GLOBAL);
                break;
            case GETUPVAL:
                defKind(summaries, ssaInstruction, SsaValueKind.UPVALUE, SsaEffect.READ_UPVALUE);
                break;
            case GETTABLE:
                defKind(summaries, ssaInstruction, SsaValueKind.TABLE_READ, SsaEffect.READ_TABLE);
                break;
            case NEWTABLE:
                defKind(summaries, ssaInstruction, SsaValueKind.TABLE_NEW, SsaEffect.NONE);
                break;
            case SELF:
                for (SsaValue def : ssaInstruction.getDefs()) {
                    SsaValueSummary summary = summaryFor(summaries, def);
                    summary.setKind(SsaValueKind.TABLE_READ);
                    summary.addEffect(SsaEffect.READ_TABLE);
                }
                break;
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
            case POW:
                defKind(summaries, ssaInstruction, SsaValueKind.ARITHMETIC, SsaEffect.NONE);
                break;
            case UNM:
            case NOT:
            case LEN:
                defKind(summaries, ssaInstruction, SsaValueKind.UNARY, SsaEffect.NONE);
                break;
            case CONCAT:
                defKind(summaries, ssaInstruction, SsaValueKind.CONCAT, SsaEffect.NONE);
                break;
            case CALL:
                for (SsaValue def : ssaInstruction.getDefs()) {
                    SsaValueSummary summary = summaryFor(summaries, def);
                    summary.setKind(SsaValueKind.CALL_RESULT);
                    summary.addEffect(SsaEffect.CALL);
                }
                break;
            case VARARG:
                defKind(summaries, ssaInstruction, SsaValueKind.VARARG, SsaEffect.NONE);
                break;
            case CLOSURE:
                defKind(summaries, ssaInstruction, SsaValueKind.CLOSURE, SsaEffect.NONE);
                break;
            case SETGLOBAL:
                effectOnly(summaries, ssaInstruction, SsaEffect.WRITE_GLOBAL);
                effectful = true;
                break;
            case SETUPVAL:
                effectOnly(summaries, ssaInstruction, SsaEffect.WRITE_UPVALUE);
                effectful = true;
                break;
            case SETTABLE:
            case SETLIST:
                effectOnly(summaries, ssaInstruction, SsaEffect.WRITE_TABLE);
                effectful = true;
                break;
            case TAILCALL:
                effectOnly(summaries, ssaInstruction, SsaEffect.CALL);
                effectful = true;
                break;
            case RETURN:
                effectOnly(summaries, ssaInstruction, SsaEffect.RETURN);
                effectful = true;
                break;
            case JMP:
            case FORPREP:
            case FORLOOP:
            case TFORLOOP:
            case EQ:
            case LT:
            case LE:
            case TEST:
            case TESTSET:
                effectOnly(summaries, ssaInstruction, SsaEffect.CONTROL_FLOW);
                effectful = true;
                for (SsaValue def : ssaInstruction.getDefs()) {
                    summaryFor(summaries, def).setKind(SsaValueKind.UNKNOWN);
                }
                break;
            case CLOSE:
                effectOnly(summaries, ssaInstruction, SsaEffect.CLOSE_UPVALUES);
                effectful = true;
                break;
            case UNKNOWN:
            default:
                throw new IllegalArgumentException("Unhandled opcode in SSA expression analyzer: " + opcode);
        }
        return effectful || opcode == Opcode.CALL || opcode == Opcode.GETTABLE || opcode == Opcode.GETGLOBAL
                || opcode == Opcode.GETUPVAL;
    }

    private void constantDef(Map<SsaValue, SsaValueSummary> summaries, SsaInstruction instruction, Object value) {
        if (instruction.getDefs().isEmpty()) {
            return;
        }
        SsaValueSummary summary = summaryFor(summaries, instruction.getDefs().get(0));
        summary.setKind(SsaValueKind.CONSTANT);
        summary.setConstantValue(value);
    }

    private void defKind(Map<SsaValue, SsaValueSummary> summaries, SsaInstruction instruction, SsaValueKind kind,
            SsaEffect effect) {
        for (SsaValue def : instruction.getDefs()) {
            SsaValueSummary summary = summaryFor(summaries, def);
            summary.setKind(kind);
            summary.addEffect(effect);
        }
    }

    private void effectOnly(Map<SsaValue, SsaValueSummary> summaries, SsaInstruction instruction, SsaEffect effect) {
        for (SsaValue def : instruction.getDefs()) {
            summaryFor(summaries, def).addEffect(effect);
        }
    }

    private SsaValueSummary summaryFor(Map<SsaValue, SsaValueSummary> summaries, SsaValue value) {
        SsaValueSummary summary = summaries.get(value);
        if (summary == null) {
            summary = new SsaValueSummary(value);
            if (value != null && value.isImplicit()) {
                summary.setKind(SsaValueKind.PARAMETER);
            }
            summaries.put(value, summary);
        }
        return summary;
    }
}
