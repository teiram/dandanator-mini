package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum RomSet {
    PLUS2A_40_EN("ROM 4.0 English", getResourcePaths("plus23", "40", "en", 0, 1, 2, 3)),
    PLUS2A_41_EN("ROM 4.1 English", getResourcePaths("plus23", "41", "en", 0, 1, 2, 3)),
    PLUS2A_40_ES("ROM 4.0 Spanish", getResourcePaths("plus23", "40", "es", 0, 1, 2, 3)),
    PLUS2A_41_ES("ROM 4.1 Spanish", getResourcePaths("plus23", "41", "es", 0, 1, 2, 3));

    private static final Logger LOGGER = LoggerFactory.getLogger(RomSet.class);
    private static final String BASE_RESOURCE_PATH = "/loader";

    private static String[] getResourcePaths(String machine, String version,
                                             String language, int ... index) {
        String[] romset = new String[index.length];
        for (int i = 0; i < index.length; i++) {
            romset[i] = String.format("%s/%s-%s-%s-%d.rom", BASE_RESOURCE_PATH,
                    machine, version, language, i);
        }
        return romset;
    }

    private final String[] resources;
    private final byte[][] roms;
    private final String name;

    RomSet(String name, String[] resources) {
        this.name = name;
        this.resources = resources;
        roms = new byte[resources.length][];
        for (int i = 0; i < resources.length; i++) {
            try {
                roms[i] = Util.fromInputStream(RomSet.class.getResourceAsStream(resources[i]));
            } catch (Exception e) {
                throw new RuntimeException("Loading resource " + resources[i], e);
            }
        }
    }

    public byte[] getRom(int i) {
        if (i < resources.length) {
            if (roms[i] == null) {
                try {
                    roms[i] = Util.fromInputStream(RomSet.class.getResourceAsStream(resources[i]));
                } catch (Exception e) {
                    LOGGER.error("Trying to load ROM from resource " + resources[i], e);
                    throw new RuntimeException(e);
                }
            }
            return roms[i];
        } else {
            throw new IllegalArgumentException("Index out of bounds");
        }
    }

    public String getName() {
        return name;
    }

    public String[] getResources() {
        return resources;
    }
}
