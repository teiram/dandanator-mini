package com.grelobites.romgenerator.zxspectrum.spectrum;

public class ConstantBitInputStream implements BitInputStream {

    private final int value;
    private int remaining;

    public ConstantBitInputStream(int value, int length) {
        this.remaining = length;
        this.value = value;
    }

    @Override
    public int read() {
        return remaining-- > 0 ? value : -1;
    }

    @Override
    public int skip(int value) {
        int returnValue = Math.min(value, remaining);
        remaining -= returnValue;
        return returnValue;
    }
}
