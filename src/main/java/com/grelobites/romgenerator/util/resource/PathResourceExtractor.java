package com.grelobites.romgenerator.util.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PathResourceExtractor implements ResourceExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PathResourceExtractor.class);
    private Path basePath;

    public PathResourceExtractor(File basePath) {
        this.basePath = basePath.toPath();
    }

    public boolean isValidEntry(Pattern matchingPattern, Path path) {
        return matchingPattern.matcher(path.toString()).matches();
    }

    @Override
    public List<String> getMatchingEntries(String path, String pattern) throws IOException {
        Pattern matchingPattern = Pattern.compile(pattern);
        return Files.walk(basePath).filter(c -> isValidEntry(matchingPattern, c))
                .map(c -> c.toString()).collect(Collectors.toList());

    }

    @Override
    public InputStream getResource(String location) throws IOException {
        LOGGER.debug("Requesting resource from file " + location);
        return new FileInputStream(location);
    }
}
