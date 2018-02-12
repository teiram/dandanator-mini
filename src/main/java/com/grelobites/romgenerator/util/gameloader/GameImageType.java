package com.grelobites.romgenerator.util.gameloader;

import com.grelobites.romgenerator.util.gameloader.loaders.MLDGameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.RomGameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.SNAGameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.Z80GameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.TapGameImageLoader;

public enum GameImageType {
    SNA(SNAGameImageLoader.class),
    Z80(Z80GameImageLoader.class),
    ROM(RomGameImageLoader.class),
    TAP(TapGameImageLoader.class),
    MLD(MLDGameImageLoader.class);

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
