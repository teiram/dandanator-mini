package com.grelobites.romgenerator.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DanTapTableEntry {
    public static byte[] EMPTY_ENTRY = new byte[DanTapConstants.TAP_TABLE_ENTRY_SIZE];

    private int slotOffset;
    private int size;
    private int compressedSize;
    private boolean compressedPayload;
    private int offset;
    private int flag;

    public static class Builder {
        private DanTapTableEntry entry = new DanTapTableEntry();

        public Builder withSlotOffset(int slotOffset) {
            entry.slotOffset = slotOffset;
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

        public Builder withFlag(int flag) {
            entry.flag = flag;
            return this;
        }

        public Builder withCompressedSize(int compressedSize) {
            entry.compressedSize = compressedSize;
            return this;
        }

        public Builder withCompressedPayload(boolean compressedPayload) {
            entry.compressedPayload = compressedPayload;
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
        buffer.put(Integer.valueOf(flag).byteValue());
        buffer.putShort(Integer.valueOf(size).shortValue());
        buffer.put(Integer.valueOf(slotOffset | ((compressedPayload ? 1 : 0 ) << 7)).byteValue());
        buffer.putShort(Integer.valueOf(compressedSize).shortValue());
        buffer.putShort(Integer.valueOf(offset).shortValue());
        return buffer.array();
    }

    @Override
    public String toString() {
        return "DanTapTableEntry{" +
                "slotOffset=" + slotOffset +
                ", size=" + size +
                ", compressedSize=" + compressedSize +
                ", compressedPayload=" + compressedPayload +
                ", offset=" + offset +
                ", flag=" + flag +
                '}';
    }
}
