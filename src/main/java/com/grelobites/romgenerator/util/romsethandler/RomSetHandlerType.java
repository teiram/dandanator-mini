package com.grelobites.romgenerator.util.romsethandler;

import com.grelobites.romgenerator.handlers.dandanatormini.v5.DandanatorMiniV5RomSetHandler;
import com.grelobites.romgenerator.handlers.dandanatormini.v4.DandanatorMiniV4RomSetHandler;
import com.grelobites.romgenerator.handlers.dandanatormini.v6.DandanatorMiniV6RomSetHandler;

public enum RomSetHandlerType {
    DDNTR_V4(DandanatorMiniV4RomSetHandler.class, "Dandanator Mini V4", false),
    DDNTR_V5(DandanatorMiniV5RomSetHandler.class, "Dandanator Mini V5", false),
    DDNTR_V6(DandanatorMiniV6RomSetHandler.class, "Dandanator Mini V6", true);

    private Class<? extends RomSetHandler> handler;
    private String displayName;
    private boolean enabled;

    RomSetHandlerType(Class<? extends RomSetHandler> handler, String displayName, boolean enabled) {
        this.handler = handler;
        this.displayName = displayName;
        this.enabled = enabled;
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

    public boolean isEnabled() {
        return enabled;
    }
}
