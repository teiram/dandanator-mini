package com.grelobites.dandanator.util.emulator.zxspectrum;

public interface Snapshot extends Peripheral {
    public void loadSnapshot(Z80VirtualMachine vm, String filename);
}

