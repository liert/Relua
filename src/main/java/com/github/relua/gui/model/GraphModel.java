package com.github.relua.gui.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 图形数据模型，负责管理所有节点与连线数据及其拓扑关系
 */
public class GraphModel {
    private final List<GraphNode> nodes = new ArrayList<>();
    private final List<GraphEdge> edges = new ArrayList<>();

    /**
     * 清空模型中的所有节点与边
     */
    public void clear() {
        nodes.clear();
        edges.clear();
    }

    /**
     * 添加节点
     * @param node 节点
     * @return 节点索引
     */
    public int addNode(GraphNode node) {
        nodes.add(node);
        return nodes.size() - 1;
    }

    /**
     * 添加有向边
     * @param sourceIndex 源节点索引
     * @param targetIndex 目标节点索引
     */
    public void addEdge(int sourceIndex, int targetIndex) {
        edges.add(new GraphEdge(sourceIndex, targetIndex));
    }

    public List<GraphNode> getNodes() {
        return nodes;
    }

    public List<GraphEdge> getEdges() {
        return edges;
    }

    public GraphNode getNode(int index) {
        if (index >= 0 && index < nodes.size()) {
            return nodes.get(index);
        }
        return null;
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getEdgeCount() {
        return edges.size();
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    /**
     * 获取指定节点的入度
     * @param nodeIndex 节点索引
     * @return 入度计数
     */
    public int getInDegree(int nodeIndex) {
        int inDegree = 0;
        for (GraphEdge edge : edges) {
            if (edge.getTargetIndex() == nodeIndex) {
                inDegree++;
            }
        }
        return inDegree;
    }

    /**
     * 获取指定节点的所有后继节点索引列表
     * @param nodeIndex 节点索引
     * @return 后继节点索引列表
     */
    public List<Integer> getSuccessors(int nodeIndex) {
        List<Integer> successors = new ArrayList<>();
        for (GraphEdge edge : edges) {
            if (edge.getSourceIndex() == nodeIndex) {
                successors.add(edge.getTargetIndex());
            }
        }
        return successors;
    }
}
