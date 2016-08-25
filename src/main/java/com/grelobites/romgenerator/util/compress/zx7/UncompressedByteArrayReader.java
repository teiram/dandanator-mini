package com.grelobites.romgenerator.util.compress.zx7;


import java.util.Arrays;

public class UncompressedByteArrayReader {
    private byte[] inputData;
    private byte[] outputData;
    private int inputIndex;
    private int outputIndex;
    private int bitMask;
    private int bitValue;

    public UncompressedByteArrayReader(byte[] inputData) {
        this.inputData = inputData;
    }

    public byte[] uncompress() {
        outputData = new byte[Zx7Compressor.MAX_SIZE];
        inputIndex = outputIndex = bitMask = 0;
        writeByte(readByte());
        while (true) {
            if (!readBit()) {
                writeByte(readByte());
            } else {
                int length = readEliasGamma() + 1;
                if (length == 0) {
                    break;
                }
                writeBytes(readOffset() + 1, length);
            }
        }
        return Arrays.copyOfRange(outputData, 0, outputIndex);
    }

    private byte readByte() {
        return inputData[inputIndex++];
    }

    private void writeByte(byte value) {
        outputData[outputIndex++] = value;
    }

    private boolean readBit() {
        bitMask >>= 1;
        if (bitMask == 0) {
            bitMask = 128;
            bitValue = readByte();
        }
        return (bitValue & bitMask) != 0;
    }

    private int readEliasGamma() {
        int i = 0;
        while (!readBit()) {
            i++;
        }
        if (i > 15) {
            return -1;
        }

        int value = 1;
        while (i-- > 0) {
            value = (value << 1) | (readBit() ? 1 : 0);
        }
        return value;
    }

    private int readOffset() {
        int value = Byte.toUnsignedInt(readByte());
        if (value < 128) {
            return value;
        } else {
            int i = readBit() ? 1 : 0;
            i = (i << 1) | (readBit() ? 1 : 0);
            i = (i << 1) | (readBit() ? 1 : 0);
            i = (i << 1) | (readBit() ? 1 : 0);
            return (value & 127 | i << 7) + 128;
        }
    }

    private void writeBytes(int offset, int length) {
        if (offset > outputIndex) {
            throw new IllegalStateException("Invalid compressed data. Trying to write past end");
        }
        while (length-- > 0) {
            int i = outputIndex - offset;
            writeByte(outputData[i >= 0 ? i : Zx7Compressor.MAX_SIZE + i]);
        }
    }
}