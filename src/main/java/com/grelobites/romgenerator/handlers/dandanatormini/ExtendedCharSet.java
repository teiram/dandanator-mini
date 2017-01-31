package com.grelobites.romgenerator.handlers.dandanatormini;

import com.grelobites.romgenerator.Constants;

public class ExtendedCharSet {
    public static final byte[] SYMBOL_128K_0 = new byte[]{
            (byte) 0x09,
            (byte) 0x18,
            (byte) 0x08,
            (byte) 0x89,
            (byte) 0x89,
            (byte) 0x09,
            (byte) 0x1D,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_128K_1 = new byte[]{
            (byte) 0xDC,
            (byte) 0x54,
            (byte) 0x54,
            (byte) 0xDC,
            (byte) 0x14,
            (byte) 0x14,
            (byte) 0xDC,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_48K_0 = new byte[]{
            (byte) 0x15,
            (byte) 0x15,
            (byte) 0x15,
            (byte) 0x9D,
            (byte) 0x85,
            (byte) 0x05,
            (byte) 0x05,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_48K_1 = new byte[]{
            (byte) 0xD0,
            (byte) 0x50,
            (byte) 0x54,
            (byte) 0xD8,
            (byte) 0x58,
            (byte) 0x54,
            (byte) 0xD4,
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
            (byte) 0x1D,
            (byte) 0x15,
            (byte) 0x15,
            (byte) 0x99,
            (byte) 0x95,
            (byte) 0x15,
            (byte) 0x15,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_ROM_1 = new byte[]{
            (byte) 0xD4,
            (byte) 0x5C,
            (byte) 0x5C,
            (byte) 0x54,
            (byte) 0x54,
            (byte) 0x54,
            (byte) 0xD4,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_SAD_FACE = new byte[] {
            (byte) 0x2C,
            (byte) 0x7E,
            (byte) 0xFF,
            (byte) 0xDB,
            (byte) 0xDB,
            (byte) 0xFF,
            (byte) 0xC3,
            (byte) 0x7E
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

    public static final int SYMBOL_SAD_FACE_CODE = 136;

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
        System.arraycopy(SYMBOL_SAD_FACE, 0, charset, (SYMBOL_SAD_FACE_CODE - CHARSET_OFFSET) * 8, SYMBOL_SAD_FACE.length);
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
