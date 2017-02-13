package com.grelobites.romgenerator.util.gameloader.loaders.tap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Keyboard {

    private static final Logger LOGGER = LoggerFactory.getLogger(Keyboard.class);
    private static class KeyPress {
        public final long timeout;
        public final Key key;
        public KeyPress(Key key, long timeout) {
            this.timeout = timeout;
            this.key = key;
        }
    }
    /*
     IN:    Reads keys (bit 0 to bit 4 inclusive)

      0xfefe  SHIFT, Z, X, C, V            0xeffe  0, 9, 8, 7, 6
      0xfdfe  A, S, D, F, G                0xdffe  P, O, I, U, Y
      0xfbfe  Q, W, E, R, T                0xbffe  ENTER, L, K, J, H
      0xf7fe  1, 2, 3, 4, 5                0x7ffe  SPACE, SYM SHFT, M, N, B
     */
    private static final int KEY_QUEUE_SIZE = 3;
    private final Clock clock;
    private KeyPress[] keyPresses = new KeyPress[KEY_QUEUE_SIZE];
    private int currentKeyPress = 0;

    public Keyboard(Clock clock) {
        this.clock = clock;
    }

    public void pressKey(int durationMillis, Key ... keys) {
        LOGGER.debug("Added keypress on clock " + clock.getTstates());
        for (Key key: keys) {
            keyPresses[currentKeyPress % KEY_QUEUE_SIZE] = new KeyPress(key,
                    clock.getTstates() + durationMillis * 3500);
            currentKeyPress++;
        }
    }

    public int getUlaBits(int address) {
        long tstates = clock.getTstates();
        int ulaValue = 0;
        for (int i = 0; i < KEY_QUEUE_SIZE; i++) {
            KeyPress keyPress = keyPresses[i];
            if (keyPress != null) {
                if (keyPress.timeout < tstates) {
                    LOGGER.debug("Removing keypress by timeout. duration was: "
                     + keyPress.timeout + " and tstates are " + tstates);
                    keyPresses[i] = null;
                } else if (keyPress.key.respondsTo(address)) {
                    ulaValue |= keyPress.key.mask;
                }
            }
        }
        if (ulaValue != 0) {
            LOGGER.debug("ULA keyboard Bits for " + Integer.toHexString(address) + "="
                    + Integer.toHexString(ulaValue));
        }
        return ~ulaValue;
    }
}
