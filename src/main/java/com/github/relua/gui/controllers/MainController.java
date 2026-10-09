package com.github.relua.gui.controllers;

import com.github.relua.gui.utils.I18nUtil;
import com.github.relua.gui.handlers.FileMenuHandler;
import com.github.relua.gui.handlers.EditMenuHandler;
import com.github.relua.gui.handlers.ViewMenuHandler;
import com.github.relua.gui.handlers.HelpMenuHandler;
import com.github.relua.gui.views.TextEditorView;
import com.github.relua.gui.views.EditorTab;
import com.github.relua.gui.views.GraphVisualizationView;
import com.github.relua.gui.views.FileTreeView;
import com.github.relua.gui.views.LogView;
import com.github.relua.gui.utils.ASTGraphConverter;
import com.github.relua.gui.utils.CFGGraphConverter;
import com.github.relua.model.LuacFile;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Constant;
import com.github.relua.model.LocalVar;
import com.github.relua.util.BytecodeFormatter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.concurrent.Task;
import com.github.relua.gui.workspace.ProjectWorkspaceManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
    private SplitPane verticalSplitPane;
    @FXML
    private TabPane textEditorTabPane;
    @FXML
    private StackPane textEditorStackPane;
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
    private MenuItem menuItemToggleTabBytecode;
    @FXML
    private MenuItem menuItemBytecodeView;
    @FXML
    private MenuItem menuItemAstView;
    @FXML
    private MenuItem menuItemCfgView;
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
    private Button btnToggleBytecode;
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
    private GraphVisualizationView graphVisualizationView;
    private TextEditorView bytecodeSubViewEditor;
    private StackPane subViewStackPane;
    private javafx.scene.layout.HBox subViewHeaderBar;
    private ToggleButton btnScopeCurrentChunk;
    private ToggleButton btnScopeFullFile;
    private boolean bytecodeShowFullFile = false;
    
    // AST到图形的转换器
    private ASTGraphConverter astGraphConverter;
    
    // CFG到图形的转换器
    private CFGGraphConverter cfgGraphConverter;
    
    // 子视图类型枚举
    private enum SubViewType {
        NONE, BYTECODE, AST, CFG
    }
    
    // 当前激活的子视图类型
    private SubViewType currentSubViewType = SubViewType.NONE;
    
    // 当前打开的文件
    private File currentFile;
    
    // 菜单处理器
    private FileMenuHandler fileMenuHandler;
    private EditMenuHandler editMenuHandler;
    private ViewMenuHandler viewMenuHandler;
    private HelpMenuHandler helpMenuHandler;
    
    // 自动同步视图相关的状态缓存
    private Chunk currentDisplayedChunk = null;
    private LuacFile lastLoadedLuacFile = null;
    
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
        
        // 初始化标签页视图
        textEditorTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        
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
        graphScrollPane.setFitToWidth(true);
        graphScrollPane.setFitToHeight(true);
        
        // 初始化字节码代码编辑器（作为子视图代码呈现，与主代码视图体验一致）
        bytecodeSubViewEditor = new TextEditorView(false);
        bytecodeSubViewEditor.getCodeArea().setEditable(false);
        Node bytecodeNode = bytecodeSubViewEditor.getView();
        VBox.setVgrow(bytecodeNode, Priority.ALWAYS);

        // 初始化子视图内容堆叠容器
        subViewStackPane = new StackPane();
        VBox.setVgrow(subViewStackPane, Priority.ALWAYS);

        // 将原有的 graphScrollPane 移入 subViewStackPane
        graphContainer.getChildren().remove(graphScrollPane);
        subViewStackPane.getChildren().addAll(graphScrollPane, bytecodeNode);
        bytecodeNode.setVisible(false);
        bytecodeNode.setManaged(false);

        // 初始化子视图顶部标题栏及范围控制按钮
        initSubViewHeader();

        graphContainer.getChildren().add(subViewStackPane);

        // 初始化国际化文本
        initializeI18n();
        
        // 默认隐藏右侧图形视图，仅保留文件树和代码视图
        mainSplitPane.getItems().remove(graphContainer);
        graphContainer.setVisible(false);
        // 重新分配主分割线位置，防止移除 graph 节点后左侧树被挤扁成 0 宽度
        mainSplitPane.setDividerPosition(0, 0.25);
        
        // 设置初始状态
        updateStatus(I18nUtil.getString("status.ready"));
        updateFileLabel(I18nUtil.getString("status.noFileOpened"));

        // 初始化菜单处理器
        fileMenuHandler = new FileMenuHandler(textEditorTabPane, astGraphConverter, cfgGraphConverter, statusLabel, fileLabel, fileTreeView);
        fileMenuHandler.setOnFileOpenedCallback(() -> {
            handleCaretPositionChanged(true);
        });
        fileMenuHandler.setOnCaretPositionChangedCallback(() -> {
            handleCaretPositionChanged();
        });
        
        editMenuHandler = new EditMenuHandler(this::getActiveTextEditorView);
        viewMenuHandler = new ViewMenuHandler(graphVisualizationView, mainSplitPane, graphContainer);
        helpMenuHandler = new HelpMenuHandler();
        
        // 设置文件树的文件打开回调
        fileTreeView.setOnFileOpenCallback(fileNode -> {
            fileMenuHandler.openFile(fileNode.getFile());
        });
        
        textEditorTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            handleActiveTabChanged(newTab);
        });

        // 确保主内容区域和日志的垂直分割比例稳定，避免日志区域在初次渲染或拉伸时挤占编辑区
        SplitPane.setResizableWithParent(logContainer, false);

        // 窗口关闭时自动执行工程与工作区持久化保存
        Platform.runLater(() -> {
            Node node = (mainBorderPane != null) ? mainBorderPane : textEditorTabPane;
            if (node != null && node.getScene() != null && node.getScene().getWindow() != null) {
                node.getScene().getWindow().setOnCloseRequest(e -> {
                    ProjectWorkspaceManager.getInstance().save(fileMenuHandler.collectOpenedTabs(), fileMenuHandler.getActiveTab());
                });
            }
        });
    }
    
    /**
     * 打开启动参数中指定的文件或文件夹
     * @param files 启动时传入的文件/文件夹列表
     */
    public void openStartupFiles(List<File> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                // 重置反编译状态并切换工作区
                fileMenuHandler.resetLoadingState();
                ProjectWorkspaceManager.getInstance().openFolderWorkspace(file);
                fileTreeView.loadFolder(file);
                updateStatus("Folder opened: " + file.getName());

                // 自动无感恢复历史打开的标签页
                List<File> historyFiles = ProjectWorkspaceManager.getInstance().getHistoricalOpenedFiles();
                for (File historyFile : historyFiles) {
                    fileMenuHandler.openFile(historyFile);
                }
            } else if (file.isFile()) {
                fileMenuHandler.openFile(file);
            }
        }
    }

    /**
     * 处理打开文件夹事件
     * @param event 事件对象
     */
    @FXML
    private void handleOpenFolder(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open Folder");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        
        // 显示文件夹选择对话框
        File selectedFolder = directoryChooser.showDialog(new Stage());
        if (selectedFolder != null) {
            // 保存旧工作区
            ProjectWorkspaceManager.getInstance().save(fileMenuHandler.collectOpenedTabs(), fileMenuHandler.getActiveTab());
            // 重置反编译状态
            fileMenuHandler.resetLoadingState();
            // 加载新工作区数据库
            ProjectWorkspaceManager.getInstance().openFolderWorkspace(selectedFolder);
            // 加载文件夹内容到文件树
            fileTreeView.loadFolder(selectedFolder);
            updateStatus("Folder opened: " + selectedFolder.getName());

            // 自动无感恢复历史打开的标签页
            List<File> historyFiles = ProjectWorkspaceManager.getInstance().getHistoricalOpenedFiles();
            for (File historyFile : historyFiles) {
                fileMenuHandler.openFile(historyFile);
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
        ProjectWorkspaceManager.getInstance().save(fileMenuHandler.collectOpenedTabs(), fileMenuHandler.getActiveTab());
        Node node = (mainBorderPane != null) ? mainBorderPane : textEditorTabPane;
        if (node != null && node.getScene() != null && node.getScene().getWindow() != null) {
            Stage stage = (Stage) node.getScene().getWindow();
            stage.close();
        }
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
     * 处理字节码视图事件
     * @param event 事件对象
     */
    @FXML
    private void handleBytecodeView(ActionEvent event) {
        toggleSubView(SubViewType.BYTECODE);
    }
    
    /**
     * 处理AST视图事件
     * @param event 事件对象
     */
    @FXML
    private void handleAstView(ActionEvent event) {
        toggleSubView(SubViewType.AST);
    }
    
    /**
     * 处理CFG视图事件
     * @param event 事件对象
     */
    @FXML
    private void handleCfgView(ActionEvent event) {
        toggleSubView(SubViewType.CFG);
    }
    
    private void initSubViewHeader() {
        subViewHeaderBar = new javafx.scene.layout.HBox(8);
        subViewHeaderBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        subViewHeaderBar.setStyle("-fx-padding: 3 6 3 6; -fx-background-color: #f7f9fa; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        graphContainer.getChildren().remove(astGraphLabel);
        astGraphLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        subViewHeaderBar.getChildren().add(astGraphLabel);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, Priority.ALWAYS);
        subViewHeaderBar.getChildren().add(spacer);

        ToggleGroup scopeGroup = new ToggleGroup();
        btnScopeCurrentChunk = new ToggleButton("当前函数");
        btnScopeFullFile = new ToggleButton("完整文件");
        btnScopeCurrentChunk.setToggleGroup(scopeGroup);
        btnScopeFullFile.setToggleGroup(scopeGroup);
        btnScopeCurrentChunk.setSelected(true);

        String scopeBtnStyle = "-fx-font-size: 11px; -fx-padding: 1 6 1 6; -fx-cursor: hand;";
        btnScopeCurrentChunk.setStyle(scopeBtnStyle);
        btnScopeFullFile.setStyle(scopeBtnStyle);

        btnScopeCurrentChunk.setOnAction(e -> {
            bytecodeShowFullFile = false;
            updateSubViewTitle(SubViewType.BYTECODE);
            handleCaretPositionChanged(true);
        });
        btnScopeFullFile.setOnAction(e -> {
            bytecodeShowFullFile = true;
            updateSubViewTitle(SubViewType.BYTECODE);
            handleCaretPositionChanged(true);
        });

        // 默认隐藏范围切换按钮（仅在字节码视图模式下显示）
        btnScopeCurrentChunk.setVisible(false);
        btnScopeCurrentChunk.setManaged(false);
        btnScopeFullFile.setVisible(false);
        btnScopeFullFile.setManaged(false);

        subViewHeaderBar.getChildren().addAll(btnScopeCurrentChunk, btnScopeFullFile);
        graphContainer.getChildren().add(0, subViewHeaderBar);
    }

    /**
     * 切换当前激活标签页的代码/字节码视图 (快捷键 Shortcut+B)
     */
    @FXML
    private void handleToggleCurrentTabBytecode(ActionEvent event) {
        EditorTab activeTab = getActiveEditorTab();
        if (activeTab != null) {
            activeTab.toggleBytecodeView();
            updateStatus(activeTab.isShowingBytecode() ? "当前标签页已切换为字节码反汇编视图" : "当前标签页已切换为Lua源码视图");
        } else {
            updateStatus("无打开的文件标签页");
        }
    }

    /**
     * 切换子视图
     * @param subViewType 子视图类型
     */
    private void toggleSubView(SubViewType subViewType) {
        // 如果当前已经是该子视图，则关闭它
        if (currentSubViewType == subViewType) {
            closeSubView();
            return;
        }
        
        // 激活新的子视图
        currentSubViewType = subViewType;
        
        boolean isBytecode = (subViewType == SubViewType.BYTECODE);
        graphScrollPane.setVisible(!isBytecode);
        graphScrollPane.setManaged(!isBytecode);
        bytecodeSubViewEditor.getView().setVisible(isBytecode);
        bytecodeSubViewEditor.getView().setManaged(isBytecode);

        btnScopeCurrentChunk.setVisible(isBytecode);
        btnScopeCurrentChunk.setManaged(isBytecode);
        btnScopeFullFile.setVisible(isBytecode);
        btnScopeFullFile.setManaged(isBytecode);

        // 确保副视图容器可见
        if (!graphContainer.isVisible()) {
            if (!mainSplitPane.getItems().contains(graphContainer)) {
                mainSplitPane.getItems().add(graphContainer);
            }
            graphContainer.setVisible(true);
        }
        
        // 强制根据当前光标位置渲染对应的 Chunk
        handleCaretPositionChanged(true);
    }
    
    /**
     * 关闭当前子视图
     */
    private void closeSubView() {
        currentSubViewType = SubViewType.NONE;
        mainSplitPane.getItems().remove(graphContainer);
        graphContainer.setVisible(false);
        updateStatus("子视图已关闭");
    }
    
    /**
     * 更新子视图标题
     * @param subViewType 子视图类型
     */
    private void updateSubViewTitle(SubViewType subViewType) {
        String funcName = (currentDisplayedChunk != null) ? currentDisplayedChunk.getFunction() : "";
        String titleSuffix = (funcName == null || funcName.isEmpty()) ? "" : " - [" + funcName + "]";
        switch (subViewType) {
            case BYTECODE:
                if (bytecodeShowFullFile) {
                    astGraphLabel.setText("字节码视图 - [完整文件]");
                } else {
                    astGraphLabel.setText("字节码视图" + titleSuffix);
                }
                break;
            case AST:
                astGraphLabel.setText("AST视图" + titleSuffix);
                break;
            case CFG:
                astGraphLabel.setText("CFG视图" + titleSuffix);
                break;
            default:
                astGraphLabel.setText("子视图" + titleSuffix);
        }
    }
    
    /**
     * 执行子视图转换（针对特定代码块，在后台线程中）
     * @param subViewType 子视图类型
     * @param chunk 目标代码块
     */
    private void executeSubViewConversionForChunk(SubViewType subViewType, Chunk chunk) {
        if (chunk == null) {
            return;
        }
        LuacFile luacFile = fileMenuHandler.getCurrentLuacFile();
        if (luacFile == null) {
            return;
        }
        
        // 创建并启动后台任务
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("正在生成" + getSubViewName(subViewType) + "...");
                
                switch (subViewType) {
                    case BYTECODE:
                        updateMessage("正在生成字节码代码视图...");
                        String bytecodeText;
                        if (bytecodeShowFullFile) {
                            bytecodeText = BytecodeFormatter.formatLuacFile(luacFile);
                        } else {
                            bytecodeText = BytecodeFormatter.formatChunk(chunk, true);
                        }
                        Platform.runLater(() -> {
                            bytecodeSubViewEditor.setText(bytecodeText);
                        });
                        break;
                    case AST:
                        updateMessage("正在生成AST视图...");
                        Platform.runLater(() -> {
                            astGraphConverter.convertToGraph(chunk, fileMenuHandler.getCurrentDecompilerResult());
                        });
                        break;
                    case CFG:
                        updateMessage("正在生成CFG视图...");
                        javafx.application.Platform.runLater(() -> {
                            cfgGraphConverter.convertToGraph(chunk, fileMenuHandler.getCurrentDecompilerResult());
                        });
                        break;
                }
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                updateStatus(getSubViewName(subViewType) + "生成成功");
            }
            
            @Override
            protected void failed() {
                updateStatus("生成" + getSubViewName(subViewType) + "失败: " + getException().getMessage());
            }
        };
        
        task.messageProperty().addListener((obs, oldMsg, newMsg) -> updateStatus(newMsg));
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * 获取子视图名称
     * @param subViewType 子视图类型
     * @return 子视图名称
     */
    private String getSubViewName(SubViewType subViewType) {
        switch (subViewType) {
            case BYTECODE:
                return "字节码视图";
            case AST:
                return "AST视图";
            case CFG:
                return "CFG视图";
            default:
                return "子视图";
        }
    }
    
    // 帮助菜单事件处理方法
    
    @FXML
    private void handleAbout(ActionEvent event) {
        helpMenuHandler.handleAbout(event);
    }

    private void handleCaretPositionChanged() {
        handleCaretPositionChanged(false);
    }

    private void handleCaretPositionChanged(boolean force) {
        LuacFile luacFile = fileMenuHandler.getCurrentLuacFile();
        if (luacFile == null) {
            currentDisplayedChunk = null;
            lastLoadedLuacFile = null;
            return;
        }

        if (luacFile != lastLoadedLuacFile) {
            lastLoadedLuacFile = luacFile;
            currentDisplayedChunk = null;
            force = true;
        }

        TextEditorView activeEditor = getActiveTextEditorView();
        if (activeEditor == null) {
            return;
        }

        // 获取当前光标所在的行号（1-based）
        int currentLine = activeEditor.getCodeArea().getCurrentParagraph() + 1;

        // 根据行号寻找匹配的最深层 Chunk
        Chunk targetChunk = findTargetChunkForLine(luacFile.getMainChunk(), currentLine);

        if (targetChunk != null && (targetChunk != currentDisplayedChunk || force)) {
            currentDisplayedChunk = targetChunk;

            // 如果图形可视化视图处于显示状态，则执行更新
            if (graphContainer.isVisible()) {
                updateSubViewTitle(currentSubViewType);
                executeSubViewConversionForChunk(currentSubViewType, targetChunk);
            }
        }
    }

    private TextEditorView getActiveTextEditorView() {
        EditorTab activeTab = getActiveEditorTab();
        return activeTab != null ? activeTab.getActiveEditorView() : null;
    }

    private EditorTab getActiveEditorTab() {
        Tab selectedTab = textEditorTabPane.getSelectionModel().getSelectedItem();
        return (selectedTab instanceof EditorTab) ? (EditorTab) selectedTab : null;
    }

    private void handleActiveTabChanged(Tab newTab) {
        if (newTab instanceof EditorTab) {
            EditorTab activeTab = (EditorTab) newTab;
            // 更新文件名和状态栏
            updateFileLabel(activeTab.getFile().getName());
            updateStatus("Active file: " + activeTab.getFile().getName());

            // 在文件树中高亮选中
            if (fileTreeView != null) {
                fileTreeView.selectFile(activeTab.getFile());
            }

            // 同步子视图
            handleCaretPositionChanged(true);
        } else {
            // 没有打开的标签页
            updateFileLabel(I18nUtil.getString("status.noFileOpened"));
            updateStatus(I18nUtil.getString("status.ready"));

            // 关闭当前子视图
            if (graphContainer.isVisible()) {
                closeSubView();
            }
        }
    }

    private Chunk findTargetChunkForLine(Chunk mainChunk, int line) {
        java.util.Map<String, com.github.relua.model.LineRange> ranges = fileMenuHandler.getChunkLineRanges();
        if (ranges == null || ranges.isEmpty()) {
            return mainChunk;
        }

        String bestChunkName = "main";
        int minLength = Integer.MAX_VALUE;

        for (java.util.Map.Entry<String, com.github.relua.model.LineRange> entry : ranges.entrySet()) {
            com.github.relua.model.LineRange range = entry.getValue();
            if (line >= range.startLine && line <= range.endLine) {
                int length = range.endLine - range.startLine;
                if (length < minLength) {
                    minLength = length;
                    bestChunkName = entry.getKey();
                }
            }
        }

        Chunk targetChunk = findChunkByName(mainChunk, bestChunkName);
        return targetChunk != null ? targetChunk : mainChunk;
    }

    private Chunk findChunkByName(Chunk current, String targetName) {
        if (current == null) {
            return null;
        }
        if (targetName.equals(current.getFunction())) {
            return current;
        }
        for (Chunk sub : current.getSubChunks()) {
            Chunk found = findChunkByName(sub, targetName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 初始化界面控件的国际化文本
     */
    private void initializeI18n() {
        menuFile.setText(I18nUtil.getString("menu.file"));
        menuEdit.setText(I18nUtil.getString("menu.edit"));
        menuView.setText(I18nUtil.getString("menu.view"));
        menuHelp.setText(I18nUtil.getString("menu.help"));

        menuItemOpen.setText(I18nUtil.getString("menu.file.open"));
        menuItemOpenFolder.setText(I18nUtil.getString("menu.file.openFolder"));
        menuItemSave.setText(I18nUtil.getString("menu.file.save"));
        menuItemSaveAs.setText(I18nUtil.getString("menu.file.saveAs"));
        menuItemExit.setText(I18nUtil.getString("menu.file.exit"));

        menuItemUndo.setText(I18nUtil.getString("menu.edit.undo"));
        menuItemRedo.setText(I18nUtil.getString("menu.edit.redo"));
        menuItemCut.setText(I18nUtil.getString("menu.edit.cut"));
        menuItemCopy.setText(I18nUtil.getString("menu.edit.copy"));
        menuItemPaste.setText(I18nUtil.getString("menu.edit.paste"));

        menuItemZoomIn.setText(I18nUtil.getString("menu.view.zoomIn"));
        menuItemZoomOut.setText(I18nUtil.getString("menu.view.zoomOut"));
        menuItemResetZoom.setText(I18nUtil.getString("menu.view.resetZoom"));

        menuItemAbout.setText(I18nUtil.getString("menu.help.about"));

        btnOpen.setText(I18nUtil.getString("toolbar.open"));
        btnSave.setText(I18nUtil.getString("toolbar.save"));
        btnUndo.setText(I18nUtil.getString("toolbar.undo"));
        btnRedo.setText(I18nUtil.getString("toolbar.redo"));
        btnZoomIn.setText(I18nUtil.getString("toolbar.zoomIn"));
        btnZoomOut.setText(I18nUtil.getString("toolbar.zoomOut"));
        btnResetZoom.setText(I18nUtil.getString("toolbar.resetZoom"));

        astGraphLabel.setText(I18nUtil.getString("label.astGraph"));
        logLabel.setText(I18nUtil.getString("label.log"));
        btnClearLog.setText(I18nUtil.getString("btn.clearLog"));
    }
}
