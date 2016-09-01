package com.grelobites.romgenerator.util.gamerenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameRendererFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameRendererFactory.class);

    public static GameRenderer getRenderer(String type) {
        try {
            return getRenderer(GameRendererType.fromString(type));
        } catch (Exception e) {
            LOGGER.debug("Defaulting to default renderer on error", e);
            return getDefaultRenderer();
        }
    }

    public static GameRenderer getRenderer(GameRendererType type) {
        try {
            return type.renderer().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static GameRenderer getDefaultRenderer() {
        return getRenderer(GameRendererType.SCREENSHOT);
    }
}
