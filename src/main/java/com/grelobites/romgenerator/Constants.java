package com.grelobites.romgenerator;

import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.PreferencesProvider;
import com.grelobites.romgenerator.util.Util;

import java.io.IOException;
import java.io.InputStream;

public class Constants {

    public static final String ROMSET_PROVIDED = "__ROMSET_PROVIDED__";

    public static final int SECTOR_SIZE = 0x1000;
    public static final int SLOT_SIZE = 0x4000;

    private static final String DEFAULT_VERSION = "10.4.3";

    public static final int CHARSET_SIZE = 768;

    public static final int SNA_HEADER_SIZE = 27;
    public static final int SNA_EXTENDED_HEADER_SIZE = 31;
	public static final int SPECTRUM_SCREEN_WIDTH = 256;
	public static final int SPECTRUM_SCREEN_HEIGHT = 192;
	public static final int SPECTRUM_SCREEN_SIZE = 6144;
	public static final int SPECTRUM_COLORINFO_SIZE = 768;
	public static final int SPECTRUM_FULLSCREEN_SIZE = SPECTRUM_SCREEN_SIZE +
			SPECTRUM_COLORINFO_SIZE;
    private static final String DEFAULT_MENU_SCREEN_RESOURCE = "menu.scr";
    private static final String SINCLAIR_SCREEN_RESOURCE = "sinclair-1982.scr";
    private static final String DEFAULT_CHARSET_RESOURCE = "charset.rom";
    private static final String THEME_RESOURCE = "view/theme.css";

    public static final String IANNA_MD5_V1 = "6ea7e538518c39a120349728aaaeae89";
    public static final String IANNA_MD5_V11 = "d8f112937f9a6b242f8654c1f9d6d2b2";
    public static final String IANNA_MD5_V111 = "101289fc50b0877b55319efc17c4ea81";

    private static final String IANNA_SCREEN_RESOURCE = "ianna.scr";

    public static final String CASTLEVANIA_MD5_V1 = "1ee9b70e785157511f62547150cf3f15";
    public static final String CASTLEVANIA_MD5_V2 = "a2dea4861b2ef343b5762b1461d90832";
    public static final String CASTLEVANIA_SCREEN_RESOURCE = "castlevania.scr";

    public static final String[][] KNOWN_ROMS = {
            {IANNA_MD5_V1, IANNA_SCREEN_RESOURCE},
            {IANNA_MD5_V11, IANNA_SCREEN_RESOURCE},
            {IANNA_MD5_V111, IANNA_SCREEN_RESOURCE},
            {CASTLEVANIA_MD5_V1, CASTLEVANIA_SCREEN_RESOURCE},
            {CASTLEVANIA_MD5_V2, CASTLEVANIA_SCREEN_RESOURCE}
    };

    public static final String[] KNOWN_DAN_SNAPS = {
            "e34a0d9ced134c9a57efbc2425243180"
    };

    public static final String[] KNOWN_MULTIPLYS = {
            "98526745b7429c20a86b3c93ba55c99e"
    };

    public static final byte[] ZEROED_SLOT = new byte[SLOT_SIZE];

    public static final byte B_01 = 1;
	public static final byte B_00 = 0;
    public static final byte B_FF = -1;


    private static byte[] DEFAULT_DANDANATOR_SCREEN;
    private static byte[] SINCLAIR_SCREEN;
    private static byte[] DEFAULT_CHARSET;

    private static String THEME_RESOURCE_URL;

    //This is just to register a preferences provider
    private static PreferencesProvider providerRegister = new PreferencesProvider(
            LocaleUtil.i18n("preferencesGeneralTab"),
            "/com/grelobites/romgenerator/view/preferences.fxml", PreferencesProvider.PRECEDENCE_GLOBAL);

    private static PreferencesProvider playerPreferences = new PreferencesProvider(
            LocaleUtil.i18n("loader"),
            "/com/grelobites/romgenerator/view/playerconfiguration.fxml", PreferencesProvider.PRECEDENCE_OTHER);


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
                    Constants.class.getClassLoader()
                            .getResourceAsStream(DEFAULT_MENU_SCREEN_RESOURCE),
                    Constants.SPECTRUM_FULLSCREEN_SIZE);
        }
        return DEFAULT_DANDANATOR_SCREEN;
    }

    public static byte[] getSinclairScreen() throws IOException {
        if (SINCLAIR_SCREEN == null) {
            SINCLAIR_SCREEN = Util.fromInputStream(
                    Constants.class.getClassLoader()
                            .getResourceAsStream(SINCLAIR_SCREEN_RESOURCE),
                    Constants.SPECTRUM_FULLSCREEN_SIZE);
        }
        return SINCLAIR_SCREEN;
    }

    public static InputStream getScreenFromResource(String resource) {
        return Constants.class.getClassLoader()
                .getResourceAsStream(resource);
    }

    public static byte[] getDefaultCharset() throws IOException {
        if (DEFAULT_CHARSET == null) {
            DEFAULT_CHARSET = Util.fromInputStream(
                    Constants.class.getClassLoader()
                            .getResourceAsStream(DEFAULT_CHARSET_RESOURCE),
                    CHARSET_SIZE);
        }
        return DEFAULT_CHARSET;
    }

    public static String getThemeResourceUrl() {
        if (THEME_RESOURCE_URL == null) {
            THEME_RESOURCE_URL = Constants.class.getResource(THEME_RESOURCE)
                    .toExternalForm();
        }
        return THEME_RESOURCE_URL;
    }

}
