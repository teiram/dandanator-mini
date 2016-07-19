package com.grelobites.dandanator.util.emulator.zxspectrum.spectrum;

import com.grelobites.dandanator.util.emulator.zxspectrum.InputPort;
import com.grelobites.dandanator.util.emulator.zxspectrum.Peripheral;
import com.grelobites.dandanator.util.emulator.zxspectrum.Z80VirtualMachine;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class FxKeyboard implements InputPort, Peripheral {
    private static final Logger LOGGER = LoggerFactory.getLogger(FxKeyboard.class);
    private static final int RESET_VALUE = 0xff;

    private static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new SimpleEntry<>(key, value);
    }

    private static int row(int value) {
        return (value - 1) << 3;
    }
    
    private static int column(int value) {
        return value - 1;
    }
    
    /**
     * Spectrum keyboard mapped to JavaFx keycodes
     */
    private static Map<KeyCode, Integer> KEYCODE_MAP = Collections.unmodifiableMap(Stream.of(
            entry(KeyCode.SHIFT, row(1) + column(1)),
            entry(KeyCode.Z, row(1) + column(2)),
            entry(KeyCode.X, row(1) + column(3)),
            entry(KeyCode.C, row(1) + column(4)),
            entry(KeyCode.V, row(1) + column(5)),

            entry(KeyCode.A, row(2) + column(1)),
            entry(KeyCode.S, row(2) + column(2)),
            entry(KeyCode.D, row(2) + column(3)),
            entry(KeyCode.F, row(2) + column(4)),
            entry(KeyCode.G, row(2) + column(5)),

            entry(KeyCode.Q, row(3) + column(1)),
            entry(KeyCode.W, row(3) + column(2)),
            entry(KeyCode.E, row(3) + column(3)),
            entry(KeyCode.R, row(3) + column(4)),
            entry(KeyCode.T, row(3) + column(5)),

            entry(KeyCode.DIGIT1, row(4) + column(1)),
            entry(KeyCode.DIGIT2, row(4) + column(2)),
            entry(KeyCode.DIGIT3, row(4) + column(3)),
            entry(KeyCode.DIGIT4, row(4) + column(4)),
            entry(KeyCode.DIGIT5, row(4) + column(5)),

            entry(KeyCode.DIGIT0, row(5) + column(1)),
            entry(KeyCode.DIGIT9, row(5) + column(2)),
            entry(KeyCode.DIGIT8, row(5) + column(3)),
            entry(KeyCode.DIGIT7, row(5) + column(4)),
            entry(KeyCode.DIGIT6, row(5) + column(5)),

            entry(KeyCode.P, row(6) + column(1)),
            entry(KeyCode.O, row(6) + column(2)),
            entry(KeyCode.I, row(6) + column(3)),
            entry(KeyCode.U, row(6) + column(4)),
            entry(KeyCode.Y, row(6) + column(5)),

            entry(KeyCode.ENTER, row(7) + column(1)),
            entry(KeyCode.L, row(7) + column(2)),
            entry(KeyCode.K, row(7) + column(3)),
            entry(KeyCode.J, row(7) + column(4)),
            entry(KeyCode.H, row(7) + column(5)),

            entry(KeyCode.SPACE, row(8) + column(1)),
            entry(KeyCode.CONTROL, row(8) + column(2)),
            entry(KeyCode.M, row(8) + column(3)),
            entry(KeyCode.N, row(8) + column(4)),
            entry(KeyCode.B, row(8) + column(5))
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

    private int keystate[] = new int[8];

    public FxKeyboard() {
        reset();
    }

    public void reset() {
        for (int i = 0; i < keystate.length; i++) {
            keystate[i] = RESET_VALUE;
        }
    }

    public void attachToScene(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyPressedEventHandler);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, keyReleasedEventHandler);
    }

    public void detachFromScene(Scene scene) {
        scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyPressedEventHandler);
        scene.removeEventFilter(KeyEvent.KEY_RELEASED, keyReleasedEventHandler);
    }

    private EventHandler<KeyEvent> keyPressedEventHandler = event ->
            doKey(event.getCode(), true);

    private EventHandler<KeyEvent> keyReleasedEventHandler = event ->
            doKey(event.getCode(), false);

    private void doKey(KeyCode code, boolean pressed) {
        LOGGER.debug("doKey code: " + code + ", pressed: " + pressed);
        Optional.ofNullable(KEYCODE_MAP.get(code)).ifPresent(it -> {
            if (pressed) {
                keystate[it >> 3] &= (1 << (it & 0x7));
            } else {
                keystate[it >> 3] |= (1 << (it & 0x07));
            }
        });
    }

    @Override
    public void onCpuReset(Z80VirtualMachine cpu) {
        reset();
    }

    @Override
    public void bind(Z80VirtualMachine cpu) {
        cpu.addInPort(SpectrumConstants.KEYBOARD_PORT, this);
    }

    @Override
    public void unbind(Z80VirtualMachine cpu) {}

    @Override
    public int inb(int port, int hi) {
        int result = 0xff;

        port &= 0xff;
        port |= (hi & 0xff) << 8;

        for (int i = 0; i < 8; i++) {
            if ((port & (0x100 << i)) == 0) {
                result &= keystate[i];
            }
        }
        return result;
    }

}
