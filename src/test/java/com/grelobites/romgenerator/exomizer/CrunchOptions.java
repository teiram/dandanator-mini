package com.grelobites.romgenerator.exomizer;


public class CrunchOptions {
    private String exportedEncoding;
    private int maxPasses;
    private int maxLength;
    private int maxOffset;
    private boolean useLiteralSequences;
    private int useImpreciseRLE;
    private int outputHeader;

    public String getExportedEncoding() {
        return exportedEncoding;
    }

    public void setExportedEncoding(String exportedEncoding) {
        this.exportedEncoding = exportedEncoding;
    }

    public int getMaxPasses() {
        return maxPasses;
    }

    public void setMaxPasses(int maxPasses) {
        this.maxPasses = maxPasses;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public int getMaxOffset() {
        return maxOffset;
    }

    public void setMaxOffset(int maxOffset) {
        this.maxOffset = maxOffset;
    }

    public boolean isUseLiteralSequences() {
        return useLiteralSequences;
    }

    public void setUseLiteralSequences(boolean useLiteralSequences) {
        this.useLiteralSequences = useLiteralSequences;
    }

    public int getUseImpreciseRLE() {
        return useImpreciseRLE;
    }

    public void setUseImpreciseRLE(int useImpreciseRLE) {
        this.useImpreciseRLE = useImpreciseRLE;
    }

    public int getOutputHeader() {
        return outputHeader;
    }

    public void setOutputHeader(int outputHeader) {
        this.outputHeader = outputHeader;
    }
}
