package com.grelobites.romgenerator.util.gameloader;

import com.grelobites.romgenerator.model.Game;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public interface GameImageLoader {
    Game load(InputStream is) throws IOException;
    void save(Game game, OutputStream os) throws IOException;
}
