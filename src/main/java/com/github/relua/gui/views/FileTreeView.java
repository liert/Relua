package com.github.relua.gui.views;

import com.github.relua.gui.model.FileNode;
import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.awt.Desktop;
import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.text.Collator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 文件树视图类，用于显示文件系统的树状结构
 */
public class FileTreeView {
    private TreeView<FileNode> treeView;
    private TreeItem<FileNode> rootItem;
    private Map<String, Image> iconMap;
    private Consumer<FileNode> onFileOpenCallback;
    
    /**
     * 构造函数
     */
    public FileTreeView() {
        // 初始化图标映射
        initializeIcons();
        
        // 创建根节点
        rootItem = new TreeItem<>(new FileNode(new File("/")));
        rootItem.setExpanded(true);
        
        // 创建TreeView
        treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.setFixedCellSize(26.0); // 声明固定行高，整型像素绝对对齐，大幅提升虚拟滚动性能并杜绝浮点行高抖动与亚像素模糊
        treeView.getStyleClass().add("file-tree-view");
        
        // 设置单元格工厂，用于显示不同类型的文件图标
        treeView.setCellFactory(param -> new FileTreeCell());
        
        // 添加鼠标事件监听器，处理节点点击和右键菜单
        treeView.setOnMouseClicked(this::handleMouseClick);
    }
    
    /**
     * 初始化图标映射，优先加载外部资源，若无则自动生成高质感现代化矢量图标
     */
    private void initializeIcons() {
        iconMap = new HashMap<>();
        String[] iconKeys = {"folder", "folder-open", "file", "lua", "image", "document", "archive"};
        for (String key : iconKeys) {
            loadIcon(key, "/icons/" + key + ".png");
            if (!iconMap.containsKey(key)) {
                Image vectorIcon = createModernVectorIcon(key);
                if (vectorIcon != null) {
                    iconMap.put(key, vectorIcon);
                }
            }
        }
    }
    
