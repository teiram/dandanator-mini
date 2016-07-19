package com.grelobites.dandanator.util.emulator.zxspectrum;

public interface OutputPort {
    public void outb(int port, int value, int states) throws Exception;
}
