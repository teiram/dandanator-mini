package com.grelobites.romgenerator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);
    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    public static int readAsLittleEndian(InputStream is) throws IOException {
        return is.read() + (is.read() << 8);
    }

    public static int readAsBigEndian(InputStream is) throws IOException {
        return ((is.read() & 0xff) << 8) | (is.read() & 0xff);
    }
    public static int readAsLittleEndian(byte[] data, int offset) {
        return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
    }

    public static void writeAsLittleEndian(OutputStream os, int value) throws IOException {
        os.write(value & 0xff);
        os.write((value >> 8) & 0xff);
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

    public static String stripSuffix(String value, String suffix) {
        int index;
        if ((index = value.lastIndexOf(suffix)) > -1) {
            return value.substring(0, index);
        } else {
            return value;
        }
    }

    public static String stripSnapshotVersion(String value) {
        return stripSuffix(value, SNAPSHOT_SUFFIX);
    }

    public static Optional<String> getFileExtension(String fileName) {
        int index;
        if ((index = fileName.lastIndexOf('.')) > -1) {
            return Optional.of(fileName.substring(index + 1));
        } else {
            return Optional.empty();
        }
    }

    public static byte[] fromInputStream(InputStream is, int size) throws IOException {
        byte[] result = new byte[size];
        DataInputStream dis = new DataInputStream(is);
        dis.readFully(result);
        return result;
    }

    public static byte[] fromInputStream(InputStream is) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[2048];
            int nread;
            while ((nread = is.read(buffer)) != -1) {
                out.write(buffer, 0, nread);
            }
            out.flush();
            out.close();
            return out.toByteArray();
        }
    }

    public static byte[] reverseByteArray(byte[] array) {
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j--] = array[i];
            array[i++] = tmp;
        }
        return array;
    }

    public static byte[] concatArrays(byte[] array1, byte[] array2) {
        byte[] result = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    public static byte[] paddedByteArray(byte[] source, int length, byte filler) {
        byte[] result = new byte[length];
        Arrays.fill(result, filler);
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }

    public static <S, T> Collection<T> collectionUpcast(Collection<S> list) {
        return list.stream().map(item -> (T) item)
            .collect(Collectors.toList());
    }

    public static String dumpAsHexString(byte[] byteArray) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (byte value: byteArray) {
            sb.append("0x").append(String.format("%02X", value)).append(" ");
        }
        sb.append(" ]");
        return sb.toString();
    }

    public static String toHexString(Integer value) {
        if (value != null) {
            return String.format("0x%04x", value);
        } else {
            return "null";
        }
    }


}
