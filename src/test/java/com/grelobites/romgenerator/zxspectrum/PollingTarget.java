package com.grelobites.romgenerator.zxspectrum;

public interface PollingTarget {
    void poll(Z80VirtualMachine cpu);
}