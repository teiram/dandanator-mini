package com.grelobites.romgenerator.util.compress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompressorFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompressorFactory.class);

    public static Compressor getCompressor(String type) {
        try {
            return getCompressor(CompressorType.fromString(type));
        } catch (Exception e) {
            LOGGER.debug("Defaulting to default compressor on error", e);
            return getDefaultCompressor();
        }
    }

    public static Compressor getCompressor(CompressorType type) {
        try {
            return type.compressor()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Compressor getDefaultCompressor() {
        return getCompressor(CompressorType.ZX7);
    }
}
