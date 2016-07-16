package com.grelobites.dandanator.util.emulator.zxspectrum.spectrum;

/**
 * $Id: Spectrum.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * Sinclair spectrum constants
 * <p>
 * $Log: Spectrum.java,v $
 * Revision 1.3  2004/06/20 16:25:58  mviara
 * Split spectrum emulator in more files.
 * Added partial support for 128K.
 */
public interface Spectrum {
    /**
     * Memory map spectrum 48K
     */
    public static final int ROM_MEMORY_END = 0x3FFF;
    public static final int RAM_MEMORY_START = 0x4000;
    public static final int SCREEN_MEMORY_SIZE = 0x1800;
    public static final int SCREEN_MEMORY_START = 0x4000;
    public static final int SCREEN_MEMORY_END = 0x57FF;
    public static final int SCREEN_ATTRIBUTE_START = 0x5800;
    public static final int SCREEN_ATTRIBUTE_END = 0x5AFF;
    public static final int SCREEN_ATTRIBUTE_SIZE = 0x0300;

    /**
     * Memory manager 128K
     */
    public static final int MMU_VIDEO = (1 << 3);
    public static final int MMU_ROM = (1 << 4);
    public static final int MMU_DISABLE = (1 << 5);
    public static final int MMU_PORT = 0x7ffd;

}
