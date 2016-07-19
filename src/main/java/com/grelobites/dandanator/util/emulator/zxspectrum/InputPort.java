package com.grelobites.dandanator.util.emulator.zxspectrum;

/**
 * <p>
 * Interface to trap a input port
 */
public interface InputPort {
    int inb(int port, int hi) throws Exception;
}
