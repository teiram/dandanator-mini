package com.grelobites.romgenerator.util.pack;

public interface Container<T extends PackedItem> {
    int getCapacity();
    void addItem(T item);
}
