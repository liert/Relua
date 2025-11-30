package com.github.relua.gui;

import com.github.relua.gui.utils.I18nUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
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
        
        // 设置场景
        Scene scene = new Scene(root, 800, 600);
        
        // 设置主舞台
        primaryStage.setTitle("Relua - Lua字节码反编译器");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}