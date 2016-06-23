package com.grelobites.dandanator.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Created by mteira on 20/6/16.
 */
public class Trainer implements PokeViewable {

    private final SimpleStringProperty nameProperty;
    private final TrainerList parent;
    private Game owner;
    private final ObservableList<PokeViewable> pokeList = FXCollections.observableArrayList();

    @Override
    public ObservableList<PokeViewable> getChildren() {
        return pokeList;
    }

    public void removeChild(PokeViewable item) {
        pokeList.remove(item);
    }

    @Override
    public TrainerList getParent() {
        return parent;
    }

    @Override
    public void addNewChild() {
        pokeList.add(new Poke(0, 0, this, getOwner()));
    }

    @Override
    public Game getOwner() {
        return owner;
    }

    @Override
    public void update(String value) {
        setName(value);
    }

    @Override
    public String getViewRepresentation() {
        return getName();
    }

    public Trainer(String name, TrainerList parent, Game owner) {
        this.owner = owner;
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


    public void addPoke(Integer address, Integer value) {
        pokeList.add(new Poke(address, value, this, getOwner()));
    }

    @Override
    public String toString() {
        return "Trainer{" +
                "nameProperty=" + nameProperty +
                ", pokeList=" + pokeList +
                '}';
    }
}

