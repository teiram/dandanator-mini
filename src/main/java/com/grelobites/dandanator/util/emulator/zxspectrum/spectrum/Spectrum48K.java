package com.grelobites.dandanator.util.emulator.zxspectrum.spectrum;


import com.grelobites.dandanator.util.emulator.zxspectrum.Z80VirtualMachine;
import com.grelobites.dandanator.util.emulator.zxspectrum.MMU;
import com.grelobites.dandanator.util.emulator.zxspectrum.Peripheral;
import com.grelobites.dandanator.util.emulator.zxspectrum.PollingTarget;

/**
 * Sinclair spectrum emulator.
 */
public class Spectrum48K implements Peripheral, MMU, PollingTarget {
    protected Z80VirtualMachine cpu;

    protected FxScreen screen;
    protected ZXSnapshotLoader snapshot;
    protected FxKeyboard keyboard;
    private byte[] memory;

    public Spectrum48K() {
        screen = new FxScreen();
        snapshot = new ZXSnapshotLoader();
        keyboard = new FxKeyboard();
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
        return memory[address] & 0xff;
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

    public FxKeyboard getKeyboard() {
        return keyboard;
    }

    public String toString() {
        return "Spectrum 48K";
    }

}
