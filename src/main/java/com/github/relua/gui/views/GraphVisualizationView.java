package com.github.relua.gui.views;

import com.github.relua.gui.model.GraphEdge;
import com.github.relua.gui.model.GraphModel;
import com.github.relua.gui.model.GraphNode;
import com.github.relua.gui.model.GraphNodeType;
import com.github.relua.gui.theme.ThemeManager;
import com.github.relua.gui.views.graph.GraphLayoutEngine;
import com.github.relua.gui.views.graph.InstructionDescriptionPanel;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.List;

/**
 * 图形可视化视图，基于 JavaFX Canvas 负责 AST 与 CFG 的图形渲染与画布交互
 */
public class GraphVisualizationView {

    /**
     * 兼容性节点类型枚举定义，内部对应 GraphNodeType
     */
    public enum NodeType {
        NORMAL(GraphNodeType.NORMAL),
        IF_BLOCK(GraphNodeType.IF_BLOCK),
        LOOP_BLOCK(GraphNodeType.LOOP_BLOCK),
        ELSE_BLOCK(GraphNodeType.ELSE_BLOCK);

        private final GraphNodeType modelType;

        NodeType(GraphNodeType modelType) {
            this.modelType = modelType;
        }

        public GraphNodeType toModelType() {
            return modelType;
        }
    }

    // 字体与排版设置
    private static final Font NODE_FONT = Font.font("Consolas", 11);
    private static final double LINE_HEIGHT = GraphNode.LINE_HEIGHT;
    private static final int NODE_PADDING = GraphNode.NODE_PADDING;

    // 核心组件与领域子系统
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final StackPane container;
    private final GraphModel model;
    private final GraphLayoutEngine layoutEngine;
    private final InstructionDescriptionPanel descriptionPanel;

    // 视口缩放与平移变换
    private double currentScale = 1.0;
    private double translationX = 0;
    private double translationY = 0;

