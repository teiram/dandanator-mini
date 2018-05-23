package com.grelobites.romgenerator.util.daad;

public class DAADScreen {
    private int slotOffset;
    private byte[] data;

    public DAADScreen(byte[] data) {
        this.data = data;
        this.slotOffset = 0;
    }

    public int getSlotOffset() {
        return slotOffset;
    }

    public void setSlotOffset(int slotOffset) {
        this.slotOffset = slotOffset;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
