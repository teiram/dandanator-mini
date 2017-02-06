package com.grelobites.romgenerator.model;

public class ChangeValue {
    private final int bank;
    private final int address;
    private final int value;

    public ChangeValue(int bank, int address, int value) {
        this.bank = bank;
        this.address = address;
        this.value = value;
    }

    public int getBank() {
        return bank;
    }

    public int getAddress() {
        return address;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("ChangeValue{bank=%d, address=%04x, value=%02x}",
                bank, address, value);
    }

}
