package com.grelobites.romgenerator.zxspectrum.spectrum;


import com.grelobites.romgenerator.zxspectrum.Z80VirtualMachine;
import com.grelobites.romgenerator.zxspectrum.MMU;
import com.grelobites.romgenerator.zxspectrum.Peripheral;
import com.grelobites.romgenerator.zxspectrum.PollingTarget;

import java.io.IOException;
import java.io.InputStream;

/**
 * Sinclair spectrum emulator.
 */
public class Spectrum48K implements Peripheral, MMU, PollingTarget {
    protected Z80VirtualMachine cpu;

    protected FxScreen screen;
    protected ZXSnapshotLoader snapshot;
    protected FxULA keyboard;
    private byte[] memory;

    public Spectrum48K() {
        screen = new FxScreen();
        snapshot = new ZXSnapshotLoader();
        keyboard = new FxULA();
        memory = new byte[0x10000];
    }

    @Override
    public void pokeb(int address, int value) {
        if (cpu.isRunning() && address < SpectrumConstants.RAM_MEMORY_START)
            return;

        memory[address] = (byte) value;

        if (address >= SpectrumConstants.SCREEN_MEMORY_START && address <=
                SpectrumConstants.SCREEN_MEMORY_END)
            screen.repaintScreen(address - SpectrumConstants.SCREEN_MEMORY_START);
        else if (address >= SpectrumConstants.SCREEN_ATTRIBUTE_START &&
                address <= SpectrumConstants.SCREEN_ATTRIBUTE_END)
            screen.repaintAttribute(address -
                    SpectrumConstants.SCREEN_ATTRIBUTE_START);
    }

    @Override
    public int peekb(int address) {
        return memory[address & 0xffff] & 0xff;
    }

    @Override
    public void onCpuReset(Z80VirtualMachine cpu) throws Exception {
        cpu.addPeripheral(screen);
        cpu.addPeripheral(snapshot);
        cpu.addPeripheral(keyboard);
    }

    @Override
    public void unbind(Z80VirtualMachine cpu) {
    }

    @Override
    public void bind(Z80VirtualMachine cpu) throws Exception {
        this.cpu = cpu;
        screen.setScreenMemory(memory, SpectrumConstants.SCREEN_MEMORY_START);
        cpu.addPollingTarget(20, this);
    }

    @Override
    public void poll(Z80VirtualMachine cpu) {
        cpu.irq();
    }

    public FxScreen getScreen() {
        return screen;
    }

    public ZXSnapshotLoader getZxSnapshot() {
        return snapshot;
    }

    public FxULA getKeyboard() {
        return keyboard;
    }

    public String toString() {
        return "Spectrum 48K";
    }

    public void play(InputStream is) throws IOException {
        keyboard.play(is);
    }

    public byte[] getMemory() {
        return this.memory;
    }

    public void stop() {
        keyboard.stop();
    }
}
