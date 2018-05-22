package com.grelobites.romgenerator.util.tap;

public class ArrayTapBlock extends BaseHeaderTapBlock implements TapBlock {
    private String variableName;

    public static class Builder {
        private ArrayTapBlock block = new ArrayTapBlock();

        public Builder withType(TapBlockType blockType) {
            block.setType(blockType);
            return this;
        }

        public Builder withVariableName(String variableName) {
            block.setVariableName(variableName);
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

        public ArrayTapBlock build() {
            return block;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    @Override
    public String toString() {
        return "ArrayTapBlock{" +
                "type=" + getType() +
                ", loadingProgramName='" + getLoadingProgramName() + '\'' +
                ", dataLength=" + getDataLength() +
                ", checksum=" + String.format("%02x", getChecksum()) +
                ", variableName='" + variableName + '\'' +
                '}';
    }
}
