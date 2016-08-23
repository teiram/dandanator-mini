package com.grelobites.romgenerator.util.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

public class ResourceExtractorFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceExtractorFactory.class);

    private static String getClassContainer(Class<?> c) {
        try {
            return c.getProtectionDomain().getCodeSource().getLocation().toURI()
                    .getPath();
        } catch (Exception e) {
            LOGGER.error("Getting container class", e);
            throw new RuntimeException(e);
        }
    }

    public static ResourceExtractor getResourceExtractor(Class<?> c) throws IOException {
        File container = new File(getClassContainer(c));
        if (container.isFile()) {
            return new JarResourceExtractor(new JarFile(container));
        } else {
            return new PathResourceExtractor(container);
        }
    }
}
