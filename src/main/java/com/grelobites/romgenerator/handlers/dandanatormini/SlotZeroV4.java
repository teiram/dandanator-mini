package com.grelobites.romgenerator.handlers.dandanatormini;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlotZeroV4 extends SlotZeroBase implements SlotZero {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlotZeroV4.class);

    public SlotZeroV4(byte[] data) {
        super(data);
    }

    @Override
    public boolean validate() {
        LOGGER.debug("Validating RomSet");
        try {
            return getMajorVersion() == 4;
        } catch (Exception e) {
            LOGGER.debug("Validation failed", e);
            return false;
        }
    }

    @Override
    public DandanatorMiniImporter getImporter() {
        return new DandanatorMiniV4Importer();
    }

}
