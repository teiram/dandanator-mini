package com.grelobites.dandanator.util;

import com.grelobites.dandanator.util.gameloader.SNAGameImageLoader;
import com.grelobites.dandanator.util.gameloader.Z80GameImageLoader;

public enum GameImageType {
    SNA(SNAGameImageLoader.class),
    Z80(Z80GameImageLoader.class);

    private Class<? extends GameImageLoader> generator;

    GameImageType(Class<? extends GameImageLoader> generator) {
        this.generator = generator;
    }

    public static GameImageType fromString(String type) {
        return GameImageType.valueOf(type.toUpperCase());
    }

    public Class<? extends GameImageLoader> generator() {
        return generator;
    }
}
