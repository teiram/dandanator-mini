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
    private static final int COMPRESSED_SLOT_THRESHOLD = 16378;
    private static final int COMPRESSED_CHUNKSLOT_THRESHOLD = 16128;

    private Compressor compressor = DandanatorMiniConfiguration.getInstance().getCompressor();

    private byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        OutputStream compressingStream = compressor.getCompressingOutputStream(os);
        compressingStream.write(data);
        compressingStream.flush();
        return os.toByteArray();
    }

    private byte[] compressSlotInternal(byte[] data) {
        try {
            return compress(data);
        } catch (Exception e) {
            throw new RuntimeException("During compression of game data", e);
        }
    }

    private static byte[] filterCompression(byte[] data, byte[] compressedData, int slot) {
        if (slot == DandanatorMiniConstants.GAME_CHUNK_SLOT) {
            return compressedData.length > COMPRESSED_CHUNKSLOT_THRESHOLD ?
                    data : compressedData;
        } else {
            return compressedData.length > COMPRESSED_SLOT_THRESHOLD ?
                    data : compressedData;
        }
    }

    @Override
    public byte[] compressSlot(int slot, byte[] data) {
        byte[] targetData = (slot == DandanatorMiniConstants.GAME_CHUNK_SLOT) ?
                Arrays.copyOfRange(data, 0, Constants.SLOT_SIZE - DandanatorMiniConstants.GAME_CHUNK_SIZE) :
                data;
        return filterCompression(targetData, compressSlotInternal(targetData), slot);
    }

}
