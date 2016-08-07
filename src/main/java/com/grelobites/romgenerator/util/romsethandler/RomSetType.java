package com.grelobites.romgenerator.util.romsethandler;

import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniCompressedRomSetHandler;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniRomSetHandler;

public enum RomSetType {
    DDNTR_V4(DandanatorMiniRomSetHandler.class),
    DDNTR_V5(DandanatorMiniCompressedRomSetHandler.class);

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
