package com.grelobites.romgenerator.util.daad;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class MLDMetadata {
    public static final int MLD_DATA_ROWS = 256;
    public static final int MLD_VERSION = 0;
    public static final int MLD_DATAROW_LENGTH = 4;
    public static final int MLD_SLOTROW_OFFSET = 0;
    public static final int TAP_TABLE_OFFSET = 1024;
    public static final int BASE_SLOT_OFFSET = TAP_TABLE_OFFSET + 40;

    public static class Builder {
        private MLDMetadata metadata = new MLDMetadata();

        public Builder withBaseSlot(int baseSlot) {
            metadata.setBaseSlot(baseSlot);
            return this;
        }

        public Builder withMldType(MLDType mldType) {
            metadata.setMldType(mldType);
            return this;
        }

        public Builder withAllocatedSectors(int allocatedSectors) {
            metadata.setAllocatedSectors(allocatedSectors);
            return this;
        }

        public Builder withTableOffset(int tableOffset) {
            metadata.setTableOffset(tableOffset);
            return this;
        }

        public Builder withDataRowLength(int dataRowLength) {
            metadata.setDataRowLength(dataRowLength);
            return this;
        }

        public Builder withDataRows(int dataRows) {
            metadata.setDataRows(dataRows);
            return this;
        }

        public Builder withSlotRowOffset(int slotRowOffset) {
            metadata.setSlotRowOffset(slotRowOffset);
            return this;
        }

        public Builder withVersion(int version) {
            metadata.setVersion(version);
            return this;
        }

        public Builder withDAADScreen(DAADScreen daadScreen) {
            metadata.setDaadScreen(daadScreen);
            return this;
        }

        public Builder withDAADResources(List<DAADResource> daadResources) {
            metadata.setDaadResources(daadResources);
            return this;
        }

        public Builder withDAADBinaries(DAADBinary[] daadBinaries) {
            metadata.setDaadBinaries(daadBinaries);
            return this;
        }

        public MLDMetadata build() {
            return metadata;
        }
    }

    private int baseSlot;
    private MLDType mldType = MLDType.MLD_48K;
    private int allocatedSectors = 0;
    private int tableOffset = DAADConstants.METADATA_OFFSET;
    private int dataRowLength = MLD_DATAROW_LENGTH;
    private int dataRows = MLD_DATA_ROWS;
    private int slotRowOffset = MLD_SLOTROW_OFFSET;
    private int version = MLD_VERSION;
    private DAADScreen daadScreen;
    private List<DAADResource> daadResources;
    private DAADBinary[] daadBinaries;

    public static Builder newBuilder() {
        return new Builder();
    }

    public int getBaseSlot() {
        return baseSlot;
    }

    public void setBaseSlot(int baseSlot) {
        this.baseSlot = baseSlot;
    }

    public MLDType getMldType() {
        return mldType;
    }

    public void setMldType(MLDType mldType) {
        this.mldType = mldType;
    }

    public int getAllocatedSectors() {
        return allocatedSectors;
    }

    public void setAllocatedSectors(int allocatedSectors) {
        this.allocatedSectors = allocatedSectors;
    }

    public int getTableOffset() {
        return tableOffset;
    }

    public void setTableOffset(int tableOffset) {
        this.tableOffset = tableOffset;
    }

    public int getDataRowLength() {
        return dataRowLength;
    }

    public void setDataRowLength(int dataRowLength) {
        this.dataRowLength = dataRowLength;
    }

    public int getDataRows() {
        return dataRows;
    }

    public void setDataRows(int dataRows) {
        this.dataRows = dataRows;
    }

    public int getSlotRowOffset() {
        return slotRowOffset;
    }

    public void setSlotRowOffset(int slotRowOffset) {
        this.slotRowOffset = slotRowOffset;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public DAADScreen getDaadScreen() {
        return daadScreen;
    }

    public void setDaadScreen(DAADScreen daadScreen) {
        this.daadScreen = daadScreen;
    }

    public List<DAADResource> getDaadResources() {
        return daadResources;
    }

    public void setDaadResources(List<DAADResource> daadResources) {
        this.daadResources = daadResources;
    }

    public DAADBinary[] getDaadBinaries() {
        return daadBinaries;
    }

    public void setDaadBinaries(DAADBinary[] daadBinaries) {
        this.daadBinaries = daadBinaries;
    }

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(DAADConstants.METADATA_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (DAADResource resource : daadResources) {
            int offset = resource.getIndex() * MLD_DATAROW_LENGTH;
            DAADTableEntry.newBuilder()
                    .withSlot(resource.getSlot())
                    .withOffset(resource.getSlotOffset())
                    .withCompression(0)
                    .build()
                    .toBuffer(buffer, offset);
        }
        int offset = TAP_TABLE_OFFSET;
        for (DAADBinary binary: daadBinaries) {
            TapTableEntry.newBuilder()
                    .withSlot(binary.getSlot())
                    .withOffset(binary.getSlotOffset())
                    .withLoadAddress(binary.getLoadAddress())
                    .build()
                    .toBuffer(buffer, offset);
            offset += 5;
        }

        buffer.position(BASE_SLOT_OFFSET);
        buffer.put(Integer.valueOf(baseSlot).byteValue())
                .put(Integer.valueOf(mldType.id()).byteValue())
                .put(Integer.valueOf(allocatedSectors).byteValue())
                .putInt(0)
                .putShort(Integer.valueOf(tableOffset).shortValue())
                .putShort(Integer.valueOf(dataRowLength).shortValue())
                .putShort(Integer.valueOf(dataRows).shortValue())
                .put(Integer.valueOf(slotRowOffset).byteValue())
                .putShort(
                        daadScreen != null ?
                                Integer.valueOf(daadScreen.getSlotOffset())
                                        .shortValue() : 0)
                .putShort(
                        daadScreen != null ?
                                Integer.valueOf(daadScreen.getData().length)
                                        .shortValue() : 0)
                .put(DAADConstants.MLD_SIGNATURE.getBytes())
                .put(Integer.valueOf(version).byteValue());

        return buffer.array();
    }
}
