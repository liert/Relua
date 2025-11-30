package com.github.relua.gui.controllers;

import com.github.relua.gui.services.I18nService;
import com.github.relua.gui.handlers.FileMenuHandler;
import com.github.relua.gui.handlers.EditMenuHandler;
import com.github.relua.gui.handlers.ViewMenuHandler;
import com.github.relua.gui.handlers.HelpMenuHandler;
import com.github.relua.gui.views.TextEditorView;
import com.github.relua.gui.views.GraphVisualizationView;
import com.github.relua.gui.views.FileTreeView;
import com.github.relua.gui.views.LogView;
import com.github.relua.gui.utils.ASTGraphConverter;
import com.github.relua.gui.utils.CFGGraphConverter;
import com.github.relua.decompiler.Decompiler;
import com.github.relua.parser.LuacParser;
import com.github.relua.model.LuacFile;
import com.github.relua.ast.AstNode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

/**
 * 主界面控制器类，处理主界面的交互逻辑
 */
public class MainController {
    // FXML注入的组件
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private ScrollPane textEditorScrollPane;
    @FXML
    private ScrollPane graphScrollPane;
    @FXML
    private VBox textEditorContainer;
    @FXML
    private VBox graphContainer;
    @FXML
    private Label statusLabel;
    @FXML
    private Label fileLabel;
    @FXML
    private Label luaCodeLabel;
    @FXML
    private Label astGraphLabel;
    
    // 菜单
    @FXML
    private Menu menuFile;
    @FXML
    private Menu menuEdit;
    @FXML
    private Menu menuView;
    @FXML
    private Menu menuHelp;
    
    // 文件菜单项
    @FXML
    private MenuItem menuItemOpen;
    @FXML
    private MenuItem menuItemOpenFolder;
    @FXML
    private MenuItem menuItemSave;
    @FXML
    private MenuItem menuItemSaveAs;
    @FXML
    private MenuItem menuItemExit;
    
    // 编辑菜单项
    @FXML
    private MenuItem menuItemUndo;
    @FXML
    private MenuItem menuItemRedo;
    @FXML
    private MenuItem menuItemCut;
    @FXML
    private MenuItem menuItemCopy;
    @FXML
    private MenuItem menuItemPaste;
    
    // 视图菜单项
    @FXML
    private MenuItem menuItemToggleGraph;
    @FXML
    private MenuItem menuItemZoomIn;
    @FXML
    private MenuItem menuItemZoomOut;
    @FXML
    private MenuItem menuItemResetZoom;
    
    // 帮助菜单项
    @FXML
    private MenuItem menuItemAbout;
    
    // 工具栏按钮
    @FXML
    private Button btnOpen;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnUndo;
    @FXML
    private Button btnRedo;
    @FXML
    private Button btnZoomIn;
    @FXML
    private Button btnZoomOut;
    @FXML
    private Button btnResetZoom;
    @FXML
    private Button btnToggleGraph;
    @FXML
    private Button btnToggleViewType;
    
    // 文件树相关
    @FXML
    private VBox fileTreeContainer;
    @FXML
    private ScrollPane fileTreeScrollPane;
    private FileTreeView fileTreeView;
    
    // 日志相关
    @FXML
    private VBox logContainer;
    @FXML
    private ScrollPane logScrollPane;
    @FXML
    private Label logLabel;
    @FXML
    private ComboBox<String> logLevelComboBox;
    @FXML
    private Button btnClearLog;
    private LogView logView;
    
    // 视图组件
    private TextEditorView textEditorView;
    private GraphVisualizationView graphVisualizationView;
    
    // AST到图形的转换器
    private ASTGraphConverter astGraphConverter;
    
    // CFG到图形的转换器
    private CFGGraphConverter cfgGraphConverter;
    
    // 当前视图类型
    private ViewType currentViewType = ViewType.AST;
    
    // 视图类型枚举
    private enum ViewType {
        AST, CFG
    }
    
    // 当前打开的文件
    private File currentFile;
    
    // 国际化服务
    private I18nService i18nService;
    
