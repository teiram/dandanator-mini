package com.grelobites.romgenerator.util.compress;

import java.io.InputStream;
import java.io.OutputStream;

public interface Compressor {
    CompressorType getCompressorType();
    OutputStream getCompressingOutputStream(OutputStream target);
    InputStream getUncompressingInputStream(InputStream source);
}
