package com.grelobites.romgenerator.handlers.dandanatormini;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlotZeroV5 extends SlotZeroBase implements SlotZero {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlotZeroV5.class);

    public SlotZeroV5(byte[] data) {
        super(data);
    }

    @Override
    public boolean validate() {
        try {
            return getMajorVersion() == 5;
        } catch (Exception e) {
            LOGGER.debug("Validation failed", e);
            return false;
        }
    }

    @Override
    public DandanatorMiniImporter getImporter() {
        return new DandanatorMiniV5Importer();
    }

}
