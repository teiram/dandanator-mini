package com.grelobites.romgenerator.util.tap;

import java.util.Arrays;

public class DataTapBlock implements TapBlock {
    private byte[] data;
    private int checksum;

    public static class Builder {
        private DataTapBlock block = new DataTapBlock();

        public Builder withData(byte[] data) {
            block.setData(data);
            return this;
        }

        public Builder withChecksum(int checksum) {
            block.setChecksum(checksum);
            return this;
        }

        public DataTapBlock build() {
            return block;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public TapBlockType getType() {
        return TapBlockType.DATA;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    @Override
    public String toString() {
        return "DataTapBlock{" +
                "data.length=" + data.length +
                ", checksum=" + String.format("0x%02x", checksum) +
                '}';
    }
}
