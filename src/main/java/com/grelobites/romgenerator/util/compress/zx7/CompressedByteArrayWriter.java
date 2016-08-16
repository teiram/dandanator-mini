package com.grelobites.romgenerator.util.compress.zx7;

public class CompressedByteArrayWriter {
    private byte[] output;
    private int bitMask;
    private int currentPos;
    private int bitIndex;

    public CompressedByteArrayWriter(int outputSize) {
        output = new byte[outputSize];
        bitMask = 0;
        currentPos = 0;
        bitIndex = 0;
    }

    public void write(byte b) {
        output[currentPos++] = b;
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
}
