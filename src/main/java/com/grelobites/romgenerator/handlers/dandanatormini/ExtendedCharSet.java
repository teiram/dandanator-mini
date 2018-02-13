package com.grelobites.romgenerator.handlers.dandanatormini;

import com.grelobites.romgenerator.Constants;

public class ExtendedCharSet {

    public static final byte[] SYMBOL_PLUS2A_0 = new byte[]{
            (byte) 0x3F,
            (byte) 0x3F,
            (byte) 0x36,
            (byte) 0x23,
            (byte) 0x37,
            (byte) 0x3E,
            (byte) 0x1F,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_PLUS2A_1 = new byte[]{
            (byte) 0xFC,
            (byte) 0x76,
            (byte) 0xAA,
            (byte) 0xA2,
            (byte) 0x6A,
            (byte) 0x2A,
            (byte) 0xFE,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_128K_0 = new byte[]{
            (byte) 0x3F,
            (byte) 0x37,
            (byte) 0x26,
            (byte) 0x37,
            (byte) 0x37,
            (byte) 0x22,
            (byte) 0x1F,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_128K_1 = new byte[]{
            (byte) 0xFC,
            (byte) 0x76,
            (byte) 0xAA,
            (byte) 0xB6,
            (byte) 0x6A,
            (byte) 0x36,
            (byte) 0xFE,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_48K_0 = new byte[]{
            (byte) 0x3F,
            (byte) 0x3B,
            (byte) 0x32,
            (byte) 0x2B,
            (byte) 0x22,
            (byte) 0x3B,
            (byte) 0x1F,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_48K_1 = new byte[]{
            (byte) 0xFC,
            (byte) 0x6A,
            (byte) 0xA6,
            (byte) 0x6E,
            (byte) 0xA6,
            (byte) 0x6A,
            (byte) 0xFE,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_16K_0 = new byte[]{
            (byte) 0x09,
            (byte) 0x19,
            (byte) 0x09,
            (byte) 0x89,
            (byte) 0x89,
            (byte) 0x09,
            (byte) 0x1D,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_16K_1 = new byte[]{
            (byte) 0xD0,
            (byte) 0x50,
            (byte) 0x14,
            (byte) 0xD8,
            (byte) 0x58,
            (byte) 0x54,
            (byte) 0x04,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_ROM_0 = new byte[]{
            (byte) 0x3F,
            (byte) 0x23,
            (byte) 0x2A,
            (byte) 0x22,
            (byte) 0x26,
            (byte) 0x2B,
            (byte) 0x1F,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_ROM_1 = new byte[]{
            (byte) 0xFC,
            (byte) 0x6A,
            (byte) 0xA2,
            (byte) 0xA2,
            (byte) 0xAA,
            (byte) 0x6A,
            (byte) 0xFE,
            (byte) 0x00
    };

    public static final int SYMBOL_SPACE = 32;
    public static final int CHARSET_OFFSET = SYMBOL_SPACE;

    public static final int SYMBOL_128K_0_CODE = 128;
    public static final int SYMBOL_128K_1_CODE = 129;

    public static final int SYMBOL_48K_0_CODE = 130;
    public static final int SYMBOL_48K_1_CODE = 131;

    public static final int SYMBOL_16K_0_CODE = 132;
    public static final int SYMBOL_16K_1_CODE = 133;

    public static final int SYMBOL_ROM_0_CODE = 134;
    public static final int SYMBOL_ROM_1_CODE = 135;

    public static final int SYMBOL_PLUS2A_0_CODE = 136;
    public static final int SYMBOL_PLUS2A_1_CODE = 137;

    private byte[] charset;

    private static void appendSymbolChars(byte[] charset) {
        System.arraycopy(SYMBOL_128K_0, 0, charset, (SYMBOL_128K_0_CODE - CHARSET_OFFSET) * 8, SYMBOL_128K_0.length);
        System.arraycopy(SYMBOL_128K_1, 0, charset, (SYMBOL_128K_1_CODE - CHARSET_OFFSET) * 8, SYMBOL_128K_1.length);
        System.arraycopy(SYMBOL_48K_0, 0, charset, (SYMBOL_48K_0_CODE - CHARSET_OFFSET) * 8, SYMBOL_48K_0.length);
        System.arraycopy(SYMBOL_48K_1, 0, charset, (SYMBOL_48K_1_CODE - CHARSET_OFFSET) * 8, SYMBOL_48K_1.length);
        System.arraycopy(SYMBOL_16K_0, 0, charset, (SYMBOL_16K_0_CODE - CHARSET_OFFSET) * 8, SYMBOL_16K_0.length);
        System.arraycopy(SYMBOL_16K_1, 0, charset, (SYMBOL_16K_1_CODE - CHARSET_OFFSET) * 8, SYMBOL_16K_1.length);
        System.arraycopy(SYMBOL_ROM_0, 0, charset, (SYMBOL_ROM_0_CODE - CHARSET_OFFSET) * 8, SYMBOL_ROM_0.length);
        System.arraycopy(SYMBOL_ROM_1, 0, charset, (SYMBOL_ROM_1_CODE - CHARSET_OFFSET) * 8, SYMBOL_ROM_1.length);
        System.arraycopy(SYMBOL_PLUS2A_0, 0, charset, (SYMBOL_PLUS2A_0_CODE - CHARSET_OFFSET) * 8, SYMBOL_PLUS2A_0.length);
        System.arraycopy(SYMBOL_PLUS2A_1, 0, charset, (SYMBOL_PLUS2A_1_CODE - CHARSET_OFFSET) * 8, SYMBOL_PLUS2A_1.length);
    }

    public ExtendedCharSet(byte[] charset) {
        this.charset = new byte[DandanatorMiniConstants.EXTENDED_CHARSET_SIZE];
        System.arraycopy(charset, 0, this.charset, 0, Constants.CHARSET_SIZE);
        appendSymbolChars(this.charset);
    }

    public byte[] getCharSet() {
        return charset;
    }
}
