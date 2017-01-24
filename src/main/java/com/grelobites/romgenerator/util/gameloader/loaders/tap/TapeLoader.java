package com.grelobites.romgenerator.util.gameloader.loaders.tap;

import com.grelobites.romgenerator.model.Game;
import java.io.InputStream;

public interface TapeLoader {

    Game loadTape(InputStream tapeFile);
}
