package com.grelobites.dandanator.util.emulator.zxspectrum.spectrum;


import com.grelobites.dandanator.util.emulator.zxspectrum.J80;
import com.grelobites.dandanator.util.emulator.zxspectrum.MMU;
import com.grelobites.dandanator.util.emulator.zxspectrum.Peripheral;
import com.grelobites.dandanator.util.emulator.zxspectrum.Polling;

/**
 * $Id: Spectrum48K.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * Sinclair spectrum emulator
 * <p>
 * $Log: Spectrum48K.java,v $
 * Revision 1.2  2005/03/18 16:40:48  mviara
 * Added support for speaker.
 * <p>
 * Revision 1.1  2004/06/20 16:25:58  mviara
 * Split spectrum emulator in more files.
 * Added partial support for 128K.
 */
public class Spectrum48K implements Peripheral, MMU, Polling, Spectrum {


    // Connected CPU
    protected J80 cpu;

    protected Screen screen;
    protected ZXSnapshot snapshot;
    private byte memory[];

    public Spectrum48K() {
        screen = new Screen();
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

    public void resetCPU(J80 cpu) throws Exception {
        cpu.addPeripheral(screen);
        cpu.addPeripheral(snapshot);
    }

    public void disconnectCPU(J80 cpu) {
    }


    public void connectCPU(J80 cpu) throws Exception {
        this.cpu = cpu;
        screen.setScreenMemory(memory, SCREEN_MEMORY_START);
        cpu.addPolling(20, this);

    }

    /**
     * Polling called every 20 ms
     */
    public void polling(J80 cpu) {
        cpu.irq();


    }


    public String toString() {
        return "Spectrum 48K $Revision: 330 $";
    }

}
