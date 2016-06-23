package com.grelobites.dandanator.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Poke implements PokeViewable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Poke.class);

    private final IntegerProperty addressProperty;
    private final IntegerProperty valueProperty;

    private static final ObservableList<PokeViewable> children = FXCollections.emptyObservableList();
    private final PokeViewable parent;
    private final Game owner;

    private static void checkInRange(int value, int minValue, int maxValue) {
        if ((value < minValue ) || (value > maxValue)) {
            throw new IllegalArgumentException("Value out of range [" + minValue + ", " + maxValue);
        }
    }

    @Override
    public void update(String value) {
        String[] pair = value.split(",");
        if (pair.length == 2) {
            try {
                this.addressProperty.set(Integer.parseUnsignedInt(pair[0].trim(), 10));
                this.addressProperty.set(Integer.parseUnsignedInt(pair[1].trim(), 10));
            } catch (Exception e) {
                LOGGER.warn("Updating AddressValueNode with user entry " + value);
            }
        }
        this.addressProperty.set(Integer.parseInt(pair[0]));
        this.valueProperty.set(Integer.parseInt(pair[1]));
    }

    @Override
    public String getViewRepresentation() {
        return String.format("%d,%d", getAddress(), getValue());
    }

    @Override
    public ObservableList<PokeViewable> getChildren() {
        return children;
    }

    @Override
    public void removeChild(PokeViewable entity) {
        throw new IllegalArgumentException("Unable to remove child from leaf node");
    }

    @Override
    public void addNewChild() {
        throw new IllegalArgumentException("Unable to add new child to leaf node");
    }

    @Override
    public PokeViewable getParent() {
        return parent;
    }

    @Override
    public Game getOwner() {
        return owner;
    }

    public Poke(Integer address, Integer value, Trainer parent, Game owner) {
        this.owner = owner;
        this.parent = parent;
        this.addressProperty = new SimpleIntegerProperty(address);
        this.valueProperty = new SimpleIntegerProperty(value);
    }

    public IntegerProperty addressProperty() {
        return this.addressProperty;
    }

    public IntegerProperty valueProperty() {
        return this.valueProperty;
    }

    public Integer getAddress() {
        return addressProperty.get();
    }

    public void setAddress(Integer address) {
        addressProperty.set(address);
    }

    public Integer getValue() {
        return valueProperty.get();
    }

    public void setValue(Integer value) {
        this.valueProperty.set(value);
    }

    public byte[] addressBytes() {
        byte[] address = new byte[2];
        address[0] = (byte) (getAddress() & 0xff);
        address[1] = (byte) ((getAddress() >> 8) & 0xff);
        return address;
    }

    public byte valueBytes() {
        return (byte) (getValue() & 0xff);
    }

    @Override
    public String toString() {
        return "Poke{" +
                "addressProperty=" + addressProperty +
                ", valueProperty=" + valueProperty +
                '}';
    }
}
