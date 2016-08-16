package com.grelobites.romgenerator.util.compress.zx7b;

import com.grelobites.romgenerator.util.compress.Compressor;
import com.grelobites.romgenerator.util.compress.CompressorType;

import java.io.InputStream;
import java.io.OutputStream;

public class Zx7bCompressor implements Compressor {

    public static final int MAX_SIZE = 16384;
    public static final boolean BACKWARDS_DEFAULT = false;

    @Override
    public CompressorType getCompressorType() {
        return CompressorType.ZX7B;
    }

    @Override
    public OutputStream getCompressingOutputStream(OutputStream target) {
        return new Zx7bOutputStream(target);
    }

    @Override
    public InputStream getUncompressingInputStream(InputStream source) {
        return new Zx7bInputStream(source);
    }
}
