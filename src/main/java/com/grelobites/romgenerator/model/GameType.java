package com.grelobites.romgenerator.model;


public enum GameType {
    ROM(0, "ROM"),
    RAM16(1, "16K"),
    RAM48(3, "48K"),
    RAM128(8, "128K"),
    RAM48_MLD(0x83, "48K MLD"),
    RAM128_MLD(0x88, "128K MLD");

    public static int MLD_MASK = 0x80;

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
        for (GameType type: GameType.values()) {
            if (type.typeId() == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown typeid " + id);
    }

    public static boolean isMLD(GameType gameType) {
        return (gameType.typeId & MLD_MASK) != 0;
    }
}
