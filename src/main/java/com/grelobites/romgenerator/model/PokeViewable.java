package com.grelobites.romgenerator.model;

import javafx.collections.ObservableList;

public interface PokeViewable {

    ObservableList<PokeViewable> getChildren();

    void removeChild(PokeViewable item);

    PokeViewable getParent();

    void addNewChild();

    String getViewRepresentation();

    SnapshotGame getOwner();

    void update(String value);
}
