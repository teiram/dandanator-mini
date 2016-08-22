package com.grelobites.romgenerator.util.romsethandler;

import com.grelobites.romgenerator.handlers.dandanatormini.v5.DandanatorMiniV5RomSetHandler;
import com.grelobites.romgenerator.handlers.dandanatormini.v4.DandanatorMiniV4RomSetHandler;

public enum RomSetHandlerType {
    DDNTR_V4(DandanatorMiniV4RomSetHandler.class, "Dandanator Mini V4"),
    DDNTR_V5(DandanatorMiniV5RomSetHandler.class, "Dandanator Mini V5");

    private Class<? extends RomSetHandler> handler;
    private String displayName;

    RomSetHandlerType(Class<? extends RomSetHandler> handler, String displayName) {
        this.handler = handler;
        this.displayName = displayName;
    }

    public static RomSetHandlerType fromString(String type) {
        return RomSetHandlerType.valueOf(type.toUpperCase());
    }

    public Class<? extends RomSetHandler> handler() {
        return handler;
    }

    public String displayName() {
        return displayName;
    }
}
