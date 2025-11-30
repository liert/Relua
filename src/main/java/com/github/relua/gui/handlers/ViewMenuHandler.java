package com.github.relua.gui.handlers;

import com.github.relua.gui.views.GraphVisualizationView;
import javafx.event.ActionEvent;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;

/**
 * 视图菜单处理器，处理视图菜单的交互逻辑
 */
public class ViewMenuHandler {
    private GraphVisualizationView graphVisualizationView;
    private SplitPane mainSplitPane;
    private VBox graphContainer;

    /**
     * 构造函数
     * @param graphVisualizationView 图形可视化视图
     * @param mainSplitPane 主分割面板
     * @param graphContainer 图形容器
     */
    public ViewMenuHandler(GraphVisualizationView graphVisualizationView, SplitPane mainSplitPane, VBox graphContainer) {
        this.graphVisualizationView = graphVisualizationView;
        this.mainSplitPane = mainSplitPane;
        this.graphContainer = graphContainer;
    }

    /**
     * 处理切换图形事件
     * @param event 事件对象
     */
    public void handleToggleGraph(ActionEvent event) {
        if (graphContainer.isVisible()) {
            graphContainer.setVisible(false);
            mainSplitPane.getItems().remove(graphContainer);
        } else {
            mainSplitPane.getItems().add(graphContainer);
            graphContainer.setVisible(true);
        }
    }

    /**
     * 处理放大事件
     * @param event 事件对象
     */
    public void handleZoomIn(ActionEvent event) {
        graphVisualizationView.zoomIn();
    }

    /**
     * 处理缩小事件
     * @param event 事件对象
     */
    public void handleZoomOut(ActionEvent event) {
        graphVisualizationView.zoomOut();
    }

    /**
     * 处理重置缩放事件
     * @param event 事件对象
     */
    public void handleResetZoom(ActionEvent event) {
        graphVisualizationView.resetZoom();
    }
}