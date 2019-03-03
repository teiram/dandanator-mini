package com.grelobites.romgenerator.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
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

    public int sizeInBytes() {
        return (entries.size() + 1) * DanTapConstants.TAP_TABLE_ENTRY_SIZE;
    }

    public List<DanTapTableEntry> entries() {
        return entries;
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (DanTapTableEntry entry : entries) {
            bos.write(entry.toByteArray());
        }
        bos.write(DanTapTableEntry.EMPTY_ENTRY);
        return bos.toByteArray();
    }

    public static DanTapTable fromByteArray(byte[] data, int offset) throws IOException {
        DanTapTable table = new DanTapTable();
        byte[] entryBytes = new byte[DanTapConstants.TAP_TABLE_ENTRY_SIZE];
        boolean endOfTable = false;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data, offset, data.length - offset)) {
            while (!endOfTable) {
                if (bis.read(entryBytes) == DanTapConstants.TAP_TABLE_ENTRY_SIZE) {
                    DanTapTableEntry entry = DanTapTableEntry.fromByteArray(entryBytes);
                    if (entry.getSize() > 0) {
                        table.addEntry(entry);
                    } else {
                        LOGGER.debug("Got end of table with entry {}", entry);
                        endOfTable = true;
                    }
                } else {
                    LOGGER.warn("Exhausted data reading from byte array");
                    endOfTable = true;
                }
            }
        }
        return table;
    }

    @Override
    public String toString() {
        return "DanTapTable{" +
                "entries=" + entries +
                '}';
    }
}
