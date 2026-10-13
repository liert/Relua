package com.github.relua.gui.preferences;

/** 字体设置应用到 RichTextFX 区域及其行号，保持语法样式和折叠样式独立。 */
public final class EditorAppearance {
    private EditorAppearance() {}

    public static String fontStyle(AppSettings settings) {
        String family = settings.getFontFamily().replace("\\", "\\\\").replace("\"", "\\\"");
        return "-fx-font-family: \"" + family + "\", Consolas, monospace; -fx-font-size: "
                + settings.getFontSize() + "px;";
    }
}
