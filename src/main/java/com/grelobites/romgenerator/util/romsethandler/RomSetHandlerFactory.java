package com.grelobites.romgenerator.util.romsethandler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RomSetHandlerFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(RomSetHandlerFactory.class);

    public static RomSetHandler getHandler(String type) {
        try {
            return getHandler(RomSetHandlerType.fromString(type));
        } catch (Exception e) {
            LOGGER.debug("Defaulting to default RomSet handler on error", e);
            return getDefaultHandler();
        }
    }

    public static RomSetHandler getHandler(RomSetHandlerType type) {
        try {
            return type.handler()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static RomSetHandler getDefaultHandler() {
        return getHandler(RomSetHandlerType.DDNTR_V7);
    }
}
