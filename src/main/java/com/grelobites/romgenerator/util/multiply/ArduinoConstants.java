package com.grelobites.romgenerator.util.multiply;

import java.io.InputStream;

public class ArduinoConstants {
    private static final String HEX_RESOURCE = "/multiply/multiply.hex";

    public static InputStream hexResource() {
        return ArduinoConstants.class.getResourceAsStream(HEX_RESOURCE);
    }
}
