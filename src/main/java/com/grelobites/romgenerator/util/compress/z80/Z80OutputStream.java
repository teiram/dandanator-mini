package com.grelobites.romgenerator.util.compress.z80;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Z80OutputStream extends FilterOutputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z80OutputStream.class);

    private static final int COMPRESS_MARK = 0xED;
    private static final int VALUE_MASK = 0xff;
    private int cachedValue;
    private int cachedValueCount;
    private boolean writeEndMark = false;
    private boolean endMarkWritten = false;

    public Z80OutputStream(OutputStream out) {
        super(out);
    }

    public Z80OutputStream(OutputStream out, boolean writeEndMark) {
        this(out);
        this.writeEndMark = writeEndMark;
    }

    private void writeEndMark() throws IOException {
        if (writeEndMark && !endMarkWritten) {
            out.write(0);
            out.write(COMPRESS_MARK);
            out.write(COMPRESS_MARK);
            out.write(0);
            endMarkWritten = true;
        }
    }

    private void flushCached() throws IOException {
        if (cachedValueCount > (cachedValue == COMPRESS_MARK ? 1 : 4)) {
            out.write(COMPRESS_MARK);
            out.write(COMPRESS_MARK);
            out.write(cachedValueCount);
            out.write(cachedValue);
        } else {
            while (cachedValueCount-- > 0) {
                out.write(cachedValue);
            }
        }
        cachedValueCount = 0;
    }

    private boolean wasSimpleMark(int b) {
        return cachedValue == COMPRESS_MARK &&
                cachedValueCount == 1 &&
                b != COMPRESS_MARK;
    }

    @Override
    public void write(int b) throws IOException {
        b &= VALUE_MASK;
        if (wasSimpleMark(b)) {
            out.write(COMPRESS_MARK);
            out.write(b);
            cachedValueCount = 0;
        } else {
            if (cachedValueCount > 0) {
                if (cachedValue == b) {
                    cachedValueCount++;
                    if (cachedValueCount == 255) {
                        flushCached();
                    }
                } else {
                    flushCached();
                    cachedValue = b;
                    cachedValueCount = 1;
                }
            } else {
                cachedValue = b;
                cachedValueCount = 1;
            }
        }
    }

    @Override
    public void flush() throws IOException {
        flushCached();
        writeEndMark();
        super.flush();
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
