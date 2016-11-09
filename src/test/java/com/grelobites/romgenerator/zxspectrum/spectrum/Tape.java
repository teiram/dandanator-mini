package com.grelobites.romgenerator.zxspectrum.spectrum;

import com.grelobites.romgenerator.zxspectrum.InputPort;
import com.grelobites.romgenerator.zxspectrum.Peripheral;
import com.grelobites.romgenerator.zxspectrum.Z80VirtualMachine;

public class Tape implements Peripheral, InputPort {

    private int cycleOnLastInput = 0;
    private static final int DEFAULT_IN_VALUE = 0;
    private boolean playing = false;
    private Z80VirtualMachine cpu;
    private byte[] tap;
    private int currentTapPos = 0;


    public void play(byte[] tap) {
        if (cpu != null) {
            this.tap = tap;
            currentTapPos = 0;
            cycleOnLastInput = cpu.cycleCounter;
            playing = true;
        } else {
            throw new IllegalStateException("Tape not bound to CPU");
        }
    }

    public void stop() {
        playing = false;
        this.tap = null;
    }

    @Override
    public void bind(Z80VirtualMachine cpu) throws Exception {
        this.cpu = cpu;
    }

    @Override
    public void unbind(Z80VirtualMachine cpu) throws Exception {
        this.cpu = null;
    }

    @Override
    public void onCpuReset(Z80VirtualMachine cpu) throws Exception {

    }

    private int getNextBitFromTap() {
        int deltaCycles = cpu.cycleCounter - cycleOnLastInput;

    }

    @Override
    public int inb(int port, int hi) throws Exception {
        return playing ? getNextBitFromTap() : DEFAULT_IN_VALUE;
    }
}
