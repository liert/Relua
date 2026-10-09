package com.github.relua.gui.views;

import com.github.relua.gui.model.GraphModel;
import com.github.relua.gui.model.GraphNode;
import com.github.relua.gui.model.GraphNodeType;
import com.github.relua.gui.views.graph.GraphLayoutEngine;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 图形数据模型与布局计算引擎单元测试
 */
public class GraphLayoutEngineTest {

    @BeforeAll
    public static void initJFX() {
        try {
            java.lang.reflect.Method startupMethod = Platform.class.getMethod("startup", Runnable.class);
            startupMethod.invoke(null, (Runnable) () -> {});
        } catch (Throwable ignored) {
            // JavaFX 环境已启动或具备时忽略异常
        }
    }

    @Test
    public void testGraphModelOperations() {
        GraphModel model = new GraphModel();
        assertTrue(model.isEmpty());

        int n0 = model.addNode(new GraphNode("BB0\n[0-1]", 0, 0, GraphNodeType.NORMAL));
        int n1 = model.addNode(new GraphNode("BB1\n[2-3]\nMOVE R0 R1", 0, 0, GraphNodeType.IF_BLOCK));
        int n2 = model.addNode(new GraphNode("BB2\n[4-5]", 0, 0, GraphNodeType.ELSE_BLOCK));

        assertEquals(3, model.getNodeCount());
        assertEquals(GraphNodeType.IF_BLOCK, model.getNode(n1).getType());

        // 添加有向边: n0 -> n1, n0 -> n2
        model.addEdge(n0, n1);
        model.addEdge(n0, n2);

        assertEquals(2, model.getEdgeCount());
        assertEquals(0, model.getInDegree(n0));
        assertEquals(1, model.getInDegree(n1));
        assertEquals(1, model.getInDegree(n2));

        List<Integer> successors = model.getSuccessors(n0);
        assertEquals(2, successors.size());
        assertTrue(successors.contains(n1));
        assertTrue(successors.contains(n2));
    }

    @Test
    public void testHierarchicalLayoutAndBounds() {
        GraphModel model = new GraphModel();
        GraphLayoutEngine engine = new GraphLayoutEngine();

        // 构造典型钻石型分支图: n0 -> (n1, n2) -> n3
        int n0 = model.addNode(new GraphNode("BB0", 0, 0));
        int n1 = model.addNode(new GraphNode("BB1", 0, 0));
        int n2 = model.addNode(new GraphNode("BB2", 0, 0));
        int n3 = model.addNode(new GraphNode("BB3", 0, 0));

        model.addEdge(n0, n1);
        model.addEdge(n0, n2);
        model.addEdge(n1, n3);
        model.addEdge(n2, n3);

        engine.applyHierarchicalLayout(model);

        GraphNode node0 = model.getNode(n0);
        GraphNode node1 = model.getNode(n1);
        GraphNode node2 = model.getNode(n2);
        GraphNode node3 = model.getNode(n3);

        // 验证纵向层级单调递增 (n0 上方, n1/n2 中间层, n3 底层)
        assertTrue(node0.getY() < node1.getY(), "n0 应该高于 n1");
        assertEquals(node1.getY(), node2.getY(), "同层节点 n1 与 n2 的 Y 坐标应一致");
        assertTrue(node1.getY() < node3.getY(), "中间层应该高于底层 n3");

        // 验证同层节点的 X 坐标水平错开无重叠
        assertNotEquals(node1.getX(), node2.getX(), "同层节点 n1 与 n2 的 X 坐标不应重叠");

        // 验证边界计算有效
        GraphLayoutEngine.Bounds bounds = engine.calculateBounds(model);
        assertTrue(bounds.width > 0);
        assertTrue(bounds.height > 0);
        assertTrue(bounds.maxX >= bounds.minX);
        assertTrue(bounds.maxY >= bounds.minY);
    }

    @Test
    public void testTreeLayout() {
        GraphModel model = new GraphModel();
        GraphLayoutEngine engine = new GraphLayoutEngine();

        int root = model.addNode(new GraphNode("Root", 0, 0));
        int c1 = model.addNode(new GraphNode("Child1", 0, 0));
        int c2 = model.addNode(new GraphNode("Child2", 0, 0));

        model.addEdge(root, c1);
        model.addEdge(root, c2);

        engine.applyTreeLayout(model);

        assertTrue(model.getNode(root).getY() < model.getNode(c1).getY());
        assertEquals(model.getNode(c1).getY(), model.getNode(c2).getY());
        assertNotEquals(model.getNode(c1).getX(), model.getNode(c2).getX());
    }
}
