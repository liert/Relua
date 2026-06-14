package com.github.relua.gui.views;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * 图形可视化视图类，使用JavaFX Canvas实现AST和CFG的可视化展示
 */
public class GraphVisualizationView {
    // JavaFX的Canvas组件
    private final Canvas canvas;
    private final GraphicsContext gc;
    
    // 图形容器
    private final StackPane container;
    
    // 节点和边的数据结构
    private List<NodeData> nodes;
    private List<EdgeData> edges;
    
    // 当前缩放级别
    private double currentScale = 1.0;
    
    // 视图模式枚举
    private enum ViewMode {
        GRAPH, TEXT
    }
    
    // 当前视图模式
    private ViewMode currentViewMode = ViewMode.GRAPH;
    
    // 文本内容
    private String textContent = "";
    
    // 拖拽相关变量
    private int draggedNodeIndex = -1; // 当前正在拖动的节点索引，-1表示没有节点被拖动
    private double dragOffsetX = 0; // 鼠标按下时相对于节点左上角的X偏移
    private double dragOffsetY = 0; // 鼠标按下时相对于节点左上角的Y偏移
    
    // 悬浮释义面板组件
    private VBox descriptionPanel;
    private Label descTitleLabel;
    private Label descSyntaxLabel;
    private Label descTextLabel;

    // 指令中文释义映射
    private static final Map<String, String[]> OPCODE_INFO = new HashMap<>();
    
