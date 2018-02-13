package com.grelobites.romgenerator.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Optional;

public class MLDInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(MLDInfo.class);

    private static final String MLD_SIGNATURE = "MLD";
    public static final int MLD_HEADER_OFFSET = 16362;
    public static final int MLD_ALLOCATED_SECTORS_OFFSET = MLD_HEADER_OFFSET + 3;
    private static final int MLD_SIGNATURE_OFFSET = 16380;
    private static final int MLD_HEADER_SIZE = 22;

    private int headerSlot;
    private int baseSlot;
    private int mldType;
    private int requiredSectors;
    private int tableOffset;
    private int tableRowSize;
    private int tableRows;
    private int rowSlotOffset;
    private int compressedScreenOffset;
    private int compressedScreenSize;
    private int mldVersion;

    public int getMldType() {
        return mldType;
    }

    public void setMldType(int mldType) {
        this.mldType = mldType;
    }

    public int getHeaderSlot() {
        return headerSlot;
    }

    public void setHeaderSlot(int headerSlot) {
        this.headerSlot = headerSlot;
    }

    public int getCompressedScreenOffset() {
        return compressedScreenOffset;
    }

    public void setCompressedScreenOffset(int compressedScreenOffset) {
        this.compressedScreenOffset = compressedScreenOffset;
    }

    public int getCompressedScreenSize() {
        return compressedScreenSize;
    }

    public void setCompressedScreenSize(int compressedScreenSize) {
        this.compressedScreenSize = compressedScreenSize;
    }

    public int getRequiredSectors() {
        return requiredSectors;
    }

    public void setRequiredSectors(int requiredSectors) {
        this.requiredSectors = requiredSectors;
    }

    public int getTableOffset() {
        return tableOffset;
    }

    public void setTableOffset(int tableOffset) {
        this.tableOffset = tableOffset;
    }

    public int getTableRowSize() {
        return tableRowSize;
    }

    public void setTableRowSize(int tableRowSize) {
        this.tableRowSize = tableRowSize;
    }

    public int getTableRows() {
        return tableRows;
    }

    public void setTableRows(int tableRows) {
        this.tableRows = tableRows;
    }

    public int getRowSlotOffset() {
        return rowSlotOffset;
    }

    public void setRowSlotOffset(int rowSlotOffset) {
        this.rowSlotOffset = rowSlotOffset;
    }

    public int getMldVersion() {
        return mldVersion;
    }

    public void setMldVersion(int mldVersion) {
        this.mldVersion = mldVersion;
    }

    public GameType getGameType() {
        return GameType.byTypeId((mldType & 0x0F) | GameType.MLD_MASK);
    }

    public HardwareMode getHardwareMode() {
        switch (getGameType()) {
            case RAM48_MLD:
                return HardwareMode.HW_48K;
            case RAM128_MLD:
                return (mldType & 0x40) == 0 ? HardwareMode.HW_128K : HardwareMode.HW_PLUS2A;
            default:
                return HardwareMode.HW_UNKNOWN;
        }
    }

    public int getBaseSlot() {
        return baseSlot;
    }

    public void setBaseSlot(int baseSlot) {
        this.baseSlot = baseSlot;
    }

    private static Optional<MLDInfo> fromGameSlotByteArray(byte[] data) {
        LOGGER.debug("Got signature as " + new String(data, MLD_SIGNATURE_OFFSET, MLD_SIGNATURE.length()));
        if (MLD_SIGNATURE.equals(new String(data, MLD_SIGNATURE_OFFSET, MLD_SIGNATURE.length()))) {
            ByteBuffer buffer = ByteBuffer.wrap(data, MLD_HEADER_OFFSET, MLD_HEADER_SIZE);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            MLDInfo mldInfo = new MLDInfo();
            mldInfo.setBaseSlot(Byte.valueOf(buffer.get()).intValue());
            mldInfo.setMldType(Byte.valueOf(buffer.get()).intValue());
            mldInfo.setRequiredSectors(Byte.valueOf(buffer.get()).intValue());
            buffer.getInt(); //Skip four bytes
            mldInfo.setTableOffset(Short.valueOf(buffer.getShort()).intValue());
            mldInfo.setTableRowSize(Short.valueOf(buffer.getShort()).intValue());
            mldInfo.setTableRows(Short.valueOf(buffer.getShort()).intValue());
            mldInfo.setRowSlotOffset(Byte.valueOf(buffer.get()).intValue());
            mldInfo.setCompressedScreenOffset(Short.valueOf(buffer.getShort()).intValue());
            mldInfo.setCompressedScreenSize(Short.valueOf(buffer.getShort()).intValue());
            return Optional.of(mldInfo);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<MLDInfo> fromGameByteArray(List<byte[]> data) {
        LOGGER.debug("Analizing game with " + data.size() + " slots");
        for (int i = 0; i < data.size(); i++) {
            Optional<MLDInfo> mldInfoOpt = fromGameSlotByteArray(data.get(i));
            if (mldInfoOpt.isPresent()) {
                mldInfoOpt.get().setHeaderSlot(i);
                return mldInfoOpt;
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "MLDInfo{" +
                "mldType=" + Integer.toHexString(mldType & 0xFF) +
                ", requiredSectors=" + requiredSectors +
                ", tableOffset=" + tableOffset +
                ", tableRowSize=" + tableRowSize +
                ", tableRows=" + tableRows +
                ", rowSlotOffset=" + rowSlotOffset +
                ", compressedScreenOffset=" + compressedScreenOffset +
                ", compressedScreenSize=" + compressedScreenSize +
                ", mldVersion=" + mldVersion +
                '}';
    }
}
