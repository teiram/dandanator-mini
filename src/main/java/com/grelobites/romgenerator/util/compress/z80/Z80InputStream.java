package com.grelobites.romgenerator.util.compress.z80;

import java.io.IOException;
import java.io.InputStream;

public class Z80InputStream extends InputStream {

    private InputStream source;

    private int cachedValue = 0;
    private int cachedCount = 0;

    private int sourceLimit = Integer.MAX_VALUE;

    private static final int COMPRESS_MARK = 0xED;

    private boolean isSourceEof() throws SourceStreamEOFException {
        return --sourceLimit < 0;
    }

    private int readNextValue() throws SourceStreamEOFException, IOException {
        if (!isSourceEof()) {
            int value = source.read();
            if (value < 0) {
                throw new SourceStreamEOFException();
            } else {
                return value;
            }
        } else {
            return -1;
        }
    }

    private int getNextValue() throws IOException {
        try {
            int value = readNextValue();
            if (value == COMPRESS_MARK) {
                int nextValue = readNextValue();
                if (nextValue == COMPRESS_MARK) {
                    cachedCount = readNextValue() - 1;
                    if (cachedCount < 0) {
                        return -1;
                    }
                    cachedValue = readNextValue();
                    return cachedValue;
                } else if (nextValue == -1) {
                    return value;
                } else {
                    cachedCount = 1;
                    cachedValue = nextValue;
                    return value;
                }
            } else {
                return value;
            }
        } catch (SourceStreamEOFException ssee) {
            return -1;
        }
    }
    public Z80InputStream(InputStream source) {
        this.source = source;
    }

    public Z80InputStream(InputStream source, int sourceLimit) {
        this.source = source;
        this.sourceLimit = sourceLimit;
    }

    @Override
    public int read() throws IOException {
        if (cachedCount > 0) {
            cachedCount--;
            return cachedValue;
        } else {
            return getNextValue();
        }
    }

    private static class SourceStreamEOFException extends Exception {}
}
