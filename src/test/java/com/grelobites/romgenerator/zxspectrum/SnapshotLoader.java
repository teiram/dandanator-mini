package com.grelobites.romgenerator.zxspectrum;

public interface SnapshotLoader extends Peripheral {
    void load(Z80VirtualMachine vm, String filename);
}

