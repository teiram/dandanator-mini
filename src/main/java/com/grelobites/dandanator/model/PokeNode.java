package com.grelobites.dandanator.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Created by mteira on 20/6/16.
 */
public class PokeNode extends PokeEntity {
    private final SimpleStringProperty nameProperty;
    private final PokeEntity parent;

    private final ObservableList<PokeEntity> addressValueList = FXCollections.observableArrayList();

    @Override
    public ObservableList<PokeEntity> getChildren() {
        return addressValueList;
    }

    public void removeChild(PokeEntity item) {
        addressValueList.remove(item);
    }

    @Override
    public PokeEntity getParent() {
        return parent;
    }

    @Override
    public void addNewChild() {
        addressValueList.add(new AddressValueNode(0, 0, this, getOwnerGame()));
    }

    @Override
    public String getViewRepresentation() {
        return getName();
    }

    public PokeNode(String name, PokeEntity parent, Game ownerGame) {
        super(ownerGame);
        this.parent = parent;
        this.nameProperty = new SimpleStringProperty(name);
    }

    public SimpleStringProperty nameProperty() {
        return nameProperty;
    }

    public String getName() {
        return nameProperty.get();
    }

    public void setName(String name) {
        this.nameProperty.set(name);
    }

    public void update(String value) {
        setName(value);
    }

    public void addAddressValue(Integer address, Integer value) {
        addressValueList.add(new AddressValueNode(address, value, this, getOwnerGame()));
    }

    @Override
    public String toString() {
        return "PokeNode{" +
                "nameProperty=" + nameProperty +
                ", addressValueList=" + addressValueList +
                '}';
    }
}

