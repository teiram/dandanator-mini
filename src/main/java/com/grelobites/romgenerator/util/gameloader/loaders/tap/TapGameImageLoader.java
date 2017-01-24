package com.grelobites.romgenerator.util.gameloader.loaders.tap;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.tapeloader.TapeLoader48;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TapGameImageLoader implements GameImageLoader {
    @Override
    public Game load(InputStream is) throws IOException {
        TapeLoader tapeLoader = TapeLoaderFactory.getTapeLoader(HardwareMode.HW_48K);
        return tapeLoader.loadTape(is);
    }

    @Override
    public void save(Game game, OutputStream os) throws IOException {
        throw new IllegalStateException("Save to TAP not supported");

    }
}
