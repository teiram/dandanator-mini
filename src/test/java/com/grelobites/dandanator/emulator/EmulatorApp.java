package com.grelobites.dandanator.emulator;


import com.grelobites.dandanator.util.emulator.zxspectrum.J80;
import com.grelobites.dandanator.util.emulator.zxspectrum.spectrum.Spectrum48K;
import javafx.application.Application;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmulatorApp extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmulatorApp.class);

    @Override
    public void start(Stage primaryStage) throws Exception {


        final J80 cpu = new J80();
        Spectrum48K spectrum = new Spectrum48K();
        cpu.addPeripheral(spectrum);
        cpu.load(EmulatorTest.class.getResourceAsStream("/spectrum.rom"), 0);
        final ImageView imageView = new ImageView();

        new Thread(() -> {
            try {
                cpu.init();
                LOGGER.debug("CPU Initialized");
                cpu.start();
                LOGGER.debug("CPU started");
                LOGGER.debug("Image set");
            } catch (Exception e) {
                LOGGER.error("Initializing emulator", e);
            }
        }).start();
        LOGGER.debug("Started CPU initialization thread");

        ScheduledService<Void> svc = new ScheduledService<Void>() {
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    protected Void call() {
                        imageView.setImage(spectrum.getScreen().nextFrame());
                        return null;
                    }
                };
            }
        };
        svc.setPeriod(Duration.millis(100));
        svc.start();

        Thread.sleep(1000L);
        spectrum.getZxSnapshot().loadSNA("Kamikaze",
                EmulatorTest.class.getResourceAsStream("/kamikaze.sna"));

        BorderPane pane = new BorderPane();
        pane.setCenter(imageView);

        Scene scene = new Scene(pane);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {

        launch(args);
    }
}
