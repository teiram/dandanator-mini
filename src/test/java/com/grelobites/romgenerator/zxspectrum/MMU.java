package com.grelobites.romgenerator.zxspectrum;

public interface MMU extends Peripheral {

    public int peekb(int address);

    public void pokeb(int address, int value);
}