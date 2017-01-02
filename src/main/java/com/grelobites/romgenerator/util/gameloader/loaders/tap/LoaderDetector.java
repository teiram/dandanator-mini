package com.grelobites.romgenerator.util.gameloader.loaders.tap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoaderDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoaderDetector.class);
    private long lastTstates = 0;
    private int lastB = 0;
    private final Tape tape;
    private final Clock clock;
    private int successiveReads;

    public LoaderDetector(Tape tape) {
        this.tape = tape;
        this.clock = Clock.getInstance();
    }

    public void reset() {
        lastTstates = clock.getTstates();
        lastB = 0;
    }

    public void onAudioInput(Z80 processor) {
        long tstatesDelta = clock.getTstates() - lastTstates;
        int bdiff = (processor.getRegB() - lastB) & 0xff;

        //LOGGER.debug("On audio detector with bdiff " + bdiff + " and tstatesDelta " + tstatesDelta);
        if (tape.isPlaying()) {
            if (tstatesDelta > 1000 ||
                    (bdiff != 1 && bdiff != 0 && bdiff != 0xff)) {
                successiveReads++;
                if (successiveReads >= 2) {
                    LOGGER.debug("LoaderDetector stops tape " + tape
                            + " on tstatesDelta "
                            + tstatesDelta + ", bdiff " + bdiff);
                    tape.stop();
                }
            } else {
                successiveReads = 0;
            }
        } else {
            if (tstatesDelta <= 500 && (bdiff == 1 || bdiff == 0xff)) {
                successiveReads++;
                if (successiveReads >= 10) {
                    LOGGER.debug("LoaderDetector starts tape " + tape
                            + " on tstatesDelta "
                            + tstatesDelta + ", bdiff " + bdiff);
                    tape.play();
                }
            } else {
                successiveReads = 0;
            }
        }
        lastB = processor.getRegB();
        lastTstates = clock.getTstates();
    }

}
