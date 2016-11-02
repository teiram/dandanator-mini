package com.grelobites.romgenerator.handlers.dandanatormini;

import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.player.TapOutputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RomSetUtil {
    private static final String LOADER_NAME = "DivIDELoader";
    private static final int LOAD_ADDRESS = 0x6f00;
    private static final int BLOCK_SIZE = 0x8000;
    private static final int BLOCK_COUNT = 16;
    private static final String BLOCK_NAME_PREFIX = "block";

    public static void exportToDivideAsTap(InputStream romsetStream, OutputStream out) throws IOException {
        TapOutputStream tos = new TapOutputStream(out);
        tos.addProgramStream(LOADER_NAME, 10, new ByteArrayInputStream(
                DandanatorMiniConstants.getDivIdeLoader()));

        byte[] buffer = new byte[BLOCK_SIZE + 3];
        for (int i = 0; i <  BLOCK_COUNT; i++) {
            System.arraycopy(Util.fromInputStream(romsetStream, BLOCK_SIZE), 0, buffer, 0, BLOCK_SIZE);
            buffer[BLOCK_SIZE] = Integer.valueOf(i + 1).byteValue();
            Util.writeAsLittleEndian(buffer, BLOCK_SIZE + 1, Util.getBlockCrc16(buffer, BLOCK_SIZE + 1));


            tos.addCodeStream(
                    String.format("%s%02d", BLOCK_NAME_PREFIX, i),
                    LOAD_ADDRESS, buffer);
        }
        out.flush();
    }
}
