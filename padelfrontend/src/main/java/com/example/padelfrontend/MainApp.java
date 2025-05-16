package com.example.padelfrontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ManagerPage.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setScene(scene);
        stage.setTitle("Tennis Club Login");
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");// Full-screen mode
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}