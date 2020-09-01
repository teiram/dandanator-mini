package com.grelobites.romgenerator.util.multiply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class ArduinoConstants {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArduinoConstants.class);

    private static final String DEFAULT_VERSION = "1";
    private static final String HEX_FOLDER = "/multiply";
    private static final String HEX_RESOURCE_TEMPLATE = "%s/multiply.%s.hex";

    public static InputStream hexResource() {
        return hexResource(DEFAULT_VERSION);
    }

    public static InputStream hexResource(String version) {
        String resourcePath = String.format(HEX_RESOURCE_TEMPLATE, HEX_FOLDER, version);
        LOGGER.debug("Getting hex resource {}", resourcePath);
        return ArduinoConstants.class
                .getResourceAsStream(resourcePath);
    }
}
