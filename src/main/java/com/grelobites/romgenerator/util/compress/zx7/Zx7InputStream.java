package com.grelobites.romgenerator.util.compress.zx7;


import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Zx7InputStream extends FilterInputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(Zx7InputStream.class);

    private boolean backwards = Zx7Compressor.BACKWARDS_DEFAULT;
    private ByteArrayInputStream uncompressedStream;

    public Zx7InputStream(InputStream in, boolean backwards) {
        this(in);
        this.backwards = backwards;
    }

    public Zx7InputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        return getUncompressedStream().read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return getUncompressedStream().read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return getUncompressedStream().read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return getUncompressedStream().skip(n);
    }

    @Override
    public int available() throws IOException {
        return getUncompressedStream().available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        getUncompressedStream().mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        getUncompressedStream().reset();
    }

    @Override
    public boolean markSupported() {
        return getUncompressedStream().markSupported();
    }

    private ByteArrayInputStream getUncompressedStream() {
        if (uncompressedStream == null) {
            try {
                byte[] uncompressedByteArray = uncompress(
                        backwards ?
                                Util.reverseByteArray(Util.fromInputStream(in)) :
                                Util.fromInputStream(in));
                LOGGER.debug("Uncompressing byte array of size " + uncompressedByteArray.length
                        + ", backwards: " + backwards);
                uncompressedStream = new ByteArrayInputStream(
                        backwards ?
                                Util.reverseByteArray(uncompressedByteArray) :
                                uncompressedByteArray);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return uncompressedStream;
    }

    private byte[] uncompress(byte[] data) throws IOException {
        UncompressedByteArrayReader reader = new UncompressedByteArrayReader(data);

        return reader.uncompress();

    }

}
