package com.grelobites.romgenerator.util.daad;

import com.grelobites.romgenerator.util.Util;

import java.io.IOException;

public class DAADConstants {

    public static final int METADATA_OFFSET = 15298;
    public static final int METADATA_SIZE = 1086;
    public static final int BINARY_PARTS = 3;
    public static final int MLD_ALLOCATED_SECTORS = 2;
    public static final String MLD_SIGNATURE = "MLD";

    private static final String DAAD_LOADER_RESOURCE = "daad/loader.bin";
    private static byte[] DAAD_LOADER;

    private static final String DAAD_SCREEN_RESOURCE = "daad/screen.scr";
    private static byte[] DAAD_SCREEN;

    public static byte[] getDAADLoader() throws IOException {
        if (DAAD_LOADER == null) {
            DAAD_LOADER = Util.fromInputStream(
                    DAADConstants.class.getClassLoader()
                            .getResourceAsStream(DAAD_LOADER_RESOURCE));
        }
        return DAAD_LOADER;
    }

    public static byte[] getDefaultScreen() throws IOException {
        if (DAAD_SCREEN == null) {
            DAAD_SCREEN = Util.fromInputStream(
                    DAADConstants.class.getClassLoader()
                            .getResourceAsStream(DAAD_SCREEN_RESOURCE));
        }
        return DAAD_SCREEN;
    }

}
