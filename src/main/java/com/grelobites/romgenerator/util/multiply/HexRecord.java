package com.grelobites.romgenerator.util.multiply;

import com.grelobites.romgenerator.util.Util;

public class HexRecord {
    private HexRecordType type;
    private int address;
    private byte[] data;

    public HexRecordType getType() {
        return type;
    }

    public int getAddress() {
        return address;
    }

    public byte[] getData() {
        return data;
    }

    private static int decodeHexValue(String value, int offset, int size) {
        return Integer.decode("0x" + value.substring(offset, offset + size));
    }

    public static HexRecord fromLine(String line) {
        if (line.startsWith(":")) {
            int byteCount = decodeHexValue(line, 1, 2);
            int address = decodeHexValue(line, 3, 4);
            HexRecordType type = HexRecordType.fromId(decodeHexValue(line, 7, 2));
            byte[] data = null;
            if (byteCount > 0) {
                data = new byte[byteCount];
                int index = 9;
                for (int i = 0; i < byteCount; i++) {
                    data[i] = Integer.valueOf(decodeHexValue(line, index, 2)).byteValue();
                    index += 2;
                }
            }
            HexRecord record = new HexRecord();
            record.type = type;
            record.address = address;
            record.data = data;
            return record;
        } else {
            throw new IllegalArgumentException("Invalid Hex line");
        }
    }

    @Override
    public String toString() {
        return "HexRecord{" +
                "type=" + type +
                ", address=" + String.format("0x%04x", address) +
                ", data=" + Util.dumpAsHexString(data) +
                '}';
    }
}
