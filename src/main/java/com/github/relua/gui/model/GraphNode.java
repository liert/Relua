package com.github.relua.gui.model;

import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

/**
 * 图形可视化节点数据模型，封装单个节点的几何信息与展示样式
 */
public class GraphNode {
    public static final int DEFAULT_NODE_WIDTH = 150;
    public static final int DEFAULT_NODE_HEIGHT = 60;
    public static final int NODE_PADDING = 10;
    public static final double LINE_HEIGHT = 15.0;
    private static final Font NODE_FONT = Font.font("Consolas", 11);

    private String label;
    private double x;
    private double y;
    private int width;
    private int height;
    private GraphNodeType type;

    public GraphNode(String label, double x, double y) {
        this(label, x, y, GraphNodeType.NORMAL);
    }

    public GraphNode(String label, double x, double y, GraphNodeType type) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.type = type != null ? type : GraphNodeType.NORMAL;
        calculateSize(label);
    }

    /**
     * 根据标签文本自动计算节点尺寸，自适应文本多行宽高
     * @param label 节点标签文本
     */
    public void calculateSize(String label) {
        if (label == null || label.isEmpty()) {
            this.width = DEFAULT_NODE_WIDTH;
            this.height = DEFAULT_NODE_HEIGHT;
            return;
        }

        String[] lines = label.split("\\n");
        double maxLineWidth = 0;
        Text text = new Text();
        text.setFont(NODE_FONT);
        text.setBoundsType(TextBoundsType.VISUAL);

        for (String line : lines) {
            text.setText(line);
            double lineWidth = text.getBoundsInLocal().getWidth();
            if (lineWidth > maxLineWidth) {
                maxLineWidth = lineWidth;
            }
        }

        this.width = (int) Math.max(maxLineWidth + 2 * NODE_PADDING, DEFAULT_NODE_WIDTH);
        this.height = (int) (lines.length * LINE_HEIGHT + 2 * NODE_PADDING);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        calculateSize(label);
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public GraphNodeType getType() {
        return type;
    }

    public void setType(GraphNodeType type) {
        this.type = type;
    }
}
