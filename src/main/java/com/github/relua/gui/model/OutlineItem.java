package com.github.relua.gui.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 结构大纲与函数符号条目模型
 */
public class OutlineItem {

    /**
     * 符号类型枚举
     */
    public enum ItemType {
        MAIN_CHUNK("📦", "主代码块"),
        FUNCTION("ƒ", "函数"),
        LOCAL_FUNCTION("λ", "局部函数"),
        METHOD("⚡", "类/表方法"),
        VARIABLE("x", "局部变量");

        private final String badge;
        private final String description;

        ItemType(String badge, String description) {
            this.badge = badge;
            this.description = description;
        }

        public String getBadge() {
            return badge;
        }

        public String getDescription() {
            return description;
        }
    }

    private final String name;
    private final String signature; // e.g. "(a, b)"
    private final ItemType type;
    private final int startLine;    // 1-based
    private final int endLine;      // 1-based
    private final List<OutlineItem> children = new ArrayList<>();

    public OutlineItem(String name, String signature, ItemType type, int startLine, int endLine) {
        this.name = name != null ? name : "anonymous";
        this.signature = signature != null ? signature : "()";
        this.type = type != null ? type : ItemType.FUNCTION;
        this.startLine = Math.max(1, startLine);
        this.endLine = Math.max(this.startLine, endLine);
    }

    public String getName() {
        return name;
    }

    public String getSignature() {
        return signature;
    }

    public ItemType getType() {
        return type;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public List<OutlineItem> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void addChild(OutlineItem child) {
        if (child != null) {
            this.children.add(child);
        }
    }

    public boolean containsLine(int line) {
        return line >= startLine && line <= endLine;
    }

    @Override
    public String toString() {
        String rangeStr = (startLine == endLine) ? "Ln " + startLine : "Ln " + startLine + "-" + endLine;
        return type.getBadge() + " " + name + signature + " [" + rangeStr + "]";
    }
}
