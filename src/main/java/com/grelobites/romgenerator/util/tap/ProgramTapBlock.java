package com.grelobites.romgenerator.util.tap;

public class ProgramTapBlock extends BaseHeaderTapBlock implements TapBlock {
    private int autoStartLine;
    private int programLength;

    public static class Builder {
        private ProgramTapBlock block = new ProgramTapBlock();

        public Builder withAutoStartLine(int autoStartLine) {
            block.setAutoStartLine(autoStartLine);
            return this;
        }

        public Builder withProgramLength(int programLength) {
            block.setProgramLength(programLength);
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

        public ProgramTapBlock build() {
            return block;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public ProgramTapBlock() {
        setType(TapBlockType.PROGRAM);
    }

    public int getAutoStartLine() {
        return autoStartLine;
    }

    public void setAutoStartLine(int autoStartLine) {
        this.autoStartLine = autoStartLine;
    }

    public int getProgramLength() {
        return programLength;
    }

    public void setProgramLength(int programLength) {
        this.programLength = programLength;
    }

    @Override
    public String toString() {
        return "ProgramTapBlock{" +
                "loadingProgramName='" + getLoadingProgramName() + '\'' +
                ", dataLength=" + getDataLength() +
                ", checksum=" + String.format("0x%02x", getChecksum()) +
                ", autoStartLine=" + autoStartLine +
                ", programLength=" + programLength +
                '}';
    }
}
