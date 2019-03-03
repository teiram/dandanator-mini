package com.grelobites.romgenerator.model;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum GameType {
    ROM(0, "ROM"),
    RAM16(1, "16K"),
    RAM48(3, "48K"),
    RAM128(8, "128K"),
    RAM48_MLD(0x83, "48K MLD"),
    RAM128_MLD(0x88, "128K MLD"),
    DAN_SNAP(0xC3, "DAN-SNAP"),
    DAN_SNAP128(0xC8, "DAN-SNAP-128"),
    DAN_TAP(0x8B, "DAN-TAP");

    private static final Logger LOGGER = LoggerFactory.getLogger(GameType.class);
    public static int MLD_MASK = 0x80;
    public static int DAN_SNAP_MASK = 0x40;

    private int typeId;
    private String screenName;

    GameType(int typeId, String screenName) {
        this.typeId = typeId;
        this.screenName = screenName;
    }

    public int typeId() {
        return typeId;
    }

    public String screenName() {
        return screenName;
    }

    public static GameType byTypeId(int id) {
        int correctedId = id & 0xff;
        LOGGER.debug("Get GameType for {}", String.format("%02x", id));

        for (GameType type: GameType.values()) {
            if (type.typeId() == correctedId) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown typeid " +
                String.format("%02x", correctedId));
    }

    public static boolean isMLD(GameType gameType) {
        return (gameType.typeId & MLD_MASK) != 0;
    }

    public static boolean isDanSnap(GameType gameType) {
        return isMLD(gameType) && ((gameType.typeId & DAN_SNAP_MASK) != 0);
    }
}
