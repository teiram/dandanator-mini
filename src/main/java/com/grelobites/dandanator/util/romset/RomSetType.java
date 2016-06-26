package com.grelobites.dandanator.util.romset;

import com.grelobites.dandanator.util.romset.handler.DandanatorMiniRomSetHandler;

public enum RomSetType {
    DANDANATOR_MINI(DandanatorMiniRomSetHandler.class);

    private Class<? extends RomSetHandler> handler;

    RomSetType(Class<? extends RomSetHandler> handler) {
        this.handler = handler;
    }

    public static RomSetType fromString(String type) {
        return RomSetType.valueOf(type.toUpperCase());
    }

    public Class<? extends RomSetHandler> handler() {
        return handler;
    }
}
