package com.github.relua.gui.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** 只读 AST 展示模型，字段角色保留真实父子关系。 */
public final class AstTreeNode {
    public final String id, role, type, summary;
    public final int pc, originalLine, sourceLine;
    public final List<AstTreeNode> children;

    public AstTreeNode(String id, String role, String type, String summary, int pc,
                       int originalLine, int sourceLine, List<AstTreeNode> children) {
        this.id = id; this.role = role; this.type = type; this.summary = summary;
        this.pc = pc; this.originalLine = originalLine; this.sourceLine = sourceLine;
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    /** 搜索所有页，保留匹配节点的祖先；匹配父节点时保留完整子树。 */
    public AstTreeNode filter(String query) {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty() || (role + " " + type + " " + summary).toLowerCase(Locale.ROOT).contains(needle)) return this;
        List<AstTreeNode> matches = new ArrayList<>();
        for (AstTreeNode child : children) {
            AstTreeNode match = child.filter(query);
            if (match != null) matches.add(match);
        }
        return matches.isEmpty() ? null : new AstTreeNode(id, role, type, summary, pc, originalLine, sourceLine, matches);
    }

    @Override public String toString() {
        String compact = summary.replace('\n', ' ').replace('\r', ' ');
        if (compact.length() > 130) compact = compact.substring(0, 127) + "...";
        return (role.isEmpty() ? "" : role + ": ") + type + (compact.isEmpty() ? "" : " · " + compact);
    }
}
