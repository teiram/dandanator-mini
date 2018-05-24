package com.grelobites.romgenerator.util.pack;

import java.util.Comparator;
import java.util.List;

public class PackAlgorithms {

    public static <T extends PackedItem> void binBFDPack(List<? extends Container<T>> containers, List<T> items) {
        items.sort(Comparator.comparingInt(PackedItem::getSize));
        for (T item : items) {
            boolean packed = false;
            for (Container container : containers) {
                if (container.getCapacity() >= item.getSize()) {
                    container.addItem(item);
                    packed = true;
                }
            }
            if (!packed) {
                throw new IllegalArgumentException("Unable to pack element of size " +
                        item.getSize());
            }
            containers.sort(Comparator.comparingInt(Container<T>::getCapacity)
                    .reversed());
        }
    }
}
