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

    public static DanTapTableEntry fromByteArray(byte[] data) {
        DanTapTableEntry entry = new DanTapTableEntry();
        ByteBuffer buffer = ByteBuffer.wrap(data, 0, DanTapConstants.TAP_TABLE_ENTRY_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN);
        entry.flag = Byte.toUnsignedInt(buffer.get());
        entry.size = Short.toUnsignedInt(buffer.getShort());
        int slotOffset = Byte.toUnsignedInt(buffer.get());
        entry.slotOffset = slotOffset & 0x7f;
        entry.compressedPayload = (slotOffset & 0x80) != 0;
        entry.compressedSize = Short.toUnsignedInt(buffer.getShort());
        entry.offset = Short.toUnsignedInt(buffer.getShort());
        return entry;
    }

    public int getSlotOffset() {
        return slotOffset;
    }

    public int getSize() {
        return size;
    }

    public int getCompressedSize() {
        return compressedSize;
    }

    public boolean isCompressedPayload() {
        return compressedPayload;
    }

    public int getOffset() {
        return offset;
    }

    public int getFlag() {
        return flag;
    }

    @Override
    public String toString() {
        return "DanTapTableEntry{" +
                "slotOffset=" + slotOffset +
                ", size=" + size +
                ", compressedSize=" + compressedSize +
                ", compressedPayload=" + compressedPayload +
                ", offset=" + offset +
                ", flag=" + String.format("0x%02x", flag & 0xff) +
                '}';
    }
}
