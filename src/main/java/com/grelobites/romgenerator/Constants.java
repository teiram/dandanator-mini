package com.grelobites.romgenerator;

import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.util.PreferencesProvider;
import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Constants {

    public static final String ROMSET_PROVIDED = "__ROMSET_PROVIDED__";
    public static final int SLOT_SIZE = 0x4000;

    private static final String DEFAULT_VERSION = "5.0";

    public static final int CHARSET_SIZE = 768;

    public static final int SNA_HEADER_SIZE = 27;
    public static final int SNA_EXTENDED_HEADER_SIZE = 31;
	public static final int SPECTRUM_SCREEN_WIDTH = 256;
	public static final int SPECTRUM_SCREEN_HEIGHT = 192;
	public static final int SPECTRUM_SCREEN_SIZE = 6144;
	public static final int SPECTRUM_SCREEN_OFFSET = 0x4000;
	public static final int SPECTRUM_COLORINFO_SIZE = 768;
	public static final int SPECTRUM_FULLSCREEN_SIZE = SPECTRUM_SCREEN_SIZE +
			SPECTRUM_COLORINFO_SIZE;
    private static final String DEFAULT_MENU_SCREEN_RESOURCE = "menu.scr";
    private static final String SINCLAIR_SCREEN_RESOURCE = "sinclair-1982.scr";
    private static final String DEFAULT_CHARSET_RESOURCE = "charset.rom";

    public static final byte B_01 = 1;
	public static final byte B_00 = 0;
	public static final byte B_10 = 0x10;

    private static byte[] DEFAULT_DANDANATOR_SCREEN;
    private static byte[] SINCLAIR_SCREEN;
    private static byte[] DEFAULT_CHARSET;

    //This is just to register a preferences provider
    private static PreferencesProvider providerRegister = new PreferencesProvider("General",
            "/com/grelobites/romgenerator/view/preferences.fxml", true);


    public static String currentVersion() {
        String version = Constants.class.getPackage()
                .getImplementationVersion();
        if (version == null) {
            version = DEFAULT_VERSION;
        }
        return version;
    }

    public static byte[] getDefaultMenuScreen() throws IOException {
        if (DEFAULT_DANDANATOR_SCREEN == null) {
            DEFAULT_DANDANATOR_SCREEN = Util.fromInputStream(
                    DandanatorMiniConstants.class.getClassLoader()
                            .getResourceAsStream(DEFAULT_MENU_SCREEN_RESOURCE),
                    Constants.SPECTRUM_FULLSCREEN_SIZE);
        }
        return DEFAULT_DANDANATOR_SCREEN;
    }

    public static byte[] getSinclairScreen() throws IOException {
        if (SINCLAIR_SCREEN == null) {
            SINCLAIR_SCREEN = Util.fromInputStream(
                    DandanatorMiniConstants.class.getClassLoader()
                            .getResourceAsStream(SINCLAIR_SCREEN_RESOURCE),
                    Constants.SPECTRUM_FULLSCREEN_SIZE);
        }
        return SINCLAIR_SCREEN;
    }

    public static byte[] getDefaultCharset() throws IOException {
        if (DEFAULT_CHARSET == null) {
            DEFAULT_CHARSET = Util.fromInputStream(
                    DandanatorMiniConstants.class.getClassLoader()
                            .getResourceAsStream(DEFAULT_CHARSET_RESOURCE),
                    CHARSET_SIZE);
        }
        return DEFAULT_CHARSET;
    }
}
