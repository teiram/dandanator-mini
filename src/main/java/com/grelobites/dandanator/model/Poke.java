package com.grelobites.dandanator.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Poke {

    private String name;
    private List<AddressValue> addressValues;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AddressValue> getAddressValues() {
        if (addressValues == null) {
            return Collections.EMPTY_LIST;
        } else {
            return addressValues;
        }
    }

    public void setAddressValues(List<AddressValue> addressValues) {
        this.addressValues = addressValues;
    }

    public void addAddressValue(AddressValue addressValue) {
        if (this.addressValues == null) {
            this.addressValues = new ArrayList<AddressValue>();
        }
        this.addressValues.add(addressValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Poke poke = (Poke) o;

        if (name != null ? !name.equals(poke.name) : poke.name != null) return false;
        return addressValues != null ? addressValues.equals(poke.addressValues) : poke.addressValues == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (addressValues != null ? addressValues.hashCode() : 0);
        return result;
    }
}
