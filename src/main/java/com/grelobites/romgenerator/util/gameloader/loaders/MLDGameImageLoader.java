package com.grelobites.romgenerator.util.gameloader.loaders;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.*;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MLDGameImageLoader implements GameImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(MLDGameImageLoader.class);


    @Override
    public Game load(InputStream is) throws IOException {
        byte[] gameImage = Util.fromInputStream(is);
        LOGGER.debug("Using image of " + gameImage.length + " bytes");
        if ((gameImage.length % Constants.SLOT_SIZE) == 0) {
            List<byte[]> gameSlots = new ArrayList<>();
            for (int i = 0; i < (gameImage.length / Constants.SLOT_SIZE); i++) {
                gameSlots.add(Arrays.copyOfRange(gameImage, i * Constants.SLOT_SIZE,
                        (i + 1) * Constants.SLOT_SIZE));
            }
            MLDGame game = new MLDGame(GameType.RAM128, gameSlots);
            return game;
        } else {
            throw new IllegalArgumentException("MLD size must be multiple of 16384");
        }
    }

    @Override
    public void save(Game game, OutputStream os) throws IOException {
        throw new IllegalArgumentException("Not implemented yet");
    }

}
