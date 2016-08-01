package com.grelobites.romgenerator.util.romsethandler;

import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniRomSetHandler;

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
