package com.grelobites.dandanator.util.pokeimporter;

import com.grelobites.dandanator.util.pokeimporter.importer.POKPokeImporter;

public enum PokeType {
    POK(POKPokeImporter.class);

    private Class<? extends PokeImporter> generator;

    PokeType(Class<? extends PokeImporter> generator) {
        this.generator = generator;
    }

    public static PokeType fromString(String type) {
        return PokeType.valueOf(type.toUpperCase());
    }

    public Class<? extends PokeImporter> generator() {
        return generator;
    }
}
