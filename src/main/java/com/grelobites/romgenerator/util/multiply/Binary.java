package com.grelobites.romgenerator.util.multiply;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Binary {
    private int address;
    private ArrayList<HexRecord> records;

    public void addRecord(HexRecord record) {
        if (record.getType() == HexRecordType.DATA) {
            if (records == null) {
                records = new ArrayList<>();
                address = record.getAddress();
            } else {
                if (getNextAddress() != record.getAddress()) {
                    throw new IllegalArgumentException("Attempt to add non-contiguous record");
                }
            }
            records.add(record);
        } else {
            throw new IllegalArgumentException("Only data hex records allowed in binary");
        }
    }

    public int getAddress() {
        return address;
    }

    public boolean isEmpty() {
        return records == null || records.size() == 0;
    }

    public int getNextAddress() {
        if (records.size() > 0) {
            HexRecord lastRecord = records.get(records.size() - 1);
            return lastRecord.getAddress() + lastRecord.getData().length;
        } else {
            throw new IllegalStateException("Trying to get last address from empty binary");
        }
    }

    public byte[] toByteArray() throws IOException {
        if (records != null && records.size() > 0) {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            for (HexRecord record : records) {
                result.write(record.getData());
            }
            return result.toByteArray();
        } else {
            return null;
        }
    }
}
