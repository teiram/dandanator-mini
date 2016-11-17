package com.grelobites.romgenerator.z80core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

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
        } else {
            LOGGER.warn("Attempt to write on ROM address " + Integer.toHexString(address));
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
    public void load(InputStream is, int pos, int size) {
        try {
            int read = is.read(memoryMap, pos, size);
            LOGGER.debug("Read " + read + " bytes into memory");
        } catch (Exception e) {
            throw new RuntimeException("Loading memory map", e);
        }
    }

    @Override
    public byte[] asByteArray() {
        return memoryMap;
    }

}
