package com.grelobites.dandanator.util.gameloader;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameImageLoaderFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameImageLoaderFactory.class);

    public static GameImageLoader getLoader(String type) {
        try {
            return getLoader(GameImageType.fromString(type));
        } catch (Exception e) {
            LOGGER.debug("Defaulting to default loader on error", e);
            return getDefaultLoader();
        }
    }

    public static GameImageLoader getLoader(GameImageType type) {
        try {
            return type.generator()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static GameImageLoader getDefaultLoader() {
        return getLoader(GameImageType.SNA);
    }
}
