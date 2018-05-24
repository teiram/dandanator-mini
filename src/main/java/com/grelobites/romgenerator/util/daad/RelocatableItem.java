package com.grelobites.romgenerator.util.daad;

import com.grelobites.romgenerator.util.pack.PackedItem;

public interface RelocatableItem extends PackedItem {
    byte[] getData();
    int getSlot();
    void setSlot(int slot);
    int getSlotOffset();
    void setSlotOffset(int slotOffset);
}
