package com.grelobites.romgenerator.util.tap;

public class BaseHeaderTapBlock {
    private TapBlockType type;
    private String loadingProgramName;
    private int dataLength;
    private int checksum;

    public TapBlockType getType() {
        return type;
    }

    public void setType(TapBlockType type) {
        this.type = type;
    }

    public String getLoadingProgramName() {
        return loadingProgramName;
    }

    public void setLoadingProgramName(String loadingProgramName) {
        this.loadingProgramName = loadingProgramName;
    }

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

}
