package com.grelobites.dandanator;

import com.grelobites.dandanator.util.LocaleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class Constants {

    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.class);

    private static final String DEFAULT_DANDANATOR_SCREEN_RESOURCE = "dandanator.scr";
    private static final String SINCLAIR_SCREEN_RESOURCE = "sinclair-1982.scr";
    private static final String DEFAULT_CHARSET_RESOURCE = "charset.rom";
    private static final String DANDANATOR_ROM_RESOURCE = "dandanator-mini.rom";
    private static final String EXTRA_ROM_RESOURCE = "test.rom";
    private static final String DEFAULT_VERSION = "4.3";

	public static final int SNA_HEADER_SIZE = 27;
	public static final int SPECTRUM_SCREEN_WIDTH = 256;
	public static final int SPECTRUM_SCREEN_HEIGHT = 192;
	public static final int SPECTRUM_SCREEN_SIZE = 6144;
	public static final int SPECTRUM_SCREEN_OFFSET = 0x4000;
	public static final int SPECTRUM_COLORINFO_SIZE = 768;
	public static final int SPECTRUM_FULLSCREEN_SIZE = SPECTRUM_SCREEN_SIZE +
			SPECTRUM_COLORINFO_SIZE;

    public static final int SLOT_COUNT = 10;
    public static final int GAMENAME_SIZE = 33;
    public static final int POKE_ENTRY_SIZE = 3;
    public static final int POKE_ZONE_SIZE = 3200;
    public static final int POKE_HEADER_SIZE = 3 * SLOT_COUNT;

    public static final int SLOT_SIZE = 0x4000;

	public static final int BASEROM_SIZE = 4096;
	public static final int CHARSET_SIZE = 768;
	public static final byte B_01 = 1;
	public static final byte B_00 = 0;
	public static final byte B_10 = 0x10;
    public static final int MAX_POKES_PER_TRAINER = 6;
    public static final int MAX_TRAINERS_PER_GAME = 8;

    public static final String DEFAULT_EXTRAROMKEY_MESSAGE = LocaleUtil.i18n("extraRomDefaultMessage");
    public static final String DEFAULT_TOGGLEPOKESKEY_MESSAGE = LocaleUtil.i18n("togglePokesDefaultMessage");
    public static final String DEFAULT_LAUNCHGAME_MESSAGE = LocaleUtil.i18n("launchGameDefaultMessage");
    public static final String DEFAULT_SELECTPOKE_MESSAGE = LocaleUtil.i18n("selectPokesDefaultMessage");
    public static final int TOGGLE_POKES_MESSAGE_MAXLENGTH = 23;
    public static final int EXTRA_ROM_MESSAGE_MAXLENGTH = 23;
    public static final int LAUNCH_GAME_MESSAGE_MAXLENGTH = 23;
    public static final int SELECT_POKE_MESSAGE_MAXLENGTH = 23;

    private static byte[] DEFAULT_DANDANATOR_SCREEN;
    private static byte[] SINCLAIR_SCREEN;
    private static byte[] DEFAULT_CHARSET;
    private static byte[] DANDANATOR_ROM;
    private static byte[] EXTRA_ROM;

    private static byte[] fromInputStream(InputStream is, int size) throws IOException {
        byte[] result = new byte[size];
        int len = is.read(result, 0, size);
        if (len != size) {
            LOGGER.warn("Unexpected number of bytes read from stream. Read: " + len
            + ", expected: " + size);
        }
        return result;
    }

    public static byte[] getDefaultDandanatorScreen() throws IOException {
        if (DEFAULT_DANDANATOR_SCREEN == null) {
            DEFAULT_DANDANATOR_SCREEN = fromInputStream(
                    Constants.class.getClassLoader()
                    .getResourceAsStream(DEFAULT_DANDANATOR_SCREEN_RESOURCE),
                    SPECTRUM_FULLSCREEN_SIZE);
        }
        return DEFAULT_DANDANATOR_SCREEN;
    }

    public static byte[] getSinclairScreen() throws IOException {
        if (SINCLAIR_SCREEN == null) {
            SINCLAIR_SCREEN = fromInputStream(
                    Constants.class.getClassLoader()
                            .getResourceAsStream(SINCLAIR_SCREEN_RESOURCE),
                    SPECTRUM_FULLSCREEN_SIZE);
        }
        return SINCLAIR_SCREEN;
    }

    public static byte[] getDefaultCharset() throws IOException {
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

    public static byte[] getExtraRom() throws IOException {
        if (EXTRA_ROM == null) {
            EXTRA_ROM = fromInputStream(
                    Constants.class.getClassLoader()
                            .getResourceAsStream(EXTRA_ROM_RESOURCE),
                    SLOT_SIZE);
        }
        return EXTRA_ROM;
    }

    public static String currentVersion() {
        String version = Constants.class.getPackage()
                .getImplementationVersion();
        if (version == null) {
            version = DEFAULT_VERSION;
        }
        return version;
    }

}
