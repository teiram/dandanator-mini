package com.grelobites.romgenerator.util.gameloader;

import com.grelobites.romgenerator.util.gameloader.loaders.*;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.TapGameImageLoader;

import java.util.Arrays;

public enum GameImageType {
    SNA(SNAGameImageLoader.class, "sna"),
    Z80(Z80GameImageLoader.class, "z80"),
    ROM(RomGameImageLoader.class, "rom"),
    TAP(TapGameImageLoader.class, "tap"),
    MLD(MLDGameImageLoader.class, "mld"),
    DAAD(DAADGameImageLoader.class, "daad", "zip");

    private Class<? extends GameImageLoader> generator;
    private String[] supportedExtensions;

    GameImageType(Class<? extends GameImageLoader> generator,
                  String... supportedExtensions) {
        this.generator = generator;
        this.supportedExtensions = supportedExtensions;
    }

    public static GameImageType fromExtension(String extension) {
        for (GameImageType type : GameImageType.values()) {
            if (type.supportsExtension(extension)) {
                return type;
            }
        }
        return null;
    }

    public boolean supportsExtension(String extension) {
        for (String candidate : supportedExtensions) {
            if (candidate.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    public Class<? extends GameImageLoader> generator() {
        return generator;
    }
}
