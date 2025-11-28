package com.github.relua.decompiler;

import java.util.Set;

/**
 * SESE区域类，用于表示单入口单出口区域
 */
public class SESERegion {
    private BasicBlock entry; // 入口基本块
    private BasicBlock exit; // 出口基本块
    private Set<BasicBlock> blocks; // 区域内的所有基本块
    private RegionType type; // 区域类型
    
    // 区域类型枚举
    public enum RegionType {
        IF_THEN,        // if-then结构
        IF_THEN_ELSE,   // if-then-else结构
        WHILE_LOOP,      // while循环
        REPEAT_LOOP,     // repeat-until循环
        FOR_LOOP,        // for循环
        SEQUENCE,        // 顺序结构
        SIMPLE_BLOCK,    // 简单基本块
        COMPOUND         // 复合结构
    }
    
    public SESERegion(BasicBlock entry, BasicBlock exit, Set<BasicBlock> blocks, RegionType type) {
        this.entry = entry;
        this.exit = exit;
        this.blocks = blocks;
        this.type = type;
    }
    
    // getter方法
    public BasicBlock getEntry() { return entry; }
    public BasicBlock getExit() { return exit; }
    public Set<BasicBlock> getBlocks() { return blocks; }
    public RegionType getType() { return type; }
}