package com.grelobites.dandanator.model;

import javafx.collections.ObservableList;

public interface PokeViewable {

    ObservableList<PokeViewable> getChildren();

    void removeChild(PokeViewable item);

    PokeViewable getParent();

    void addNewChild();

    String getViewRepresentation();

    Game getOwner();

    void update(String value);
}