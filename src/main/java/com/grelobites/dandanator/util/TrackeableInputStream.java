package com.grelobites.dandanator.util;

import java.io.IOException;
import java.io.InputStream;

public class TrackeableInputStream extends InputStream {
    private static final int EOF = -1;
    private InputStream delegate;
    private long position;

    public TrackeableInputStream(InputStream delegate) {
        this.delegate = delegate;
        this.position = 0;
    }

    public long position() {
        return position;
    }

    @Override
    public int read() throws IOException {
        int value = delegate.read();
        if (value != EOF) {
            position++;
        }
        return value;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = delegate.skip(n);
        position += skipped;
        return skipped;
    }
}
