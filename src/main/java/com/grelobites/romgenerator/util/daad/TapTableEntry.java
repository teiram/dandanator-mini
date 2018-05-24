package com.grelobites.romgenerator.util.daad;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TapTableEntry {
    private int slot;
    private int offset;
    private int loadAddress;

    public static class Builder {
        private TapTableEntry entry = new TapTableEntry();

        public Builder withSlot(int slot) {
            entry.setSlot(slot);
            return this;
        }

        public Builder withOffset(int offset) {
            entry.setOffset(offset);
            return this;
        }

        public Builder withLoadAddress(int loadAddress) {
            entry.setLoadAddress(loadAddress);
            return this;
        }

        public TapTableEntry build() {
            return entry;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
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

    public int getLoadAddress() {
        return loadAddress;
    }

    public void setLoadAddress(int loadAddress) {
        this.loadAddress = loadAddress;
    }

    public void toBuffer(ByteBuffer buffer, int offset) {
        buffer.put(offset, Integer.valueOf(slot).byteValue())
                .putShort(offset + 1, Integer.valueOf(offset).shortValue())
                .putShort(offset + 2, Integer.valueOf(loadAddress).shortValue());
    }
}
