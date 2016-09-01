package com.grelobites.romgenerator.util.compress.zx7;

public class CompressedByteArrayWriter {
    private byte[] output;
    private int bitMask;
    private int currentPos;
    private int bitIndex;
    private int diff;
    private int delta;

    public CompressedByteArrayWriter(int inputSize, int outputSize) {
        output = new byte[outputSize];
        bitMask = 0;
        currentPos = 0;
        bitIndex = 0;
        delta = 0;
        diff = outputSize - inputSize;
    }

    public void write(byte b) {
        output[currentPos++] = b;
        diff--;
    }

    public void read(int n) {
        diff += n;
        if (diff > delta) {
            delta = diff;
        }
    }

    public void write(int b) {
        write((byte) b);
    }

    public void writeBit(int value) {
        if (bitMask == 0) {
            bitMask = 0x80;
            bitIndex = currentPos;
            write(0);
        }
        if (value > 0) {
            output[bitIndex] |= bitMask;
        }
        bitMask >>= 1;
    }

    public byte[] asByteArray() {
        return output;
    }

    public void writeEliasGamma(int value) {
        int i;

        for (i = 2; i <= value; i <<= 1) {
            writeBit(0);
        }
        while ((i >>= 1) > 0) {
            writeBit(value & i);
        }
    }

    public int getDelta() {
        return delta;
    }
}
