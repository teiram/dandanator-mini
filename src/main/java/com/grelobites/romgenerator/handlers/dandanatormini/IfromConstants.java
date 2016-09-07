package com.grelobites.romgenerator.handlers.dandanatormini;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.PreferencesProvider;
import com.grelobites.romgenerator.util.Util;

import java.io.IOException;

public class IfromConstants {
    public static final int SLOT_COUNT = 10;
    public static final int MAX_GAMES = 25;
    public static final int GAME_SLOTS = 30; //Total ROM Size is 32 slots (minus 1 slot (code) and last (test rom))
    public static final int POKE_HEADER_SIZE = 3 * SLOT_COUNT;
    public static final int GAMENAME_SIZE = 33;
    public static final int GAMENAME_EFFECTIVE_SIZE = 29;
    public static final int POKE_ENTRY_SIZE = 3;
    public static final int POKE_NAME_SIZE = 24;
    public static final int POKE_EFFECTIVE_NAME_SIZE = 20;
    public static final int POKE_ZONE_SIZE = 3200;
    public static final int MAX_POKES_PER_TRAINER = 6;
    public static final int MAX_TRAINERS_PER_GAME = 8;
    public static final String DEFAULT_CUSTOMROMKEY_MESSAGE = LocaleUtil.i18n("preferences.ifrom.customRomLabel");
    public static final String DEFAULT_TOGGLEPOKESKEY_MESSAGE = LocaleUtil.i18n("togglePokesDefaultMessage");
    public static final String DEFAULT_LAUNCHGAME_MESSAGE = LocaleUtil.i18n("launchGameDefaultMessage");
    public static final String DEFAULT_SELECTPOKE_MESSAGE = LocaleUtil.i18n("selectPokesDefaultMessage");
    public static final int TOGGLE_POKES_MESSAGE_MAXLENGTH = 23;
    public static final int EXTRA_ROM_MESSAGE_MAXLENGTH = 23;
    public static final int LAUNCH_GAME_MESSAGE_MAXLENGTH = 23;
    public static final int SELECT_POKE_MESSAGE_MAXLENGTH = 23;
    private static final String IFROM_ROM_RESOURCE = "ifrom/ifrom.rom";
    private static final String CUSTOM_ROM_RESOURCE = "ifrom/custom.rom";
    public static final int POKE_TARGET_ADDRESS = 49284;
    public static final int GAME_CHUNK_SIZE = 256;
    public static final int GAME_CHUNK_SLOT = 2;
    public static final int VERSION_SIZE = 32;
    public static final int FILLER_BYTE = 0xFF;
    public static final int EXTENDED_CHARSET_SIZE = 896;
    public static final int BASEROM_SIZE = 3072;
    public static final int ROM_RET_JUMP_LOCATION = 0x006c;
    private static final String DEFAULT_BACKGROUND_IMAGE_RESOURCE = "ifrom/menu.scr";
    private static byte[] BASE_ROM;
    private static byte[] CUSTOM_ROM;
    private static byte[] DEFAULT_BACKGROUND_IMAGE;

    public static byte[] getBaseRom() throws IOException {
        if (BASE_ROM == null) {
            BASE_ROM = Util.fromInputStream(
                    IfromConstants.class.getClassLoader()
                            .getResourceAsStream(IFROM_ROM_RESOURCE),
                    BASEROM_SIZE);
        }
        return BASE_ROM;
    }

    public static byte[] getCustomRom() throws IOException {
        if (CUSTOM_ROM == null) {
            CUSTOM_ROM = Util.fromInputStream(
                    IfromConstants.class.getClassLoader()
                            .getResourceAsStream(CUSTOM_ROM_RESOURCE),
                    Constants.SLOT_SIZE);
        }
        return CUSTOM_ROM;
    }

    public static byte[] getDefaultBackgroundImage() throws IOException {
        if (DEFAULT_BACKGROUND_IMAGE == null) {
            DEFAULT_BACKGROUND_IMAGE = Util.fromInputStream(
                    IfromConstants.class.getClassLoader()
                            .getResourceAsStream(DEFAULT_BACKGROUND_IMAGE_RESOURCE),
                    Constants.SPECTRUM_FULLSCREEN_SIZE);
        }
        return DEFAULT_BACKGROUND_IMAGE;
    }
}
