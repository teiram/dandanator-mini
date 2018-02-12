package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.util.LocaleUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TrainerList implements PokeViewable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainerList.class);

    private static final int MAX_TRAINERS_PER_GAME = 8;
    private static final String NEW_TRAINER_NAME = LocaleUtil.i18n("newPokeMessage");
    private static final String EMPTY_STRING = "";

    public static final TrainerList EMPTY_LIST = new TrainerList(null);

    private ObservableList<PokeViewable> children = FXCollections.observableArrayList();
    private SnapshotGame owner;

    @Override
    public ObservableList<PokeViewable> getChildren() {
        return children;
    }

    @Override
    public void removeChild(PokeViewable item) {
        children.remove(item);
    }

    @Override
    public SnapshotGame getOwner() {
        return owner;
    }

    @Override
    public PokeViewable getParent() {
        throw new IllegalArgumentException("Cannot get parent from root");
    }

    @Override
    public void addNewChild() {
        if (children.size() < MAX_TRAINERS_PER_GAME) {
            children.add(new Trainer(NEW_TRAINER_NAME, this));
        } else {
            LOGGER.info("No more trainers allowed");
        }
    }

    @Override
    public String getViewRepresentation() {
        return EMPTY_STRING;
    }

    @Override
    public void update(String value) {
        throw new IllegalArgumentException("Cannot update the root entity");
    }

    public TrainerList(SnapshotGame owner) {
        this.owner = owner;
    }

    public void setOwner(SnapshotGame owner) {
        this.owner = owner;
    }

    public Optional<Trainer> addTrainerNode(String name) {
        if (children.size() < MAX_TRAINERS_PER_GAME) {
            Trainer trainer = new Trainer(name, this);
            children.add(trainer);
            return Optional.of(trainer);
        } else {
            LOGGER.info("No more trainers allowed");
            return Optional.empty();
        }
    }

   @Override
    public String toString() {
        return "TrainerList{" +
                "children=" + children +
                '}';
    }
}
