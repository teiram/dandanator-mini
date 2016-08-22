package com.grelobites.romgenerator.handlers.dandanatormini.v5;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConfiguration;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.util.RamGameCompressor;
import com.grelobites.romgenerator.util.compress.Compressor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class DandanatorMiniRamGameCompressor implements RamGameCompressor {

    private Compressor compressor = DandanatorMiniConfiguration.getInstance().getCompressor();

    private byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        OutputStream compressingStream = compressor.getCompressingOutputStream(os);
        compressingStream.write(data);
        compressingStream.flush();
        return os.toByteArray();
    }

    @Override
    public byte[] compressSlot(int slot, byte[] data) {
        try {
            if (slot == DandanatorMiniConstants.GAME_CHUNK_SLOT) {
                return compress(Arrays.copyOfRange(data, 0,
                        Constants.SLOT_SIZE - DandanatorMiniConstants.GAME_CHUNK_SIZE));
            } else {
                return compress(data);
            }
        } catch (Exception e) {
            throw new RuntimeException("During compression of slot " + slot, e);
        }
    }
}
