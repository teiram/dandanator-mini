package com.grelobites.romgenerator.util;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.resource.ResourceExtractor;
import com.grelobites.romgenerator.util.resource.ResourceExtractorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CharSetFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CharSetFactory.class);
    private static final String CHARSETS_LOCATION_PATH = "/charsets";
    private static final String CHARSET_REGEXP = ".*CHR";

    private List<String> charSetLocations;
    private ResourceExtractor resourceExtractor;

    public CharSetFactory() {
        try {
            resourceExtractor = ResourceExtractorFactory.getResourceExtractor(getClass());
            charSetLocations = resourceExtractor.getMatchingEntries(CHARSETS_LOCATION_PATH, CHARSET_REGEXP);
        } catch (Exception e) {
            LOGGER.warn("Unable to initialize CharSetFactory", e);
            charSetLocations = null;
        }
        LOGGER.debug("CharSetFactory initialized with " + charSetLocations);
    }

    public Integer charSetCount() {
        return 1 + (charSetLocations == null ? 0 : charSetLocations.size());
    }

    public byte[] getCharSetAt(Number index) {
        try {
            if (charSetLocations != null) {
                int charSetIndex = index.intValue();
                if (charSetIndex == 0) {
                    return Constants.getDefaultCharset();
                } else if (charSetIndex <= charSetLocations.size()) {
                    return Util.fromInputStream(resourceExtractor.getResource(charSetLocations.get(charSetIndex - 1)));
                } else {
                    LOGGER.warn("Trying to access unexisting charset at index " + charSetIndex);
                }
            }
            return Constants.getDefaultCharset();

        } catch (Exception e) {
            LOGGER.error("Trying to get charset", e);
            throw new RuntimeException(e);
        }
    }
}
