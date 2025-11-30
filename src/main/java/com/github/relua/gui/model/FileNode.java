package com.github.relua.gui.model;

import java.io.File;

/**
 * 文件树节点模型类，用于表示文件系统中的文件和文件夹
 */
public class FileNode {
    private File file;
    private boolean isDirectory;
    private FileNode parent;
    
    /**
     * 构造函数
     * @param file 文件对象
     */
    public FileNode(File file) {
        this.file = file;
        this.isDirectory = file.isDirectory();
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
        this.isDirectory = file.isDirectory();
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
        return file.getName();
    }
    
    /**
     * 获取文件路径
     * @return 文件路径
     */
    public String getPath() {
        return file.getAbsolutePath();
    }
    
    /**
     * 获取子文件节点
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
        
        FileNode[] children = new FileNode[files.length];
        for (int i = 0; i < files.length; i++) {
            children[i] = new FileNode(files[i]);
            children[i].setParent(this);
        }
        
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
        return file.equals(other.file);
    }
    
    @Override
    public int hashCode() {
        return file.hashCode();
    }
}