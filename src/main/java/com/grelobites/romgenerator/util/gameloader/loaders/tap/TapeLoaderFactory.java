package com.grelobites.romgenerator.util.gameloader.loaders.tap;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.tapeloader.TapeLoader128;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.tapeloader.TapeLoader48;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.tapeloader.TapeLoaderPlus2A;

public class TapeLoaderFactory {

    public static TapeLoader getTapeLoader(HardwareMode hwMode) {
        switch (hwMode) {
            case HW_48K:
                return new TapeLoader48();
            case HW_128K:
                return new TapeLoader128();
            case HW_PLUS2A:
                TapeLoaderPlus2A tapeLoader = new TapeLoaderPlus2A();
                tapeLoader.setRomResources(Configuration.getInstance().getPlus2ARomSet().getResources());
                return tapeLoader;
            default:
                throw new IllegalArgumentException("Unsupported Hardware by TapeLoader");
        }
    }
}
