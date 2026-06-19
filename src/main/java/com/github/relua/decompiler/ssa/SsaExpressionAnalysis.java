package com.github.relua.decompiler.ssa;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class SsaExpressionAnalysis {
    private final SsaFunction function;
    private final Map<SsaValue, SsaValueSummary> summaries;
    private final int analyzedInstructionCount;
    private final int effectfulInstructionCount;

    SsaExpressionAnalysis(SsaFunction function, Map<SsaValue, SsaValueSummary> summaries,
            int analyzedInstructionCount, int effectfulInstructionCount) {
        this.function = function;
        this.summaries = new LinkedHashMap<>(summaries);
        this.analyzedInstructionCount = analyzedInstructionCount;
        this.effectfulInstructionCount = effectfulInstructionCount;
    }

    public SsaFunction getFunction() {
        return function;
    }

    public SsaValueSummary getSummary(SsaValue value) {
        return summaries.get(value);
    }

    public SsaValueSummary resolveCopySummary(SsaValue value) {
        Set<SsaValue> visited = new HashSet<>();
        SsaValue current = value;
        SsaValueSummary summary = summaries.get(current);
        while (summary != null && summary.getKind() == SsaValueKind.COPY && summary.getCopySource() != null
                && visited.add(current)) {
            current = summary.getCopySource();
            summary = summaries.get(current);
        }
        return summary;
    }

    public Map<SsaValue, SsaValueSummary> getSummaries() {
        return Collections.unmodifiableMap(summaries);
    }

    public int getAnalyzedInstructionCount() {
        return analyzedInstructionCount;
    }

    public int getEffectfulInstructionCount() {
        return effectfulInstructionCount;
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("SSA Expression Analysis: ").append(function.getChunk().getFunction()).append('\n');
        sb.append("  instructions=").append(analyzedInstructionCount)
                .append(" effectful=").append(effectfulInstructionCount).append('\n');
        for (SsaValueSummary summary : summaries.values()) {
            sb.append("  ").append(summary)
                    .append(" uses=").append(function.getUseCount(summary.getValue()))
                    .append('\n');
        }
        return sb.toString();
    }
}
