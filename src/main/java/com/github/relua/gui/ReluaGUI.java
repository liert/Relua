package com.github.relua.gui;

import com.github.relua.gui.controllers.MainController;
import com.github.relua.gui.theme.ThemeManager;
import com.github.relua.gui.theme.WindowsTitleBarUtil;
import com.github.relua.gui.utils.I18nUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Relua GUI应用程序主类
 */
public class ReluaGUI extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // 初始化国际化支持
        I18nUtil.initialize(Locale.CHINA);

        // 加载主界面FXML文件
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Parent root = loader.load();

        // 打开命令行传入的文件/文件夹
        MainController controller = loader.getController();
        if (controller != null) {
            List<File> startupFiles = new ArrayList<>();
            for (String path : getParameters().getRaw()) {
                startupFiles.add(new File(path));
            }
            controller.openStartupFiles(startupFiles);
        }

        // 设置场景（采用适合现代屏幕的舒适尺寸）
        Scene scene = new Scene(root, 1100, 720);

        // 应用现代化设计主题系统（默认专业暗色，支持偏好持久化）
        ThemeManager.getInstance().applyTheme(scene);

        // 设置主舞台
        primaryStage.setTitle("Relua - Lua 字节码反编译与逆向分析套件");
        primaryStage.setMinWidth(850);
        primaryStage.setMinHeight(550);
        primaryStage.setScene(scene);
        primaryStage.show();

        // 消除 Windows 原生亮蓝色边框，启用沉浸式暗色标题栏与边框自适应
        WindowsTitleBarUtil.applyTitleBarTheme(primaryStage, ThemeManager.getInstance().isDark());
        ThemeManager.getInstance().addListener(newTheme -> {
            WindowsTitleBarUtil.applyTitleBarTheme(primaryStage, newTheme == ThemeManager.Theme.DARK);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}