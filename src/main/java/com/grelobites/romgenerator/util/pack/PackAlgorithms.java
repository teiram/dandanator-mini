package com.grelobites.romgenerator.util.pack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

public class PackAlgorithms {
    private static final Logger LOGGER = LoggerFactory.getLogger(PackAlgorithms.class);

    public static <T extends PackedItem> void binBFDPack(List<? extends Container<T>> containers, List<T> items) {
        items.sort(Comparator.comparingInt(PackedItem::getSize).reversed());
        LOGGER.debug("Packing {} items in {} containers", items.size(), containers.size());
        for (T item : items) {
            LOGGER.debug("Packing item of size {}", item.getSize());
            boolean packed = false;
            for (Container container : containers) {
                LOGGER.debug("Trying in container of capacity {}", container.getCapacity());
                if (container.getCapacity() >= item.getSize()) {
                    container.addItem(item);
                    packed = true;
                    LOGGER.debug("Succeed. Capacity reduced to {}", container.getCapacity());
                    break;
                }
            }
            if (!packed) {
                throw new IllegalArgumentException("Unable to pack element of size " +
                        item.getSize());
            }
            containers.sort(Comparator.comparingInt(Container::getCapacity));
        }
    }
}