    /**
     * 加载图标
     * @param key 图标键
     * @param path 图标路径
     */
    private void loadIcon(String key, String path) {
        try {
            InputStream stream = getClass().getResourceAsStream(path);
            if (stream != null) {
                iconMap.put(key, new Image(stream));
                stream.close();
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 自动绘制高质感 16x16 现代 IDE 风格矢量图标
     * @param type 图标类型标识
     * @return 生成的内存 Image 图标
     */
    private Image createModernVectorIcon(String type) {
        try {
            Canvas canvas = new Canvas(16, 16);
            GraphicsContext gc = canvas.getGraphicsContext2D();

            switch (type) {
                case "folder":
                    // 现代金黄色文件夹（闭合态）
                    gc.setFill(Color.web("#e5a93b"));
                    gc.fillRoundRect(1.5, 2.5, 6, 3.5, 2, 2);
                    gc.fillRoundRect(1.5, 4.5, 13, 9, 2.5, 2.5);
                    gc.setStroke(Color.web("#fcd34d"));
                    gc.setLineWidth(0.8);
                    gc.strokeLine(2.5, 5.5, 13.5, 5.5);
                    break;
                case "folder-open":
                    // 现代金黄色文件夹（展开态）
                    gc.setFill(Color.web("#d97706"));
                    gc.fillRoundRect(1.5, 2.5, 6, 3.5, 2, 2);
                    gc.fillRoundRect(1.5, 4.5, 13, 9, 2.5, 2.5);
                    gc.setFill(Color.web("#fbbf24"));
                    gc.fillPolygon(new double[]{0.5, 3.5, 15.5, 12.5}, new double[]{13.5, 7.5, 7.5, 13.5}, 4);
                    break;
                case "lua":
                    // 专业 Lua 脚本标志（深青蓝底 + 经典星月结构）
                    gc.setFill(Color.web("#0284c7"));
                    gc.fillRoundRect(1.5, 1.5, 13, 13, 3, 3);
                    gc.setFill(Color.WHITE);
                    gc.fillOval(3.5, 3.5, 7, 7);
                    gc.setFill(Color.web("#0369a1"));
                    gc.fillOval(5.0, 5.0, 4, 4);
                    gc.setFill(Color.WHITE);
                    gc.fillOval(10.5, 3.0, 2.5, 2.5);
                    break;
                case "image":
                    // 现代紫粉色图片卡片
                    gc.setFill(Color.web("#a855f7"));
                    gc.fillRoundRect(2, 2, 12, 12, 2, 2);
                    gc.setFill(Color.WHITE);
                    gc.fillOval(4, 4, 2.5, 2.5);
                    gc.fillPolygon(new double[]{3, 7, 10, 13, 13, 3}, new double[]{12, 8, 10, 7, 12, 12}, 6);
                    break;
                case "document":
                    // 天蓝色文档
                    gc.setFill(Color.web("#38bdf8"));
                    gc.fillRoundRect(2, 1.5, 11, 13, 2, 2);
                    gc.setFill(Color.WHITE);
                    gc.fillRect(4, 4, 7, 1.5);
                    gc.fillRect(4, 7, 7, 1.5);
                    gc.fillRect(4, 10, 5, 1.5);
                    break;
                case "archive":
                    // 暖橙色归档包
                    gc.setFill(Color.web("#ea580c"));
                    gc.fillRoundRect(2, 2, 12, 12, 2, 2);
                    gc.setStroke(Color.web("#ffedd5"));
                    gc.setLineWidth(1);
                    gc.strokeLine(8, 2, 8, 14);
                    gc.strokeLine(5, 7, 11, 7);
                    break;
                default: // "file"
                    // 现代银灰色代码文档（带折角）
                    gc.setFill(Color.web("#94a3b8"));
                    gc.fillRoundRect(2.5, 1.5, 11, 13, 2, 2);
                    gc.setFill(Color.web("#cbd5e1"));
                    gc.fillPolygon(new double[]{9.5, 13.5, 9.5}, new double[]{1.5, 5.5, 5.5}, 3);
                    gc.setFill(Color.web("#475569"));
                    gc.fillRect(4.5, 6, 6, 1.2);
                    gc.fillRect(4.5, 8.5, 6, 1.2);
                    gc.fillRect(4.5, 11, 4, 1.2);
                    break;
            }

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            return canvas.snapshot(params, null);
        } catch (Throwable ignored) {
            return null;
        }
    }
    
    /**
     * 处理鼠标点击事件
     * @param event 鼠标事件
     */
    private void handleMouseClick(MouseEvent event) {
        TreeItem<FileNode> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return;
        }
        
        // 左键双击打开文件或文件夹
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            FileNode fileNode = selectedItem.getValue();
            if (fileNode.isDirectory()) {
                // 切换文件夹展开/折叠状态
                selectedItem.setExpanded(!selectedItem.isExpanded());
            } else {
                // 打开文件
                handleFileOpen(fileNode);
            }
        } 
        // 右键点击显示上下文菜单
        else if (event.getButton() == MouseButton.SECONDARY) {
            showContextMenu(selectedItem, event.getScreenX(), event.getScreenY());
        }
    }
    
    /**
     * 显示上下文菜单
     * @param item 选中的树节点
     * @param x 鼠标X坐标
     * @param y 鼠标Y坐标
     */
    private void showContextMenu(TreeItem<FileNode> item, double x, double y) {
        if (item == null || item.getValue() == null) {
            return;
        }
        ContextMenu contextMenu = new ContextMenu();
        FileNode fileNode = item.getValue();
        File file = fileNode.getFile();
        
        // 打开菜单项
        MenuItem openItem = new MenuItem(fileNode.isDirectory() ? "展开/折叠 (Expand/Collapse)" : "在编辑器中打开 (Open)");
        openItem.setOnAction(e -> {
            if (fileNode.isDirectory()) {
                item.setExpanded(!item.isExpanded());
            } else {
                handleFileOpen(fileNode);
            }
        });
        contextMenu.getItems().add(openItem);
        contextMenu.getItems().add(new SeparatorMenuItem());

        // 复制绝对路径
        MenuItem copyPathItem = new MenuItem("复制绝对路径 (Copy Full Path)");
        copyPathItem.setOnAction(e -> copyTextToClipboard(file.getAbsolutePath()));
        contextMenu.getItems().add(copyPathItem);

        // 复制文件名
        MenuItem copyNameItem = new MenuItem("复制文件名 (Copy Name)");
        copyNameItem.setOnAction(e -> copyTextToClipboard(file.getName()));
        contextMenu.getItems().add(copyNameItem);

        contextMenu.getItems().add(new SeparatorMenuItem());

        // 在文件资源管理器中显示
        MenuItem revealItem = new MenuItem("在文件资源管理器中显示 (Reveal in Explorer)");
        revealItem.setOnAction(e -> revealInSystemExplorer(file));
        contextMenu.getItems().add(revealItem);
        
        // 如果是文件夹，添加新建菜单项
        if (fileNode.isDirectory()) {
            contextMenu.getItems().add(new SeparatorMenuItem());
            MenuItem newFolderItem = new MenuItem("新建文件夹");
            newFolderItem.setOnAction(e -> handleNewFolder(fileNode));
            contextMenu.getItems().add(newFolderItem);
            
            MenuItem newFileItem = new MenuItem("新建文件");
            newFileItem.setOnAction(e -> handleNewFile(fileNode));
            contextMenu.getItems().add(newFileItem);
        }
        
        contextMenu.getItems().add(new SeparatorMenuItem());

        // 重命名菜单项
        MenuItem renameItem = new MenuItem("重命名");
        renameItem.setOnAction(e -> handleRename(fileNode));
        contextMenu.getItems().add(renameItem);
        
        // 删除菜单项
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setOnAction(e -> handleDelete(fileNode));
        contextMenu.getItems().add(deleteItem);
        
        // 显示上下文菜单（确保拥有活动的 Scene 和 Window）
        if (treeView.getScene() != null && treeView.getScene().getWindow() != null) {
            contextMenu.show(treeView, x, y);
        }
    }

    /**
     * 将指定文本复制到系统剪贴板
     * @param text 要复制的文本
     */
    private void copyTextToClipboard(String text) {
        if (text == null) {
            return;
        }
        try {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
        } catch (Exception ignored) {
        }
    }

    /**
     * 在操作系统的文件管理器中定位并选中文件
     * @param file 目标文件或文件夹
     */
    private void revealInSystemExplorer(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        new Thread(() -> {
            try {
                String osName = System.getProperty("os.name", "").toLowerCase();
                if (osName.contains("win")) {
                    // Windows 下精准选中该文件
                    new ProcessBuilder("explorer.exe", "/select,", file.getAbsolutePath()).start();
                } else if (osName.contains("mac")) {
                    // macOS 下定位文件
                    new ProcessBuilder("open", "-R", file.getAbsolutePath()).start();
                } else {
                    // Linux 或其它平台打开父目录
                    File target = file.isDirectory() ? file : file.getParentFile();
                    if (target != null && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        Desktop.getDesktop().open(target);
                    }
                }
            } catch (Exception e) {
                try {
                    File target = file.isDirectory() ? file : file.getParentFile();
                    if (target != null && Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(target);
                    }
                } catch (Exception ignored) {
                }
            }
        }, "RevealInExplorerThread").start();
    }
    
    /**
     * 处理文件打开事件
     * @param fileNode 文件节点
     */
    private void handleFileOpen(FileNode fileNode) {
        // 调用文件打开回调
        if (onFileOpenCallback != null) {
            onFileOpenCallback.accept(fileNode);
        }
    }
    
    /**
     * 设置文件打开回调
     * @param callback 文件打开回调
     */
    public void setOnFileOpenCallback(Consumer<FileNode> callback) {
        this.onFileOpenCallback = callback;
    }
    
    /**
     * 处理新建文件夹事件
     * @param parentNode 父文件夹节点
     */
    private void handleNewFolder(FileNode parentNode) {
        // 这里可以添加新建文件夹的逻辑
        System.out.println("新建文件夹: " + parentNode.getPath());
    }
    
    /**
     * 处理新建文件事件
     * @param parentNode 父文件夹节点
     */
    private void handleNewFile(FileNode parentNode) {
        // 这里可以添加新建文件的逻辑
        System.out.println("新建文件: " + parentNode.getPath());
    }
    
    /**
     * 处理重命名事件
     * @param fileNode 文件节点
     */
    private void handleRename(FileNode fileNode) {
        // 这里可以添加重命名文件的逻辑
        System.out.println("重命名: " + fileNode.getPath());
    }
    
    /**
     * 处理删除事件
     * @param fileNode 文件节点
     */
    private void handleDelete(FileNode fileNode) {
        // 这里可以添加删除文件的逻辑
        System.out.println("删除: " + fileNode.getPath());
    }
    
    /**
     * 加载文件夹内容
     * @param file 文件夹对象
     */
    public void loadFolder(File file) {
        if (!file.isDirectory()) {
            return;
        }
        
        // 创建文件夹节点
        FileNode folderNode = new FileNode(file);
        TreeItem<FileNode> folderItem = new TreeItem<>(folderNode);
        folderItem.setExpanded(true);
        
        // 清空根节点
        rootItem.getChildren().clear();
        
        // 添加文件夹到根节点
        rootItem.getChildren().add(folderItem);
        
        // 加载文件夹内容
        loadChildren(folderItem);
    }
    
    /**
     * 区域感知比较器，用于模拟 Windows 文件管理器的本地化（如中文拼音）自然排序
     */
    private static final Collator COLLATOR = Collator.getInstance(Locale.getDefault());
    static {
        // 设置强度为 SECONDARY，忽略英文字母大小写差异进行基础比较
        COLLATOR.setStrength(Collator.SECONDARY);
    }

    /**
     * 加载子节点，按照 Windows 文件夹排序方式对子文件进行排序：
     * 核心规则：文件夹排在最上面，文件排在下面；同类节点内部按 Windows 自然语言与数值顺序排列
     * 
     * @param parentItem 父节点
     */
    private void loadChildren(TreeItem<FileNode> parentItem) {
        if (parentItem == null) {
            return;
        }
        FileNode parentNode = parentItem.getValue();
        if (parentNode == null || !parentNode.isDirectory()) {
            return;
        }
        
        // 获取当前目录下的子文件与子文件夹列表
        File[] files = parentNode.getFile().listFiles();
        if (files == null) {
            return;
        }
        
        // 核心排序：严格按照 Windows 规则排序（文件夹全部置顶在最上方，文件全部在下方，同类自然排序）
        Arrays.sort(files, this::compareFiles);
        
        // 确保在 JavaFX 应用线程中更新 UI 视图树节点
        if (Platform.isFxApplicationThread()) {
            updateChildren(parentItem, files);
        } else {
            Platform.runLater(() -> updateChildren(parentItem, files));
        }
    }
    
    /**
     * 更新父节点的子节点列表，并为子文件夹绑定展开懒加载监听器
     * 
     * @param parentItem 父节点
     * @param files 已按 Windows 规则排好序的文件数组（文件夹在上面，文件在下面）
     */
    private void updateChildren(TreeItem<FileNode> parentItem, File[] files) {
        parentItem.getChildren().clear();
        for (File file : files) {
            FileNode childNode = new FileNode(file);
            TreeItem<FileNode> childItem = new TreeItem<>(childNode);
            
            // 如果是文件夹，添加一个空的子节点作为占位符以显示展开箭头，并监听展开事件
            if (file.isDirectory()) {
                childItem.getChildren().add(new TreeItem<>(null));
                // 监听子文件夹展开事件：当用户点击箭头展开且子节点尚未加载时，触发自动加载
                childItem.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                    if (isExpanded && childItem.getChildren().size() == 1 && childItem.getChildren().get(0).getValue() == null) {
                        loadChildren(childItem);
                    }
                });
            }
            
            parentItem.getChildren().add(childItem);
        }
    }

    /**
     * 比较两个树节点，严格遵循 Windows 文件夹排序规则：
     * 1. 文件夹排在最上面，文件排在下面
     * 2. 文件夹内部与文件内部，均按 Windows 自然排序（忽略大小写、数值自然递增、中文拼音等）
     * 
     * @param item1 第一个树节点
     * @param item2 第二个树节点
     * @return 比较结果（负数表示 item1 在前，正数表示 item2 在前，0 表示相等）
     */
    public int compareTreeItems(TreeItem<FileNode> item1, TreeItem<FileNode> item2) {
        if (item1 == null && item2 == null) return 0;
        if (item1 == null) return 1;
        if (item2 == null) return -1;
        
        FileNode node1 = item1.getValue();
        FileNode node2 = item2.getValue();
        if (node1 == null && node2 == null) return 0;
        if (node1 == null) return 1;
        if (node2 == null) return -1;
        
        return compareFileNodes(node1, node2);
    }

    /**
     * 比较两个文件节点模型：
     * 1. 文件夹排在最上面，文件排在下面
     * 2. 同类节点按 Windows 自然语言与数值顺序排序
     * 
     * @param node1 第一个文件节点
     * @param node2 第二个文件节点
     * @return 比较结果
     */
    public int compareFileNodes(FileNode node1, FileNode node2) {
        if (node1 == null && node2 == null) return 0;
        if (node1 == null) return 1;
        if (node2 == null) return -1;
        
        boolean isDir1 = node1.isDirectory();
        boolean isDir2 = node2.isDirectory();
        
        // 核心排序规则：文件夹在上，文件在下
        if (isDir1 && !isDir2) {
            return -1; // node1是文件夹，node2是文件，文件夹排在前面
        } else if (!isDir1 && isDir2) {
            return 1;  // node1是文件，node2是文件夹，文件夹排在前面
        } else {
            // 同为文件夹或同为文件，按照 Windows 文件名自然排序规则比较名称
            return compareNatural(node1.getName(), node2.getName());
        }
    }

    /**
     * 比较两个文件对象，严格遵循 Windows 文件夹排序规则：
     * 1. 文件夹排在最上面，文件排在下面
     * 2. 同类节点按 Windows 自然顺序（忽略大小写、数字数值自然递增、中文拼音等）排序
     * 
     * @param f1 第一个文件对象
     * @param f2 第二个文件对象
     * @return 比较结果（负数表示 f1 排在前面，正数表示 f2 排在前面）
     */
    public int compareFiles(File f1, File f2) {
        if (f1 == null && f2 == null) return 0;
        if (f1 == null) return 1;
        if (f2 == null) return -1;
        
        boolean isDir1 = f1.isDirectory();
        boolean isDir2 = f2.isDirectory();
        
        // 核心排序规则：文件夹在上，文件在下
        if (isDir1 && !isDir2) {
            return -1; // f1是文件夹，f2是文件，f1排在前面
        } else if (!isDir1 && isDir2) {
            return 1;  // f1是文件，f2是文件夹，f2排在前面
        } else {
            // 同为文件夹或同为文件，按照 Windows 文件名自然排序规则比较
            return compareNatural(f1.getName(), f2.getName());
        }
    }

    /**
     * 模拟 Windows 资源管理器（StrCmpLogicalW）的文件名自然排序规则：
     * 1. 支持数字连续串作为完整数值进行比较（例如 file2 排在 file10 前面）
     * 2. 忽略英文字母大小写差异
     * 3. 支持中文拼音自然排序（例如“李”排在“张”前面）
     * 4. 数值相同时前导零较少的排在前面（Windows 规范：1 < 01 < 001）
     * 5. 若忽略大小写与数值完全一致，按原始大小写精确比较，保证比较器自洽满足严格弱序（Strict Weak Ordering）
     * 
     * @param s1 第一个文件名
     * @param s2 第二个文件名
     * @return 比较结果（负数、0、正数）
     */
    private int compareNatural(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return 1;
        if (s2 == null) return -1;
        if (s1.equals(s2)) return 0;

        int len1 = s1.length();
        int len2 = s2.length();
        int i = 0, j = 0;
        int zeroDiff = 0; // 当数字数值相同时，记录前导零的长度差异作为备选判定

        while (i < len1 && j < len2) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(j);

            // 当两边同时遇到连续数字时，按完整数字的数值大小进行自然比较
            if (Character.isDigit(c1) && Character.isDigit(c2)) {
                int start1 = i;
                while (i < len1 && Character.isDigit(s1.charAt(i))) {
                    i++;
                }
                String numStr1 = s1.substring(start1, i);

                int start2 = j;
                while (j < len2 && Character.isDigit(s2.charAt(j))) {
                    j++;
                }
                String numStr2 = s2.substring(start2, j);

                // 使用 BigInteger 防止超长数字串溢出
                BigInteger b1 = new BigInteger(numStr1);
                BigInteger b2 = new BigInteger(numStr2);
                int numComp = b1.compareTo(b2);
                if (numComp != 0) {
                    return numComp;
                }

                // 数值相同但前导零数量不同（例如 "1" 与 "01"），记录第一次出现的长度差作为备选判定
                if (zeroDiff == 0 && numStr1.length() != numStr2.length()) {
                    zeroDiff = Integer.compare(numStr1.length(), numStr2.length());
                }
            } else {
                // 非全数字情况，使用本地化 Collator 比较字符（支持中文拼音和忽略大小写）
                int charComp = COLLATOR.compare(String.valueOf(c1), String.valueOf(c2));
                if (charComp != 0) {
                    return charComp;
                }
                i++;
                j++;
            }
        }

        // 一方已遍历完，短串排在前面
        if (i < len1) return 1;
        if (j < len2) return -1;

        // 若自然数值完全相同，前导零较少的排在前面（Windows规则：1 < 01 < 001）
        if (zeroDiff != 0) {
            return zeroDiff;
        }

        // 若忽略大小写与前导零完全一致，按原始字符串大小写精确比较，保证 Comparator 的传递性与严格弱序
        return s1.compareTo(s2);
    }
    
    /**
     * 获取TreeView控件
     * @return TreeView控件
     */
    public TreeView<FileNode> getView() {
        return treeView;
    }
    
    /**
     * 添加单个文件到文件树中，并保持 Windows 排序（文件夹在上，文件在下）
     * 
     * @param file 文件对象
     */
    public void addFile(File file) {
        if (file == null) {
            return;
        }
        
        // 如果当前处于文件夹模式，且为普通文件，则不追加独立节点，保持原有文件夹结构不变
        if (isFolderMode()) {
            return;
        }
        
        // 检查根节点下是否已存在相同绝对路径的节点，防止重复添加
        for (TreeItem<FileNode> child : rootItem.getChildren()) {
            if (child.getValue() != null && child.getValue().getFile().getAbsolutePath().equals(file.getAbsolutePath())) {
                return;
            }
        }
        
        // 创建文件或文件夹节点
        FileNode fileNode = new FileNode(file);
        TreeItem<FileNode> fileItem = new TreeItem<>(fileNode);
        
        // 如果是文件夹，添加占位符并绑定展开监听器
        if (file.isDirectory()) {
            fileItem.getChildren().add(new TreeItem<>(null));
            fileItem.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                if (isExpanded && fileItem.getChildren().size() == 1 && fileItem.getChildren().get(0).getValue() == null) {
                    loadChildren(fileItem);
                }
            });
        }
        
        // 添加到根节点
        rootItem.getChildren().add(fileItem);
        
        // 严格按照 Windows 规则排序根节点的子列表：文件夹在上面，文件在下面
        rootItem.getChildren().sort(this::compareTreeItems);
    }

    /**
     * 判断当前是否处于文件夹打开模式
     * @return 如果根节点下有文件夹节点则返回true
     */
    public boolean isFolderMode() {
        if (rootItem.getChildren().isEmpty()) {
            return false;
        }
        TreeItem<FileNode> firstChild = rootItem.getChildren().get(0);
        return firstChild != null && firstChild.getValue() != null && firstChild.getValue().isDirectory();
    }

    /**
     * 获取当前打开的根文件夹
     * @return 根文件夹，如果未处于文件夹模式则返回 null
     */
    public File getRootFolder() {
        if (!isFolderMode()) {
            return null;
        }
        TreeItem<FileNode> firstChild = rootItem.getChildren().get(0);
        return firstChild != null && firstChild.getValue() != null ? firstChild.getValue().getFile() : null;
    }

    /**
     * 在文件树中选中并高亮对应的文件节点
     * @param file 要选中的文件
     */
    public void selectFile(File file) {
        if (file == null || rootItem == null || rootItem.getChildren().isEmpty()) {
            return;
        }
        selectFileRecursively(rootItem, file);
    }

    private boolean selectFileRecursively(TreeItem<FileNode> parentItem, File targetFile) {
        FileNode parentNode = parentItem.getValue();
        if (parentNode != null && parentNode.getFile().getAbsolutePath().equals(targetFile.getAbsolutePath())) {
            // 找到目标文件，选中并滚动
            Platform.runLater(() -> {
                treeView.getSelectionModel().select(parentItem);
                int row = treeView.getRow(parentItem);
                if (row >= 0) {
                    treeView.scrollTo(row);
                }
            });
            return true;
        }

        if (parentNode == null || !parentNode.isDirectory()) {
            return false;
        }

        // 如果是文件夹，确认是否已经加载了真实的子节点
        if (parentItem.getChildren().size() == 1 && parentItem.getChildren().get(0).getValue() == null) {
            // 强制同步加载子节点以允许搜索
            loadChildren(parentItem);
        }

        // 检查 targetFile 是否在这个文件夹下面
        String parentPath = parentNode.getFile().getAbsolutePath();
        String targetPath = targetFile.getAbsolutePath();
        
        // 跨平台路径比较
        if (!targetPath.startsWith(parentPath)) {
            return false;
        }

        for (TreeItem<FileNode> child : parentItem.getChildren()) {
            if (child.getValue() != null) {
                String childPath = child.getValue().getFile().getAbsolutePath();
                if (targetPath.equals(childPath) || targetPath.startsWith(childPath + File.separator) || targetPath.startsWith(childPath + "/")) {
                    parentItem.setExpanded(true); // 展开父节点
                    boolean found = selectFileRecursively(child, targetFile);
                    if (found) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 文件树单元格类，用于显示不同类型的文件图标
     */
    private class FileTreeCell extends TreeCell<FileNode> {
        @Override
        protected void updateItem(FileNode item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            
            // 设置文本
            setText(item.getName());
            
            // 设置图标
            ImageView iconView = null;
            if (item.isDirectory()) {
                // 文件夹图标
                TreeItem<FileNode> treeItem = getTreeItem();
                Image folderIcon = (treeItem != null && treeItem.isExpanded()) ? iconMap.get("folder-open") : iconMap.get("folder");
                if (folderIcon != null) {
                    iconView = new ImageView(folderIcon);
                }
            } else {
                // 文件图标
                String extension = getFileExtension(item.getName());
                Image fileIcon = null;
                
                // 根据文件扩展名选择图标
                switch (extension) {
                    case "lua":
                    case "luac":
                    case "out":
                        fileIcon = iconMap.get("lua");
                        break;
                    case "png":
                    case "jpg":
                    case "jpeg":
                    case "gif":
                    case "ico":
                    case "svg":
                        fileIcon = iconMap.get("image");
                        break;
                    case "txt":
                    case "md":
                    case "pdf":
                    case "json":
                    case "xml":
                        fileIcon = iconMap.get("document");
                        break;
                    case "zip":
                    case "tar":
                    case "gz":
                    case "7z":
                    case "rar":
                        fileIcon = iconMap.get("archive");
                        break;
                    default:
                        fileIcon = iconMap.get("file");
                        break;
                }
                
                if (fileIcon != null) {
                    iconView = new ImageView(fileIcon);
                }
            }
            
            // 设置图标大小
            if (iconView != null) {
                iconView.setFitWidth(16);
                iconView.setFitHeight(16);
                setGraphic(iconView);
            } else {
                setGraphic(null);
            }
            
            // 如果是文件夹且当前节点已展开，但尚未加载真实子节点（仅包含占位符），则触发加载真实子节点
            TreeItem<FileNode> treeItem = getTreeItem();
            if (item.isDirectory() && treeItem != null && treeItem.isExpanded()
                    && treeItem.getChildren().size() == 1 && treeItem.getChildren().get(0).getValue() == null) {
                loadChildren(treeItem);
            }
        }
        
        /**
         * 获取文件扩展名
         * @param fileName 文件名
         * @return 文件扩展名
         */
        private String getFileExtension(String fileName) {
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
                return "";
            }
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
    }
}