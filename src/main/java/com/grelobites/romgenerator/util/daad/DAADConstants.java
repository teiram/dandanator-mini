package com.grelobites.romgenerator.util.daad;

import com.grelobites.romgenerator.util.Util;

import java.io.IOException;

public class DAADConstants {

    public static final int METADATA_OFFSET = 15554;
    public static final int METADATA_SIZE = 830;
    public static final int BINARY_PARTS = 3;

    private static final String DAAD_LOADER_RESOURCE = "/daad/loader.bin";
    private static byte[] DAAD_LOADER;

    public static byte[] getDAADLoader() throws IOException {
        if (DAAD_LOADER == null) {
            DAAD_LOADER = Util.fromInputStream(
                    DAADConstants.class.getClassLoader()
                            .getResourceAsStream(DAAD_LOADER_RESOURCE));
        }
        return DAAD_LOADER;
    }

}
