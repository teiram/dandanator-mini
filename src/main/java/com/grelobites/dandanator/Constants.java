package com.grelobites.dandanator;

import java.io.IOException;
import java.io.InputStream;

public class Constants {

    private static final String DEFAULT_DANDANATOR_SCREEN_RESOURCE = "dandanator.scr";
    private static final String SINCLAIR_SCREEN_RESOURCE = "sinclair-1982.scr";
    private static final String DEFAULT_CHARSET_RESOURCE = "charset.rom";
    private static final String DANDANATOR_ROM_RESOURCE = "dandanator-mini.rom";
    private static final String TEST_ROM_RESOURCE = "test.rom";

	public static final int SNA_HEADER_SIZE = 27;
	public static final int SPECTRUM_SCREEN_WIDTH = 256;
	public static final int SPECTRUM_SCREEN_HEIGHT = 192;
	public static final int SPECTRUM_SCREEN_SIZE = 6144;
	public static final int SPECTRUM_SCREEN_OFFSET = 0x4000;
	public static final int SPECTRUM_COLORINFO_SIZE = 768;
	public static final int SPECTRUM_FULLSCREEN_SIZE = SPECTRUM_SCREEN_SIZE +
			SPECTRUM_COLORINFO_SIZE;

    public static final int SLOTS_512K_ROM = 10;
    public static final int MAX_SLOTS = SLOTS_512K_ROM;
    public static final int GAMENAME_SIZE = 33;
    public static final int POKE_ENTRY_SIZE = 3;
    public static final int POKE_ZONE_SIZE = 3200;
    public static final int POKE_HEADER_SIZE = 3 * MAX_SLOTS;

    public static final int SLOT_SIZE = 0x4000;

	public static final int BASEROM_SIZE = 4096;
	public static final int CHARSET_SIZE = 768;
	public static final byte B_01 = 1;
	public static final byte B_00 = 0;
	public static final byte B_10 = 0x10;
    public static final int MAX_POKES_PER_TRAINER = 6;
    public static final int MAX_TRAINERS_PER_GAME = 8;

    public static final String DEFAULT_TESTROMKEY_MESSAGE = "Test Rom";
    public static final String DEFAULT_TOGGLEPOKESKEY_MESSAGE = "Toggle Pokes";
    public static final String DEFAULT_LAUNCHGAME_MESSAGE = "Launch Game";
    public static final String DEFAULT_SELECTPOKE_MESSAGE = "Select Pokes";

    private static byte[] DEFAULT_DANDANATOR_SCREEN;
    private static byte[] SINCLAIR_SCREEN;
    private static byte[] DEFAULT_CHARSET;
    private static byte[] DANDANATOR_ROM;
    private static byte[] TEST_ROM;

    private static byte[] fromInputStream(InputStream is, int size) throws IOException {
        byte[] result = new byte[size];
        is.read(result, 0, size);
        return result;
    }

    public static final byte[] getDefaultDandanatorScreen() throws IOException {
        if (DEFAULT_DANDANATOR_SCREEN == null) {
            DEFAULT_DANDANATOR_SCREEN = fromInputStream(
                    Constants.class.getClassLoader()
                    .getResourceAsStream(DEFAULT_DANDANATOR_SCREEN_RESOURCE),
                    SPECTRUM_FULLSCREEN_SIZE);
        }
        return DEFAULT_DANDANATOR_SCREEN;
    }

    public static final byte[] getSinclairScreen() throws IOException {
        if (SINCLAIR_SCREEN == null) {
            SINCLAIR_SCREEN = fromInputStream(
                    Constants.class.getClassLoader()
                            .getResourceAsStream(SINCLAIR_SCREEN_RESOURCE),
                    SPECTRUM_FULLSCREEN_SIZE);
        }
        return SINCLAIR_SCREEN;
    }

    public static final byte[] getDefaultCharset() throws IOException {
        if (DEFAULT_CHARSET == null) {
            DEFAULT_CHARSET = fromInputStream(
                    Constants.class.getClassLoader()
                            .getResourceAsStream(DEFAULT_CHARSET_RESOURCE),
                    CHARSET_SIZE);
        }
        return DEFAULT_CHARSET;
    }

    public static byte[] getDandanatorRom() throws IOException {
        if (DANDANATOR_ROM == null) {
            DANDANATOR_ROM = fromInputStream(
                    Constants.class.getClassLoader()
                            .getResourceAsStream(DANDANATOR_ROM_RESOURCE),
                    BASEROM_SIZE);
        }
        return DANDANATOR_ROM;
    }

    public static byte[] getTestRom() throws IOException {
        if (TEST_ROM == null) {
            TEST_ROM = fromInputStream(
                    Constants.class.getClassLoader()
                            .getResourceAsStream(TEST_ROM_RESOURCE),
                    SLOT_SIZE);
        }
        return TEST_ROM;
    }

    public static String currentVersion() {
        String version = Constants.class.getPackage()
                .getImplementationVersion();
        if (version == null) {
            version = "Developing";
        }
        return version;
    }

}