    // 菜单处理器
    private FileMenuHandler fileMenuHandler;
    private EditMenuHandler editMenuHandler;
    private ViewMenuHandler viewMenuHandler;
    private HelpMenuHandler helpMenuHandler;
    
    /**
     * 初始化方法，在FXML加载完成后调用
     */
    @FXML
    public void initialize() {
        // 初始化文件树视图
        fileTreeView = new FileTreeView();
        fileTreeScrollPane.setContent(fileTreeView.getView());
        fileTreeScrollPane.setFitToWidth(true);
        fileTreeScrollPane.setFitToHeight(true);
        
        // 初始化文本编辑器视图
        textEditorView = new TextEditorView();
        textEditorScrollPane.setContent(textEditorView.getView());
        
        // 初始化图形可视化视图
        graphVisualizationView = new GraphVisualizationView();
        graphScrollPane.setContent(graphVisualizationView.getView());
        
        // 初始化AST到图形的转换器
        astGraphConverter = new ASTGraphConverter(graphVisualizationView);
        
        // 初始化CFG到图形的转换器
        cfgGraphConverter = new CFGGraphConverter(graphVisualizationView);
        
        // 初始化日志视图
        logView = new LogView();
        logView.setLogLevelComboBox(logLevelComboBox);
        logView.setClearLogButton(btnClearLog);
        logScrollPane.setContent(logView.getLogTextArea());
        logScrollPane.setFitToWidth(true);
        logScrollPane.setFitToHeight(true);
        
        // 设置ScrollPane的fitToWidth和fitToHeight属性，确保内容自动调整到适合ScrollPane的尺寸
        textEditorScrollPane.setFitToWidth(true);
        textEditorScrollPane.setFitToHeight(true);
        graphScrollPane.setFitToWidth(true);
        graphScrollPane.setFitToHeight(true);
        
        // 初始化国际化服务
        i18nService = new I18nService();
        
        // 设置国际化文本
        i18nService.initializeMenus(menuFile, menuEdit, menuView, menuHelp);
        i18nService.initializeFileMenuItems(menuItemOpen, menuItemOpenFolder, menuItemSave, menuItemSaveAs, menuItemExit);
        i18nService.initializeEditMenuItems(menuItemUndo, menuItemRedo, menuItemCut, menuItemCopy, menuItemPaste);
        i18nService.initializeViewMenuItems(menuItemToggleGraph, menuItemZoomIn, menuItemZoomOut, menuItemResetZoom);
        i18nService.initializeHelpMenuItems(menuItemAbout);
        i18nService.initializeToolbarButtons(btnOpen, btnSave, btnUndo, btnRedo, btnZoomIn, btnZoomOut, btnResetZoom, btnToggleGraph, btnToggleViewType);
        i18nService.initializeLabels(luaCodeLabel, astGraphLabel, logLabel, btnClearLog);
        
        // 设置初始状态
        updateStatus(i18nService.getInitialStatusText());
        updateFileLabel(i18nService.getInitialFileLabelText());
        
        // 测试日志输出
        java.util.logging.Logger.getLogger("MainController").info("应用程序启动成功");
        java.util.logging.Logger.getLogger("MainController").fine("调试信息: 日志系统已初始化");
        java.util.logging.Logger.getLogger("MainController").warning("警告信息: 这是一个测试警告");
        java.util.logging.Logger.getLogger("MainController").severe("错误信息: 这是一个测试错误");
        
        // 初始化菜单处理器
        fileMenuHandler = new FileMenuHandler(textEditorView, astGraphConverter, cfgGraphConverter, i18nService, statusLabel, fileLabel, fileTreeView);
        editMenuHandler = new EditMenuHandler(textEditorView);
        viewMenuHandler = new ViewMenuHandler(graphVisualizationView, mainSplitPane, graphContainer);
        helpMenuHandler = new HelpMenuHandler(i18nService);
    }
    
