package com.grelobites.romgenerator.util.compress.zx7b;

public class OutputByteArrayWriter {
    private byte[] output;
    private int bitMask;
    private int currentPos;
    private int bitIndex;

    public OutputByteArrayWriter(int outputSize) {
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
        int bits = 0;
        int rvalue = 0;
        while (value > 1) {
            ++bits;
            rvalue <<= 1;
            rvalue |= value & 1;
            value >>= 1;
        }
        while (bits-- != 0) {
            writeBit(0);
            writeBit(rvalue & 1);
            rvalue >>= 1;
        }
        writeBit(1);
    }

}
