package com.grelobites.romgenerator.zxspectrum;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.zxspectrum.spectrum.Spectrum48K;
import javafx.animation.AnimationTimer;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class EmulatorInstance {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmulatorInstance.class);
    private static final long DEFAULT_UPDATE_SCREEN_RATE = 100;
    private Spectrum48K spectrumPeripheral;
    private Z80VirtualMachine currentVm;
    private Thread emulatorThread;
    private AnimationTimer screenUpdateService;
    private long updateScreenRate = DEFAULT_UPDATE_SCREEN_RATE;

    private InputStream tapStream;

    private Z80VirtualMachine newEmulator() throws Exception {
        currentVm = new Z80VirtualMachine();
        currentVm.setMhz(50);
        //currentVm.setTrapInput(true);
        //currentVm.setTrapOutput(true);
        currentVm.addPeripheral(spectrumPeripheral);
        currentVm.load(EmulatorInstance.class.getResourceAsStream("/48.rom"), 0);

        /*
        currentVm.addInterceptor(0x556, z80 -> {
            LOGGER.debug("Intercepted LD-BYTES. " + z80.dumpStatus());
            LOGGER.debug("Load block of " + z80.DE() + " bytes at " + z80.IX);
            if (tapStream != null) {
                int address = z80.IX;
                try {
                    tapStream.skip(3);
                    for (int i = 0; i < z80.DE(); i++) {
                        z80.pokeb(address++, tapStream.read());
                    }
                    tapStream.skip(1);
                    LOGGER.debug("After interception: " + z80.dumpStatus());
                    LOGGER.debug("Available: " + tapStream.available());
                    z80.F |= Z80.CF;
                    if (tapStream.available() == 0) {
                        saveSna(z80);
                    }
                    z80.setPC(z80.pop());

                } catch (Exception e) {
                    LOGGER.error("Loading tap", e);
                }
            }
        });
        */

        return currentVm;
    }

    public void saveSna(Z80 z80) {
        try (FileOutputStream fos = new FileOutputStream("/Users/mteira/Desktop/output.tap")) {
            fos.write(
                    ByteBuffer.allocate(49179)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .put(Integer.valueOf(z80.I).byteValue())
                            .putShort(Integer.valueOf(z80.HL1()).shortValue())
                            .putShort(Integer.valueOf(z80.DE1()).shortValue())
                            .putShort(Integer.valueOf(z80.BC1()).shortValue())
                            .putShort(Integer.valueOf(z80.AF1()).shortValue())
                            .putShort(Integer.valueOf(z80.HL()).shortValue())
                            .putShort(Integer.valueOf(z80.DE()).shortValue())
                            .putShort(Integer.valueOf(z80.BC()).shortValue())
                            .putShort(Integer.valueOf(z80.IY).shortValue())
                            .putShort(Integer.valueOf(z80.IX).shortValue())
                            .put(Integer.valueOf(z80.IM).byteValue())
                            .put(Integer.valueOf(z80.R).byteValue())
                            .putShort(Integer.valueOf(z80.AF()).shortValue())
                            .putShort(Integer.valueOf(z80.SP).shortValue())
                            .put(Integer.valueOf(z80.I_Vector).byteValue())
                            .put(Integer.valueOf(0).byteValue())
                            .put(Arrays.copyOfRange(spectrumPeripheral.getMemory(), 0x4000, 0xffff))
                            .array());
        } catch (Exception e) {
            LOGGER.error("Writing SNA", e);
        }
    }

    public EmulatorInstance() throws Exception {
        spectrumPeripheral = new Spectrum48K();
    }

    public void setTapStream(InputStream tapStream) {
        this.tapStream = tapStream;
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

    public void playTap(InputStream tapStream) throws IOException {
        spectrumPeripheral.play(tapStream);
    }

    public void stopTap() {
        spectrumPeripheral.stop();
    }

    public void loadGame(Game game) {
        LOGGER.debug("Loading Game in emulator");
        try {
            currentVm.pause();
            /*
            spectrumPeripheral.getZxSnapshot()
                    .loadSNA(game.getName(), game.getDataStream());
                    */
            currentVm.resume();
        } catch (Exception e) {
            LOGGER.error("Loading Snapshot", e);
            currentVm.reset();
        }
    }
}