    /**
     * 处理打开文件夹事件
     * @param event 事件对象
     */
    @FXML
    private void handleOpenFolder(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Folder");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        
        // 创建一个虚拟的文件过滤器，用于模拟文件夹选择
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Folders", "*")
        );
        
        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            // 如果选择的是文件，获取其父文件夹
            File folder = selectedFile.isDirectory() ? selectedFile : selectedFile.getParentFile();
            if (folder != null) {
                // 加载文件夹内容到文件树
                fileTreeView.loadFolder(folder);
                updateStatus("Folder opened: " + folder.getName());
            }
        }
    }
    
    /**
     * 更新状态栏
     * @param status 状态信息
     */
    private void updateStatus(String status) {
        statusLabel.setText(status);
    }
    
    /**
     * 更新文件标签
     * @param fileName 文件名
     */
    private void updateFileLabel(String fileName) {
        fileLabel.setText(fileName);
    }
    
    // 文件菜单事件处理方法
    
    @FXML
    private void handleOpenFile(ActionEvent event) {
        fileMenuHandler.handleOpenFile(event);
    }
    
    @FXML
    private void handleSaveFile(ActionEvent event) {
        fileMenuHandler.handleSaveFile(event);
    }
    
    @FXML
    private void handleSaveAsFile(ActionEvent event) {
        fileMenuHandler.handleSaveAsFile(event);
    }
    
    @FXML
    private void handleExit(ActionEvent event) {
        Stage stage = (Stage) mainBorderPane.getScene().getWindow();
        stage.close();
    }
    
    // 编辑菜单事件处理方法
    
    @FXML
    private void handleUndo(ActionEvent event) {
        editMenuHandler.handleUndo(event);
    }
    
    @FXML
    private void handleRedo(ActionEvent event) {
        editMenuHandler.handleRedo(event);
    }
    
    @FXML
    private void handleCut(ActionEvent event) {
        editMenuHandler.handleCut(event);
    }
    
    @FXML
    private void handleCopy(ActionEvent event) {
        editMenuHandler.handleCopy(event);
    }
    
    @FXML
    private void handlePaste(ActionEvent event) {
        editMenuHandler.handlePaste(event);
    }
    
    // 视图菜单事件处理方法
    
    @FXML
    private void handleToggleGraph(ActionEvent event) {
        viewMenuHandler.handleToggleGraph(event);
    }
    
    @FXML
    private void handleZoomIn(ActionEvent event) {
        viewMenuHandler.handleZoomIn(event);
    }
    
    @FXML
    private void handleZoomOut(ActionEvent event) {
        viewMenuHandler.handleZoomOut(event);
    }
    
    @FXML
    private void handleResetZoom(ActionEvent event) {
        viewMenuHandler.handleResetZoom(event);
    }
    
    /**
     * 处理切换视图事件（AST/CFG）
     * @param event 事件对象
     */
    @FXML
    private void handleToggleViewType(ActionEvent event) {
        // 切换视图类型
        currentViewType = (currentViewType == ViewType.AST) ? ViewType.CFG : ViewType.AST;
        
        // 更新标签文本
        if (currentViewType == ViewType.AST) {
            astGraphLabel.setText("AST视图");
        } else {
            astGraphLabel.setText("CFG视图");
        }
        
        // 根据当前视图类型重新生成图形
        if (fileMenuHandler.getCurrentLuacFile() != null) {
            if (currentViewType == ViewType.AST) {
                astGraphConverter.convertToGraph(fileMenuHandler.getCurrentLuacFile());
            } else {
                cfgGraphConverter.convertToGraph(fileMenuHandler.getCurrentLuacFile());
            }
        }
    }
    
    // 辅助方法
    
    /**
     * 将文本保存到文件
     * @param file 文件对象
     */
    private void saveToFile(File file) {
        try {
            textEditorView.saveToFile(file);
            currentFile = file;
            updateFileLabel(file.getName());
            updateStatus("File saved successfully");
        } catch (IOException e) {
            updateStatus("Error saving file: " + e.getMessage());
            showError(i18nService.getErrorDialogTitle(), i18nService.getFileSaveFailedMessage(e.getMessage()));
        }
    }
    
    /**
     * 显示错误对话框
     * @param title 对话框标题
     * @param message 错误消息
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * 显示信息对话框
     * @param title 对话框标题
     * @param message 信息消息
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // 帮助菜单事件处理方法
    
    @FXML
    private void handleAbout(ActionEvent event) {
        helpMenuHandler.handleAbout(event);
    }
}