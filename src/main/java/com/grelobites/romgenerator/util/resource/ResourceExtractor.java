package com.grelobites.romgenerator.util.resource;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ResourceExtractor {

    List<String> getMatchingEntries(String path, String pattern) throws IOException;

    InputStream getResource(String path) throws IOException;

}
