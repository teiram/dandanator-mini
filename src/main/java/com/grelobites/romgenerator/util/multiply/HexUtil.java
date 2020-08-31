package com.grelobites.romgenerator.util.multiply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class HexUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(HexUtil.class);

    public static List<Binary> toBinaryList(InputStream hexInputStream) throws IOException {
        List<Binary> binaryItems = new ArrayList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(hexInputStream));
        Binary currentBinary = null;
        String line;
        boolean hexEof = false;
        while ((line = br.readLine()) != null && !hexEof) {
            HexRecord record = HexRecord.fromLine(line);
            LOGGER.debug("HexRecord read {}", record);
            switch (record.getType()) {
                case DATA:
                    if (currentBinary == null) {
                        currentBinary = new Binary();
                    } else {
                        if (record.getAddress() != currentBinary.getNextAddress()) {
                            LOGGER.debug("Opening new binary. Last Address {}, new Start Address {}",
                                    record.getAddress(), currentBinary.getNextAddress());
                            binaryItems.add(currentBinary);
                            currentBinary = new Binary();
                        }
                    }
                    currentBinary.addRecord(record);
                    break;
                case EOF:
                    LOGGER.debug("Reached EOF HEX record");
                    if (!currentBinary.isEmpty()) {
                        binaryItems.add(currentBinary);
                    }
                    hexEof = true;
                    break;
                default:
                    LOGGER.error("Unsupported HEX record");
            }
        }
        return binaryItems;
    }
}
