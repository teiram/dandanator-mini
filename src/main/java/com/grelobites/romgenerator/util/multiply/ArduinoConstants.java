package com.grelobites.romgenerator.util.multiply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class ArduinoConstants {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArduinoConstants.class);

    public enum ArduinoTarget {
        MULTIPLY("multiply"),
        DANDANATOR_V3("dandanator-v3");

        private String id;

        ArduinoTarget(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private static final String DEFAULT_VERSION = "1";
    private static final String HEX_FOLDER = "/multiply";
    private static final String HEX_RESOURCE_TEMPLATE = "%s/%s.%s.hex";

    public static InputStream hexResource(ArduinoTarget target) {
        return hexResource(target, DEFAULT_VERSION);
    }

    public static InputStream hexResource(ArduinoTarget target, String version) {
        String resourcePath = String.format(HEX_RESOURCE_TEMPLATE, HEX_FOLDER,
                target.id(), version);
        LOGGER.debug("Getting hex resource {}", resourcePath);
        return ArduinoConstants.class
                .getResourceAsStream(resourcePath);
    }
}
