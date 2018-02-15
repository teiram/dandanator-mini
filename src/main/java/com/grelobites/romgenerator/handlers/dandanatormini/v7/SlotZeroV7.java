package com.grelobites.romgenerator.handlers.dandanatormini.v7;

import com.grelobites.romgenerator.handlers.dandanatormini.v6.SlotZeroV6;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlotZeroV7 extends SlotZeroV6 {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlotZeroV6.class);

    public SlotZeroV7(byte[] data) {
        super(data);
    }

    @Override
    public boolean validate() {
        try {
            return getMajorVersion() == 7;
        } catch (Exception e) {
            LOGGER.debug("Validation failed", e);
            return false;
        }
    }
}
