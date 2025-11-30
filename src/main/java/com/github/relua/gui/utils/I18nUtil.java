package com.github.relua.gui.utils;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 国际化工具类，用于加载和管理国际化资源
 */
public class I18nUtil {
    // 默认语言
    private static final Locale DEFAULT_LOCALE = Locale.CHINA;
    // 资源包基础名称
    private static final String BUNDLE_BASE_NAME = "i18n.messages";
    
    // 当前资源包
    private static ResourceBundle resourceBundle;
    // 当前语言
    private static Locale currentLocale;
    
    static {
        // 初始化国际化资源
        initialize(DEFAULT_LOCALE);
    }
    
    /**
     * 初始化国际化资源
     * @param locale 语言环境
     */
    public static void initialize(Locale locale) {
        currentLocale = locale;
        resourceBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale);
    }
    
    /**
     * 获取当前语言环境
     * @return 当前语言环境
     */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }
    
    /**
     * 获取国际化文本
     * @param key 文本键
     * @return 国际化文本
     */
    public static String getString(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (Exception e) {
            // 如果找不到对应的文本，返回键名作为默认值
            return key;
        }
    }
    
    /**
     * 获取带占位符的国际化文本
     * @param key 文本键
     * @param args 占位符参数
     * @return 格式化后的国际化文本
     */
    public static String getString(String key, Object... args) {
        String pattern = getString(key);
        return String.format(pattern, args);
    }
    
    /**
     * 切换语言
     * @param locale 新的语言环境
     */
    public static void switchLocale(Locale locale) {
        initialize(locale);
    }
    
    /**
     * 切换到中文
     */
    public static void switchToChinese() {
        switchLocale(Locale.CHINA);
    }
    
    /**
     * 切换到英文
     */
    public static void switchToEnglish() {
        switchLocale(Locale.ENGLISH);
    }
}