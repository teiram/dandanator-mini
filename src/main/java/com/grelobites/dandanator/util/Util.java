package com.grelobites.dandanator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    public static int asLittleEndian(InputStream is) throws IOException {
        return is.read() + (is.read() << 8);
    }

    public static String getNullTerminatedString(InputStream is, int maxLength) throws IOException {
        return getNullTerminatedString(is, 0, maxLength);
    }

    public static String substring(String value, int maxLength) {
        if (value != null) {
            return value.length() > maxLength ?
                    value.substring(0, maxLength) :
                    value;
        } else {
            return null;
        }
    }

    public static String getNullTerminatedString(InputStream is, int skip, int maxLength) throws IOException {
        byte[] buffer = new byte[maxLength];
        int nextByte;
        int index = 0;
        while ((nextByte = is.read()) != -1 &&  index < maxLength) {
            if (nextByte != 0) {
                buffer[index++] = (byte) nextByte;
            } else {
                break;
            }
        }
        long remainder = maxLength - index - 1;
        long read = is.skip(remainder);
        if (read != remainder) {
            LOGGER.warn("Unexpected number of bytes skipped from stream. Was: " + read + ", expected: " + remainder);
        }
        return new String(buffer, skip, index - skip);
    }
}
