package com.grelobites.dandanator.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Created by mteira on 20/6/16.
 */
public class PokeEntity {
    private static final String NEW_POKE_NAME = "New Poke";
    private static final String EMPTY_STRING = "";

    private ObservableList<PokeEntity> children = FXCollections.observableArrayList();
    private Game ownerGame;

    public ObservableList<PokeEntity> getChildren() {
        return children;
    }

    public void removeChild(PokeEntity item) {
        children.remove(item);
    }

    public Game getOwnerGame() {
        return ownerGame;
    }

    public PokeEntity(Game ownerGame) {
        this.ownerGame = ownerGame;
    }

    public PokeEntity getParent() {
        throw new IllegalArgumentException("Cannot get parent from root");
    }

    public void update(String value) {
        throw new IllegalArgumentException("Cannot update the root entity");
    }

    public void addNewChild() {
        children.add(new PokeNode(NEW_POKE_NAME, this, ownerGame));
    }

    public String getViewRepresentation() {
        return EMPTY_STRING;
    }

    public void addPokeNode(String name) {
        children.add(new PokeNode(name, this, ownerGame));
    }


    @Override
    public String toString() {
        return "PokeEntity{" +
                "children=" + children +
                '}';
    }
}
