package com.grelobites.romgenerator.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DanTapTableEntry {
    public static byte[] EMPTY_ENTRY = new byte[DanTapConstants.TAP_TABLE_ENTRY_SIZE];

    private int slot;
    private int size;
    private int compressedSize;
    private boolean compressed;
    private int offset;

    public static class Builder {
        private DanTapTableEntry entry;

        public Builder withSlot(int slot) {
            entry.slot = slot;
            return this;
        }

        public Builder withSize(int size) {
            entry.size = size;
            return this;
        }

        public Builder withOffset(int offset) {
            entry.offset = offset;
            return this;
        }

        public Builder withCompressedSize(int compressedSize) {
            entry.compressedSize = compressedSize;
            return this;
        }

        public Builder withCompressed(boolean compressed) {
            entry.compressed = compressed;
            return this;
        }

        public DanTapTableEntry build() {
            return entry;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(DanTapConstants.TAP_TABLE_ENTRY_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(Integer.valueOf(slot).byteValue());
        buffer.putShort(Integer.valueOf(size).shortValue());
        buffer.putShort(Integer.valueOf(compressedSize).shortValue());
        buffer.putShort(Integer.valueOf(offset).shortValue());
        return buffer.array();
    }

}
