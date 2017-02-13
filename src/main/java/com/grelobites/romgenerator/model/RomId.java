package com.grelobites.romgenerator.model;

public enum RomId {
    ROM_PLUS2A_40(0),
    ROM_PLUS2A_41(1),
    ROM_128K(2);

    private int romid;

    private RomId(int romid) {
        this.romid = romid;
    }

    public int romId() {
        return romid;
    }

    public static RomId fromRomId(int romId) {
        return romId == ROM_PLUS2A_40.romId() ? ROM_PLUS2A_40 : ROM_PLUS2A_41;
    }
}
