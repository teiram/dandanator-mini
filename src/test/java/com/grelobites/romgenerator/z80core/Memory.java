package com.grelobites.romgenerator.z80core;

import java.io.InputStream;

public interface Memory {
    int peek8(int address);

    void poke8(int address, int value);

    int peek16(int address);

    void poke16(int address, int word);

    void load(InputStream is, int pos, int size);

    byte[] asByteArray();
}
