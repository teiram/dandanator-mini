package com.grelobites.romgenerator.util.player;

public enum EncodingSpeed {
    SPEED_STANDARD(0, "1700 bps"),
    SPEED_TURBO_1(1, "3000 bps"),
    SPEED_TURBO_2(2, "4500 bps"),
    SPEED_LECHES_1(3, "11000 bps"),
    SPEED_LECHES_2(4, "14700 bps"),
    SPEED_LECHES_3(5, "16000 bps");

    private int speed;
    private String displayInfo;

    public static EncodingSpeed of(int speed) {
        for (EncodingSpeed item: values()) {
            if (item.speed() == speed) {
                return item;
            }
        }
        throw new IllegalArgumentException("No encoding speed defined for speed " + speed);
    }

    EncodingSpeed(int speed, String displayInfo) {
        this.speed = speed;
        this.displayInfo = displayInfo;
    }

    public int speed() {
        return speed;
    }

    @Override
    public String toString() {
        return displayInfo;
    }

}
