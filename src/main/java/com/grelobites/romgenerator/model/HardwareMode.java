package com.grelobites.romgenerator.model;

public enum HardwareMode {
    HW_48K("48k", 0),
    HW_128K("128k", 4),
    HW_PLUS2("+2", 12),
    HW_PLUS2A("+2A", 13),
    HW_PLUS3("+3", 7),
    HW_UNSUPPORTED("-", 0x7f);

    private static HardwareMode[][] MODE_TABLE = new HardwareMode[][] {
            new HardwareMode[] {
                HW_48K, HW_UNSUPPORTED, HW_UNSUPPORTED,
                    HW_128K, HW_UNSUPPORTED, HW_UNSUPPORTED,
                    HW_UNSUPPORTED, HW_PLUS3, HW_PLUS3,
                    HW_UNSUPPORTED, HW_UNSUPPORTED, HW_UNSUPPORTED,
                    HW_PLUS2, HW_PLUS2A},
            new HardwareMode[] {HW_48K, HW_UNSUPPORTED, HW_UNSUPPORTED,
                    HW_UNSUPPORTED, HW_128K, HW_UNSUPPORTED,
                    HW_UNSUPPORTED, HW_PLUS3, HW_PLUS3,
                    HW_UNSUPPORTED, HW_UNSUPPORTED, HW_UNSUPPORTED,
                    HW_PLUS2, HW_PLUS2A}
    };

    private static boolean validVersion(int version) {
        return version > 0 && version < 4;
    }

    public static HardwareMode fromZ80Mode(int version, int hardwareMode) {
        if (validVersion(version)) {
            if (version == 1) {
                return HW_48K;
            } else {
                HardwareMode[] versionModes = MODE_TABLE[version - 2];
                return hardwareMode < versionModes.length ? versionModes[hardwareMode] : HW_UNSUPPORTED;
            }
        } else {
            throw new IllegalArgumentException("Invalid Z80 version provided");
        }
    }

    public static HardwareMode fromIntValueMode(int mode) {
        return fromZ80Mode(3, mode);
    }

    private int intValue;
    private String displayName;

    HardwareMode(String displayName, int intValue) {
        this.intValue = intValue;
        this.displayName = displayName;
    }
    public int intValue() {
        return intValue;
    }

    public String displayName() {
        return displayName;
    }
}