    // 鼠标交互状态
    private int draggedNodeIndex = -1;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;
    private boolean isPanning = false;
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    public GraphVisualizationView() {
        this.model = new GraphModel();
        this.layoutEngine = new GraphLayoutEngine();
        this.descriptionPanel = new InstructionDescriptionPanel();

        // 初始化自适应画布
        canvas = new Canvas();
        canvas.setPickOnBounds(true);
        gc = canvas.getGraphicsContext2D();

        // 初始化主容器，叠加画布与悬浮面板
        container = new StackPane(canvas, descriptionPanel);

        // 画布尺寸跟随父容器自适应绑定
        canvas.widthProperty().bind(container.widthProperty());
        canvas.heightProperty().bind(container.heightProperty());

        // 监听画布尺寸改变，触发重绘与首次自适应
        canvas.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal.doubleValue() <= 0 && newVal.doubleValue() > 0) {
                fitToViewer();
            }
            drawGraph();
        });
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal.doubleValue() <= 0 && newVal.doubleValue() > 0) {
                fitToViewer();
            }
            drawGraph();
        });

        // 注册主题变更监听器，当切换暗色/浅色时自动自适应重绘流图
        ThemeManager.getInstance().addListener(newTheme -> Platform.runLater(this::drawGraph));

        // 注册鼠标拖拽、平移与滚轮交互事件
        setupMouseEventHandlers();

        // 初始绘制
        drawGraph();
    }

    /**
     * 设置鼠标事件监听，实现节点拖拽移动、画布空白处平移与 Ctrl+滚轮缩放
     */
    private void setupMouseEventHandlers() {
        // 鼠标按下：检测节点抓取或进入平移模式
        canvas.setOnMousePressed(event -> {
            double mouseX = event.getX() / currentScale;
            double mouseY = event.getY() / currentScale;

            double logicalX = mouseX - translationX;
            double logicalY = mouseY - translationY;

            boolean nodeClicked = false;
            List<GraphNode> nodes = model.getNodes();

            for (int i = 0; i < nodes.size(); i++) {
                GraphNode node = nodes.get(i);
                if (logicalX >= node.getX() && logicalX <= node.getX() + node.getWidth()
                        && logicalY >= node.getY() && logicalY <= node.getY() + node.getHeight()) {
                    nodeClicked = true;
                    draggedNodeIndex = i;
                    dragOffsetX = logicalX - node.getX();
                    dragOffsetY = logicalY - node.getY();

                    // 计算相对垂直坐标，检测点击了哪一行指令
                    double localY = logicalY - node.getY();
                    int lineIdx = (int) ((localY - NODE_PADDING) / LINE_HEIGHT);

                    String[] lines = node.getLabel().split("\\n");
                    if (lineIdx >= 3 && lineIdx < lines.length) {
                        descriptionPanel.updateInstructionDescription(lines[lineIdx]);
                    } else {
                        descriptionPanel.resetInstructionDescription();
                    }
                    break;
                }
            }

            if (!nodeClicked) {
                descriptionPanel.resetInstructionDescription();
                isPanning = true;
                lastMouseX = event.getX();
                lastMouseY = event.getY();
            }
        });

        // 鼠标拖动：移动被选节点或平移画布
        canvas.setOnMouseDragged(event -> {
            if (draggedNodeIndex != -1) {
                double mouseX = event.getX() / currentScale;
                double mouseY = event.getY() / currentScale;

                GraphNode node = model.getNode(draggedNodeIndex);
                if (node != null) {
                    node.setX(mouseX - translationX - dragOffsetX);
                    node.setY(mouseY - translationY - dragOffsetY);
                    drawGraph();
                }
            } else if (isPanning) {
                double deltaX = event.getX() - lastMouseX;
                double deltaY = event.getY() - lastMouseY;

                translationX += deltaX / currentScale;
                translationY += deltaY / currentScale;

                lastMouseX = event.getX();
                lastMouseY = event.getY();

                drawGraph();
            }
        });

        // 鼠标释放：重置拖拽和平移状态
        canvas.setOnMouseReleased(event -> {
            draggedNodeIndex = -1;
            isPanning = false;
        });

        // Ctrl + 滚轮缩放画布
        canvas.setOnScroll((ScrollEvent event) -> {
            if (event.isControlDown()) {
                if (event.getDeltaY() > 0) {
                    zoom(1.1);
                } else if (event.getDeltaY() < 0) {
                    zoom(0.9);
                }
                event.consume();
            }
        });
    }

    /**
     * 清空当前图形的所有节点、连线与状态
     */
    public void clearGraph() {
        model.clear();
        descriptionPanel.resetInstructionDescription();
        translationX = 0;
        translationY = 0;
        drawGraph();
    }

    /**
     * 添加普通节点
     * @param label 节点标签文本
     * @return 节点索引
     */
    public int addNode(String label) {
        return addNode(label, NodeType.NORMAL);
    }

    /**
     * 添加指定类型的节点（支持兼容 NodeType）
     * @param label 节点标签文本
     * @param type 节点类型
     * @return 节点索引
     */
    public int addNode(String label, NodeType type) {
        return addNode(label, type != null ? type.toModelType() : GraphNodeType.NORMAL);
    }

    /**
     * 添加指定类型的节点
     * @param label 节点标签文本
     * @param type 领域模型节点类型
     * @return 节点索引
     */
    public int addNode(String label, GraphNodeType type) {
        double x = 50 + (model.getNodeCount() * (GraphNode.DEFAULT_NODE_WIDTH + 100));
        double y = 50;
        int index = model.addNode(new GraphNode(label, x, y, type));
        drawGraph();
        return index;
    }

    /**
     * 添加有向连线
     * @param source 源节点索引
     * @param target 目标节点索引
     */
    public void addEdge(int source, int target) {
        model.addEdge(source, target);
        drawGraph();
    }

    /**
     * 绘制图形，渲染节点与正交连接线
     */
    private void drawGraph() {
        // 根据是否存在 CFG 节点动态控制释义面板显示
        boolean isCFG = model.getNodes().stream()
                .anyMatch(node -> node.getLabel() != null && node.getLabel().startsWith("BB"));
        descriptionPanel.setVisible(isCFG);

        boolean isDark = ThemeManager.getInstance().isDark();

        // 1. 填充现代化底色
        Color bgColor = isDark ? Color.web("#1e1e1e") : Color.web("#f8f9fa");
        gc.setFill(bgColor);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 2. 绘制微点阵网格（Subtle Dot Grid）营造专业 IDE 流图氛围
        Color dotColor = isDark ? Color.web("#2c2c30") : Color.web("#e2e8f0");
        gc.setFill(dotColor);
        double gridStep = 24.0;
        double offsetX = (translationX * currentScale) % gridStep;
        if (offsetX < 0) offsetX += gridStep;
        double offsetY = (translationY * currentScale) % gridStep;
        if (offsetY < 0) offsetY += gridStep;

        for (double gx = offsetX; gx < canvas.getWidth(); gx += gridStep) {
            for (double gy = offsetY; gy < canvas.getHeight(); gy += gridStep) {
                gc.fillRect(gx, gy, 1.2, 1.2);
            }
        }

        // 3. 保存并应用缩放平移矩阵
        gc.save();
        gc.scale(currentScale, currentScale);
        gc.translate(translationX, translationY);

        drawGraphContent(isDark);

        gc.restore();
    }

    /**
     * 绘制图形的核心内容（边、箭头和现代圆角卡片）
     */
    private void drawGraphContent(boolean isDark) {
        List<GraphNode> nodes = model.getNodes();
        List<GraphEdge> edges = model.getEdges();

        Color edgeColor = isDark ? Color.web("#858585") : Color.web("#64748b");

        // 1. 绘制正交折线边与箭头
        for (GraphEdge edge : edges) {
            GraphNode sourceNode = model.getNode(edge.getSourceIndex());
            GraphNode targetNode = model.getNode(edge.getTargetIndex());
            if (sourceNode == null || targetNode == null) continue;

            double startX = sourceNode.getX() + sourceNode.getWidth() / 2.0;
            double startY = sourceNode.getY() + sourceNode.getHeight();
            double endX = targetNode.getX() + targetNode.getWidth() / 2.0;
            double endY = targetNode.getY();

            gc.setStroke(edgeColor);
            gc.setLineWidth(1.6);

            if (startY < endY) {
                // 正向边：在两层垂直正中间折线
                double midY = startY + (endY - startY) / 2.0;
                gc.beginPath();
                gc.moveTo(startX, startY);
                gc.lineTo(startX, midY);
                gc.lineTo(endX, midY);
                gc.lineTo(endX, endY);
                gc.stroke();
            } else {
                // 反向回边：在外侧偏置绕行，防止穿透节点
                double leftEdge = Math.min(sourceNode.getX(), targetNode.getX());
                double rightEdge = Math.max(sourceNode.getX() + sourceNode.getWidth(),
                        targetNode.getX() + targetNode.getWidth());

                double detourX = (endX < startX) ? (leftEdge - 30.0) : (rightEdge + 30.0);

                gc.beginPath();
                gc.moveTo(startX, startY);
                gc.lineTo(startX, startY + 15.0);
                gc.lineTo(detourX, startY + 15.0);
                gc.lineTo(detourX, endY - 15.0);
                gc.lineTo(endX, endY - 15.0);
                gc.lineTo(endX, endY);
                gc.stroke();
            }

            // 绘制正交箭头（垂直向下指示）
            drawDownwardArrow(endX, endY, edgeColor);
        }

        // 2. 绘制节点卡片（圆角矩形）
        double cornerRadius = 6.0;
        for (GraphNode node : nodes) {
            Color cardFill;
            Color cardBorder;
            Color textColor;

            if (isDark) {
                switch (node.getType()) {
                    case IF_BLOCK:
                        cardFill = Color.web("#142d42");
                        cardBorder = Color.web("#007acc");
                        textColor = Color.web("#9cdcfe");
                        break;
                    case LOOP_BLOCK:
                        cardFill = Color.web("#153322");
                        cardBorder = Color.web("#38a169");
                        textColor = Color.web("#a8ffcc");
                        break;
                    case ELSE_BLOCK:
                        cardFill = Color.web("#382f18");
                        cardBorder = Color.web("#d97706");
                        textColor = Color.web("#fde68a");
                        break;
                    default:
                        cardFill = Color.web("#252526");
                        cardBorder = Color.web("#474747");
                        textColor = Color.web("#cccccc");
                        break;
                }
            } else {
                switch (node.getType()) {
                    case IF_BLOCK:
                        cardFill = Color.web("#e0f2fe");
                        cardBorder = Color.web("#0284c7");
                        textColor = Color.web("#0369a1");
                        break;
                    case LOOP_BLOCK:
                        cardFill = Color.web("#dcfce7");
                        cardBorder = Color.web("#16a34a");
                        textColor = Color.web("#15803d");
                        break;
                    case ELSE_BLOCK:
                        cardFill = Color.web("#fef9c3");
                        cardBorder = Color.web("#ca8a04");
                        textColor = Color.web("#854d0e");
                        break;
                    default:
                        cardFill = Color.web("#ffffff");
                        cardBorder = Color.web("#cbd5e1");
                        textColor = Color.web("#1e293b");
                        break;
                }
            }

            // 绘制卡片圆角背景
            gc.setFill(cardFill);
            gc.fillRoundRect(node.getX(), node.getY(), node.getWidth(), node.getHeight(), cornerRadius, cornerRadius);

            // 绘制卡片外边框
            gc.setStroke(cardBorder);
            gc.setLineWidth(1.2);
            gc.strokeRoundRect(node.getX(), node.getY(), node.getWidth(), node.getHeight(), cornerRadius, cornerRadius);

            // 绘制多行文本
            gc.setFill(textColor);
            gc.setFont(NODE_FONT);

            String[] lines = node.getLabel().split("\\n");
            double currentY = node.getY() + NODE_PADDING + LINE_HEIGHT;

            for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
                String line = lines[lineIdx];
                double textX;
                if (lineIdx < 2 || line.trim().isEmpty()) {
                    // 居中绘制标题行
                    Text text = new Text(line);
                    text.setFont(NODE_FONT);
                    double textWidth = text.getBoundsInLocal().getWidth();
                    textX = node.getX() + (node.getWidth() - textWidth) / 2.0;
                } else {
                    // 左对齐绘制汇编指令
                    textX = node.getX() + NODE_PADDING;
                }

                gc.fillText(line, textX, currentY);
                currentY += LINE_HEIGHT;
            }
        }
    }

    /**
     * 绘制垂直向下指示的实心箭头
     */
    private void drawDownwardArrow(double x, double y, Color arrowColor) {
        double arrowSize = 5.5;
        gc.setFill(arrowColor);
        gc.fillPolygon(
            new double[]{x, x - arrowSize, x + arrowSize},
            new double[]{y, y - arrowSize, y - arrowSize},
            3
        );
    }

    /**
     * 应用层次排版布局并自适应居中视图
     */
    public void applyLayout() {
        if (model.isEmpty()) return;
        layoutEngine.applyHierarchicalLayout(model);
        fitToViewer();
        drawGraph();
    }

    /**
     * 应用树状排版布局
     */
    public void applyTreeLayout() {
        if (model.isEmpty()) return;
        translationX = 0;
        translationY = 0;
        layoutEngine.applyTreeLayout(model);
        drawGraph();
    }

    /**
     * 自适应缩放和居中整个图形
     */
    public void fitToViewer() {
        if (model.isEmpty()) return;

        GraphLayoutEngine.Bounds bounds = layoutEngine.calculateBounds(model);

        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
        if (canvasWidth <= 0) canvasWidth = 800;
        if (canvasHeight <= 0) canvasHeight = 600;

        double padding = 40;
        double scaleX = canvasWidth / (bounds.width + 2 * padding);
        double scaleY = canvasHeight / (bounds.height + 2 * padding);

        double fitScale = Math.min(scaleX, scaleY);
        currentScale = Math.max(0.1, Math.min(1.2, fitScale));

        double graphCenterX = bounds.minX + bounds.width / 2.0;
        double graphCenterY = bounds.minY + bounds.height / 2.0;

        double canvasCenterX = canvasWidth / 2.0 / currentScale;
        double canvasCenterY = canvasHeight / 2.0 / currentScale;

        translationX = canvasCenterX - graphCenterX;
        translationY = canvasCenterY - graphCenterY - (20 / currentScale);
    }

    private void zoom(double scaleFactor) {
        currentScale *= scaleFactor;
        drawGraph();
    }

    public void zoomIn() {
        zoom(1.2);
    }

    public void zoomOut() {
        zoom(0.8);
    }

    public void resetZoom() {
        currentScale = 1.0;
        translationX = 0;
        translationY = 0;
        drawGraph();
    }

    public void switchToGraphMode() {
        drawGraph();
    }

    public Node getView() {
        return container;
    }
}