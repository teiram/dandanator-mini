package com.grelobites.dandanator.util.emulator.zxspectrum;

public interface PollingTarget {
    void poll(Z80VirtualMachine cpu);
}