package com.grelobites.romgenerator.util.player;

import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TapOutputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapOutputStream.class);

    private static final int HEADER_LENGTH = 19;
    private static final int NAME_LENGTH = 10;
    private static final int CHECKSUM_INDEX = 20;

    private static final int PROGRAM_TYPE = 0;
    private static final int NUMBERARRAY_TYPE = 1;
    private static final int CHARARRAY_TYPE = 2;
    private static final int CODE_TYPE = 3;

    private static final int HEADER_FLAG = 0;
    private static final int DATA_FLAG = 0xff;

    private static final int NO_PARAMETER_VALUE = 32768;

    private static String paddedName(String name, int length) {
        return name.length() > length ? name.substring(0, length) :
                String.format("%1$-" + length + "s", name);
    }

    private static void setChecksum(byte[] buffer) {
        int checksum = 0;
        for (int i = 2; i < buffer.length - 1; i++) {
            checksum ^= buffer[i];
        }
        buffer[buffer.length - 1] = Integer.valueOf(checksum).byteValue();
    }

    private static byte[] getTapHeader(String name, int type, int dataLength,
                                       int parameter1, int parameter2) {
        byte[] buffer = ByteBuffer.allocate(HEADER_LENGTH + 2) //Plus checksum placeholder
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(Integer.valueOf(HEADER_LENGTH).shortValue())
                .put(Integer.valueOf(HEADER_FLAG).byteValue())
                .put(Integer.valueOf(type).byteValue())
                .put(paddedName(name, NAME_LENGTH).getBytes())
                .putShort(Integer.valueOf(dataLength).shortValue())
                .putShort(Integer.valueOf(parameter1).shortValue())
                .putShort(Integer.valueOf(parameter2).shortValue())
                .array();
        setChecksum(buffer);
        return buffer;
    }

    private static byte[] getTapProgramHeader(String name, Integer autoexecLineNumber, int programLength) {
        return getTapHeader(name, PROGRAM_TYPE,
                programLength,
                autoexecLineNumber == null ? NO_PARAMETER_VALUE : autoexecLineNumber,
                programLength);
    }

    private static byte[] getTapCodeHeader(String name, int codeStartAddress, int codeLength) {
        return getTapHeader(name, CODE_TYPE,
                codeLength,
                codeStartAddress, NO_PARAMETER_VALUE);
    }

    private static byte[] getTapDataBlock(byte[] data) {
        byte[] block = ByteBuffer.allocate(data.length + 4) //Plus length(2), flag and checksum
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(Integer.valueOf(data.length + 2).shortValue())
                .put(Integer.valueOf(DATA_FLAG).byteValue())
                .put(data)
                .array();
        setChecksum(block);
        return block;
    }

    private OutputStream out;

    public TapOutputStream(OutputStream out) {
        this.out = out;
    }

    public void addProgramStream(String name, Integer autoExecLineNumber, InputStream in) throws IOException {
        byte[] dataBlock = getTapDataBlock(Util.fromInputStream(in));
        //Program length is the data block length without the length bytes, flag and checksum
        byte[] header = getTapProgramHeader(name, autoExecLineNumber, dataBlock.length - 4);
        out.write(header);
        out.write(dataBlock);
    }

    public void addCodeStream(String name, int codeStartAddress, InputStream in) throws IOException {
        byte[] dataBlock = getTapDataBlock(Util.fromInputStream(in));
        //Program length is the data block length without the length bytes, flag and checksum
        byte[] header = getTapCodeHeader(name, codeStartAddress, dataBlock.length - 4);
        out.write(header);
        out.write(dataBlock);
    }

}
