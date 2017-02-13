package com.grelobites.romgenerator.model;

public class ChangeValue {
    private final int address;
    private final int value;

    public ChangeValue(int address, int value) {
        this.address = address;
        this.value = value;
    }

    public int getAddress() {
        return address;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("ChangeValue{address=%04x, value=%02x}",
                address, value);
    }

}
