package com.github.relua.gui.theme;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Windows 平台原生窗口装饰与 DWM 属性增强工具：
 * 通过 Windows 桌面窗口管理器 (DWM) API 消除原生亮蓝色边框，启用沉浸式暗色标题栏与边框自适应配色
 */
public class WindowsTitleBarUtil {

    private static final Logger LOGGER = Logger.getLogger(WindowsTitleBarUtil.class.getName());

    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");

    // Windows DWM 属性常量
    public static final int DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1 = 19;
    public static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    public static final int DWMWA_BORDER_COLOR = 34;
    public static final int DWMWA_CAPTION_COLOR = 35;
    public static final int DWMWA_TEXT_COLOR = 36;

    /**
     * DWM API JNA 映射接口
     */
    public interface Dwmapi extends StdCallLibrary {
        Dwmapi INSTANCE = Native.load("dwmapi", Dwmapi.class, W32APIOptions.DEFAULT_OPTIONS);

        int DwmSetWindowAttribute(HWND hwnd, int dwAttribute, Pointer pvAttribute, int cbAttribute);
    }

    /**
     * 将指定 JavaFX Stage 的系统标题栏和窗口外边框设置为沉浸式暗色或与主题匹配的颜色
     * @param stage 目标窗口舞台
     * @param isDark 是否为暗色模式
     */
    public static void applyTitleBarTheme(Stage stage, boolean isDark) {
        if (!IS_WINDOWS || stage == null) {
            return;
        }

        // 确保在窗口准备就绪或稍后重试以获取准确的 HWND
        Platform.runLater(() -> applyTitleBarThemeInternal(stage, isDark, 0));
    }

    private static void applyTitleBarThemeInternal(Stage stage, boolean isDark, int retryCount) {
        HWND hwnd = getWindowHandle(stage);
        if (hwnd == null) {
            if (retryCount < 5) {
                // 如果初次未取到（例如刚调用 show），稍后重试获取
                Platform.runLater(() -> applyTitleBarThemeInternal(stage, isDark, retryCount + 1));
            }
            return;
        }

        try {
            // 1. 设置沉浸式暗色模式（Windows 10 1809+ / Windows 11）
            setIntDwmAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, isDark ? 1 : 0);
            setIntDwmAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1, isDark ? 1 : 0);

            // 2. 定制窗口外边框颜色（彻底消除亮蓝色系统自带边框，Windows 11 支持）
            // COLORREF 格式为 0x00BBGGRR
            int borderColor = isDark ? rgbToColorRef(0x38, 0x38, 0x38) : rgbToColorRef(0xd1, 0xd5, 0xdb);
            setIntDwmAttribute(hwnd, DWMWA_BORDER_COLOR, borderColor);

            // 3. 定制标题栏背景与文字颜色（Windows 11 支持）
            int captionColor = isDark ? rgbToColorRef(0x2d, 0x2d, 0x30) : rgbToColorRef(0xf8, 0xf9, 0xfa);
            int textColor = isDark ? rgbToColorRef(0xcc, 0xcc, 0xcc) : rgbToColorRef(0x33, 0x33, 0x33);
            setIntDwmAttribute(hwnd, DWMWA_CAPTION_COLOR, captionColor);
            setIntDwmAttribute(hwnd, DWMWA_TEXT_COLOR, textColor);

            // 4. 强制通知系统刷新窗口非客户区框架
            User32.INSTANCE.SetWindowPos(hwnd, null, 0, 0, 0, 0,
                    WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE | WinUser.SWP_NOZORDER | WinUser.SWP_FRAMECHANGED);
        } catch (Throwable t) {
            LOGGER.fine("设置 Windows DWM 标题栏属性失败: " + t.getMessage());
        }
    }

    /**
     * 将 RGB 分量转换为 Windows COLORREF 格式 (0x00BBGGRR)
     */
    private static int rgbToColorRef(int r, int g, int b) {
        return (r & 0xFF) | ((g & 0xFF) << 8) | ((b & 0xFF) << 16);
    }

    /**
     * 向 DWM 传递 4 字节整数属性值
     */
    private static void setIntDwmAttribute(HWND hwnd, int attribute, int value) {
        try {
            Memory mem = new Memory(4);
            mem.setInt(0, value);
            Dwmapi.INSTANCE.DwmSetWindowAttribute(hwnd, attribute, mem, 4);
        } catch (Throwable ignored) {
        }
    }

    /**
     * 获取 JavaFX Stage 对应的 Windows 原生 HWND 句柄（具备多重兜底策略）
     */
    public static HWND getWindowHandle(Stage stage) {
        if (stage == null) {
            return null;
        }

        // 策略 1: 通过 JavaFX 底层 Peer 反射提取 HWND
        try {
            Method getPeerMethod = Window.class.getDeclaredMethod("getPeer");
            getPeerMethod.setAccessible(true);
            Object peer = getPeerMethod.invoke(stage);
            if (peer != null) {
                Method getPlatformWindowMethod = peer.getClass().getMethod("getPlatformWindow");
                Object platformWindow = getPlatformWindowMethod.invoke(peer);
                if (platformWindow != null) {
                    Method getNativeHandleMethod = platformWindow.getClass().getMethod("getNativeHandle");
                    long handle = (Long) getNativeHandleMethod.invoke(platformWindow);
                    if (handle != 0) {
                        return new HWND(new Pointer(handle));
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // 策略 2: 通过 Window 标题精确查找原生窗口句柄
        try {
            String title = stage.getTitle();
            if (title != null && !title.isEmpty()) {
                HWND hwnd = User32.INSTANCE.FindWindow(null, title);
                if (hwnd != null) {
                    return hwnd;
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }
}
