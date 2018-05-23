package com.grelobites.romgenerator.util.daad;

public interface RelocatableItem {
    byte[] getData();
    int getSize();
    int getSlot();
    void setSlot(int slot);
    int getSlotOffset();
    void setSlotOffset(int slotOffset);
}
