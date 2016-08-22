package com.grelobites.romgenerator.util.romsethandler;

import com.grelobites.romgenerator.handlers.dandanatormini.v5.DandanatorMiniV5RomSetHandler;
import com.grelobites.romgenerator.handlers.dandanatormini.v4.DandanatorMiniV4RomSetHandler;

public enum RomSetType {
    DDNTR_V4(DandanatorMiniV4RomSetHandler.class),
    DDNTR_V5(DandanatorMiniV5RomSetHandler.class);

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
