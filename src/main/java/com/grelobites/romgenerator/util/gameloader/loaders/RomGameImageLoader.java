package com.grelobites.romgenerator.util.gameloader.loaders;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.RomGame;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RomGameImageLoader implements GameImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(RomGameImageLoader.class);

    private static final int ROM_SIZE = Constants.SLOT_SIZE;

    @Override
    public Game load(InputStream is) throws IOException {
        byte[] gameImage = Util.fromInputStream(is);
        LOGGER.debug("Read " + gameImage.length + " bytes from game image");
        if (gameImage.length == ROM_SIZE) {
            return new RomGame(gameImage);
        } else {
            throw new IllegalArgumentException("Unsupported ROM size: " + gameImage.length);
        }
    }

    @Override
    public void save(Game game, OutputStream os) throws IOException {
        if (game instanceof RomGame) {
            os.write(game.getSlot(0));
        } else {
            throw new IllegalArgumentException("Non ROM games cannot be saved as ROM");
        }
    }
}
