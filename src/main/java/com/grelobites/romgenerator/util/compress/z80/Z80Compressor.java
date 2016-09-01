package com.grelobites.romgenerator.util.compress.z80;

import com.grelobites.romgenerator.util.compress.Compressor;
import com.grelobites.romgenerator.util.compress.CompressorType;

import java.io.InputStream;
import java.io.OutputStream;

public class Z80Compressor implements Compressor {


    @Override
    public CompressorType getCompressorType() {
        return CompressorType.Z80;
    }

    @Override
    public OutputStream getCompressingOutputStream(OutputStream target) {
        return new Z80OutputStream(target);
    }

    @Override
    public InputStream getUncompressingInputStream(InputStream source) {
        return new Z80InputStream(source);
    }
}
