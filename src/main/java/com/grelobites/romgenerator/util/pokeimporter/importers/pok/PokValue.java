package com.grelobites.romgenerator.util.pokeimporter.importers.pok;

public class PokValue {
    private static final int MIN_ADDR_VALUE = 16384;
    private static final int MAX_ADDR_VALUE = 65535;

    private Integer address;
    private Integer value;
    private Integer originalValue;
    private Integer bank;

    private static Integer checkAddressRange(Integer address) {
        if (address < MIN_ADDR_VALUE || address > MAX_ADDR_VALUE) {
            throw new IllegalArgumentException("Address out of range: " + address);
        } else {
            return address;
        }
    }

    public Integer getValue() {
        return value;
    }

    public Integer getAddress() {
        return address;
    }

    public void setAddress(Integer address) {
        this.address = checkAddressRange(address & 0xffff);
    }

    public void setAddress(String address) {
        setAddress(Integer.parseInt(address.trim()));
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public void setValue(String value) {
        setValue(Integer.parseInt(value.trim()));
    }

    public Integer getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(Integer originalValue) {
        this.originalValue = originalValue & 0xff;
    }

    public void setOriginalValue(String originalValue) {
        setOriginalValue(Integer.parseInt(originalValue.trim()));
    }

    public Integer getBank() {
        return bank;
    }

    public void setBank(Integer bank) {
        this.bank = bank & 0x0f;
    }

    public void setBank(String bank) {
        setBank(Integer.parseInt(bank.trim()));
    }

    public boolean isCompatibleSpectrum48K() {
        return (this.bank & 0x08) != 0;
    }

    public boolean isInteractive() {
        return this.value == 256;
    }

    @Override
    public String toString() {
        return "PokValue{" +
                "address=" + address +
                ", value=" + value +
                ", originalValue=" + originalValue +
                ", bank=" + bank +
                '}';
    }
}
