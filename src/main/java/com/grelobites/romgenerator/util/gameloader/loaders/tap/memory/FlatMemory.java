package com.grelobites.romgenerator.util.gameloader.loaders.tap.memory;

import com.grelobites.romgenerator.util.gameloader.loaders.tap.Clock;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlatMemory implements Memory {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlatMemory.class);

    private static final int ROM_TOP = 0x4000;
    private final Clock clock;
    private byte[] memoryMap;

    public FlatMemory(Clock clock, int size) {
        LOGGER.debug("Initializing memory with size " + size);
        memoryMap = new byte[size];
        this.clock = clock;
    }

    @Override
    public int peek8(int address) {
        clock.addTstates(3);
        return address < memoryMap.length ? memoryMap[address] & 0xff : 0xff;
    }

    @Override
    public void poke8(int address, int value) {
        if (address >= ROM_TOP) {
            clock.addTstates(3);
            if (address < memoryMap.length) {
                memoryMap[address] = (byte) value;
            } else {
                LOGGER.debug("Trying to write past the memory size");
            }
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
        LOGGER.debug("Loading {} data length from position {} to position {} with size {}",
                data.length, srcPos, address, size);
        try {
            System.arraycopy(data, srcPos, memoryMap, address, size);
        } catch (Exception e) {
            throw new RuntimeException("Loading memory map", e);
        }
    }

    public void reset() {
        for (int i = ROM_TOP; i < memoryMap.length; i++) {
            memoryMap[i] = 0;
        }
    }

    @Override
    public byte[] asByteArray() {
        return memoryMap;
    }

}
