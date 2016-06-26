package com.grelobites.dandanator.util.romset;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RomSetHandlerFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(RomSetHandlerFactory.class);

    public static RomSetHandler getHandler(String type) {
        try {
            return getHandler(RomSetType.fromString(type));
        } catch (Exception e) {
            LOGGER.debug("Defaulting to default RomSet handler on error", e);
            return getDefaultHandler();
        }
    }

    public static RomSetHandler getHandler(RomSetType type) {
        try {
            return type.handler()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static RomSetHandler getDefaultHandler() {
        return getHandler(RomSetType.DANDANATOR_MINI);
    }
}
