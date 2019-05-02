package com.grelobites.romgenerator.util.romsethandler;

import com.grelobites.romgenerator.handlers.dandanatormini.v9.DandanatorMiniV9RomSetHandler;

public enum RomSetHandlerType {
    DDNTR_V9(DandanatorMiniV9RomSetHandler.class, "Dandanator Mini V9", true);

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
