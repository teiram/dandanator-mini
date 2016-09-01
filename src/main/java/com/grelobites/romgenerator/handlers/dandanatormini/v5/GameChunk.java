package com.grelobites.romgenerator.handlers.dandanatormini.v5;

public class GameChunk {
    private int address;
    private int length;
    private byte[] data;

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
