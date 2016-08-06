package com.grelobites.romgenerator.util.compress.zx7b;

import com.grelobites.romgenerator.util.compress.Compressor;
import com.grelobites.romgenerator.util.compress.CompressorType;

import java.io.InputStream;
import java.io.OutputStream;

public class Zx7bCompressor implements Compressor {

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
        throw new UnsupportedOperationException("Still not implemented");
    }
}
