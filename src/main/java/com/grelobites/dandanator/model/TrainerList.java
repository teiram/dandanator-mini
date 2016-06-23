package com.grelobites.dandanator.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TrainerList implements PokeViewable {
    private static final String NEW_TRAINER_NAME = "New Poke";
    private static final String EMPTY_STRING = "";

    private ObservableList<PokeViewable> children = FXCollections.observableArrayList();
    private Game owner;

    @Override
    public ObservableList<PokeViewable> getChildren() {
        return children;
    }

    @Override
    public void removeChild(PokeViewable item) {
        children.remove(item);
    }

    @Override
    public Game getOwner() {
        return owner;
    }

    @Override
    public PokeViewable getParent() {
        throw new IllegalArgumentException("Cannot get parent from root");
    }

    @Override
    public void addNewChild() {
        children.add(new Trainer(NEW_TRAINER_NAME, this, getOwner()));
    }

    @Override
    public String getViewRepresentation() {
        return EMPTY_STRING;
    }

    @Override
    public void update(String value) {
        throw new IllegalArgumentException("Cannot update the root entity");
    }

    public TrainerList(Game owner) {
        this.owner = owner;
    }

    public Trainer addTrainerNode(String name) {
        Trainer trainer = new Trainer(name, this, owner);
        children.add(trainer);
        return trainer;
    }

    @Override
    public String toString() {
        return "TrainerList{" +
                "children=" + children +
                '}';
    }
}
