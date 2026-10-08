package com.github.relua.gui.views.graph;

import com.github.relua.gui.model.GraphModel;
import com.github.relua.gui.model.GraphNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图形布局计算引擎，专门负责分层拓扑布局、树形排版与图形边界计算
 */
public class GraphLayoutEngine {

    /**
     * 图形边界计算结果
     */
    public static class Bounds {
        public final double minX;
        public final double maxX;
        public final double minY;
        public final double maxY;
        public final double width;
        public final double height;

        public Bounds(double minX, double maxX, double minY, double maxY) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            double w = maxX - minX;
            double h = maxY - minY;
            this.width = w <= 0 ? GraphNode.DEFAULT_NODE_WIDTH : w;
            this.height = h <= 0 ? GraphNode.DEFAULT_NODE_HEIGHT : h;
        }
    }

    /**
     * 计算整个图模型的几何边界
     * @param model 图模型
     * @return 几何边界
     */
    public Bounds calculateBounds(GraphModel model) {
        if (model == null || model.isEmpty()) {
            return new Bounds(0, GraphNode.DEFAULT_NODE_WIDTH, 0, GraphNode.DEFAULT_NODE_HEIGHT);
        }

        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (GraphNode node : model.getNodes()) {
            minX = Math.min(minX, node.getX());
            maxX = Math.max(maxX, node.getX() + node.getWidth());
            minY = Math.min(minY, node.getY());
            maxY = Math.max(maxY, node.getY() + node.getHeight());
        }

        return new Bounds(minX, maxX, minY, maxY);
    }

    /**
     * 应用层次布局（适合有向控制流图）
     * @param model 图模型
     */
    public void applyHierarchicalLayout(GraphModel model) {
        if (model == null || model.isEmpty()) {
            return;
        }

        List<GraphNode> nodes = model.getNodes();
        Map<Integer, Integer> nodeLevels = calculateNodeLevels(model);

        // 按层级分组节点
        Map<Integer, List<Integer>> levelToNodes = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            int level = nodeLevels.getOrDefault(i, 0);
            levelToNodes.computeIfAbsent(level, k -> new ArrayList<>()).add(i);
        }

        int maxLevel = levelToNodes.keySet().stream().max(Integer::compare).orElse(0);

        // 动态计算每一层中节点的最大高度，以确定下一层的起始Y坐标，彻底避免纵向覆盖
        double[] levelY = new double[maxLevel + 1];
        double[] levelMaxHeight = new double[maxLevel + 1];

        for (int level = 0; level <= maxLevel; level++) {
            List<Integer> levelNodes = levelToNodes.getOrDefault(level, new ArrayList<>());
            double maxHeight = 0;
            for (int nodeIdx : levelNodes) {
                maxHeight = Math.max(maxHeight, nodes.get(nodeIdx).getHeight());
            }
            levelMaxHeight[level] = maxHeight > 0 ? maxHeight : GraphNode.DEFAULT_NODE_HEIGHT;
        }

        // 节点垂直层级间的净空隙
        double verticalGap = 80;
        levelY[0] = 50; // 起始层Y为50
        for (int level = 1; level <= maxLevel; level++) {
            levelY[level] = levelY[level - 1] + levelMaxHeight[level - 1] + verticalGap;
        }

        // 布局每个层级的节点
        for (int level = 0; level <= maxLevel; level++) {
            List<Integer> levelNodes = levelToNodes.getOrDefault(level, new ArrayList<>());
            if (levelNodes.isEmpty()) continue;

            // 节点间的水平间距，防止左右重叠
            double horizontalGap = 60;

            // 计算该层级所有节点的总宽度
            double totalWidth = 0;
            for (int nodeIdx : levelNodes) {
                totalWidth += nodes.get(nodeIdx).getWidth() + horizontalGap;
            }
            totalWidth -= horizontalGap; // 减去最后一个节点后多余的间距

            // 将当前层级的节点居中放置在 X = 0 附近
            double startX = -totalWidth / 2.0;
            double currentX = startX;
            for (int nodeIdx : levelNodes) {
                GraphNode node = nodes.get(nodeIdx);
                node.setX(currentX);
                node.setY(levelY[level]);
                currentX += node.getWidth() + horizontalGap;
            }
        }
    }

    /**
     * 计算每个节点的层级
     */
    private Map<Integer, Integer> calculateNodeLevels(GraphModel model) {
        Map<Integer, Integer> nodeLevels = new HashMap<>();
        int count = model.getNodeCount();

        // 初始化所有节点层级为-1（未访问）
        for (int i = 0; i < count; i++) {
            nodeLevels.put(i, -1);
        }

        // 查找入度为0的节点作为起始节点
        List<Integer> startNodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (model.getInDegree(i) == 0) {
                startNodes.add(i);
            }
        }

        // 如果没有入度为0的节点，使用第一个节点
        if (startNodes.isEmpty() && count > 0) {
            startNodes.add(0);
        }

        // BFS计算层级
        for (int startNode : startNodes) {
            bfsCalculateLevels(startNode, nodeLevels, model);
        }

        // 确保所有节点都有层级
        for (int i = 0; i < count; i++) {
            if (nodeLevels.get(i) == -1) {
                nodeLevels.put(i, 0);
            }
        }

        return nodeLevels;
    }

    /**
     * BFS计算节点层级
     */
    private void bfsCalculateLevels(int startNode, Map<Integer, Integer> nodeLevels, GraphModel model) {
        List<Integer> queue = new ArrayList<>();
        queue.add(startNode);
        nodeLevels.put(startNode, 0);

        while (!queue.isEmpty()) {
            int currentNode = queue.remove(0);
            int currentLevel = nodeLevels.get(currentNode);

            // 查找所有后继节点
            List<Integer> successors = model.getSuccessors(currentNode);
            for (int successor : successors) {
                // 忽略回边以符合顺序流，并防止有向循环图导致无限入队
                if (successor <= currentNode) {
                    continue;
                }
                int successorLevel = nodeLevels.get(successor);
                // 如果后继节点未访问，或者可以通过当前路径获得更高的层级
                if (successorLevel == -1 || successorLevel < currentLevel + 1) {
                    nodeLevels.put(successor, currentLevel + 1);
                    queue.add(successor);
                }
            }
        }
    }

    /**
     * 应用树状布局
     * @param model 图模型
     */
    public void applyTreeLayout(GraphModel model) {
        if (model == null || model.isEmpty()) {
            return;
        }
        layoutTree(model, 0, 50, 50, 150, 100);
    }

    /**
     * 递归布局树状结构
     */
    private void layoutTree(GraphModel model, int nodeIndex, double x, double y,
            double horizontalSpacing, double verticalSpacing) {
        GraphNode node = model.getNode(nodeIndex);
        if (node == null) return;

        node.setX(x);
        node.setY(y);

        List<Integer> successors = model.getSuccessors(nodeIndex);
        if (successors.isEmpty()) {
            return;
        }

        double totalWidth = (successors.size() - 1) * horizontalSpacing;
        double startX = x - totalWidth / 2.0;

        for (int i = 0; i < successors.size(); i++) {
            int childIndex = successors.get(i);
            layoutTree(model, childIndex, startX + i * horizontalSpacing, y + verticalSpacing,
                    horizontalSpacing, verticalSpacing);
        }
    }
}
