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
import com.github.relua.model.LuacFile;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Constant;
import com.github.relua.model.LocalVar;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
    private TextEditorView textEditorView;
    private GraphVisualizationView graphVisualizationView;
    
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
        i18nService.initializeViewMenuItems(menuItemZoomIn, menuItemZoomOut, menuItemResetZoom);
        i18nService.initializeHelpMenuItems(menuItemAbout);
        i18nService.initializeToolbarButtons(btnOpen, btnSave, btnUndo, btnRedo, btnZoomIn, btnZoomOut, btnResetZoom);
        i18nService.initializeLabels(luaCodeLabel, astGraphLabel, logLabel, btnClearLog);
        
        // 默认隐藏右侧图形视图，仅保留文件树和代码视图
        mainSplitPane.getItems().remove(graphContainer);
        graphContainer.setVisible(false);
        
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
        
        // 设置文件树的文件打开回调
        fileTreeView.setOnFileOpenCallback(fileNode -> {
            fileMenuHandler.openFile(fileNode.getFile());
        });
    }
    
    /**
     * 处理打开文件夹事件
     * @param event 事件对象
     */
    @FXML
    private void handleOpenFolder(ActionEvent event) {
        // 使用DirectoryChooser来选择文件夹，而不是FileChooser
        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle("Open Folder");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        
        // 显示文件夹选择对话框
        File selectedFolder = directoryChooser.showDialog(new Stage());
        if (selectedFolder != null) {
            // 加载文件夹内容到文件树
            fileTreeView.loadFolder(selectedFolder);
            updateStatus("Folder opened: " + selectedFolder.getName());
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
        
        // 确保图形容器可见
        if (!graphContainer.isVisible()) {
            if (!mainSplitPane.getItems().contains(graphContainer)) {
                mainSplitPane.getItems().add(graphContainer);
            }
            graphContainer.setVisible(true);
        }
        
        // 根据子视图类型更新标题
        updateSubViewTitle(subViewType);
        
        // 执行视图转换（在后台线程中）
        executeSubViewConversion(subViewType);
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
        switch (subViewType) {
            case BYTECODE:
                astGraphLabel.setText("字节码视图");
                break;
            case AST:
                astGraphLabel.setText("AST视图");
                break;
            case CFG:
                astGraphLabel.setText("CFG视图");
                break;
            default:
                astGraphLabel.setText("子视图");
        }
    }
    
    /**
     * 执行子视图转换（在后台线程中）
     * @param subViewType 子视图类型
     */
    private void executeSubViewConversion(SubViewType subViewType) {
        // 获取当前打开的LuacFile
        LuacFile luacFile = fileMenuHandler.getCurrentLuacFile();
        if (luacFile == null) {
            updateStatus("请先打开一个文件");
            return;
        }
        
        // 创建并启动后台任务
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // 更新状态信息
                updateMessage("正在生成" + getSubViewName(subViewType) + "...");
                
                // 根据子视图类型执行转换
                switch (subViewType) {
                    case BYTECODE:
                        // 字节码视图处理逻辑
                        updateMessage("正在生成字节码视图...");
                        generateBytecodeView(luacFile);
                        break;
                    case AST:
                        // AST视图处理逻辑
                        updateMessage("正在生成AST视图...");
                        astGraphConverter.convertToGraph(luacFile);
                        break;
                    case CFG:
                        // CFG视图处理逻辑
                        updateMessage("正在生成CFG视图...");
                        cfgGraphConverter.convertToGraph(luacFile);
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
        
        // 绑定状态信息
        task.messageProperty().addListener((obs, oldMsg, newMsg) -> updateStatus(newMsg));
        
        // 启动任务线程
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * 生成字节码视图
     * @param luacFile Luac文件对象
     */
    private void generateBytecodeView(LuacFile luacFile) {
        // 获取主代码块
        Chunk mainChunk = luacFile.getMainChunk();
        if (mainChunk == null) {
            return;
        }
        
        // 生成字节码内容
        StringBuilder bytecodeContent = new StringBuilder();
        
        // 添加文件头信息
        bytecodeContent.append("=== Luac File Header ===\n");
        bytecodeContent.append(String.format("Version: %s\n", luacFile.getLuaVersion()));
        bytecodeContent.append(String.format("Endianness: %s\n", luacFile.isLittleEndian() ? "Little Endian" : "Big Endian"));
        bytecodeContent.append(String.format("Instruction Size: %d bytes\n", luacFile.getInstructionSize()));
        bytecodeContent.append(String.format("Lua Number Size: %d bytes\n", luacFile.getLuaNumberSize()));
        bytecodeContent.append(String.format("Integral Flag: %d\n\n", luacFile.getIntegralFlag()));
        
        // 添加主代码块信息
        bytecodeContent.append("=== Main Chunk ===\n");
        bytecodeContent.append(String.format("Function: %s\n", mainChunk.getFunction()));
        bytecodeContent.append(String.format("Line Defined: %d\n", mainChunk.getLineDefined()));
        bytecodeContent.append(String.format("Last Line Defined: %d\n", mainChunk.getLastLineDefined()));
        bytecodeContent.append(String.format("Parameters: %d\n", mainChunk.getNumParams()));
        bytecodeContent.append(String.format("Vararg: %d\n", mainChunk.getIsVararg()));
        bytecodeContent.append(String.format("Max Stack Size: %d\n", mainChunk.getMaxStackSize()));
        bytecodeContent.append(String.format("Upvalues: %d\n", mainChunk.getNup()));
        bytecodeContent.append(String.format("Instructions: %d\n", mainChunk.getInstructions().size()));
        bytecodeContent.append(String.format("Constants: %d\n", mainChunk.getConstants().size()));
        bytecodeContent.append(String.format("Sub Chunks: %d\n", mainChunk.getSubChunks().size()));
        bytecodeContent.append(String.format("Local Vars: %d\n\n", mainChunk.getLocalVars().size()));
        
        // 添加指令列表
        bytecodeContent.append("=== Instructions ===\n");
        bytecodeContent.append(String.format("%4s | %8s | %-12s | %s\n", "PC", "Code", "Opcode", "Operands"));
        bytecodeContent.append("----+--------+------------+--------\n");
        
        List<Instruction> instructions = mainChunk.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instr = instructions.get(i);
            String codeHex = String.format("0x%08X", instr.getCode());
            String opcode = instr.getOpcode().name();
            String operands = String.format("A=%d B=%d C=%d Bx=%d sBx=%d", 
                                          instr.getA(), instr.getB(), instr.getC(), 
                                          instr.getBx(), instr.getSBx());
            
            bytecodeContent.append(String.format("%4d | %8s | %-12s | %s\n", i, codeHex, opcode, operands));
        }
        
        // 添加常量表
        bytecodeContent.append("\n=== Constants ===\n");
        List<Constant> constants = mainChunk.getConstants();
        for (int i = 0; i < constants.size(); i++) {
            Constant constant = constants.get(i);
            bytecodeContent.append(String.format("%4d: %s\n", i, constant.toString()));
        }
        
        // 添加局部变量表
        bytecodeContent.append("\n=== Local Variables ===\n");
        List<LocalVar> localVars = mainChunk.getLocalVars();
        for (LocalVar localVar : localVars) {
            bytecodeContent.append(String.format("%s: startPC=%d endPC=%d\n", 
                                          localVar.getName(), localVar.getStartPC(), localVar.getEndPC()));
        }
        
        // 将字节码内容显示在图形视图中
        graphVisualizationView.setTextContent(bytecodeContent.toString());
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
