package com.grelobites.romgenerator.util.daad;

public class DAADBinary implements RelocatableItem {
    private byte[] data;
    private int loadAddress;
    private int slot;
    private int slotOffset;

    public static class Builder {
        private DAADBinary part = new DAADBinary();

        public Builder withData(byte[] data) {
            part.setData(data);
            return this;
        }
        public Builder withLoadAddress(int loadAddress) {
            part.setLoadAddress(loadAddress);
            return this;
        }
        public DAADBinary build() {
            return part;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getLoadAddress() {
        return loadAddress;
    }

    public void setLoadAddress(int loadAddress) {
        this.loadAddress = loadAddress;
    }

    @Override
    public int getSize() {
        return data.length;
    }

    @Override
    public int getSlot() {
        return slot;
    }

    @Override
    public void setSlot(int slot) {
        this.slot = slot;
    }

    @Override
    public int getSlotOffset() {
        return slotOffset;
    }

    @Override
    public void setSlotOffset(int slotOffset) {
        this.slotOffset = slotOffset;
    }
}
