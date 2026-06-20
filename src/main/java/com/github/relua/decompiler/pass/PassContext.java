package com.github.relua.decompiler.pass;

import com.github.relua.decompiler.CodeGeneratorContext;
import com.github.relua.decompiler.DecompilerPipeline;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

/**
 * 传递给各个优化 pass 的上下文容器
 */
public class PassContext {
    private final CodeGeneratorContext context;
    private final DecompilerPipeline pipeline;
    private final Set<String> parentDeclared;
    private final Set<String> upvalueNames;
    private final Set<String> optimizerUpvalues;
    private final Map<String, Set<Integer>> capturedLocalDefinitionPcs;
    private Set<String> declaredVariables = new HashSet<>();

    public PassContext(CodeGeneratorContext context, DecompilerPipeline pipeline,
                       Set<String> parentDeclared, Set<String> upvalueNames,
                       Set<String> optimizerUpvalues, Map<String, Set<Integer>> capturedLocalDefinitionPcs) {
        this.context = context;
        this.pipeline = pipeline;
        this.parentDeclared = parentDeclared != null ? parentDeclared : new HashSet<>();
        this.upvalueNames = upvalueNames != null ? upvalueNames : new HashSet<>();
        this.optimizerUpvalues = optimizerUpvalues != null ? optimizerUpvalues : new HashSet<>();
        this.capturedLocalDefinitionPcs = capturedLocalDefinitionPcs != null ? capturedLocalDefinitionPcs : new HashMap<>();
    }

    public CodeGeneratorContext getContext() {
        return context;
    }

    public DecompilerPipeline getPipeline() {
        return pipeline;
    }

    public Set<String> getParentDeclared() {
        return parentDeclared;
    }

    public Set<String> getUpvalueNames() {
        return upvalueNames;
    }

    public Set<String> getOptimizerUpvalues() {
        return optimizerUpvalues;
    }

    public Map<String, Set<Integer>> getCapturedLocalDefinitionPcs() {
        return capturedLocalDefinitionPcs;
    }

    public Set<String> getDeclaredVariables() {
        return declaredVariables;
    }

    public void setDeclaredVariables(Set<String> declaredVariables) {
        this.declaredVariables = declaredVariables;
    }
}
