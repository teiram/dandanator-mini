package com.grelobites.romgenerator.util.tap;

public class CodeTapBlock extends BaseHeaderTapBlock implements TapBlock {
    private int startAddress;

    public static class Builder {
        private CodeTapBlock block = new CodeTapBlock();

        public Builder withStartAddress(int autoStartLine) {
            block.setStartAddress(autoStartLine);
            return this;
        }

        public Builder withLoadingProgramName(String loadingProgramName) {
            block.setLoadingProgramName(loadingProgramName);
            return this;
        }

        public Builder withDataLength(int dataLength) {
            block.setDataLength(dataLength);
            return this;
        }

        public Builder withChecksum(int checksum) {
            block.setChecksum(checksum);
            return this;
        }

        public CodeTapBlock build() {
            return block;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public CodeTapBlock() {
        setType(TapBlockType.CODE);
    }

    public int getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(int startAddress) {
        this.startAddress = startAddress;
    }

    @Override
    public String toString() {
        return "CodeTapBlock{" +
                "loadingProgramName='" + getLoadingProgramName() + '\'' +
                ", dataLength=" + getDataLength() +
                ", checksum=" + String.format("0x%02x", getChecksum()) +
                ", startAddress=" + String.format("0x%04x", startAddress) +
                '}';
    }
}
