package com.grelobites.romgenerator.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Trainer implements PokeViewable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Trainer.class);
    private static final int MAX_POKES_PER_TRAINER = 6;

    private final SimpleStringProperty nameProperty;
    private final TrainerList parent;
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
        if (pokeList.size() < MAX_POKES_PER_TRAINER) {
            pokeList.add(new Poke(Poke.LOWEST_ADDRESS, Poke.LOWEST_VALUE, this));
        } else {
            LOGGER.info("No more pokes allowed");
        }
    }

    @Override
    public SnapshotGame getOwner() {
        return parent.getOwner();
    }

    @Override
    public void update(String value) {
        setName(value);
    }

    @Override
    public String getViewRepresentation() {
        return getName();
    }

    public Trainer(String name, TrainerList parent) {
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
        if (pokeList.size() < MAX_POKES_PER_TRAINER) {
            pokeList.add(new Poke(address, value, this));
        } else {
            LOGGER.info("No more pokes allowed");
        }
    }

    @Override
    public String toString() {
        return "Trainer{" +
                "nameProperty=" + nameProperty +
                ", pokeList=" + pokeList +
                '}';
    }
}

