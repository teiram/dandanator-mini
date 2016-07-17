package com.grelobites.dandanator.util.emulator.zxspectrum;

import com.grelobites.dandanator.model.Game;
import com.grelobites.dandanator.util.emulator.zxspectrum.spectrum.Spectrum48K;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmulatorInstance {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmulatorInstance.class);
    private static final long DEFAULT_UPDATE_SCREEN_RATE = 100;
    private J80 cpu;
    private Spectrum48K spectrumPeripheral;
    private Thread emulatorThread;
    private ScheduledService<Void> screenUpdateService;
    private long updateScreenRate = DEFAULT_UPDATE_SCREEN_RATE;

    public EmulatorInstance() throws Exception {
        cpu = new J80();
        spectrumPeripheral = new Spectrum48K();
        cpu.addPeripheral(spectrumPeripheral);
        cpu.load(EmulatorInstance.class.getResourceAsStream("/spectrum.rom"), 0);
    }

    public void setUpdateScreenRate(long updateScreenRate) {
        this.updateScreenRate = updateScreenRate;
    }

    public void start(ImageView imageView) {
        emulatorThread = new Thread(() -> {
            try {
                cpu.init();
                cpu.start();
            } catch (Exception e) {
                LOGGER.error("Initializing emulator", e);
            }
        });
        emulatorThread.start();

        screenUpdateService = new ScheduledService<Void>() {
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    protected Void call() {
                        imageView.setImage(spectrumPeripheral.getScreen().nextFrame());
                        return null;
                    }
                };
            }
        };
        screenUpdateService.setPeriod(Duration.millis(updateScreenRate));
        screenUpdateService.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOGGER.debug("Emulator thread interrupted!");
        }
    }

    public void pause() {
        LOGGER.debug("Pausing emulator");
        cpu.pause();
    }

    public void resume() {
        LOGGER.debug("Resuming emulator");
        cpu.resume();
    }

    public void reset() {
        LOGGER.debug("Resetting emulator");
        cpu.reset();
    }

    public void stop() {
        cpu.terminate();
        screenUpdateService.cancel();
        try {
            emulatorThread.join();
        } catch (Exception e) {
            LOGGER.error("Waiting for emulator to stop", e);
        }
    }

    public void loadGame(Game game) {
        LOGGER.debug("Loading Game in emulator");
        try {
            pause();
            spectrumPeripheral.getZxSnapshot()
                    .loadSNA(game.getName(), game.getDataStream());
            resume();
        } catch (Exception e) {
            LOGGER.error("Loading Snapshot", e);
            reset();
        }
    }
}
