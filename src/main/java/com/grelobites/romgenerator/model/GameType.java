package com.grelobites.romgenerator.model;


public enum GameType {
    ROM(0),
    RAM16(1),
    RAM48(3),
    RAM128_LO(8),
    RAM128_HI(9);

    private int typeId;

    GameType(int typeId) {
        this.typeId = typeId;
    }

    public int typeId() {
        return typeId;
    }
}
