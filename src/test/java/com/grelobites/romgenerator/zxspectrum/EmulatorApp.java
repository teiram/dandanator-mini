package com.grelobites.romgenerator.zxspectrum;

import com.grelobites.romgenerator.Constants;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;

import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class EmulatorApp extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmulatorApp.class);
    private EmulatorInstance emulatorInstance;

    public static void main(String[] args) {
        launch(args);
    }

    private MenuItem playTap(Scene scene) {
        MenuItem playTap = new MenuItem("Play TAP...");

        playTap.setOnAction(f -> {
            try {
                FileChooser fileChooser = new FileChooser();
                final File tapFile = fileChooser.showOpenDialog(scene.getWindow());
                if (tapFile != null) {
                    try {
                        emulatorInstance.playTap(new FileInputStream(tapFile));
                    } catch (IOException e) {
                        LOGGER.error("Playing tap", e);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Selecting emulated TAP", e);
            }
        });
        return playTap;
    }

    private MenuItem stopTap() {
        MenuItem stopTap = new MenuItem("Stop TAP");
        stopTap.setOnAction(f -> {
            try {
                emulatorInstance.stopTap();
            } catch (Exception e) {
                LOGGER.error("Stopping emulated TAP", e);
            }
        });
        return stopTap;
    }

    private void populateMenuBar(MenuBar menuBar, Scene scene) {
        Menu fileMenu = new Menu("File");

        fileMenu.getItems().addAll(
                playTap(scene),
                stopTap());

        menuBar.getMenus().addAll(fileMenu);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        BorderPane mainPane = new BorderPane();
        AnchorPane emulatorPane = new AnchorPane();
        mainPane.setCenter(emulatorPane);
        Scene scene = new Scene(mainPane, Constants.SPECTRUM_SCREEN_WIDTH, Constants.SPECTRUM_SCREEN_HEIGHT + 32);
        primaryStage.setScene(scene);
        emulatorInstance = new EmulatorInstance();
        MenuBar menuBar = new MenuBar();
        populateMenuBar(menuBar, scene);
        mainPane.setTop(menuBar);
        emulatorInstance.start(emulatorPane);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}
