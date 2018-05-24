package com.grelobites.romgenerator.util.daad;

public class DAADResource implements RelocatableItem {
    private byte[] data;
    private int index;
    private int slot;
    private int slotOffset;

    public DAADResource(int index, byte[] data) {
        this.index = index;
        this.data = data;
        this.slot = 0;
        this.slotOffset = 0;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public int getSize() {
        return data.length;
    }

    @Override
    public int getSlot() {
        return slot;
    }

    @Override
    public void setSlot(int slot) {
        this.slot = slot;
    }

    @Override
    public int getSlotOffset() {
        return slotOffset;
    }

    @Override
    public void setSlotOffset(int slotOffset) {
        this.slotOffset = slotOffset;
    }
}
