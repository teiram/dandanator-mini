package com.grelobites.romgenerator.handlers.dandanatormini.v9;

public class V9Constants {
    protected static final int VERSION_SIZE = 8;
    protected static final int CBLOCKS_OFFSET = 16360;
    protected static final int GAME_STRUCT_SIZE = 131;
    protected static final int BORDER_EFFECT_OFFSET = 16380;
    protected static final int GREY_ZONE_OFFSET = 6860;
    protected static final int VERSION_OFFSET = 16352;

    protected static final int BASEROM_SIZE = 3584;
    protected static final int SNA_HEADER_SIZE = 31;
    protected static final int GAME_LAUNCH_SIZE = 18;
    protected static final int GAME_STRUCT_OFFSET = 3585;

    protected static int EXTRA_ROM_SLOT = 32;
    protected static int INTERNAL_ROM_SLOT = 33;

    protected static int DANTAP_ROM_SLOT = 30;
    protected static final int DANTAP_COMMON_CODE_LENGTH = 162;
    protected static final int DANTAP_TABLE_CODE_LENGTH = 136;
    protected static int DANTAP_TAPTABLE_OFFSET = 2 + DANTAP_COMMON_CODE_LENGTH + DANTAP_TABLE_CODE_LENGTH;

    protected static final int HEADER_7FFD_OFFSET = 29;
    protected static final int HEADER_1FFD_OFFSET = 30;
}
