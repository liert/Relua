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
import com.github.relua.gui.views.OutlineView;
import javafx.geometry.Orientation;
import com.github.relua.gui.utils.ASTGraphConverter;
import com.github.relua.gui.utils.CFGGraphConverter;
import com.github.relua.model.LuacFile;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Constant;
import com.github.relua.model.LocalVar;
import com.github.relua.util.BytecodeFormatter;
import com.github.relua.gui.theme.ThemeManager;
import com.github.relua.gui.theme.WindowsTitleBarUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeView;
import javafx.scene.control.IndexRange;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.application.Platform;
import javafx.concurrent.Task;
import com.github.relua.gui.dialogs.BatchExportDialog;
import com.github.relua.gui.dialogs.FindInFilesDialog;
import com.github.relua.gui.services.BatchExportService;
import com.github.relua.gui.utils.BytecodeSyncUtils;
import com.github.relua.gui.workspace.ProjectWorkspaceManager;
import com.github.relua.gui.workspace.RecentHistoryManager;

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
    private Label cursorPositionLabel;
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
    private Menu menuOpenRecent;
    @FXML
    private MenuItem menuItemBatchExport;
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
    @FXML
    private MenuItem menuItemFind;
    @FXML
    private MenuItem menuItemFindInFiles;
    
    // 视图菜单项
    @FXML
    private MenuItem menuItemSideBySide;
    @FXML
    private MenuItem menuItemToggleTabBytecode;
    @FXML
    private MenuItem menuItemToggleOutline;
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
    private Button btnOpenFolder;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnBatchExport;
    @FXML
    private Button btnFindInFiles;
    @FXML
    private Button btnSideBySide;
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
    @FXML
    private Button btnToggleTheme;
    @FXML
    private MenuItem menuItemDarkTheme;
    @FXML
    private MenuItem menuItemLightTheme;
    
    // 文件树相关
    @FXML
    private VBox fileTreeContainer;
    @FXML
    private ScrollPane fileTreeScrollPane;
    private FileTreeView fileTreeView;
    private OutlineView outlineView;
    private SplitPane sidebarSplitPane;
    
    // 资源管理器宽度范围约束常量与状态
    private static final double FILE_TREE_MIN_WIDTH = 180.0;
    private static final double FILE_TREE_DEFAULT_WIDTH = 260.0;
    private static final double FILE_TREE_MAX_WIDTH = 420.0;
    private boolean isAdjustingDivider = false;
    private double currentFileTreeWidth = FILE_TREE_DEFAULT_WIDTH;
    
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
    private boolean isSynchronizingTrace = false;
    
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
        // 初始化文件树与结构大纲视图
        fileTreeView = new FileTreeView();
        TreeView<?> treeNode = fileTreeView.getView();
        VBox.setVgrow(treeNode, Priority.ALWAYS);

        outlineView = new OutlineView();
        outlineView.setOnNavigate(targetLine -> {
            TextEditorView activeEditor = getActiveTextEditorView();
            if (activeEditor != null) {
                activeEditor.jumpToLine(targetLine);
                updateStatus("大纲导航：已跳转至第 " + targetLine + " 行");
            }
        });
        Node outlineNode = outlineView.getView();
        VBox.setVgrow(outlineNode, Priority.ALWAYS);

        // 创建左侧栏垂直分割容器：上方为资源管理器，下方为函数符号大纲
        VBox explorerBox = new VBox();
        VBox.setVgrow(explorerBox, Priority.ALWAYS);
        HBox explorerHeader = new HBox();
        explorerHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        explorerHeader.getStyleClass().add("sidebar-header");
        explorerHeader.getChildren().add(new Label("项目资源管理器 (EXPLORER)"));
        explorerBox.getChildren().addAll(explorerHeader, treeNode);

        sidebarSplitPane = new SplitPane();
        sidebarSplitPane.setOrientation(Orientation.VERTICAL);
        sidebarSplitPane.getItems().addAll(explorerBox, outlineNode);
        sidebarSplitPane.setDividerPositions(0.55);
        VBox.setVgrow(sidebarSplitPane, Priority.ALWAYS);

        if (fileTreeContainer != null) {
            fileTreeContainer.getChildren().clear();
            fileTreeContainer.getChildren().add(sidebarSplitPane);
        }
        
        // 初始化标签页视图
        textEditorTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        
        // 初始化图形可视化视图
        graphVisualizationView = new GraphVisualizationView();
        graphScrollPane.setContent(graphVisualizationView.getView());
        
        // 初始化AST到图形的转换器
        astGraphConverter = new ASTGraphConverter(graphVisualizationView);
        
        // 初始化CFG到图形的转换器
        cfgGraphConverter = new CFGGraphConverter(graphVisualizationView);
        
        // 初始化日志视图（采用与代码区域相同的高清 RichTextFX 渲染引擎，杜绝普通 TextArea 与 ScrollPane 的嵌套模糊）
        logView = new LogView();
        logView.setLogLevelComboBox(logLevelComboBox);
        logView.setClearLogButton(btnClearLog);
        Node logNode = logView.getView();
        VBox.setVgrow(logNode, Priority.ALWAYS);
        if (logContainer != null) {
            logContainer.getChildren().remove(logScrollPane);
            logContainer.getChildren().add(logNode);
        }
        
        // 设置ScrollPane的fitToWidth和fitToHeight属性，确保内容自动调整到适合ScrollPane的尺寸
        graphScrollPane.setFitToWidth(true);
        graphScrollPane.setFitToHeight(true);
        
        // 初始化字节码代码编辑器（作为子视图代码呈现，与主代码视图体验一致）
        bytecodeSubViewEditor = new TextEditorView(false);
        bytecodeSubViewEditor.getCodeArea().setEditable(false);

        // 注册字节码子视图光标联动：点击某指令行时，自动高亮定位到左侧主编辑器的 Lua 源码对应行
        bytecodeSubViewEditor.getCodeArea().caretPositionProperty().addListener((obs, oldVal, newVal) -> {
            if (isSynchronizingTrace || !graphContainer.isVisible() || currentSubViewType != SubViewType.BYTECODE) {
                return;
            }
            if (newVal == null) {
                return;
            }
            int paragraph = bytecodeSubViewEditor.getCodeArea().getCurrentParagraph();
            if (paragraph < 0 || paragraph >= bytecodeSubViewEditor.getCodeArea().getParagraphs().size()) {
                return;
            }
            String lineText = bytecodeSubViewEditor.getCodeArea().getParagraph(paragraph).getText();
            int sourceLine = BytecodeSyncUtils.extractSourceLineFromBytecode(lineText);
            if (sourceLine > 0) {
                TextEditorView activeEditor = getActiveTextEditorView();
                if (activeEditor != null) {
                    isSynchronizingTrace = true;
                    activeEditor.jumpToLine(sourceLine);
                    int pc = BytecodeSyncUtils.extractPcFromBytecode(lineText);
                    String pcSuffix = (pc >= 0) ? " (指令 PC " + pc + ")" : "";
                    updateStatus("双向联动追踪：已定位至 Lua 源码第 " + sourceLine + " 行" + pcSuffix);
                    Platform.runLater(() -> isSynchronizingTrace = false);
                }
            }
        });

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

        // 初始化资源管理器面板宽度范围约束与防全屏自动膨胀
        initFileTreePaneConstraints();
        
        // 设置初始状态
        updateStatus(I18nUtil.getString("status.ready"));
        updateFileLabel(I18nUtil.getString("status.noFileOpened"));

        // 初始化菜单处理器
        fileMenuHandler = new FileMenuHandler(textEditorTabPane, astGraphConverter, cfgGraphConverter, statusLabel, fileLabel, fileTreeView);
        fileMenuHandler.setOnFileOpenedCallback(() -> {
            handleCaretPositionChanged(true);
            refreshRecentMenu();
            if (outlineView != null) {
                outlineView.updateOutline(fileMenuHandler.getActiveTab());
            }
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
            refreshRecentMenu();
        });
        
        textEditorTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            handleActiveTabChanged(newTab);
        });

        // 确保主内容区域和日志的垂直分割比例稳定，避免日志区域在初次渲染或拉伸时挤占编辑区
        SplitPane.setResizableWithParent(logContainer, false);

        // 刷新最近打开记录菜单
        refreshRecentMenu();

        // 初始化外部文件与工程目录拖拽即开交互 (Drag & Drop)
        initDragAndDrop();

        // 初始状态栏光标指示
        updateCursorPositionStatus(null);

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
                openFolder(file);
            } else if (file.isFile()) {
                fileMenuHandler.openFile(file);
                refreshRecentMenu();
            }
        }
    }

    /**
     * 打开指定的工程文件夹
     * @param folder 目标文件夹
     */
    public void openFolder(File folder) {
        if (folder == null || !folder.exists()) {
            return;
        }
        // 保存旧工作区
        ProjectWorkspaceManager.getInstance().save(fileMenuHandler.collectOpenedTabs(), fileMenuHandler.getActiveTab());
        // 重置反编译状态
        fileMenuHandler.resetLoadingState();
        // 加载新工作区数据库
        ProjectWorkspaceManager.getInstance().openFolderWorkspace(folder);
        // 加载文件夹内容到文件树
        fileTreeView.loadFolder(folder);
        updateStatus("Folder opened: " + folder.getName());

        RecentHistoryManager.getInstance().addRecentPath(folder.getAbsolutePath());
        refreshRecentMenu();

        // 自动无感恢复历史打开的标签页
        List<File> historyFiles = ProjectWorkspaceManager.getInstance().getHistoricalOpenedFiles();
        for (File historyFile : historyFiles) {
            fileMenuHandler.openFile(historyFile);
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
            openFolder(selectedFolder);
        }
    }

    /**
     * 刷新“打开最近 (Open Recent)”历史菜单
     */
    private void refreshRecentMenu() {
        if (menuOpenRecent == null) {
            return;
        }
        menuOpenRecent.getItems().clear();
        List<String> paths = RecentHistoryManager.getInstance().getRecentPaths();
        if (paths.isEmpty()) {
            MenuItem emptyItem = new MenuItem("无最近打开记录");
            emptyItem.setDisable(true);
            menuOpenRecent.getItems().add(emptyItem);
            return;
        }

        for (String path : paths) {
            File f = new File(path);
            String label = f.getName();
            if (label.isEmpty()) {
                label = path;
            }
            if (!f.exists()) {
                label += " (已不存在)";
            }
            MenuItem item = new MenuItem(label + "  [" + path + "]");
            item.setOnAction(e -> {
                if (!f.exists()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    Window window = (mainBorderPane != null && mainBorderPane.getScene() != null) ? mainBorderPane.getScene().getWindow() : null;
                    alert.initOwner(window);
                    alert.setTitle("路径不存在");
                    alert.setHeaderText(null);
                    alert.setContentText("指定的路径已不存在: " + path + "\n已自动从历史列表中移除。");
                    alert.showAndWait();
                    RecentHistoryManager.getInstance().removeRecentPath(path);
                    refreshRecentMenu();
                    return;
                }
                if (f.isDirectory()) {
                    openFolder(f);
                } else if (f.isFile()) {
                    fileMenuHandler.openFile(f);
                    RecentHistoryManager.getInstance().addRecentPath(path);
                    refreshRecentMenu();
                }
            });
            menuOpenRecent.getItems().add(item);
        }

        menuOpenRecent.getItems().add(new SeparatorMenuItem());
        MenuItem clearItem = new MenuItem("清空最近记录 (Clear Recent)");
        clearItem.setOnAction(e -> {
            RecentHistoryManager.getInstance().clearRecentPaths();
            refreshRecentMenu();
        });
        menuOpenRecent.getItems().add(clearItem);
    }

    /**
     * 处理一键批量反编译导出工程
     */
    @FXML
    private void handleBatchExport(ActionEvent event) {
        Window window = (mainBorderPane != null && mainBorderPane.getScene() != null) ? mainBorderPane.getScene().getWindow() : null;
        File sourceRoot = ProjectWorkspaceManager.getInstance().getCurrentRootFolder();

        // 如果未打开文件夹工程，尝试使用当前打开的文件所在目录或弹窗提示选择
        if (sourceRoot == null || !sourceRoot.isDirectory()) {
            EditorTab activeTab = fileMenuHandler.getActiveTab();
            if (activeTab != null && activeTab.getFile() != null && activeTab.getFile().getParentFile() != null) {
                sourceRoot = activeTab.getFile().getParentFile();
            } else {
                DirectoryChooser srcChooser = new DirectoryChooser();
                srcChooser.setTitle("选择要批量反编译导出的源文件夹");
                srcChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                File selected = srcChooser.showDialog(window != null ? window : new Stage());
                if (selected == null) {
                    return;
                }
                sourceRoot = selected;
            }
        }

        // 扫描候选文件
        BatchExportService service = new BatchExportService();
        List<File> candidates = service.scanCandidateFiles(sourceRoot);
        if (candidates.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(window);
            alert.setTitle("未找到有效文件");
            alert.setHeaderText(null);
            alert.setContentText("在目录 " + sourceRoot.getAbsolutePath() + " 下未找到任何可反编译的 Lua 源码或二进制字节码文件。");
            alert.showAndWait();
            return;
        }

        // 提示选择导出目标根目录
        DirectoryChooser targetChooser = new DirectoryChooser();
        targetChooser.setTitle("选择工程导出保存目录");
        File initialDir = sourceRoot.getParentFile() != null ? sourceRoot.getParentFile() : new File(System.getProperty("user.home"));
        targetChooser.setInitialDirectory(initialDir);
        File targetRoot = targetChooser.showDialog(window != null ? window : new Stage());
        if (targetRoot == null) {
            return;
        }

        // 弹出批量导出进度对话框并开始执行
        BatchExportDialog dialog = new BatchExportDialog(window, sourceRoot, targetRoot, candidates);
        dialog.showAndWait();
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
    
    @FXML
    private void handleFind(ActionEvent event) {
        editMenuHandler.handleFind(event);
    }

    /**
     * 处理全局项目查找 (Find in Files... 快捷键 Shortcut+Shift+F)
     */
    @FXML
    private void handleFindInFiles(ActionEvent event) {
        Window window = (mainBorderPane != null && mainBorderPane.getScene() != null) ? mainBorderPane.getScene().getWindow() : null;

        // 获取工程根目录
        File rootFolder = (fileTreeView != null) ? fileTreeView.getRootFolder() : null;
        if (rootFolder == null) {
            rootFolder = ProjectWorkspaceManager.getInstance().getCurrentRootFolder();
        }

        // 收集所有打开标签页的实时内存文本 (优先检索最新编辑和反编译文本)
        java.util.Map<File, String> openTabTexts = new java.util.LinkedHashMap<>();
        for (Tab tab : textEditorTabPane.getTabs()) {
            if (tab instanceof EditorTab) {
                EditorTab et = (EditorTab) tab;
                if (et.getFile() != null) {
                    openTabTexts.put(et.getFile(), et.getLuaEditorView().getCodeArea().getText());
                }
            }
        }

        // 获取当前编辑器选中的文本作为初始检索词
        String initialQuery = null;
        TextEditorView activeEditor = getActiveTextEditorView();
        if (activeEditor != null && activeEditor.getCodeArea() != null) {
            String selected = activeEditor.getCodeArea().getSelectedText();
            if (selected != null && !selected.trim().isEmpty() && !selected.contains("\n")) {
                initialQuery = selected.trim();
            }
        }

        FindInFilesDialog dialog = new FindInFilesDialog(
                window,
                rootFolder,
                openTabTexts,
                initialQuery,
                (targetFile, line, startCol, endCol) -> {
                    openFileAndJump(targetFile, line, startCol, endCol);
                }
        );
        dialog.show();
    }

    /**
     * 打开目标文件并平滑跳转至指定的行号和列区间
     * @param targetFile 目标文件
     * @param line 目标行号 (1-based)
     * @param startCol 起始列偏移 (0-based)
     * @param endCol 结束列偏移 (0-based)
     */
    public void openFileAndJump(File targetFile, int line, int startCol, int endCol) {
        if (targetFile == null) {
            return;
        }
        fileMenuHandler.openFile(targetFile, openedTab -> {
            Platform.runLater(() -> {
                TextEditorView editor = openedTab.getActiveEditorView();
                if (editor != null) {
                    editor.jumpToLineAndColumn(line, startCol, endCol);
                    updateStatus("全局检索导航：已定位至 " + targetFile.getName() + " 第 " + line + " 行");
                }
            });
        });
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
        subViewHeaderBar.getStyleClass().add("editor-tab-header");

        graphContainer.getChildren().remove(astGraphLabel);
        astGraphLabel.getStyleClass().add("view-mode-hint");
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
     * 切换当前激活标签页的源码与字节码并排分屏对照模式 (快捷键 Shortcut+Alt+B)
     */
    @FXML
    private void handleSideBySideView(ActionEvent event) {
        EditorTab activeTab = getActiveEditorTab();
        if (activeTab != null) {
            if (activeTab.isSideBySide()) {
                activeTab.setViewMode(EditorTab.ViewMode.LUA_SOURCE);
                updateStatus("当前标签页已切回 Lua 源码视图");
            } else {
                activeTab.setViewMode(EditorTab.ViewMode.SIDE_BY_SIDE);
                updateStatus("当前标签页已切换为源码与字节码并排分屏对照模式 (Side-by-Side)");
            }
        } else {
            updateStatus("无打开的文件标签页");
        }
    }

    /**
     * 循环切换当前激活标签页的视图模式 (快捷键 Shortcut+B)
     */
    @FXML
    private void handleToggleCurrentTabBytecode(ActionEvent event) {
        EditorTab activeTab = getActiveEditorTab();
        if (activeTab != null) {
            activeTab.toggleBytecodeView();
            updateStatus("当前标签页视图已切换为: " + activeTab.getViewMode().getDisplayName());
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
            Platform.runLater(this::applyFileTreeDividerPosition);
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
        Platform.runLater(this::applyFileTreeDividerPosition);
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
        executeSubViewConversionForChunk(subViewType, chunk, -1);
    }

    /**
     * 执行子视图转换并支持指定行号的即时联动定位
     * @param subViewType 子视图类型
     * @param chunk 目标代码块
     * @param targetSourceLine 需同步定位的源码行号（<=0 表示不定位）
     */
    private void executeSubViewConversionForChunk(SubViewType subViewType, Chunk chunk, int targetSourceLine) {
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
                            if (targetSourceLine > 0) {
                                syncBytecodeSubViewToSourceLine(targetSourceLine);
                            }
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
     * 将右侧字节码子视图平滑滚动并高亮至指定的 Lua 源码行对应的指令
     * @param sourceLine 目标 Lua 源码行号（1-based）
     */
    private void syncBytecodeSubViewToSourceLine(int sourceLine) {
        if (bytecodeSubViewEditor == null || isSynchronizingTrace) {
            return;
        }
        if (!graphContainer.isVisible() || currentSubViewType != SubViewType.BYTECODE) {
            return;
        }
        String bytecodeText = bytecodeSubViewEditor.getCodeArea().getText();
        if (bytecodeText == null || bytecodeText.isEmpty()) {
            return;
        }
        int targetParagraph = BytecodeSyncUtils.findBytecodeParagraphForSourceLine(bytecodeText, sourceLine);
        if (targetParagraph >= 0 && targetParagraph < bytecodeSubViewEditor.getCodeArea().getParagraphs().size()) {
            isSynchronizingTrace = true;
            try {
                bytecodeSubViewEditor.jumpToLine(targetParagraph + 1);
            } finally {
                Platform.runLater(() -> isSynchronizingTrace = false);
            }
        }
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
        TextEditorView activeEditor = getActiveTextEditorView();
        updateCursorPositionStatus(activeEditor);

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

        if (activeEditor == null) {
            return;
        }

        // 获取当前光标所在的行号（1-based）
        int currentLine = activeEditor.getCodeArea().getCurrentParagraph() + 1;
        if (outlineView != null) {
            outlineView.highlightEnclosingFunction(currentLine);
        }

        // 根据行号寻找匹配的最深层 Chunk
        Chunk targetChunk = findTargetChunkForLine(luacFile.getMainChunk(), currentLine);

        if (targetChunk != null && (targetChunk != currentDisplayedChunk || force)) {
            currentDisplayedChunk = targetChunk;

            // 如果图形可视化视图处于显示状态，则执行更新
            if (graphContainer.isVisible()) {
                updateSubViewTitle(currentSubViewType);
                executeSubViewConversionForChunk(currentSubViewType, targetChunk, currentLine);
            }
        } else if (graphContainer.isVisible() && currentSubViewType == SubViewType.BYTECODE) {
            // 同一个 Chunk 内部换行，即时同步高亮对应的字节码指令
            syncBytecodeSubViewToSourceLine(currentLine);
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

            // 更新左侧结构大纲
            if (outlineView != null) {
                outlineView.updateOutline(activeTab);
            }

            // 在文件树中高亮选中
            if (fileTreeView != null) {
                fileTreeView.selectFile(activeTab.getFile());
            }

            // 同步子视图与光标指示
            handleCaretPositionChanged(true);
        } else {
            // 没有打开的标签页
            updateFileLabel(I18nUtil.getString("status.noFileOpened"));
            updateStatus(I18nUtil.getString("status.ready"));
            updateCursorPositionStatus(null);

            if (outlineView != null) {
                outlineView.updateOutline(null);
            }

            // 关闭当前子视图
            if (graphContainer.isVisible()) {
                closeSubView();
            }
        }
    }

    /**
     * 切换显示或隐藏左侧结构大纲面板 (快捷键 Shortcut+Alt+O)
     */
    @FXML
    private void handleToggleOutline(ActionEvent event) {
        if (sidebarSplitPane == null || outlineView == null) {
            return;
        }
        Node outlineNode = outlineView.getView();
        if (sidebarSplitPane.getItems().contains(outlineNode)) {
            sidebarSplitPane.getItems().remove(outlineNode);
            updateStatus("已隐藏结构大纲面板");
        } else {
            sidebarSplitPane.getItems().add(outlineNode);
            sidebarSplitPane.setDividerPositions(0.55);
            updateStatus("已显示结构大纲面板");
        }
    }

    /**
     * 更新底部状态栏光标行列与选区信息
     * @param activeEditor 当前活跃的代码/字节码编辑器视图
     */
    private void updateCursorPositionStatus(TextEditorView activeEditor) {
        if (cursorPositionLabel == null) {
            return;
        }
        if (activeEditor == null || activeEditor.getCodeArea() == null) {
            cursorPositionLabel.setText("Ln 1, Col 1");
            return;
        }
        int paragraph = activeEditor.getCodeArea().getCurrentParagraph() + 1;
        int column = activeEditor.getCodeArea().getCaretColumn() + 1;
        IndexRange selection = activeEditor.getCodeArea().getSelection();
        int selLength = (selection != null) ? selection.getLength() : 0;
        if (selLength > 0) {
            cursorPositionLabel.setText(String.format("Ln %d, Col %d (%d 已选)", paragraph, column, selLength));
        } else {
            cursorPositionLabel.setText(String.format("Ln %d, Col %d", paragraph, column));
        }
    }

    /**
     * 初始化外部文件与工程目录拖拽即开交互 (Drag & Drop)
     */
    private void initDragAndDrop() {
        if (mainBorderPane == null) {
            return;
        }

        mainBorderPane.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        mainBorderPane.setOnDragEntered(event -> {
            if (event.getDragboard().hasFiles()) {
                if (!mainBorderPane.getStyleClass().contains("drag-over")) {
                    mainBorderPane.getStyleClass().add("drag-over");
                }
            }
            event.consume();
        });

        mainBorderPane.setOnDragExited(event -> {
            mainBorderPane.getStyleClass().remove("drag-over");
            event.consume();
        });

        mainBorderPane.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                List<File> files = db.getFiles();
                if (files != null && !files.isEmpty()) {
                    File firstFile = files.get(0);
                    if (firstFile.isDirectory()) {
                        openFolder(firstFile);
                        success = true;
                    } else {
                        for (File f : files) {
                            if (f.isFile()) {
                                fileMenuHandler.openFile(f);
                                RecentHistoryManager.getInstance().addRecentPath(f.getAbsolutePath());
                                success = true;
                            }
                        }
                        if (success) {
                            refreshRecentMenu();
                        }
                    }
                }
            }
            mainBorderPane.getStyleClass().remove("drag-over");
            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * 初始化资源管理器面板宽度范围约束与防全屏自动膨胀
     */
    private void initFileTreePaneConstraints() {
        if (fileTreeContainer == null || mainSplitPane == null) {
            return;
        }

        // 设置不可随父容器按比例拉伸（全屏或拉大窗口时，多余空间全部分配给代码编辑区）
        SplitPane.setResizableWithParent(fileTreeContainer, false);
        fileTreeContainer.setMinWidth(FILE_TREE_MIN_WIDTH);
        fileTreeContainer.setPrefWidth(FILE_TREE_DEFAULT_WIDTH);
        fileTreeContainer.setMaxWidth(FILE_TREE_MAX_WIDTH);

        // 监听实际宽度变化，记录用户手动调整后的有效宽度
        fileTreeContainer.widthProperty().addListener((obs, oldW, newW) -> {
            if (newW != null && newW.doubleValue() >= FILE_TREE_MIN_WIDTH && newW.doubleValue() <= FILE_TREE_MAX_WIDTH) {
                currentFileTreeWidth = newW.doubleValue();
            }
        });

        // 监听 mainSplitPane 宽度变化（窗口拉大、最大化全屏等），保持左侧栏像素宽度稳定
        mainSplitPane.widthProperty().addListener((obs, oldW, newW) -> {
            applyFileTreeDividerPosition();
        });

        // 延迟至初次布局完成后初始化分割线位置，并注册用户手动拖拽范围约束监听器
        Platform.runLater(() -> {
            if (!mainSplitPane.getDividers().isEmpty()) {
                mainSplitPane.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> {
                    if (isAdjustingDivider || newVal == null) {
                        return;
                    }
                    double totalWidth = mainSplitPane.getWidth();
                    if (totalWidth <= 0) {
                        return;
                    }
                    double targetPx = newVal.doubleValue() * totalWidth;
                    if (targetPx < FILE_TREE_MIN_WIDTH) {
                        isAdjustingDivider = true;
                        mainSplitPane.setDividerPosition(0, FILE_TREE_MIN_WIDTH / totalWidth);
                        currentFileTreeWidth = FILE_TREE_MIN_WIDTH;
                        isAdjustingDivider = false;
                    } else if (targetPx > FILE_TREE_MAX_WIDTH) {
                        isAdjustingDivider = true;
                        mainSplitPane.setDividerPosition(0, FILE_TREE_MAX_WIDTH / totalWidth);
                        currentFileTreeWidth = FILE_TREE_MAX_WIDTH;
                        isAdjustingDivider = false;
                    } else {
                        currentFileTreeWidth = targetPx;
                    }
                });
            }
            applyFileTreeDividerPosition();
        });
    }

    /**
     * 将资源管理器分割线位置锚定在 [FILE_TREE_MIN_WIDTH, FILE_TREE_MAX_WIDTH] 的绝对像素范围
     */
    private void applyFileTreeDividerPosition() {
        if (isAdjustingDivider || mainSplitPane == null || mainSplitPane.getDividers().isEmpty()) {
            return;
        }
        double totalWidth = mainSplitPane.getWidth();
        if (totalWidth <= 0) {
            return;
        }
        double targetWidth = Math.max(FILE_TREE_MIN_WIDTH, Math.min(FILE_TREE_MAX_WIDTH, currentFileTreeWidth));
        double targetPos = targetWidth / totalWidth;
        isAdjustingDivider = true;
        mainSplitPane.setDividerPosition(0, targetPos);
        isAdjustingDivider = false;
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

        btnOpen.setText("📂 " + I18nUtil.getString("toolbar.open"));
        btnOpen.setTooltip(new Tooltip("打开单个 Lua 字节码或源码文件 (Ctrl+O)"));

        if (btnOpenFolder != null) {
            btnOpenFolder.setText("📁 打开工程");
            btnOpenFolder.setTooltip(new Tooltip("打开并分析整个工程文件夹"));
        }

        btnSave.setText("💾 " + I18nUtil.getString("toolbar.save"));
        btnSave.setTooltip(new Tooltip("保存当前反编译或分析文本 (Ctrl+S)"));

        if (btnBatchExport != null) {
            btnBatchExport.setText("📦 批量导出");
            btnBatchExport.setTooltip(new Tooltip("批量反编译导出整个工程项目 (Ctrl+Shift+E)"));
        }

        if (btnFindInFiles != null) {
            btnFindInFiles.setText("🔍 全局查找");
            btnFindInFiles.setTooltip(new Tooltip("在当前工程与所有打开的文件中全文检索 (Ctrl+Shift+F)"));
        }

        if (btnSideBySide != null) {
            btnSideBySide.setText("◫ 并排分屏");
            btnSideBySide.setTooltip(new Tooltip("开启或关闭源码与字节码并排分屏对照模式 (Ctrl+Alt+B)"));
        }

        btnToggleBytecode.setText("⇄ 切换视图");
        btnToggleBytecode.setTooltip(new Tooltip("循环切换 Lua 源码、分屏对照与字节码反汇编视图 (Ctrl+B)"));

        btnUndo.setText("↶ " + I18nUtil.getString("toolbar.undo"));
        btnUndo.setTooltip(new Tooltip("撤销操作 (Ctrl+Z)"));

        btnRedo.setText("↷ " + I18nUtil.getString("toolbar.redo"));
        btnRedo.setTooltip(new Tooltip("重做操作 (Ctrl+Y)"));

        btnZoomIn.setText("🔍+ " + I18nUtil.getString("toolbar.zoomIn"));
        btnZoomIn.setTooltip(new Tooltip("放大可视化分析图"));

        btnZoomOut.setText("🔍- " + I18nUtil.getString("toolbar.zoomOut"));
        btnZoomOut.setTooltip(new Tooltip("缩小可视化分析图"));

        btnResetZoom.setText("⟲ " + I18nUtil.getString("toolbar.resetZoom"));
        btnResetZoom.setTooltip(new Tooltip("重置可视化视图缩放至 100%"));

        if (btnToggleTheme != null) {
            btnToggleTheme.setText("🌓 切换主题");
            btnToggleTheme.setTooltip(new Tooltip("在现代暗色与现代浅色主题之间快速切换"));
        }

        astGraphLabel.setText(I18nUtil.getString("label.astGraph"));
        logLabel.setText(I18nUtil.getString("label.log"));
        btnClearLog.setText(I18nUtil.getString("btn.clearLog"));
        btnClearLog.setTooltip(new Tooltip("清空运行控制台日志输出"));
    }

    /**
     * 切换到现代暗色主题
     */
    @FXML
    private void handleDarkTheme(ActionEvent event) {
        Scene scene = (mainBorderPane != null) ? mainBorderPane.getScene() : null;
        ThemeManager.getInstance().setTheme(ThemeManager.Theme.DARK, scene);
        updateStageTitleBarTheme(scene, true);
    }

    /**
     * 切换到现代浅色主题
     */
    @FXML
    private void handleLightTheme(ActionEvent event) {
        Scene scene = (mainBorderPane != null) ? mainBorderPane.getScene() : null;
        ThemeManager.getInstance().setTheme(ThemeManager.Theme.LIGHT, scene);
        updateStageTitleBarTheme(scene, false);
    }

    /**
     * 快速循环切换主题
     */
    @FXML
    private void handleToggleTheme(ActionEvent event) {
        Scene scene = (mainBorderPane != null) ? mainBorderPane.getScene() : null;
        boolean nextDark = !ThemeManager.getInstance().isDark();
        ThemeManager.Theme next = nextDark ? ThemeManager.Theme.DARK : ThemeManager.Theme.LIGHT;
        ThemeManager.getInstance().setTheme(next, scene);
        updateStageTitleBarTheme(scene, nextDark);
    }

    private void updateStageTitleBarTheme(Scene scene, boolean isDark) {
        if (scene != null && scene.getWindow() instanceof Stage) {
            WindowsTitleBarUtil.applyTitleBarTheme((Stage) scene.getWindow(), isDark);
        }
    }
}
