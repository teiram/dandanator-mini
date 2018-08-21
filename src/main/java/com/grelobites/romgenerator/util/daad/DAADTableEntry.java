package com.grelobites.romgenerator.util.daad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class DAADTableEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(DAADTableEntry.class);
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

    @Override
    public String toString() {
        return "DAADTableEntry{" +
                "slot=" + slot +
                ", offset=" + String.format("0x%04x", offset) +
                ", compression=" + compression +
                '}';
    }

    public void toBuffer(ByteBuffer buffer, int bufferOffset) {
        LOGGER.debug("Dumping {}", this);
        buffer.put(bufferOffset, Integer.valueOf(slot + 1).byteValue())
                .put(bufferOffset + 1, Integer.valueOf(compression).byteValue())
                .putShort(bufferOffset + 2, Integer.valueOf(offset).shortValue());
    }
}
