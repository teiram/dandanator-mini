package com.grelobites.romgenerator.util;

public interface RamGameCompressor {
    byte[] compressSlot(int slot, byte[] data);
}
