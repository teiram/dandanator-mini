package com.grelobites.romgenerator.util.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

public class JarResourceExtractor implements ResourceExtractor {
    private JarFile jarFile;

    public JarResourceExtractor(JarFile jarFile) {
        this.jarFile = jarFile;
    }

    private boolean isMatchingJarEntry(JarEntry jarEntry, String path, Pattern pattern) {
        if (jarEntry.getSize() > 0) {
            Matcher matcher = pattern.matcher(jarEntry.getName());
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> getMatchingEntries(String path, String pattern) throws IOException {
        Pattern entryPattern = Pattern.compile(pattern);
        List<String> matchingEntries = new ArrayList<>();
        for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
            JarEntry entry = e.nextElement();
            if (isMatchingJarEntry(entry, path, entryPattern)) {
                matchingEntries.add(entry.getName());
            }
        }
        return matchingEntries;
    }

    @Override
    public InputStream getResource(String location) throws IOException {
        ZipEntry zipEntry = jarFile.getEntry(location);
        if (zipEntry != null) {
            return jarFile.getInputStream(zipEntry);
        } else {
            throw new IllegalArgumentException("Requesting unexisting zip entry " + location);
        }
    }
}
