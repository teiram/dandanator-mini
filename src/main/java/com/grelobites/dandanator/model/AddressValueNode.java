package com.grelobites.dandanator.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mteira on 20/6/16.
 */
public class AddressValueNode  extends PokeEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressValueNode.class);

    private final IntegerProperty addressProperty;
    private final IntegerProperty valueProperty;

    private final ObservableList<PokeEntity> children = FXCollections.emptyObservableList();
    private final PokeEntity parent;

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
    public ObservableList<PokeEntity> getChildren() {
        return children;
    }

    @Override
    public void removeChild(PokeEntity entity) {
        throw new IllegalArgumentException("Unable to remove child from leaf node");
    }

    @Override
    public PokeEntity getParent() {
        return parent;
    }

    public AddressValueNode(Integer address, Integer value, PokeEntity parent, Game ownerGame) {
        super(ownerGame);
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
        return "AddressValueNode{" +
                "addressProperty=" + addressProperty +
                ", valueProperty=" + valueProperty +
                '}';
    }
}
