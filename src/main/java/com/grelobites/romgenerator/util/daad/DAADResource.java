package com.grelobites.romgenerator.util.daad;

public class DAADResource implements RelocatableItem {
    private byte[] data;
    private int index;
    private int slot;
    private int slotOffset;
    private SlotCoordinates coordinates;

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

    public SlotCoordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(SlotCoordinates coordinates) {
        this.coordinates = coordinates;
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
