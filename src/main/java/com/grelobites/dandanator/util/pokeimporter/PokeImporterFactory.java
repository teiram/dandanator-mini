package com.grelobites.dandanator.util.pokeimporter;


public class PokeImporterFactory {

    public static PokeImporter getLoader(String type) {
        return getLoader(PokeType.valueOf(type.toUpperCase()));
    }

    public static PokeImporter getLoader(PokeType type) {
        try {
            return type.generator()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PokeImporter getDefaultLoader() {
        return getLoader(PokeType.POK);
    }
}
