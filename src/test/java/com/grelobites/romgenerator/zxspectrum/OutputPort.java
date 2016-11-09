package com.grelobites.romgenerator.zxspectrum;

public interface OutputPort {
    public void outb(int port, int value, int states) throws Exception;
}
