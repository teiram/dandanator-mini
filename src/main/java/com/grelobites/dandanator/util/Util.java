package com.grelobites.dandanator.util;

import java.io.IOException;
import java.io.InputStream;

public class Util {
    public static int asLittleEndian(InputStream is) throws IOException {
        return is.read() + (is.read() << 8);
    }

    public static String getNullTerminatedString(InputStream is, int maxLength) throws IOException {
        return getNullTerminatedString(is, 0, maxLength);
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
        is.skip(maxLength - index - 1);
        return new String(buffer, skip, index - skip);
    }
}
