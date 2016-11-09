package com.grelobites.romgenerator.zxspectrum;


public interface InputPort {
    int inb(int port, int hi) throws Exception;
}
