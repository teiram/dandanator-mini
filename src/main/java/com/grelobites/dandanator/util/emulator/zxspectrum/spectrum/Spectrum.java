package com.grelobites.dandanator.util.emulator.zxspectrum.spectrum;

public interface Spectrum {
    /**
     * Memory map spectrum 48K
     */
    int ROM_MEMORY_END = 0x3FFF;
    int RAM_MEMORY_START = 0x4000;
    int SCREEN_MEMORY_SIZE = 0x1800;
    int SCREEN_MEMORY_START = 0x4000;
    int SCREEN_MEMORY_END = 0x57FF;
    int SCREEN_ATTRIBUTE_START = 0x5800;
    int SCREEN_ATTRIBUTE_END = 0x5AFF;
    int SCREEN_ATTRIBUTE_SIZE = 0x0300;

    /**
     * Memory manager 128K
     */
    int MMU_VIDEO = (1 << 3);
    int MMU_ROM = (1 << 4);
    int MMU_DISABLE = (1 << 5);
    int MMU_PORT = 0x7ffd;

}
