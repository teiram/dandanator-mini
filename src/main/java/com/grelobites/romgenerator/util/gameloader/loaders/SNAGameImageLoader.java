package com.grelobites.romgenerator.util.gameloader.loaders;

import com.grelobites.romgenerator.util.SNAHeader;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class SNAGameImageLoader implements GameImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(SNAGameImageLoader.class);

    @Override
    public byte[] load(InputStream is) throws IOException {
        byte[] gameImage = new byte[GameImageLoader.IMAGE_SIZE];
        int nread = is.read(gameImage);
        LOGGER.debug("Read " + nread + " bytes from game image");
        boolean isValidSnaImage = SNAHeader.fromByteArray(gameImage).validate();
        if (isValidSnaImage) {
            return gameImage;
        } else {
            throw new IllegalArgumentException("SNA doesn't pass validations");
        }
    }
}
