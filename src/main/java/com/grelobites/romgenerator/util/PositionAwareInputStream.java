package com.grelobites.romgenerator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PositionAwareInputStream extends InputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(PositionAwareInputStream.class);

    private static final int EOF = -1;
    private InputStream delegate;
    private long position;

    public PositionAwareInputStream(byte[] byteArray) {
        this.delegate = new ByteArrayInputStream(byteArray);
        this.position = 0;
    }

    public PositionAwareInputStream(InputStream delegate) {
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

    public void safeSkip(long n) throws IOException {
        long skipped = skip(n);
        if (skipped != n) {
            throw new IOException("Unable to skip from stream. Bytes " + n);
        }
    }

    public byte[] getAsByteArray(int length) throws IOException {
        byte[] result = new byte[length];
        this.read(result, 0, length);

        return result;
    }

    public int getAsLittleEndian() throws IOException {
        byte[] address = new byte[2];
        this.read(address);
        return (address[0] & 0xff) | ((address[1] & 0xff) << 8);
    }

    public String getNullTerminatedString(int maxLength) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        int counter = 0;
        while ((b = this.read()) != 0 && counter < maxLength) {
            sb.append((char) b);
            counter++;
        }
        LOGGER.debug("Appended " + counter + " characters: " + sb.toString());
        LOGGER.debug("Will skip " + (maxLength - counter));
        skip(maxLength - counter - 1);
        return sb.toString();
    }
}
