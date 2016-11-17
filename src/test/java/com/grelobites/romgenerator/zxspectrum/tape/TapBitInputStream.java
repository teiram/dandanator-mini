package com.grelobites.romgenerator.zxspectrum.tape;

import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TapBitInputStream implements BitInputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapBitInputStream.class);

    private InputStream tapData;
    private BitInputStream currentBlock;

    public TapBitInputStream(InputStream tapData) throws IOException {
        this.tapData = tapData;
    }

    private boolean setupNextBlock() throws IOException {
        int length = Util.readAsLittleEndian(tapData);
        LOGGER.debug("Next block length " + length);
        if (length > 0) {
            currentBlock = new CompositeBitInputStream(
                    new HeaderBitInputStream(),
                    new BlockBitInputStream(new ByteArrayInputStream(Util
                    .fromInputStream(tapData, length))));
            return true;
        } else {
            currentBlock = null;
            return false;
        }
    }

    @Override
    public int read() {
        try {
            do {
                if (currentBlock != null) {
                    int value = currentBlock.read();
                    if (value != -1) {
                        return value;
                    }
                }
            } while (setupNextBlock());
        } catch (Exception e) {
            LOGGER.debug("During read", e);
        }
        return -1;
    }

    @Override
    public int skip(int value) {
        if (value <= 0) {
            return value;
        }
        int remaining = value;
        try {
            do {
                if (currentBlock != null) {
                    int delta = currentBlock.skip(remaining);
                    if (delta > 0) {
                        remaining -= delta;
                    } else {
                        if (!setupNextBlock()) break;
                    }
                } else {
                    if (!setupNextBlock()) break;
                }
            } while (remaining > 0);
        } catch (Exception e) {
            LOGGER.debug("In skip", e);
        }
        return value - remaining;
    }
}
