package com.grelobites.romgenerator.util.gameloader.loaders.tap;

public interface Memory {
    int peek8(int address);

    void poke8(int address, int value);

    int peek16(int address);

    void poke16(int address, int word);

    void load(byte[] data, int srcPos, int address, int size);

    byte[] asByteArray();
}
