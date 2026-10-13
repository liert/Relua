package com.github.relua.gui.model;

import java.io.File;
import java.util.Arrays;

/**
 * 资源树中的文件、文件夹、文件内函数与加载状态。
 */
public class FileNode {
    private File file;
    private boolean isDirectory;
    private FileNode parent;
    private final OutlineItem function;
    private final String status;
    
    /**
     * 构造函数
     * @param file 文件对象
     */
    public FileNode(File file) {
        this(file, null, null);
    }

    private FileNode(File file, OutlineItem function, String status) {
        this.file = file;
        this.function = function;
        this.status = status;
        this.isDirectory = function == null && status == null && file.isDirectory();
    }

    public static FileNode function(File file, OutlineItem function) {
        return new FileNode(file, function, null);
    }

    public static FileNode status(File file, String message) {
        return new FileNode(file, null, message);
    }

    public boolean isFunction() {
        return function != null;
    }

    public OutlineItem getFunction() {
        return function;
    }

    public boolean isStatus() {
        return status != null;
    }
    
    /**
     * 获取文件对象
     * @return 文件对象
     */
    public File getFile() {
        return file;
    }
    
    /**
     * 设置文件对象
     * @param file 文件对象
     */
    public void setFile(File file) {
        this.file = file;
        this.isDirectory = function == null && status == null && file.isDirectory();
    }
    
    /**
     * 判断是否为文件夹
     * @return 是否为文件夹
     */
    public boolean isDirectory() {
        return isDirectory;
    }
    
    /**
     * 获取父节点
     * @return 父节点
     */
    public FileNode getParent() {
        return parent;
    }
    
    /**
     * 设置父节点
     * @param parent 父节点
     */
    public void setParent(FileNode parent) {
        this.parent = parent;
    }
    
    /**
     * 获取文件名
     * @return 文件名
     */
    public String getName() {
        return function != null ? function.getName() : status != null ? status : file.getName();
    }
    
    /**
     * 获取文件路径
     * @return 文件路径
     */
    public String getPath() {
        return file.getAbsolutePath();
    }
    
    /**
     * 获取子文件节点列表，严格遵循 Windows 文件夹排序规则：
     * 文件夹在上面，文件在下面，同类节点按不区分大小写的自然顺序排列
     * 
     * @return 子文件节点数组
     */
    public FileNode[] getChildren() {
        if (!isDirectory) {
            return new FileNode[0];
        }
        
        File[] files = file.listFiles();
        if (files == null) {
            return new FileNode[0];
        }
        
        // 创建子文件节点数组并建立父子节点引用关系
        FileNode[] children = new FileNode[files.length];
        for (int i = 0; i < files.length; i++) {
            children[i] = new FileNode(files[i]);
            children[i].setParent(this);
        }
        
        // 按照 Windows 规则排序：文件夹排在最上面，文件排在下面
        Arrays.sort(children, (n1, n2) -> {
            boolean isDir1 = n1.isDirectory();
            boolean isDir2 = n2.isDirectory();
            if (isDir1 && !isDir2) {
                return -1; // 文件夹排在前面
            } else if (!isDir1 && isDir2) {
                return 1;  // 文件排在后面
            } else {
                return n1.getName().compareToIgnoreCase(n2.getName());
            }
        });
        
        return children;
    }
    
    @Override
    public String toString() {
        return getName();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FileNode other = (FileNode) obj;
        return file.equals(other.file) && java.util.Objects.equals(status, other.status)
                && (function == null ? other.function == null : other.function != null
                && function.getStartLine() == other.function.getStartLine()
                && function.getName().equals(other.function.getName()));
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(file, status, function != null ? function.getStartLine() : null,
                function != null ? function.getName() : null);
    }
}
