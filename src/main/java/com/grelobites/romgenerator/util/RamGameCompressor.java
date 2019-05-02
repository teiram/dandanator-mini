package com.grelobites.romgenerator.util;

import com.grelobites.romgenerator.model.GameType;

public interface RamGameCompressor {
    byte[] compressSlot(GameType gameType, int slot, byte[] data);
}
