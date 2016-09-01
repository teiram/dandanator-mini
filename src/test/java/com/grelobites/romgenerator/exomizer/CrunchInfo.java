package com.grelobites.romgenerator.exomizer;

public class CrunchInfo {
    private boolean literalSequencesUsed;
    private int neededSafetyOffset;

    public boolean isLiteralSequencesUsed() {
        return literalSequencesUsed;
    }

    public void setLiteralSequencesUsed(boolean literalSequencesUsed) {
        this.literalSequencesUsed = literalSequencesUsed;
    }

    public int getNeededSafetyOffset() {
        return neededSafetyOffset;
    }

    public void setNeededSafetyOffset(int neededSafetyOffset) {
        this.neededSafetyOffset = neededSafetyOffset;
    }
}
