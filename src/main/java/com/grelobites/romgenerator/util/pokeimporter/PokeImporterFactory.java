package com.grelobites.romgenerator.util.pokeimporter;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PokeImporterFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(PokeImporterFactory.class);

    public static PokeImporter getImporter(String type) {
        try {
            return getImporter(PokeType.fromString(type));
        } catch (Exception e) {
            LOGGER.debug("Using default importer on error", e);
            return getDefaultImporter();
        }
    }

    public static PokeImporter getImporter(PokeType type) {
        try {
            return type.generator()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PokeImporter getDefaultImporter() {
        return getImporter(PokeType.POK);
    }
}
