package com.grelobites.dandanator.util.emulator.zxspectrum;

import com.grelobites.dandanator.model.Game;
import com.grelobites.dandanator.util.emulator.zxspectrum.spectrum.Spectrum48K;
import javafx.animation.AnimationTimer;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmulatorInstance {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmulatorInstance.class);
    private static final long DEFAULT_UPDATE_SCREEN_RATE = 100;
    private Spectrum48K spectrumPeripheral;
    private Z80VirtualMachine currentVm;
    private Thread emulatorThread;
    private AnimationTimer screenUpdateService;
    private long updateScreenRate = DEFAULT_UPDATE_SCREEN_RATE;

    private Z80VirtualMachine newEmulator() throws Exception {
        currentVm = new Z80VirtualMachine();
        currentVm.addPeripheral(spectrumPeripheral);
        currentVm.load(EmulatorInstance.class.getResourceAsStream("/spectrum.rom"), 0);
        return currentVm;
    }

    public EmulatorInstance() throws Exception {
        spectrumPeripheral = new Spectrum48K();
    }

    public void setUpdateScreenRate(long updateScreenRate) {
        this.updateScreenRate = updateScreenRate;
    }

    public void start(Pane emulatorPane) {
        emulatorThread = new Thread(() -> {
            boolean terminated = false;
            while (!terminated) {
                try {
                    LOGGER.debug("Starting ZX Emulator");
                    newEmulator().run();
                    terminated = true;
                } catch (Exception e) {
                    LOGGER.error("ZXEmulator finished unexpectedly", e);
                }
            }
        }, "ZXEmulatorThread");
        emulatorThread.start();

        screenUpdateService = new AnimationTimer() {
            private long lastUpdate = 0;
            private ImageView lastFrame = null;
            private long updatePeriod = 100;
            @Override
            public void handle(long now) {
                if (now - lastUpdate > updatePeriod) {
                    lastUpdate = now;
                    ImageView nextFrame = spectrumPeripheral.getScreen().nextFrame();
                    if (nextFrame != null) {
                        emulatorPane.getChildren().clear();
                        emulatorPane.getChildren().add(nextFrame);
                        lastFrame = nextFrame;
                    }
                }
            }
        };
        screenUpdateService.start();
        LOGGER.debug("Scene to attach: " + emulatorPane.getScene());
        spectrumPeripheral.getKeyboard()
                .attachToScene(emulatorPane.getScene());
    }

    public void pause() {
        LOGGER.debug("Pausing emulator");
        currentVm.pause();
        screenUpdateService.stop();
    }

    public void resume() {
        LOGGER.debug("Resuming emulator");
        screenUpdateService.start();
        currentVm.resume();
    }

    public void reset() {
        LOGGER.debug("Resetting emulator");
        currentVm.reset();
    }

    public boolean isRunning() {
        return currentVm.isRunning();
    }

    public void stop() {
        currentVm.stop();
        screenUpdateService.stop();
        try {
            emulatorThread.join();
        } catch (Exception e) {
            LOGGER.error("Waiting for emulator to stop", e);
        }
    }

    public void loadGame(Game game) {
        LOGGER.debug("Loading Game in emulator");
        try {
            currentVm.pause();
            spectrumPeripheral.getZxSnapshot()
                    .loadSNA(game.getName(), game.getDataStream());
            currentVm.resume();
        } catch (Exception e) {
            LOGGER.error("Loading Snapshot", e);
            currentVm.reset();
        }
    }
}
