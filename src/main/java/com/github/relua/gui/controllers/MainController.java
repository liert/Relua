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
import com.github.relua.gui.views.HexView;
import com.github.relua.gui.navigation.Bookmark;
import com.github.relua.gui.navigation.BookmarkManager;
import com.github.relua.gui.navigation.NavigationHistoryManager;
import com.github.relua.gui.navigation.NavigationLocation;
import com.github.relua.gui.utils.ASTGraphConverter;
import com.github.relua.gui.utils.CFGGraphConverter;
import com.github.relua.model.LuacFile;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Constant;
import com.github.relua.model.LocalVar;
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
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.CheckMenuItem;
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
import com.github.relua.gui.dialogs.PreferencesDialog;
import com.github.relua.gui.services.BatchExportService;
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
    private MenuItem menuItemExportSource;
    @FXML
    private MenuItem menuItemSaveAnalysis;
    @FXML
    private MenuItem menuItemPreferences;
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

    // 导航与书签菜单项
    @FXML
    private Menu menuNavigate;
    @FXML
    private MenuItem menuItemNavigateBack;
    @FXML
    private MenuItem menuItemNavigateForward;
    @FXML
    private MenuItem menuItemToggleBookmark;
    @FXML
    private MenuItem menuItemNextBookmark;
    @FXML
    private MenuItem menuItemPreviousBookmark;
    @FXML
    private MenuItem menuItemClearBookmarks;
    
    // 视图菜单项
    @FXML
    private RadioMenuItem menuItemCodeView;
    @FXML
    private CheckMenuItem menuItemSplitView;
    @FXML
    private RadioMenuItem menuItemBytecodeView;
    @FXML
    private CheckMenuItem menuItemHexView;
    @FXML
    private CheckMenuItem menuItemAstView;
    @FXML
    private CheckMenuItem menuItemCfgView;
    @FXML
    private Menu menuAnalysisPanel;
    @FXML
    private Menu menuGraphZoom;
    @FXML
    private MenuItem menuItemCloseAnalysisPanel;
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
    private Button btnExportSource;
    @FXML
    private Button btnSaveAnalysis;
    @FXML
    private Button btnBatchExport;
    @FXML
    private Button btnFindInFiles;
    @FXML
    private Button btnSideBySide;
    @FXML
    private Button btnToggleBytecode;
    @FXML
    private Button btnNavBack;
    @FXML
    private Button btnNavForward;
    @FXML
    private Button btnToggleBookmark;
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
    
    // 文件树相关
    @FXML
    private VBox fileTreeContainer;
    @FXML
    private ScrollPane fileTreeScrollPane;
    private FileTreeView fileTreeView;
    
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
    private HexView hexView;
    private StackPane subViewStackPane;
    
    // AST到图形的转换器
    private ASTGraphConverter astGraphConverter;
    
    // CFG到图形的转换器
    private CFGGraphConverter cfgGraphConverter;
    
    // 子视图类型枚举
    private enum SubViewType {
        NONE, AST, CFG, HEX
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
        // 文件内函数直接显示在资源树中。
        fileTreeView = new FileTreeView();
        TreeView<?> treeNode = fileTreeView.getView();
        VBox.setVgrow(treeNode, Priority.ALWAYS);

        fileTreeScrollPane.setContent(treeNode);
        ThemeManager.getInstance().addListener(theme -> {
            Runnable update = () -> {
                if (mainBorderPane.getScene() != null) ThemeManager.getInstance().applyTheme(mainBorderPane.getScene());
            };
            if (Platform.isFxApplicationThread()) update.run();
            else Platform.runLater(update);
        });
        
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
        
        // 初始化十六进制 Hex 视图组件
        hexView = new HexView();
        Node hexNode = hexView.getView();
        VBox.setVgrow(hexNode, Priority.ALWAYS);
        hexNode.setVisible(false);
        hexNode.setManaged(false);

        // 初始化子视图内容堆叠容器
        subViewStackPane = new StackPane();
        VBox.setVgrow(subViewStackPane, Priority.ALWAYS);

        // 图形和十六进制共用分析面板；字节码统一由编辑器显示。
        graphContainer.getChildren().remove(graphScrollPane);
        subViewStackPane.getChildren().addAll(graphScrollPane, hexNode);

        // 初始化分析面板标题栏
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
            EditorTab tab = fileMenuHandler.getActiveTab();
            if (tab != null) {
                wireTabNavigationCallbacks(tab);
            }
            handleCaretPositionChanged(true);
            refreshRecentMenu();
        });
        fileMenuHandler.setOnCaretPositionChangedCallback(() -> {
            handleCaretPositionChanged();
        });
        
        editMenuHandler = new EditMenuHandler(this::getActiveTextEditorView);
        viewMenuHandler = new ViewMenuHandler(graphVisualizationView, mainSplitPane, graphContainer);
        helpMenuHandler = new HelpMenuHandler();
        menuView.setOnShowing(event -> refreshViewMenuState());
        refreshViewMenuState();
        
        // 设置文件树的文件打开回调
        fileTreeView.setOnFileOpenCallback(fileNode -> {
            fileMenuHandler.openFile(fileNode.getFile());
            refreshRecentMenu();
        });
        fileTreeView.setOnFunctionNavigate((file, function) -> {
            fileMenuHandler.openFile(file, tab -> {
                // 函数行号对应 Lua 源码，即使原来显示 Bytecode 也定位到 Code。
                if (tab.getPrimaryViewMode() != EditorTab.ViewMode.LUA_SOURCE) {
                    tab.setPrimaryViewMode(EditorTab.ViewMode.LUA_SOURCE);
                }
                Platform.runLater(() -> {
                    TextEditorView editor = tab.getLuaEditorView();
                    editor.jumpToLine(function.getStartLine());
                    editor.getCodeArea().requestFocus();
                    updateStatus("函数导航：" + function.getName() + "，第 " + function.getStartLine() + " 行");
                });
            });
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

        // 设置导航工具栏按钮提示
        if (btnNavBack != null) btnNavBack.setTooltip(new Tooltip("向后导航 (Alt+Left)"));
        if (btnNavForward != null) btnNavForward.setTooltip(new Tooltip("向前导航 (Alt+Right)"));
        if (btnToggleBookmark != null) btnToggleBookmark.setTooltip(new Tooltip("切换当前行书签 (Ctrl+F11)"));

        // 仅持久化 .relua 分析数据，原始源码和字节码保持只读。
        Platform.runLater(() -> {
            Node node = (mainBorderPane != null) ? mainBorderPane : textEditorTabPane;
            if (node != null && node.getScene() != null && node.getScene().getWindow() != null) {
                node.getScene().getWindow().setOnCloseRequest(e -> {
                    fileMenuHandler.saveAnalysis();
                    fileMenuHandler.resetLoadingState();
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
        fileMenuHandler.saveAnalysis();
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
        fileMenuHandler.restoreWorkspaceTabs();
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

        // 用户主动导出时只在内存中汇总本次会话的分析内容。
        for (EditorTab tab : fileMenuHandler.collectOpenedTabs()) {
            if (tab.isFileLoaded() && tab.getFile() != null) {
                ProjectWorkspaceManager.getInstance().recordFileModification(tab.getFile(),
                        tab.getRenamedVariables(), tab.getLuaEditorView().getText(), tab.getLineComments());
            }
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
    private void handleExportSource(ActionEvent event) {
        fileMenuHandler.handleExportSource(event);
    }

    @FXML
    private void handleSaveAnalysis(ActionEvent event) {
        fileMenuHandler.handleSaveAnalysis(event);
    }

    @FXML
    private void handlePreferences(ActionEvent event) {
        Window owner = mainBorderPane.getScene() != null ? mainBorderPane.getScene().getWindow() : null;
        new PreferencesDialog(owner).showAndWait();
    }
    
    @FXML
    private void handleExit(ActionEvent event) {
        fileMenuHandler.saveAnalysis();
        fileMenuHandler.resetLoadingState();
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
                wireTabNavigationCallbacks(openedTab);
                TextEditorView editor = openedTab.getActiveEditorView();
                if (editor != null) {
                    editor.jumpToLineAndColumn(line, startCol, endCol);
                    updateStatus("代码导航：已定位至 " + targetFile.getName() + " 第 " + line + " 行");
                }
            });
        });
    }

    // 导航与书签事件处理方法

    private void wireTabNavigationCallbacks(EditorTab tab) {
        if (tab == null) return;
        tab.setEditorConfigurator(editor -> {
            editor.setOnNavigateHistoryRequested(isBack -> {
                if (isBack) handleNavigateBack(null);
                else handleNavigateForward(null);
            });
            editor.setOnNavigateRequested((f, line) -> openFileAndJump(f, line, 0, 0));
        });
    }

    @FXML
    private void handleNavigateBack(ActionEvent event) {
        TextEditorView activeEditor = getActiveTextEditorView();
        EditorTab activeTab = getActiveEditorTab();
        NavigationLocation currentLoc = null;
        if (activeEditor != null && activeTab != null && activeTab.getFile() != null) {
            currentLoc = new NavigationLocation(
                    activeTab.getFile(),
                    activeEditor.getCodeArea().getCurrentParagraph() + 1,
                    activeEditor.getCodeArea().getCaretColumn()
            );
        }
        NavigationLocation target = NavigationHistoryManager.getInstance().navigateBack(currentLoc);
        if (target != null) {
            navigateToLocation(target);
        } else {
            updateStatus("已到达导航历史最起点");
        }
    }

    @FXML
    private void handleNavigateForward(ActionEvent event) {
        TextEditorView activeEditor = getActiveTextEditorView();
        EditorTab activeTab = getActiveEditorTab();
        NavigationLocation currentLoc = null;
        if (activeEditor != null && activeTab != null && activeTab.getFile() != null) {
            currentLoc = new NavigationLocation(
                    activeTab.getFile(),
                    activeEditor.getCodeArea().getCurrentParagraph() + 1,
                    activeEditor.getCodeArea().getCaretColumn()
            );
        }
        NavigationLocation target = NavigationHistoryManager.getInstance().navigateForward(currentLoc);
        if (target != null) {
            navigateToLocation(target);
        } else {
            updateStatus("已到达导航历史最新点");
        }
    }

    private void navigateToLocation(NavigationLocation target) {
        if (target == null || target.getFile() == null) {
            return;
        }
        openFileAndJump(target.getFile(), target.getLine(), target.getColumn(), target.getColumn());
        updateStatus("向后/向前导航：已跳转至 " + target.getFile().getName() + " 第 " + target.getLine() + " 行");
    }

    @FXML
    private void handleToggleBookmark(ActionEvent event) {
        TextEditorView activeEditor = getActiveTextEditorView();
        EditorTab activeTab = getActiveEditorTab();
        if (activeEditor == null || activeTab == null || activeTab.getFile() == null) {
            updateStatus("无打开的编辑文件");
            return;
        }
        int line = activeEditor.getCodeArea().getCurrentParagraph() + 1;
        String lineText = activeEditor.getCodeArea().getParagraph(line - 1).getText();
        boolean added = BookmarkManager.getInstance().toggleBookmark(activeTab.getFile(), line, lineText);
        updateStatus(added ? "已在第 " + line + " 行添加书签 (🔖)" : "已移除第 " + line + " 行书签");
    }

    @FXML
    private void handleNextBookmark(ActionEvent event) {
        TextEditorView activeEditor = getActiveTextEditorView();
        EditorTab activeTab = getActiveEditorTab();
        File currentFile = activeTab != null ? activeTab.getFile() : null;
        int currentLine = activeEditor != null ? activeEditor.getCodeArea().getCurrentParagraph() + 1 : 1;
        Bookmark next = BookmarkManager.getInstance().getNextBookmark(currentFile, currentLine);
        if (next != null) {
            openFileAndJump(next.getFile(), next.getLine(), 0, 0);
            updateStatus("跳转至下一个书签: " + next.getFile().getName() + " 第 " + next.getLine() + " 行");
        } else {
            updateStatus("当前没有设置任何书签");
        }
    }

    @FXML
    private void handlePreviousBookmark(ActionEvent event) {
        TextEditorView activeEditor = getActiveTextEditorView();
        EditorTab activeTab = getActiveEditorTab();
        File currentFile = activeTab != null ? activeTab.getFile() : null;
        int currentLine = activeEditor != null ? activeEditor.getCodeArea().getCurrentParagraph() + 1 : 1;
        Bookmark prev = BookmarkManager.getInstance().getPreviousBookmark(currentFile, currentLine);
        if (prev != null) {
            openFileAndJump(prev.getFile(), prev.getLine(), 0, 0);
            updateStatus("跳转至上一个书签: " + prev.getFile().getName() + " 第 " + prev.getLine() + " 行");
        } else {
            updateStatus("当前没有设置任何书签");
        }
    }

    @FXML
    private void handleClearAllBookmarks(ActionEvent event) {
        BookmarkManager.getInstance().clearAll();
        updateStatus("已清除所有书签");
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
    private void handleCodeView(ActionEvent event) {
        showEditorView(EditorTab.ViewMode.LUA_SOURCE);
    }

    @FXML
    private void handleBytecodeView(ActionEvent event) {
        showEditorView(EditorTab.ViewMode.BYTECODE);
    }

    private void showEditorView(EditorTab.ViewMode mode) {
        EditorTab tab = getActiveEditorTab();
        if (tab != null) {
            tab.setPrimaryViewMode(mode);
            updateStatus("当前标签页已显示 " + mode.getDisplayName());
        }
        refreshViewMenuState();
    }

    @FXML
    private void handleCloseAnalysisPanel(ActionEvent event) {
        closeSubView();
    }

    private void refreshViewMenuState() {
        EditorTab tab = getActiveEditorTab();
        boolean ready = tab != null && tab.isFileLoaded();
        menuItemCodeView.setDisable(!ready);
        menuItemBytecodeView.setDisable(!ready);
        menuItemSplitView.setDisable(!ready);
        menuItemCodeView.setSelected(tab != null && tab.getPrimaryViewMode() == EditorTab.ViewMode.LUA_SOURCE);
        menuItemBytecodeView.setSelected(tab != null && tab.getPrimaryViewMode() == EditorTab.ViewMode.BYTECODE);
        menuItemSplitView.setSelected(tab != null && tab.isSideBySide());
        menuAnalysisPanel.setDisable(!ready);
        boolean bytecode = ready && tab.getLuacFile() != null;
        menuItemAstView.setDisable(!bytecode);
        menuItemCfgView.setDisable(!bytecode);
        menuItemHexView.setSelected(currentSubViewType == SubViewType.HEX);
        menuItemAstView.setSelected(currentSubViewType == SubViewType.AST);
        menuItemCfgView.setSelected(currentSubViewType == SubViewType.CFG);
        menuItemCloseAnalysisPanel.setDisable(currentSubViewType == SubViewType.NONE);
        boolean graph = currentSubViewType == SubViewType.AST || currentSubViewType == SubViewType.CFG;
        menuGraphZoom.setDisable(!graph || !bytecode);
        btnZoomIn.setDisable(!graph || !bytecode);
        btnZoomOut.setDisable(!graph || !bytecode);
        btnResetZoom.setDisable(!graph || !bytecode);
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

    /**
     * 处理十六进制Hex视图事件
     * @param event 事件对象
     */
    @FXML
    private void handleHexView(ActionEvent event) {
        toggleSubView(SubViewType.HEX);
    }
    
    private void initSubViewHeader() {
        HBox subViewHeaderBar = new HBox(8);
        subViewHeaderBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        subViewHeaderBar.getStyleClass().add("editor-tab-header");

        graphContainer.getChildren().remove(astGraphLabel);
        astGraphLabel.getStyleClass().add("view-mode-hint");
        subViewHeaderBar.getChildren().add(astGraphLabel);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, Priority.ALWAYS);
        subViewHeaderBar.getChildren().add(spacer);

        graphContainer.getChildren().add(0, subViewHeaderBar);
    }

    /**
     * 切换当前激活标签页的源码与字节码并排分屏对照模式 (快捷键 Shortcut+Alt+B)
     */
    @FXML
    private void handleSideBySideView(ActionEvent event) {
        EditorTab activeTab = getActiveEditorTab();
        if (activeTab != null) {
            activeTab.setSplitView(!activeTab.isSideBySide());
            updateStatus(activeTab.isSideBySide() ? "当前标签页已开启 Split view"
                    : "当前标签页已关闭 Split view，保留 " + activeTab.getPrimaryViewMode().getDisplayName() + " 视图");
        } else {
            updateStatus("无打开的文件标签页");
        }
        refreshViewMenuState();
    }

    /**
     * 工具栏切换主面板的 Code/Bytecode，保留分屏状态。
     */
    @FXML
    private void handleToggleCurrentTabBytecode(ActionEvent event) {
        EditorTab activeTab = getActiveEditorTab();
        if (activeTab != null) {
            showEditorView(activeTab.getPrimaryViewMode() == EditorTab.ViewMode.BYTECODE
                    ? EditorTab.ViewMode.LUA_SOURCE : EditorTab.ViewMode.BYTECODE);
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
        
        boolean isHex = (subViewType == SubViewType.HEX);
        boolean isGraph = (subViewType == SubViewType.AST || subViewType == SubViewType.CFG);

        graphScrollPane.setVisible(isGraph);
        graphScrollPane.setManaged(isGraph);
        hexView.getView().setVisible(isHex);
        hexView.getView().setManaged(isHex);


        // 确保副视图容器可见
        if (!graphContainer.isVisible()) {
            if (!mainSplitPane.getItems().contains(graphContainer)) {
                mainSplitPane.getItems().add(graphContainer);
            }
            graphContainer.setVisible(true);
            Platform.runLater(this::applyFileTreeDividerPosition);
        }
        
        if (subViewType == SubViewType.HEX) {
            updateSubViewTitle(SubViewType.HEX);
            EditorTab activeTab = getActiveEditorTab();
            if (activeTab != null && activeTab.getFile() != null) {
                hexView.loadFile(activeTab.getFile());
            } else {
                hexView.clear();
            }
            updateStatus("已打开十六进制 Hex 与底层二进制 Inspector 视窗");
        } else {
            // 强制根据当前光标位置渲染对应的 Chunk
            handleCaretPositionChanged(true);
        }
        refreshViewMenuState();
    }
    
    /**
     * 关闭当前子视图
     */
    private void closeSubView() {
        currentSubViewType = SubViewType.NONE;
        mainSplitPane.getItems().remove(graphContainer);
        graphContainer.setVisible(false);
        refreshViewMenuState();
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
            case AST:
                astGraphLabel.setText("AST视图" + titleSuffix);
                break;
            case CFG:
                astGraphLabel.setText("CFG视图" + titleSuffix);
                break;
            case HEX:
                EditorTab hexActiveTab = getActiveEditorTab();
                String fileName = (hexActiveTab != null && hexActiveTab.getFile() != null) ? hexActiveTab.getFile().getName() : "";
                astGraphLabel.setText("十六进制 Hex 视窗" + (fileName.isEmpty() ? "" : " - [" + fileName + "]"));
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
        
        EditorTab conversionTab = getActiveEditorTab();
        com.github.relua.decompiler.pipeline.DecompilerResult existingResult = conversionTab.getDecompilerResult();
        // 缓存源码不加载 AST/CFG；用户打开图视图时再在后台生成。
        // 创建并启动后台任务
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("正在生成" + getSubViewName(subViewType) + "...");
                com.github.relua.decompiler.pipeline.DecompilerResult graphResult = existingResult;
                if ((subViewType == SubViewType.AST || subViewType == SubViewType.CFG) && graphResult == null) {
                    com.github.relua.decompiler.Decompiler decompiler = new com.github.relua.decompiler.Decompiler();
                    decompiler.decompile(luacFile);
                    graphResult = decompiler.getLastResult();
                }
                final com.github.relua.decompiler.pipeline.DecompilerResult preparedResult = graphResult;
                
                switch (subViewType) {
                    case AST:
                        updateMessage("正在生成AST视图...");
                        Platform.runLater(() -> {
                            if (conversionTab != getActiveEditorTab() || currentSubViewType != subViewType) return;
                            conversionTab.setDecompilerResult(preparedResult);
                            astGraphConverter.convertToGraph(chunk, preparedResult);
                        });
                        break;
                    case CFG:
                        updateMessage("正在生成CFG视图...");
                        javafx.application.Platform.runLater(() -> {
                            if (conversionTab != getActiveEditorTab() || currentSubViewType != subViewType) return;
                            conversionTab.setDecompilerResult(preparedResult);
                            cfgGraphConverter.convertToGraph(chunk, preparedResult);
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
            case AST:
                return "AST视图";
            case CFG:
                return "CFG视图";
            case HEX:
                return "十六进制Hex视图";
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
        refreshViewMenuState();
        TextEditorView activeEditor = getActiveTextEditorView();
        updateCursorPositionStatus(activeEditor);

        if (graphContainer.isVisible() && currentSubViewType == SubViewType.HEX) {
            return;
        }

        LuacFile luacFile = fileMenuHandler.getCurrentLuacFile();
        if (luacFile == null) {
            currentDisplayedChunk = null;
            lastLoadedLuacFile = null;
            if (currentSubViewType == SubViewType.AST || currentSubViewType == SubViewType.CFG) closeSubView();
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
        refreshViewMenuState();
        if (newTab instanceof EditorTab) {
            EditorTab activeTab = (EditorTab) newTab;
            wireTabNavigationCallbacks(activeTab);
            // 更新文件名和状态栏
            updateFileLabel(activeTab.getFile().getName());
            updateStatus("Active file: " + activeTab.getFile().getName());

            // 在文件树中高亮选中
            if (fileTreeView != null) {
                fileTreeView.trackEditor(activeTab);
                fileTreeView.selectFile(activeTab.getFile());
            }

            if (graphContainer.isVisible() && currentSubViewType == SubViewType.HEX) {
                updateSubViewTitle(SubViewType.HEX);
                hexView.loadFile(activeTab.getFile());
            } else {
                // 同步子视图与光标指示
                handleCaretPositionChanged(true);
            }
        } else {
            // 没有打开的标签页
            updateFileLabel(I18nUtil.getString("status.noFileOpened"));
            updateStatus(I18nUtil.getString("status.ready"));
            updateCursorPositionStatus(null);

            if (hexView != null) {
                hexView.clear();
            }

            // 关闭当前子视图
            if (graphContainer.isVisible()) {
                closeSubView();
            }
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
        menuItemExportSource.setText(I18nUtil.getString("menu.file.exportSource"));
        menuItemSaveAnalysis.setText(I18nUtil.getString("menu.file.saveAnalysis"));
        menuItemPreferences.setText(I18nUtil.getString("menu.file.preferences"));
        menuItemExit.setText(I18nUtil.getString("menu.file.exit"));

        menuItemUndo.setText(I18nUtil.getString("menu.edit.undo"));
        menuItemRedo.setText(I18nUtil.getString("menu.edit.redo"));
        menuItemCut.setText(I18nUtil.getString("menu.edit.cut"));
        menuItemCopy.setText(I18nUtil.getString("menu.edit.copy"));
        menuItemPaste.setText(I18nUtil.getString("menu.edit.paste"));

        menuItemZoomIn.setText(I18nUtil.getString("menu.view.zoomIn"));
        menuItemZoomOut.setText(I18nUtil.getString("menu.view.zoomOut"));
        menuItemResetZoom.setText(I18nUtil.getString("menu.view.resetZoom"));
        menuAnalysisPanel.setText(I18nUtil.getString("menu.view.analysisPanel"));
        menuGraphZoom.setText(I18nUtil.getString("menu.view.graphZoom"));
        menuItemHexView.setText(I18nUtil.getString("menu.view.hex"));
        menuItemAstView.setText(I18nUtil.getString("menu.view.ast"));
        menuItemCfgView.setText(I18nUtil.getString("menu.view.cfg"));
        menuItemCloseAnalysisPanel.setText(I18nUtil.getString("menu.view.closeAnalysisPanel"));

        menuItemAbout.setText(I18nUtil.getString("menu.help.about"));

        btnOpen.setText("📂 " + I18nUtil.getString("toolbar.open"));
        btnOpen.setTooltip(new Tooltip("打开单个 Lua 字节码或源码文件 (Ctrl+O)"));

        if (btnOpenFolder != null) {
            btnOpenFolder.setText("📁 打开工程");
            btnOpenFolder.setTooltip(new Tooltip("打开并分析整个工程文件夹"));
        }

        btnExportSource.setText("⇩ " + I18nUtil.getString("toolbar.exportSource"));
        btnSaveAnalysis.setText("💾 " + I18nUtil.getString("toolbar.saveAnalysis"));
        btnSaveAnalysis.setTooltip(new Tooltip(I18nUtil.getString("toolbar.saveAnalysisTooltip")));
        btnExportSource.setTooltip(new Tooltip("将当前 Lua 源码导出到独立文件 (Ctrl+E)"));

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
        btnToggleBytecode.setTooltip(new Tooltip("切换主面板 Code / Bytecode，保留 Split view 状态"));

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
