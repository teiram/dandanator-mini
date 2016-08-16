package com.grelobites.romgenerator.util.compress.zx7;

import com.grelobites.romgenerator.util.compress.Compressor;
import com.grelobites.romgenerator.util.compress.CompressorType;

import java.io.InputStream;
import java.io.OutputStream;

public class Zx7Compressor implements Compressor {

    public static final int MAX_SIZE = 16384;
    public static final boolean BACKWARDS_DEFAULT = false;

    @Override
    public CompressorType getCompressorType() {
        return CompressorType.ZX7;
    }

    @Override
    public OutputStream getCompressingOutputStream(OutputStream target) {
        return new Zx7OutputStream(target);
    }

    @Override
    public InputStream getUncompressingInputStream(InputStream source) {
        return new Zx7InputStream(source);
    }
}
