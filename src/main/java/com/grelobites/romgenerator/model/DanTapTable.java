package com.grelobites.romgenerator.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DanTapTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DanTapTable.class);
    List<DanTapTableEntry> entries = new ArrayList<>();

    public void addEntry(DanTapTableEntry entry) {
        LOGGER.debug("Adding TapTable entry {}", entry);
        entries.add(entry);
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (DanTapTableEntry entry : entries) {
            bos.write(entry.toByteArray());
        }
        bos.write(DanTapTableEntry.EMPTY_ENTRY);
        return bos.toByteArray();
    }
}
