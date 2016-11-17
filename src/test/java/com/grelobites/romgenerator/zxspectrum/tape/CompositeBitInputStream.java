package com.grelobites.romgenerator.zxspectrum.tape;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeBitInputStream implements BitInputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeBitInputStream.class);

    private BitInputStream[] streams;
    int streamIndex;

    public CompositeBitInputStream(BitInputStream ...streams) {
        this.streams = streams;
        this.streamIndex = 0;
    }

    @Override
    public int read() {
        while (streamIndex < streams.length) {
            int value;
            if ((value = streams[streamIndex].read()) != -1) {
                return value;
            } else {
                streamIndex++;
            }
        }
        return -1;
    }

    @Override
    public int skip(int value) {
        int skipped = 0;
        while (skipped < value && streamIndex < streams.length) {
            skipped += streams[streamIndex].skip(value);
            if (skipped < value) {
                streamIndex++;
            }
        }
        return skipped;
    }
}
