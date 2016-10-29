package com.grelobites.romgenerator.handlers.dandanatormini.model;


public class GameBlock {
    public int initSlot;
    public int start;
    public int size;
    public boolean compressed;
    public byte[] data;
    public byte[] rawdata;

    public int getInitSlot() {
        return initSlot;
    }
    public int getStart() {
        return start;
    }

    public void setInitSlot(int initSlot) {
        this.initSlot = initSlot;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "GameBlock{" +
                "initSlot=" + initSlot +
                ", start=" + start +
                ", size=" + size +
                ", compressed=" + compressed +
                '}';
    }

}
