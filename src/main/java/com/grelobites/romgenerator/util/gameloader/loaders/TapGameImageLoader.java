package com.grelobites.romgenerator.util.gameloader.loaders;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.TapSnapGameImageLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TapGameImageLoader implements GameImageLoader {

    @Override
    public Game load(InputStream is) throws IOException {
        GameImageLoader loader = Configuration.getInstance()
                .isDanTapSupport() ? new DanTapGameImageLoader() : new TapSnapGameImageLoader();
        return loader.load(is);
    }

    @Override
    public void save(Game game, OutputStream os) throws IOException {
        GameImageLoader loader = Configuration.getInstance()
                .isDanTapSupport() ? new DanTapGameImageLoader() : new TapSnapGameImageLoader();
        loader.save(game, os);
    }
}
