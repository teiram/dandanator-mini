package com.grelobites.romgenerator.zxspectrum.tape;

import com.grelobites.romgenerator.zxspectrum.InputPort;
import com.grelobites.romgenerator.zxspectrum.Peripheral;
import com.grelobites.romgenerator.zxspectrum.Z80VirtualMachine;

import java.io.IOException;
import java.io.InputStream;

public class Tape implements Peripheral, InputPort {

    private int cycleOnLastInput = 0;
    private static final int DEFAULT_IN_VALUE = 0;
    private boolean playing = false;
    private Z80VirtualMachine cpu;
    private TapBitInputStream tapStream;
    private int currentTapPos = 0;


    public void play(InputStream tap) throws IOException {
        if (cpu != null) {
            this.tapStream = new TapBitInputStream(tap);
            currentTapPos = 0;
            cycleOnLastInput = cpu.cycleCounter;
            playing = true;
        } else {
            throw new IllegalStateException("Tape not bound to CPU");
        }
    }

    public void stop() {
        playing = false;
        this.tapStream = null;
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
        cycleOnLastInput = cpu.cycleCounter;
    }

    private int getNextBitFromTap() {
        int deltaCycles = cpu.cycleCounter - cycleOnLastInput;
        tapStream.skip(deltaCycles);
        return tapStream.read();
    }

    @Override
    public int inb(int port, int hi) throws Exception {
        return playing ? getNextBitFromTap() : DEFAULT_IN_VALUE;
    }
}
