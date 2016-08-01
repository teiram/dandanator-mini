package com.grelobites.romgenerator.util.gameloader;

import com.grelobites.romgenerator.Constants;

import java.io.IOException;
import java.io.InputStream;


public interface GameImageLoader {
    int IMAGE_SIZE = Constants.SNA_HEADER_SIZE + 0xC000;

    byte[] load(InputStream is) throws IOException;
}
