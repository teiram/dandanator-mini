package com.grelobites.romgenerator.util.daad;

public class SlotCoordinates {
    private int slot;
    private int offset;

    public SlotCoordinates(int slot, int offset) {
        this.slot = slot;
        this.offset = offset;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
