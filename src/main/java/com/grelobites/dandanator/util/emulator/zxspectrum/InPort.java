package com.grelobites.dandanator.util.emulator.zxspectrum;

/**
 * <p>
 * Interface to trap a input port
 */
public interface InPort {
    /**
     * Input port
     *
     * @param port - Specify the Z80 input port
     */
    int inb(int port, int hi) throws Exception;
}
