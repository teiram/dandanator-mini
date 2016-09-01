package com.grelobites.romgenerator.util.compress.z80;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Z80OutputStream extends FilterOutputStream {

    private static final int COMPRESS_MARK = 0xED;
    private int cachedValue;
    private int cachedValueCount;

    public Z80OutputStream(OutputStream out) {
        super(out);
    }

    private void writeEndMark() throws IOException {
        out.write(0);
        out.write(COMPRESS_MARK);
        out.write(COMPRESS_MARK);
        out.write(0);
    }

    private void flushCached() throws IOException {
        if (cachedValueCount > 4 || cachedValue == COMPRESS_MARK) {
            out.write(COMPRESS_MARK);
            out.write(COMPRESS_MARK);
            out.write(cachedValueCount);
            out.write(cachedValue);
        } else {
            while (cachedValueCount-- > 0) {
                out.write(cachedValue);
            }
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (cachedValueCount > 0) {
            if (cachedValue == b) {
                cachedValueCount++;
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

    @Override
    public void flush() throws IOException {
        flushCached();
        writeEndMark();
        super.flush();
    }

    @Override
    public void close() throws IOException {
        flush();
        super.close();
    }
}
