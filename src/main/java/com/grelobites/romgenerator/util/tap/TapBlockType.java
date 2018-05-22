package com.grelobites.romgenerator.util.tap;

import java.util.Optional;

public enum TapBlockType {
    PROGRAM(0),
    NUMARRAY(1),
    CHARARRAY(2),
    CODE(3),
    DATA(255);

    private int id;

    TapBlockType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static Optional<TapBlockType> fromId(int id) {
        for (TapBlockType blockType : TapBlockType.values()) {
            if (blockType.id == id) {
                return Optional.of(blockType);
            }
        }
        return Optional.empty();
    }
}
