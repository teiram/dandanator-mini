package com.grelobites.dandanator.model;

import java.util.Arrays;

/**
 * Created by mteira on 17/6/16.
 */
public class AddressValue {

    private byte[] address = new byte[2];
    private byte value;

    private static void checkInRange(int value, int minValue, int maxValue) {
        if ((value < minValue ) || (value > maxValue)) {
            throw new IllegalArgumentException("Value out of range [" + minValue + ", " + maxValue);
        }
    }

    public AddressValue(String address, String value) {
        int addressIntRep = Integer.parseInt(address);
        int valueIntRep = Integer.parseInt(value);
        checkInRange(addressIntRep, 0, 0xffff);
        checkInRange(valueIntRep, 0, 0xff);
        this.address[0] = (byte) (addressIntRep & 0xff);
        this.address[1] = (byte) ((addressIntRep >> 8) & 0xff);
        this.value = (byte) (valueIntRep & 0xff);
    }

    public byte[] address() {
        return this.address;
    }

    public byte value() {
        return this.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AddressValue that = (AddressValue) o;

        if (value != that.value) return false;
        return Arrays.equals(address, that.address);

    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(address);
        result = 31 * result + (int) value;
        return result;
    }
}
