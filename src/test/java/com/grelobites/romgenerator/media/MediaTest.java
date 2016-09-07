package com.grelobites.romgenerator.media;

import com.grelobites.romgenerator.util.Util;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MediaTest extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaTest.class);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        File file = new File("/Users/mteira/Desktop/test.wav");
        FileOutputStream fos = new FileOutputStream(file);
        CompressedWavOutputStream wos = new CompressedWavOutputStream(fos, WavOutputFormat.defaultDataFormat());
        wos.write(Util.fromInputStream(new FileInputStream("/Users/mteira/Desktop/ifrom.rom")));
        wos.close();
        fos.close();
        LOGGER.debug("Creating media from " + file.toURI().toURL().toExternalForm());

        Media media = new Media(file.toURI().toURL().toExternalForm());
        media.errorProperty().addListener(
                (observable, oldValue, newValue) -> {
            LOGGER.error("Media exception happened ", newValue);
        });
        MediaPlayer player = new MediaPlayer(media);
        player.setOnError(() -> LOGGER.error("Player error: " + player.getError()));
        player.setAutoPlay(true);
        MediaView mediaView = new MediaView(player);
        AnchorPane pane = new AnchorPane();
        Scene scene = new Scene(pane, 200, 200);
        primaryStage.setScene(scene);
        pane.getChildren().add(mediaView);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}
