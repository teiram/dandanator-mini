package com.grelobites.romgenerator.util.gameloader.loaders.tap;

import java.util.Arrays;

public class MemoryBank {

    private final byte[] data;
    private final MemoryBankType type;

    public MemoryBank(MemoryBankType type, int bankSize) {
        this.type = type;
        data = new byte[bankSize];
    }

    public int getSize() {
        return data.length;
    }

    public MemoryBankType getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "MemoryBank{" +
                "data.length=" + data.length +
                ", type=" + type +
                '}';
    }
}
