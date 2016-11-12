package com.grelobites.romgenerator.zxspectrum.tape;

public interface BitInputStream {
    int read();
    int skip(int value);
}