    static {
        OPCODE_INFO.put("MOVE", new String[]{"R(A) := R(B)", "寄存器赋值。将寄存器 B 的值复制给寄存器 A。"});
        OPCODE_INFO.put("LOADK", new String[]{"R(A) := Kstr(Bx)", "加载常量。将常量池中的常量 Bx 载入寄存器 A。"});
        OPCODE_INFO.put("LOADBOOL", new String[]{"R(A) := (Bool)B; if (C) pc++", "加载布尔值。将布尔值 B（0为false，1为true）载入寄存器 A。如果 C 非 0，则跳过下一条指令。"});
        OPCODE_INFO.put("LOADNIL", new String[]{"R(A) := ... := R(B) := nil", "批量加载 nil。将寄存器 A 到 B 范围内的所有寄存器设为 nil。"});
        OPCODE_INFO.put("GETUPVAL", new String[]{"R(A) := UpValue[B]", "获取 Upvalue。将 Upvalue B 的值载入寄存器 A。"});
        OPCODE_INFO.put("GETGLOBAL", new String[]{"R(A) := Gtab[Kstr(Bx)]", "获取全局变量。将全局变量 Bx 的值载入寄存器 A。"});
        OPCODE_INFO.put("GETTABLE", new String[]{"R(A) := R(B)[RK(C)]", "读取表元素。以寄存器或常量 C 为键，读取表 B 中的值并存入寄存器 A。"});
        OPCODE_INFO.put("SETGLOBAL", new String[]{"Gtab[Kstr(Bx)] := R(A)", "设置全局变量。将寄存器 A 的值赋给全局变量 Bx。"});
        OPCODE_INFO.put("SETUPVAL", new String[]{"UpValue[B] := R(A)", "设置 Upvalue。将寄存器 A 的值赋给 Upvalue B。"});
        OPCODE_INFO.put("SETTABLE", new String[]{"R(A)[RK(B)] := RK(C)", "写入表元素。以寄存器或常量 B 为键，将寄存器或常量 C 的值存入表 A 中。"});
        OPCODE_INFO.put("NEWTABLE", new String[]{"R(A) := {} (size = B,C)", "创建新表。在寄存器 A 中创建一个新表，预分配数组大小为 B，哈希表大小为 C。"});
        OPCODE_INFO.put("SELF", new String[]{"R(A+1) := R(B); R(A) := R(B)[RK(C)]", "对象方法准备。用于面向对象调用。将对象 B 复制到 R(A+1)，并将方法 B[RK(C)] 读入寄存器 A。"});
        OPCODE_INFO.put("ADD", new String[]{"R(A) := RK(B) + RK(C)", "加法运算。将寄存器或常量 B 与 C 相加，结果存入寄存器 A。"});
        OPCODE_INFO.put("SUB", new String[]{"R(A) := RK(B) - RK(C)", "减法运算。将寄存器或常量 B 与 C 相减，结果存入寄存器 A。"});
        OPCODE_INFO.put("MUL", new String[]{"R(A) := RK(B) * RK(C)", "乘法运算。将寄存器或常量 B 与 C 相乘，结果存入寄存器 A。"});
        OPCODE_INFO.put("DIV", new String[]{"R(A) := RK(B) / RK(C)", "除法运算。将寄存器或常量 B 与 C 相除，结果存入寄存器 A。"});
        OPCODE_INFO.put("MOD", new String[]{"R(A) := RK(B) % RK(C)", "取模运算。将寄存器或常量 B 对 C 求余，结果存入寄存器 A。"});
        OPCODE_INFO.put("POW", new String[]{"R(A) := RK(B) ^ RK(C)", "幂运算。将寄存器或常量 B 进行 C 次方幂运算，结果存入寄存器 A。"});
        OPCODE_INFO.put("UNM", new String[]{"R(A) := -R(B)", "取负运算。将寄存器 B 的数值取负，结果存入寄存器 A。"});
        OPCODE_INFO.put("NOT", new String[]{"R(A) := not R(B)", "逻辑非运算。将寄存器 B 进行逻辑非（not）运算，结果存入寄存器 A。"});
        OPCODE_INFO.put("LEN", new String[]{"R(A) := length of R(B)", "长度运算。获取寄存器 B（表或字符串）的长度，结果存入寄存器 A。"});
        OPCODE_INFO.put("CONCAT", new String[]{"R(A) := R(B) .. ... .. R(C)", "字符串拼接。将寄存器 B 到 C 范围内的值拼接为字符串，存入寄存器 A。"});
        OPCODE_INFO.put("JMP", new String[]{"pc += sBx", "无条件跳转。将程序计数器 PC 加上偏移量 sBx，实现跳转。"});
        OPCODE_INFO.put("EQ", new String[]{"if ((RK(B) == RK(C)) ~= A) then pc++", "等于测试。比较寄存器或常量 B 与 C。若结果与 A（0或1）不一致，则跳过下一条 JMP 指令。"});
        OPCODE_INFO.put("LT", new String[]{"if ((RK(B) < RK(C)) ~= A) then pc++", "小于测试。比较寄存器或常量 B 是否小于 C。若结果与 A 不一致，则跳过下一条 JMP 指令。"});
        OPCODE_INFO.put("LE", new String[]{"if ((RK(B) <= RK(C)) ~= A) then pc++", "小于等于测试。比较寄存器或常量 B 是否小于等于 C。若结果与 A 不一致，则跳过下一条 JMP 指令。"});
        OPCODE_INFO.put("TEST", new String[]{"if not (R(A) <=> C) then pc++", "条件测试。检查寄存器 A 的真假值是否与 C（0为false，1为true）一致。若不一致，则跳过下一条 JMP 指令。"});
        OPCODE_INFO.put("TESTSET", new String[]{"if (R(B) <=> C) then R(A) := R(B) else pc++", "测试并赋值。如果寄存器 B 的真假值与 C 一致，则将其赋值给寄存器 A，否则跳过下一条 JMP 指令。"});
        OPCODE_INFO.put("CALL", new String[]{"R(A), ... := R(A)(R(A+1), ... , R(A+B-1))", "函数调用。调用寄存器 A 中的函数，传入 B-1 个参数，并将 C-1 个返回值存回 R(A) 起始的寄存器。"});
        OPCODE_INFO.put("TAILCALL", new String[]{"return R(A)(R(A+1), ... , R(A+B-1))", "尾调用。以尾递归或尾调用的形式调用寄存器 A 中的函数，不占用当前栈帧。"});
        OPCODE_INFO.put("RETURN", new String[]{"return R(A), ... , R(A+B-2)", "函数返回。返回寄存器 A 开始的 B-1 个返回值。若 B 为 0，表示返回当前栈顶的所有值。"});
        OPCODE_INFO.put("FORLOOP", new String[]{"R(A)+=R(A+2); if R(A) <?= R(A+1) then { pc+=sBx; R(A+3)=R(A) }", "数值型循环迭代。将计数器 R(A) 加上步长 R(A+2)。若未达到终点 R(A+1)，则跳转回循环体并将 R(A+3) 设为计数器值。"});
        OPCODE_INFO.put("FORPREP", new String[]{"R(A)-=R(A+2); pc+=sBx", "数值型循环准备。将计数器减去步长，初始化循环参数，并无条件跳转至 FORLOOP 指令进行首次判断。"});
        OPCODE_INFO.put("TFORLOOP", new String[]{"R(A+3), ... := R(A)(R(A+1), R(A+2)); if R(A+3) ~= nil then { R(A+2)=R(A+3); pc+=sBx }", "泛型循环迭代。调用迭代器函数 R(A)，传入状态 R(A+1) 和控制变量 R(A+2)。如果返回的首个值不为 nil，则更新控制变量并跳转回循环体。"});
        OPCODE_INFO.put("SETLIST", new String[]{"R(A)[(c-1)*FPF+i] := R(A+i), 1 <= i <= b", "批量设置表元素。用于快速初始化数组。将寄存器 R(A+1) 起的 B 个值批量填入表 A 的指定索引区间。"});
        OPCODE_INFO.put("CLOSE", new String[]{"close all upvalues >= R(A)", "关闭 Upvalue。关闭所有在栈中等于或高于寄存器 A 地址的活动 Upvalue。"});
        OPCODE_INFO.put("CLOSURE", new String[]{"R(A) := closure(KPROTO[Bx])", "创建闭包。根据子函数原型 Bx 创建一个新的闭包，并存入寄存器 A。"});
        OPCODE_INFO.put("VARARG", new String[]{"R(A), ... := vararg()", "接收变长参数。将当前函数的变长参数（...）载入到寄存器 A 开始 of B-1 个寄存器中。"});
    }
    
    
    // 节点尺寸
    private static final int DEFAULT_NODE_WIDTH = 150;
    private static final int DEFAULT_NODE_HEIGHT = 60;
    private static final int NODE_PADDING = 10;
    private static final int NODE_SPACING = 100;
    private static final int VERTICAL_SPACING = 120;
    
