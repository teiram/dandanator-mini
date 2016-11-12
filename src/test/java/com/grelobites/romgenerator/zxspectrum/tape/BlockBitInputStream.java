package com.grelobites.romgenerator.zxspectrum.tape;

import java.io.InputStream;

public class BlockBitInputStream implements BitInputStream {

    private InputStream data;

    public BlockBitInputStream(InputStream data) {
        this.data = data;
    }

    @Override
    public int read() {
        return 0;
    }

    @Override
    public int skip(int value) {
        return 0;
    }
}
