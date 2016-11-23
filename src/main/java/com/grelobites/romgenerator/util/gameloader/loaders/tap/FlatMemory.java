package com.grelobites.romgenerator.util.gameloader.loaders.tap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlatMemory implements Memory {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlatMemory.class);

    private static final int ROM_TOP = 0x4000;
    private Clock clock;
    private byte[] memoryMap;

    public FlatMemory(int size) {
        LOGGER.debug("Initializing memory with size " + size);
        memoryMap = new byte[size];
        clock = Clock.getInstance();
    }

    @Override
    public int peek8(int address) {
        clock.addTstates(3);
        return memoryMap[address] & 0xff;
    }

    @Override
    public void poke8(int address, int value) {
        if (address >= ROM_TOP) {
            clock.addTstates(3);
            memoryMap[address] = (byte) value;
        }
    }

    @Override
    public int peek16(int address) {
        int lsb = peek8(address);
        int msb = peek8(address + 1);
        return (msb << 8) | lsb;
    }

    @Override
    public void poke16(int address, int word) {
        poke8(address, word);
        poke8(address + 1, word >>> 8);
    }

    @Override
    public void load(byte[] data, int srcPos, int address, int size) {
        try {
            System.arraycopy(data, srcPos, memoryMap, address, size);
        } catch (Exception e) {
            throw new RuntimeException("Loading memory map", e);
        }
    }

    @Override
    public byte[] asByteArray() {
        return memoryMap;
    }

}
