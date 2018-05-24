package com.grelobites.romgenerator.util.daad;

import java.nio.ByteBuffer;

public class DAADTableEntry {
    private int slot;
    private int offset;
    private int compression;

    public static class Builder {
        private DAADTableEntry entry = new DAADTableEntry();

        public Builder withSlot(int slot) {
            entry.setSlot(slot);
            return this;
        }

        public Builder withOffset(int offset) {
            entry.setOffset(offset);
            return this;
        }

        public Builder withCompression(int compression) {
            entry.setCompression(compression);
            return this;
        }

        public DAADTableEntry build() {
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

    public int getCompression() {
        return compression;
    }

    public void setCompression(int compression) {
        this.compression = compression;
    }

    public void toBuffer(ByteBuffer buffer, int offset) {
        buffer.put(offset, Integer.valueOf(slot).byteValue())
              .putShort(offset + 1, Integer.valueOf(offset).shortValue())
              .put(offset + 3, Integer.valueOf(compression).byteValue());
    }
}
