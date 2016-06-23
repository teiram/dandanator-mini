package com.grelobites.dandanator.util.gameloader;


public class GameImageLoaderFactory {

    public static GameImageLoader getLoader(String type) {
        return getLoader(GameImageType.valueOf(type.toUpperCase()));
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