    // 字体设置
    private static final Font NODE_FONT = Font.font("Consolas", 11);
    private static final double LINE_HEIGHT = 15;
    
    // 节点类型枚举
    public enum NodeType {
        NORMAL, IF_BLOCK, LOOP_BLOCK, ELSE_BLOCK
    }
    
    /**
     * 节点数据类
     */
    private static class NodeData {
        String label;
        double x;
        double y;
        int width;
        int height;
        NodeType type;
        
        NodeData(String label, double x, double y) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.type = NodeType.NORMAL;
            // 计算节点尺寸
            calculateSize(label);
        }
        
        NodeData(String label, double x, double y, NodeType type) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.type = type;
            // 计算节点尺寸
            calculateSize(label);
        }
        
        /**
         * 计算节点尺寸，根据标签内容自动调整
         */
        private void calculateSize(String label) {
            if (label == null || label.isEmpty()) {
                this.width = DEFAULT_NODE_WIDTH;
                this.height = DEFAULT_NODE_HEIGHT;
                return;
            }
            
            // 分割标签为多行
            String[] lines = label.split("\\n");
            
            // 计算最大行宽
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
            
            // 设置节点尺寸，添加内边距
            this.width = (int) Math.max(maxLineWidth + 2 * NODE_PADDING, DEFAULT_NODE_WIDTH);
            this.height = (int) (lines.length * LINE_HEIGHT + 2 * NODE_PADDING);
        }
    }
    
    /**
     * 边数据类
     */
    private static class EdgeData {
        int sourceIndex;
        int targetIndex;
        
        EdgeData(int sourceIndex, int targetIndex) {
            this.sourceIndex = sourceIndex;
            this.targetIndex = targetIndex;
        }
    }
    
    public GraphVisualizationView() {
        // 初始化自适应画布
        canvas = new Canvas();
        gc = canvas.getGraphicsContext2D();
        
        // 初始化容器
        container = new StackPane(canvas);
        
        // 绑定画布尺寸到容器，使画布随窗口缩放，并消除外部 ScrollPane 滚动条
        canvas.widthProperty().bind(container.widthProperty());
        canvas.heightProperty().bind(container.heightProperty());
        
        // 监听画布尺寸改变以触发重新绘制
        canvas.widthProperty().addListener((obs, oldVal, newVal) -> drawGraph());
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> drawGraph());
        
        // 初始化节点和边列表
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
        
        // 初始化悬浮释义面板
        initDescriptionPanel();
        
        // 添加鼠标事件处理程序，实现拖拽功能
        setupMouseEventHandlers();
        
        // 绘制初始图形
        drawGraph();
    }
    
    /**
     * 设置鼠标事件处理程序，实现拖拽功能
     */
    private void setupMouseEventHandlers() {
        // 鼠标按下事件处理
        canvas.setOnMousePressed(event -> {
            // 计算鼠标在画布上的坐标（考虑缩放）
            double mouseX = event.getX() / currentScale;
            double mouseY = event.getY() / currentScale;
            
            boolean nodeClicked = false;
            
            // 检查是否点击了某个节点
            for (int i = 0; i < nodes.size(); i++) {
                NodeData node = nodes.get(i);
                // 检查鼠标是否在节点范围内
                if (mouseX >= node.x && mouseX <= node.x + node.width &&
                    mouseY >= node.y && mouseY <= node.y + node.height) {
                    nodeClicked = true;
                    // 记录被拖动的节点索引
                    draggedNodeIndex = i;
                    // 计算鼠标相对于节点左上角的偏移
                    dragOffsetX = mouseX - node.x;
                    dragOffsetY = mouseY - node.y;
                    
                    // 计算相对垂直坐标，检测具体点击了第几行
                    double localY = mouseY - node.y;
                    int lineIdx = (int) ((localY - NODE_PADDING) / LINE_HEIGHT);
                    
                    String[] lines = node.label.split("\\n");
                    if (lineIdx >= 3 && lineIdx < lines.length) {
                        String line = lines[lineIdx];
                        updateInstructionDescription(line);
                    } else {
                        resetInstructionDescription();
                    }
                    break;
                }
            }
            
            if (!nodeClicked) {
                resetInstructionDescription();
            }
        });
        
        // 鼠标拖动事件处理
        canvas.setOnMouseDragged(event -> {
            // 如果有节点被拖动
            if (draggedNodeIndex != -1) {
                // 计算鼠标在画布上的坐标（考虑缩放）
                double mouseX = event.getX() / currentScale;
                double mouseY = event.getY() / currentScale;
                
                // 更新节点位置
                NodeData node = nodes.get(draggedNodeIndex);
                node.x = mouseX - dragOffsetX;
                node.y = mouseY - dragOffsetY;
                
                // 重新绘制图形
                drawGraph();
            }
        });
        
        // 鼠标释放事件处理
        canvas.setOnMouseReleased(event -> {
            // 重置拖动状态
            draggedNodeIndex = -1;
        });
    }
    
    public void clearGraph() {
        nodes.clear();
        edges.clear();
        resetInstructionDescription();
        drawGraph();
    }
    
    /**
     * 添加节点到图形中
     * @param label 节点标签
     * @return 节点索引
     */
    public int addNode(String label) {
        // 简单的布局算法：水平排列
        double x = 50 + (nodes.size() * (DEFAULT_NODE_WIDTH + NODE_SPACING));
        double y = 50;
        nodes.add(new NodeData(label, x, y));
        drawGraph();
        return nodes.size() - 1;
    }
    
    /**
     * 添加节点到图形中，指定节点类型
     * @param label 节点标签
     * @param type 节点类型
     * @return 节点索引
     */
    public int addNode(String label, NodeType type) {
        // 简单的布局算法：水平排列
        double x = 50 + (nodes.size() * (DEFAULT_NODE_WIDTH + NODE_SPACING));
        double y = 50;
        nodes.add(new NodeData(label, x, y, type));
        drawGraph();
        return nodes.size() - 1;
    }
    
    /**
     * 添加边到图形中
     * @param source 源节点索引
     * @param target 目标节点索引
     */
    public void addEdge(int source, int target) {
        edges.add(new EdgeData(source, target));
        drawGraph();
    }
    
    /**
     * 绘制图形，根据当前视图模式绘制图形或文本
     */
    private void drawGraph() {
        // 更新释义面板的可见性
        updateDescriptionPanelVisibility();
        
        // 清空画布
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // 应用缩放
        gc.save();
        gc.scale(currentScale, currentScale);
        
        if (currentViewMode == ViewMode.GRAPH) {
            // 绘制图形
            drawGraphContent();
        } else {
            // 绘制文本
            drawTextContent();
        }
        
        gc.restore();
    }
    
    /**
     * 绘制图形内容
     */
    private void drawGraphContent() {
        // 绘制边
        for (EdgeData edge : edges) {
            NodeData sourceNode = nodes.get(edge.sourceIndex);
            NodeData targetNode = nodes.get(edge.targetIndex);
            
            // 计算正交连线的起点（底端中点）和终点（顶端中点）
            double startX = sourceNode.x + sourceNode.width / 2.0;
            double startY = sourceNode.y + sourceNode.height;
            double endX = targetNode.x + targetNode.width / 2.0;
            double endY = targetNode.y;
            
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1.5);
            
            if (startY < endY) {
                // 正向边：使用折线在两层正中间进行水平折线
                double midY = startY + (endY - startY) / 2.0;
                
                gc.beginPath();
                gc.moveTo(startX, startY);
                gc.lineTo(startX, midY);
                gc.lineTo(endX, midY);
                gc.lineTo(endX, endY);
                gc.stroke();
            } else {
                // 反向边/回边：偏向外侧进行正交绕行，避免穿过节点
                double leftEdge = Math.min(sourceNode.x, targetNode.x);
                double rightEdge = Math.max(sourceNode.x + sourceNode.width, targetNode.x + targetNode.width);
                
                double detourX;
                if (endX < startX) {
                    detourX = leftEdge - 30.0; // 偏左绕行
                } else {
                    detourX = rightEdge + 30.0; // 偏右绕行
                }
                
                gc.beginPath();
                gc.moveTo(startX, startY);
                gc.lineTo(startX, startY + 15.0);
                gc.lineTo(detourX, startY + 15.0);
                gc.lineTo(detourX, endY - 15.0);
                gc.lineTo(endX, endY - 15.0);
                gc.lineTo(endX, endY);
                gc.stroke();
            }
            
            // 绘制正交箭头的指向（始终向下垂直进入）
            drawDownwardArrow(endX, endY);
        }
        
        // 绘制节点
        for (NodeData node : nodes) {
            // 绘制节点矩形，根据类型设置颜色
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1.0);
            
            // 根据节点类型设置填充颜色
            switch (node.type) {
                case IF_BLOCK:
                    gc.setFill(Color.LIGHTBLUE);
                    break;
                case LOOP_BLOCK:
                    gc.setFill(Color.LIGHTGREEN);
                    break;
                case ELSE_BLOCK:
                    gc.setFill(Color.LIGHTYELLOW);
                    break;
                default:
                    gc.setFill(Color.WHITE);
                    break;
            }
            
            gc.fillRect(node.x, node.y, node.width, node.height);
            gc.strokeRect(node.x, node.y, node.width, node.height);
            
            // 绘制节点标签，支持多行文本
            gc.setFill(Color.BLACK);
            gc.setFont(NODE_FONT);
            
            // 分割标签为多行
            String[] lines = node.label.split("\\n");
            
            // 计算文本起始位置（垂直居中）
            double startY = node.y + NODE_PADDING + LINE_HEIGHT;
            
            // 绘制每行文本
            for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
                String line = lines[lineIdx];
                double textX;
                if (lineIdx < 2 || line.trim().isEmpty()) {
                    // 居中绘制前两行（如 BB0 和 [0-1]）以及空行
                    Text text = new Text(line);
                    text.setFont(NODE_FONT);
                    double textWidth = text.getBoundsInLocal().getWidth();
                    textX = node.x + (node.width - textWidth) / 2.0;
                } else {
                    // 左对齐绘制指令和语义注释
                    textX = node.x + NODE_PADDING;
                }
                
                gc.fillText(line, textX, startY);
                startY += LINE_HEIGHT;
            }
        }
    }
    
    /**
     * 绘制文本内容
     */
    private void drawTextContent() {
        // 设置文本样式
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Consolas", 12));
        
        // 分割文本为多行
        String[] lines = textContent.split("\\n");
        
        // 绘制每行文本
        double x = 20 / currentScale;
        double y = 30 / currentScale;
        double lineHeight = 15 / currentScale;
        
        for (String line : lines) {
            gc.fillText(line, x, y);
            y += lineHeight;
        }
    }
    
    /**
     * 应用布局
     */
    public void applyLayout() {
        if (nodes.isEmpty()) return;
        
        // 对于CFG，使用层次布局
        applyHierarchicalLayout();
        drawGraph();
    }
    
    /**
     * 应用层次布局，适合有向图
     */
    private void applyHierarchicalLayout() {
        // 计算每个节点的层级
        Map<Integer, Integer> nodeLevels = calculateNodeLevels();
        
        // 按层级分组节点
        Map<Integer, List<Integer>> levelToNodes = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            int level = nodeLevels.getOrDefault(i, 0);
            levelToNodes.computeIfAbsent(level, k -> new ArrayList<>()).add(i);
        }
        
        // 计算每层的宽度
        int maxLevel = levelToNodes.keySet().stream().max(Integer::compare).orElse(0);
        
        // 布局每个节点
        for (int level = 0; level <= maxLevel; level++) {
            List<Integer> levelNodes = levelToNodes.getOrDefault(level, new ArrayList<>());
            if (levelNodes.isEmpty()) continue;
            
            // 计算该层的总宽度
            double totalWidth = 0;
            for (int nodeIndex : levelNodes) {
                totalWidth += nodes.get(nodeIndex).width + NODE_SPACING;
            }
            totalWidth -= NODE_SPACING; // 减去最后一个节点的间距
            
            // 计算起始X坐标，使该层居中
            double startX = (canvas.getWidth() / currentScale - totalWidth) / 2;
            
            // 布局该层的节点
            double currentX = startX;
            for (int nodeIndex : levelNodes) {
                NodeData node = nodes.get(nodeIndex);
                node.x = currentX;
                node.y = 50 + level * VERTICAL_SPACING;
                currentX += node.width + NODE_SPACING;
            }
        }
    }
    
    /**
     * 计算每个节点的层级
     * @return 节点索引到层级的映射
     */
    private Map<Integer, Integer> calculateNodeLevels() {
        Map<Integer, Integer> nodeLevels = new HashMap<>();
        
        // 初始化所有节点层级为-1（未访问）
        for (int i = 0; i < nodes.size(); i++) {
            nodeLevels.put(i, -1);
        }
        
        // 查找入度为0的节点作为起始节点
        List<Integer> startNodes = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            if (getInDegree(i) == 0) {
                startNodes.add(i);
            }
        }
        
        // 如果没有入度为0的节点，使用第一个节点
        if (startNodes.isEmpty() && !nodes.isEmpty()) {
            startNodes.add(0);
        }
        
        // BFS计算层级
        for (int startNode : startNodes) {
            bfsCalculateLevels(startNode, nodeLevels);
        }
        
        // 确保所有节点都有层级
        for (int i = 0; i < nodes.size(); i++) {
            if (nodeLevels.get(i) == -1) {
                nodeLevels.put(i, 0);
            }
        }
        
        return nodeLevels;
    }
    
    /**
     * BFS计算节点层级
     * @param startNode 起始节点索引
     * @param nodeLevels 节点层级映射
     */
    private void bfsCalculateLevels(int startNode, Map<Integer, Integer> nodeLevels) {
        List<Integer> queue = new ArrayList<>();
        queue.add(startNode);
        nodeLevels.put(startNode, 0);
        
        while (!queue.isEmpty()) {
            int currentNode = queue.remove(0);
            int currentLevel = nodeLevels.get(currentNode);
            
            // 查找所有后继节点
            List<Integer> successors = getSuccessors(currentNode);
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
     * 获取节点的入度
     * @param nodeIndex 节点索引
     * @return 入度
     */
    private int getInDegree(int nodeIndex) {
        int inDegree = 0;
        for (EdgeData edge : edges) {
            if (edge.targetIndex == nodeIndex) {
                inDegree++;
            }
        }
        return inDegree;
    }
    
    /**
     * 获取节点的所有后继节点
     * @param nodeIndex 节点索引
     * @return 后继节点索引列表
     */
    private List<Integer> getSuccessors(int nodeIndex) {
        List<Integer> successors = new ArrayList<>();
        for (EdgeData edge : edges) {
            if (edge.sourceIndex == nodeIndex) {
                successors.add(edge.targetIndex);
            }
        }
        return successors;
    }
    
    /**
     * 绘制向下箭头（正交连线专用）
     * @param x 箭头的顶点X坐标
     * @param y 箭头的顶点Y坐标
     */
    private void drawDownwardArrow(double x, double y) {
        double arrowSize = 6.0;
        gc.setFill(Color.BLACK);
        gc.fillPolygon(
            new double[]{x, x - arrowSize, x + arrowSize},
            new double[]{y, y - arrowSize, y - arrowSize},
            3
        );
    }
    
    /**
     * 应用树状布局
     */
    public void applyTreeLayout() {
        if (nodes.isEmpty()) return;
        
        // 简单的树状布局
        layoutTree(0, 50, 50, 150, 100);
        drawGraph();
    }
    
    /**
     * 递归布局树状结构
     * @param nodeIndex 当前节点索引
     * @param x 当前节点x坐标
     * @param y 当前节点y坐标
     * @param horizontalSpacing 水平间距
     * @param verticalSpacing 垂直间距
     */
    private void layoutTree(int nodeIndex, double x, double y, double horizontalSpacing, double verticalSpacing) {
        NodeData node = nodes.get(nodeIndex);
        node.x = x;
        node.y = y;
        
        // 查找所有子节点
        List<Integer> children = getSuccessors(nodeIndex);
        
        // 布局子节点
        if (!children.isEmpty()) {
            double totalWidth = (children.size() - 1) * horizontalSpacing;
            double startX = x - totalWidth / 2;
            
            for (int i = 0; i < children.size(); i++) {
                double childX = startX + i * horizontalSpacing;
                double childY = y + verticalSpacing;
                layoutTree(children.get(i), childX, childY, horizontalSpacing / 2, verticalSpacing);
            }
        }
    }
    
    /**
     * 缩放操作
     * @param scaleFactor 缩放因子
     */
    private void zoom(double scaleFactor) {
        currentScale *= scaleFactor;
        drawGraph();
    }
    
    /**
     * 放大
     */
    public void zoomIn() {
        zoom(1.2);
    }
    
    /**
     * 缩小
     */
    public void zoomOut() {
        zoom(0.8);
    }
    
    /**
     * 重置缩放
     */
    public void resetZoom() {
        currentScale = 1.0;
        drawGraph();
    }
    
    /**
     * 设置文本内容并切换到文本视图模式
     * @param text 要显示的文本内容
     */
    public void setTextContent(String text) {
        this.textContent = text;
        this.currentViewMode = ViewMode.TEXT;
        drawGraph();
    }
    
    /**
     * 切换到图形视图模式
     */
    public void switchToGraphMode() {
        this.currentViewMode = ViewMode.GRAPH;
        drawGraph();
    }
    
    /**
     * 清除文本内容
     */
    public void clearText() {
        this.textContent = "";
        this.currentViewMode = ViewMode.GRAPH;
        drawGraph();
    }
    
    /**
     * 获取视图节点
     * @return 视图节点
     */
    public Node getView() {
        return container;
    }

    /**
     * 初始化右下角悬浮释义面板
     */
    private void initDescriptionPanel() {
        descriptionPanel = new VBox(8); // 间距 8
        descriptionPanel.setPadding(new Insets(12));
        descriptionPanel.setMinWidth(280);
        descriptionPanel.setMaxWidth(300);
        descriptionPanel.setStyle(
            "-fx-background-color: rgba(30, 30, 30, 0.85);" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;" +
            "-fx-border-color: #555555;" +
            "-fx-border-width: 1;"
        );

        Label headerLabel = new Label("指令语义释义");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        headerLabel.setTextFill(Color.web("#00a2ff"));

        descTitleLabel = new Label("未选择指令");
        descTitleLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        descTitleLabel.setTextFill(Color.WHITE);

        descSyntaxLabel = new Label("语法: -");
        descSyntaxLabel.setFont(Font.font("Consolas", 11));
        descSyntaxLabel.setTextFill(Color.LIGHTGRAY);
        descSyntaxLabel.setWrapText(true);

        descTextLabel = new Label("点击 CFG 块中的汇编指令查看详细释义");
        descTextLabel.setFont(Font.font("System", 11));
        descTextLabel.setTextFill(Color.web("#cccccc"));
        descTextLabel.setWrapText(true);

        descriptionPanel.getChildren().addAll(headerLabel, descTitleLabel, descSyntaxLabel, descTextLabel);

        // 设置在 StackPane 的右下角
        StackPane.setAlignment(descriptionPanel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(descriptionPanel, new Insets(15));

        // 添加到容器中
        container.getChildren().add(descriptionPanel);
    }

    /**
     * 更新指令中文释义面板内容
     */
    private void updateInstructionDescription(String line) {
        if (line == null || line.trim().isEmpty()) {
            resetInstructionDescription();
            return;
        }

        // 解析指令名称
        String trimLine = line.trim();
        int colonIdx = trimLine.indexOf(':');
        if (colonIdx == -1) {
            resetInstructionDescription();
            return;
        }

        String rest = trimLine.substring(colonIdx + 1).trim();
        String[] parts = rest.split("\\s+");
        if (parts.length == 0) {
            resetInstructionDescription();
            return;
        }

        String opcode = parts[0];
        String[] info = OPCODE_INFO.get(opcode);

        if (info != null) {
            descTitleLabel.setText(opcode);
            descSyntaxLabel.setText("语法: " + info[0]);
            descTextLabel.setText(info[1]);
        } else {
            descTitleLabel.setText(opcode);
            descSyntaxLabel.setText("语法: 未知");
            descTextLabel.setText("未找到该指令的中文详细释义。");
        }
    }

    /**
     * 重置释义面板到默认状态
     */
    private void resetInstructionDescription() {
        if (descTitleLabel != null) {
            descTitleLabel.setText("未选择指令");
            descSyntaxLabel.setText("语法: -");
            descTextLabel.setText("点击 CFG 块中的汇编指令查看详细释义");
        }
    }

    /**
     * 动态更新释义面板的可见性，仅在 CFG 视图模式下显示
     */
    private void updateDescriptionPanelVisibility() {
        if (descriptionPanel != null) {
            boolean isCFG = currentViewMode == ViewMode.GRAPH && 
                            nodes.stream().anyMatch(node -> node.label.startsWith("BB"));
            descriptionPanel.setVisible(isCFG);
        }
    }
}