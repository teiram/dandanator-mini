package com.grelobites.dandanator.util.emulator.zxspectrum.spectrum;


import com.grelobites.dandanator.util.emulator.zxspectrum.Z80VirtualMachine;
import com.grelobites.dandanator.util.emulator.zxspectrum.MMU;
import com.grelobites.dandanator.util.emulator.zxspectrum.Peripheral;
import com.grelobites.dandanator.util.emulator.zxspectrum.Polling;

/**
 * Sinclair spectrum emulator.
 */
public class Spectrum48K implements Peripheral, MMU, Polling, Spectrum {
    // Connected CPU
    protected Z80VirtualMachine cpu;

    protected FxScreen screen;
    protected ZXSnapshot snapshot;
    private byte memory[];

    public Spectrum48K() {
        screen = new FxScreen();
        snapshot = new ZXSnapshot();
        memory = new byte[0x10000];
    }

    // J80.MMU
    public void pokeb(int add, int value) {
        if (cpu.isRunning() && add < RAM_MEMORY_START)
            return;

        memory[add] = (byte) value;

        if (add >= SCREEN_MEMORY_START && add <= SCREEN_MEMORY_END)
            screen.repaintScreen(add - SCREEN_MEMORY_START);
        else if (add >= SCREEN_ATTRIBUTE_START && add <= SCREEN_ATTRIBUTE_END)
            screen.repaintAttribute(add - SCREEN_ATTRIBUTE_START);
    }

    public int peekb(int add) {
        return memory[add] & 0xff;
    }

    public void resetCPU(Z80VirtualMachine cpu) throws Exception {
        cpu.addPeripheral(screen);
        cpu.addPeripheral(snapshot);
    }

    public void disconnectCPU(Z80VirtualMachine cpu) {
    }


    public void connectCPU(Z80VirtualMachine cpu) throws Exception {
        this.cpu = cpu;
        screen.setScreenMemory(memory, SCREEN_MEMORY_START);
        cpu.addPolling(20, this);

    }

    /**
     * Polling called every 20 ms
     */
    public void polling(Z80VirtualMachine cpu) {
        cpu.irq();
    }

    public FxScreen getScreen() {
        return screen;
    }

    public ZXSnapshot getZxSnapshot() {
        return snapshot;
    }

    public String toString() {
        return "Spectrum 48K";
    }

}
