package com.grelobites.romgenerator.model;

public enum HardwareMode {
    HW_48K("48k", 0, true),
    HW_48K_IF1("48k+IF1", 0, false),
    HW_48K_MGT("48k+MGT", 0, false),
    HW_SAMRAM("SamRam", 0, false),
    HW_128K("128k", 4, true),
    HW_128K_IF1("128k+IF1", 4, false),
    HW_128K_MGT("128k+MGT", 4, false),
    HW_PLUS2("+2", 12, true),
    HW_PLUS2A("+2A", 13, true),
    HW_PLUS3("+3", 7, true),
    HW_PENTAGON("Pentagon", 4, false),
    HW_SCORPION("Scorpion", 4, false),
    HW_DIDAKTIK("Didaktik", 4, false),
    HW_TC2048("TC2048", 4, false),
    HW_TC2068("TC2068", 4, false),
    HW_UNKNOWN("Unknown", 4, false);

    private static HardwareMode[][] MODE_TABLE = new HardwareMode[][] {
            new HardwareMode[] {
                HW_48K, HW_48K_IF1, HW_SAMRAM,
                    HW_128K, HW_128K_IF1, HW_UNKNOWN, HW_UNKNOWN,
                    HW_PLUS3, HW_PLUS3,
                    HW_PENTAGON, HW_SCORPION, HW_DIDAKTIK,
                    HW_PLUS2, HW_PLUS2A,
                    HW_TC2048, HW_TC2068},
            new HardwareMode[] {
                    HW_48K, HW_48K_IF1, HW_SAMRAM,
                    HW_48K_MGT, HW_128K, HW_128K_IF1,
                    HW_128K_MGT, HW_PLUS3, HW_PLUS3,
                    HW_PENTAGON, HW_SCORPION, HW_DIDAKTIK,
                    HW_PLUS2, HW_PLUS2A,
                    HW_TC2048, HW_TC2068}
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
                return hardwareMode < versionModes.length ? versionModes[hardwareMode] : HW_UNKNOWN;
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
    private boolean supported;

    HardwareMode(String displayName, int intValue, boolean supported) {
        this.intValue = intValue;
        this.displayName = displayName;
        this.supported = supported;
    }
    public int intValue() {
        return intValue;
    }

    public String displayName() {
        return displayName;
    }

    public boolean supported() {
        return supported;
    }
}
