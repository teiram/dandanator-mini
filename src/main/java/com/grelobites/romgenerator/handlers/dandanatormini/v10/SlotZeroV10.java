package com.grelobites.romgenerator.handlers.dandanatormini.v10;

import com.grelobites.romgenerator.handlers.dandanatormini.v9.SlotZeroV9;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlotZeroV10 extends SlotZeroV9 {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlotZeroV10.class);

    public SlotZeroV10(byte[] data) {
        super(data);
    }

    @Override
    public boolean validate() {
        try {
            return getMajorVersion() == 10;
        } catch (Exception e) {
            LOGGER.debug("Validation failed", e);
            return false;
        }
    }

}
