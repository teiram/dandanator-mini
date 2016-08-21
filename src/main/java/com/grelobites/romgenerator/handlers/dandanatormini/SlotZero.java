package com.grelobites.romgenerator.handlers.dandanatormini;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Optional;


public interface SlotZero {
    Logger LOGGER = LoggerFactory.getLogger(SlotZero.class);
    Class<?>[] implementations =
            new Class<?>[] {
                    SlotZeroV4.class,
                    SlotZeroV5.class
    };

    static Optional<SlotZero> getImplementation(byte[] data) {
        for (Class<?> implementationClass : implementations) {
            try {
                Class<? extends SlotZero> slotZeroClass = implementationClass.asSubclass(SlotZero.class);
                Constructor<? extends SlotZero> constructor = slotZeroClass.getConstructor(byte[].class);
                SlotZero slotZeroInstance = constructor.newInstance(data);
                if (slotZeroInstance.validate()) {
                    return Optional.of(slotZeroInstance);
                }
            } catch (Exception e) {
                LOGGER.error("Instantiating class " + implementationClass);
            }
        }
        return Optional.empty();
    }

    boolean validate();
    DandanatorMiniImporter getImporter();
    InputStream data();
}
