package com.grelobites.romgenerator.util.tap;

import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

public class TapInputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapInputStream.class);
    private InputStream source;

    public TapInputStream(InputStream is) {
        this.source = is;
    }

    private static String extractBlockName(ByteBuffer buffer) {
        byte[] programName = new byte[10];
        buffer.get(programName);
        return new String(programName).trim();
    }

    private static TapBlock createProgramTapBlock(ByteBuffer buffer) {
        return ProgramTapBlock.newBuilder()
                .withLoadingProgramName(extractBlockName(buffer))
                .withDataLength(Short.toUnsignedInt(buffer.getShort()))
                .withAutoStartLine(Short.toUnsignedInt(buffer.getShort()))
                .withProgramLength(Short.toUnsignedInt(buffer.getShort()))
                .withChecksum(Byte.toUnsignedInt(buffer.get()))
                .build();
    }

    private static TapBlock createNumArrayTapBlock(ByteBuffer buffer) {
        return ArrayTapBlock.newBuilder()
                .withType(TapBlockType.NUMARRAY)
                .withLoadingProgramName(extractBlockName(buffer))
                .withDataLength(Short.toUnsignedInt(buffer.getShort()))
                .withVariableName(String.valueOf((char) (buffer.get(13) + 128)))
                .withChecksum(Byte.toUnsignedInt(buffer.get(15)))
                .build();
    }

    private static TapBlock createCharArrayTapBlock(ByteBuffer buffer) {
        return ArrayTapBlock.newBuilder()
                .withType(TapBlockType.CHARARRAY)
                .withLoadingProgramName(extractBlockName(buffer))
                .withDataLength(Short.toUnsignedInt(buffer.getShort()))
                .withVariableName(String.valueOf((char) (buffer.get(13) + 128)))
                .withChecksum(Byte.toUnsignedInt(buffer.get(15)))
                .build();
    }

    private static TapBlock createCodeTapBlock(ByteBuffer buffer) {
        return CodeTapBlock.newBuilder()
                .withLoadingProgramName(extractBlockName(buffer))
                .withDataLength(Short.toUnsignedInt(buffer.getShort()))
                .withStartAddress(Short.toUnsignedInt(buffer.getShort()))
                .withChecksum(Byte.toUnsignedInt(buffer.get(15)))
                .build();
    }

    private static TapBlock createDataTapBlock(ByteBuffer buffer, int size) {
        byte[] data = new byte[size];
        buffer.get(data);
        return DataTapBlock.newBuilder()
                .withData(data)
                .withChecksum(buffer.get())
                .build();
    }

    public Optional<TapBlock> next() throws IOException {
        //Get length indicator
        if (source.available() > 0) {
            ByteBuffer buffer = ByteBuffer.wrap(Util.fromInputStream(source, 2))
                    .order(ByteOrder.LITTLE_ENDIAN);
            int size = Short.toUnsignedInt(buffer.getShort());
            LOGGER.debug("Size is {}", size);

            buffer = ByteBuffer.wrap(Util.fromInputStream(source, size))
                    .order(ByteOrder.LITTLE_ENDIAN);

            int flag = Byte.toUnsignedInt(buffer.get());
            if (flag == 0) {
                Optional<TapBlockType> blockType = TapBlockType.fromId(buffer.get());
                if (blockType.isPresent()) {
                    switch (blockType.get()) {
                        case PROGRAM:
                            return Optional.of(createProgramTapBlock(buffer));
                        case NUMARRAY:
                            return Optional.of(createNumArrayTapBlock(buffer));
                        case CHARARRAY:
                            return Optional.of(createCharArrayTapBlock(buffer));
                        case CODE:
                            return Optional.of(createCodeTapBlock(buffer));
                    }
                }
            } else if (flag == 255) {
                return Optional.of(createDataTapBlock(buffer, size - 2));
            } else {
                LOGGER.warn("Unexpected flag value found in TAP: " + flag);
            }
        }
        return Optional.empty();
    }
}
