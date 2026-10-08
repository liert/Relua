package com.github.relua.gui.model;

/**
 * 图形可视化边数据模型，表示两节点之间的有向关联
 */
public class GraphEdge {
    private final int sourceIndex;
    private final int targetIndex;

    public GraphEdge(int sourceIndex, int targetIndex) {
        this.sourceIndex = sourceIndex;
        this.targetIndex = targetIndex;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public int getTargetIndex() {
        return targetIndex;
    }
}
