package com.grelobites.dandanator.util.emulator.zxspectrum;

public interface SnapshotLoader extends Peripheral {
    void load(Z80VirtualMachine vm, String filename);
}

