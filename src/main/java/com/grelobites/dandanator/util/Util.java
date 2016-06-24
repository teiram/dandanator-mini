package com.grelobites.dandanator.util;

import java.io.IOException;
import java.io.InputStream;

public class Util {
    public static int asLittleEndian(InputStream is) throws IOException {
        return is.read() + (is.read() << 8);
    }

    public static String getNullTerminatedString(InputStream is, int maxLength) throws IOException {
        byte[] buffer = new byte[maxLength];
        byte nextByte;
        int index = 0;
        while ((nextByte = (byte) is.read()) != -1 &&  index < maxLength) {
            if (nextByte != 0) {
                buffer[index++] = nextByte;
            } else {
                break;
            }
        }
        return new String(buffer);
    }
}
