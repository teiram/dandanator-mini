package com.grelobites.romgenerator.util.pokeimporter;

import com.grelobites.romgenerator.util.pokeimporter.importers.pok.PoKPokeImporter;

public enum PokeType {
    POK(PoKPokeImporter.class);

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
