package com.grelobites.romgenerator.view.util;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RecursiveTreeItem<T> extends TreeItem<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecursiveTreeItem.class);

    private Callback<T, ObservableList<T>> childrenFactory;

    public RecursiveTreeItem(Callback<T, ObservableList<T>> func) {
        this(null, func, null);
    }

    public RecursiveTreeItem(final T value, Callback<T, ObservableList<T>> func, Consumer<T> changeListener) {
        this(value, null, func, changeListener);
    }

    public RecursiveTreeItem(final T value, Node graphic, Callback<T, ObservableList<T>> func,
                             Consumer<T> changeListener) {
        super(value, graphic);

        LOGGER.debug("Creating new RecursiveTreeItem for " + value);

        this.childrenFactory = func;

        if (value != null) {
            addChildrenListener(value, changeListener);
        }

        valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        addChildrenListener(newValue, changeListener);
                    }
                }
        );
    }

    private void addChildrenListener(T value, Consumer<T> changeListener) {
        final ObservableList<T> children = childrenFactory.call(value);

        children.forEach(child ->
                RecursiveTreeItem.this.getChildren().add(
                        new RecursiveTreeItem<>(child, getGraphic(), childrenFactory, changeListener)));

        children.addListener((ListChangeListener<T>) change -> {
            while (change.next()) {
                LOGGER.debug("Detected change in listener " + change);
                if (change.wasAdded()) {
                    change.getAddedSubList()
                            .forEach(t -> {
                                RecursiveTreeItem<T> newItem = new RecursiveTreeItem<>(t, getGraphic(), childrenFactory,
                                        changeListener);
                                RecursiveTreeItem.this.getChildren().add(newItem);
                                newItem.getParent().setExpanded(true);
                                Optional.of(changeListener).ifPresent(c -> c.accept(t));
                            });
                }
                if (change.wasRemoved()) {
                    change.getRemoved().forEach(t -> {
                        final List<TreeItem<T>> itemsToRemove = RecursiveTreeItem.this.getChildren()
                                .stream()
                                .filter(treeItem ->
                                        treeItem.getValue().equals(t)).collect(Collectors.toList());

                        RecursiveTreeItem.this.getChildren().removeAll(itemsToRemove);
                        Optional.of(changeListener).ifPresent(c -> c.accept(t));
                    });
                }

            }
        });
    }
}
