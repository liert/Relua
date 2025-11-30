package com.github.relua.gui.views;

import com.github.relua.gui.model.FileNode;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件树视图类，用于显示文件系统的树状结构
 */
public class FileTreeView {
    private TreeView<FileNode> treeView;
    private TreeItem<FileNode> rootItem;
    private Map<String, Image> iconMap;
    
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
        
        // 设置单元格工厂，用于显示不同类型的文件图标
        treeView.setCellFactory(param -> new FileTreeCell());
        
        // 添加鼠标事件监听器，处理节点点击和右键菜单
        treeView.setOnMouseClicked(this::handleMouseClick);
    }
    
    /**
     * 初始化图标映射
     */
    private void initializeIcons() {
        iconMap = new HashMap<>();
        
        // 尝试加载图标，如果失败则跳过，不设置图标
        try {
            // 文件夹图标
            loadIcon("folder", "/icons/folder.png");
            loadIcon("folder-open", "/icons/folder-open.png");
            
            // 文件图标
            loadIcon("file", "/icons/file.png");
            loadIcon("lua", "/icons/lua.png");
            loadIcon("image", "/icons/image.png");
            loadIcon("document", "/icons/document.png");
        } catch (Exception e) {
            // 如果图标加载失败，忽略异常
            e.printStackTrace();
        }
    }
    
    /**
     * 加载图标
     * @param key 图标键
     * @param path 图标路径
     */
    private void loadIcon(String key, String path) {
        try {
            java.io.InputStream stream = getClass().getResourceAsStream(path);
            if (stream != null) {
                iconMap.put(key, new Image(stream));
                stream.close();
            }
        } catch (Exception e) {
            // 如果图标加载失败，忽略异常
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
        ContextMenu contextMenu = new ContextMenu();
        FileNode fileNode = item.getValue();
        
        // 打开菜单项
        MenuItem openItem = new MenuItem("打开");
        openItem.setOnAction(e -> {
            if (fileNode.isDirectory()) {
                item.setExpanded(!item.isExpanded());
            } else {
                handleFileOpen(fileNode);
            }
        });
        contextMenu.getItems().add(openItem);
        
        // 如果是文件夹，添加新建菜单项
        if (fileNode.isDirectory()) {
            MenuItem newFolderItem = new MenuItem("新建文件夹");
            newFolderItem.setOnAction(e -> handleNewFolder(fileNode));
            contextMenu.getItems().add(newFolderItem);
            
            MenuItem newFileItem = new MenuItem("新建文件");
            newFileItem.setOnAction(e -> handleNewFile(fileNode));
            contextMenu.getItems().add(newFileItem);
        }
        
        // 重命名菜单项
        MenuItem renameItem = new MenuItem("重命名");
        renameItem.setOnAction(e -> handleRename(fileNode));
        contextMenu.getItems().add(renameItem);
        
        // 删除菜单项
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setOnAction(e -> handleDelete(fileNode));
        contextMenu.getItems().add(deleteItem);
        
        // 显示上下文菜单
        contextMenu.show(treeView, x, y);
    }
    
    /**
     * 处理文件打开事件
     * @param fileNode 文件节点
     */
    private void handleFileOpen(FileNode fileNode) {
        // 这里可以添加文件打开的逻辑，例如通知主控制器打开文件
        System.out.println("打开文件: " + fileNode.getPath());
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
     * 加载子节点
     * @param parentItem 父节点
     */
    private void loadChildren(TreeItem<FileNode> parentItem) {
        FileNode parentNode = parentItem.getValue();
        if (!parentNode.isDirectory()) {
            return;
        }
        
        // 获取子文件
        File[] files = parentNode.getFile().listFiles();
        if (files == null) {
            return;
        }
        
        // 清空现有子节点
        parentItem.getChildren().clear();
        
        // 添加子节点
        for (File file : files) {
            FileNode childNode = new FileNode(file);
            TreeItem<FileNode> childItem = new TreeItem<>(childNode);
            
            // 如果是文件夹，添加一个空的子节点作为占位符
            if (file.isDirectory()) {
                childItem.getChildren().add(new TreeItem<>(null));
            }
            
            parentItem.getChildren().add(childItem);
        }
    }
    
    /**
     * 获取TreeView控件
     * @return TreeView控件
     */
    public TreeView<FileNode> getView() {
        return treeView;
    }
    
    /**
     * 添加单个文件到文件树中
     * @param file 文件对象
     */
    public void addFile(File file) {
        if (file.isDirectory()) {
            return;
        }
        
        // 创建文件节点
        FileNode fileNode = new FileNode(file);
        TreeItem<FileNode> fileItem = new TreeItem<>(fileNode);
        
        // 添加文件到根节点
        rootItem.getChildren().add(fileItem);
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
                        fileIcon = iconMap.get("lua");
                        break;
                    case "png":
                    case "jpg":
                    case "jpeg":
                    case "gif":
                        fileIcon = iconMap.get("image");
                        break;
                    case "txt":
                    case "md":
                    case "pdf":
                        fileIcon = iconMap.get("document");
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
            
            // 如果是文件夹且只有一个空的子节点，加载真实的子节点
            TreeItem<FileNode> treeItem = getTreeItem();
            if (item.isDirectory() && treeItem.getChildren().size() == 1 && treeItem.getChildren().get(0).getValue() == null) {
                // 异步加载子节点，避免UI卡顿
                new Thread(() -> loadChildren(treeItem)).start();
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