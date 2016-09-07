package com.grelobites.romgenerator.handlers.dandanatormini.model;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class SlotZeroBase implements SlotZero {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlotZeroBase.class);

    private byte[] data;

    public SlotZeroBase(byte[] data) {
        this.data = data;
    }

    @Override
    public InputStream data() {
        return new ByteArrayInputStream(data);
    }

    protected int getMajorVersion() throws IOException {

        String majorVersion = getVersion();
        int index;
        if ((index = majorVersion.indexOf('.')) > -1) {
            majorVersion = majorVersion.substring(0, index);
        }
        LOGGER.debug("Version of romset detected as " + majorVersion);
        return Integer.parseInt(majorVersion);
    }

    protected String getVersion() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(data, Constants.SLOT_SIZE - DandanatorMiniConstants.VERSION_SIZE,
                DandanatorMiniConstants.VERSION_SIZE);

        //Skip the starting 'v' of the version string
        return Util.getNullTerminatedString(stream, 1, DandanatorMiniConstants.VERSION_SIZE);
    }

    @Override
    public boolean getDisableBorderEffect() {
        return false;
    }
}
