package com.grelobites.romgenerator.util.multiply;

public enum HexRecordType {
    DATA(0),
    EOF(1);

    private static final HexRecordType[] INDEXED = {DATA, EOF};

    int id;

    HexRecordType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static HexRecordType fromId(int id) {
        if (id < INDEXED.length) {
            return INDEXED[id];
        } else {
            throw new IllegalArgumentException("Invalid HexRecordType id");
        }
    }

}
