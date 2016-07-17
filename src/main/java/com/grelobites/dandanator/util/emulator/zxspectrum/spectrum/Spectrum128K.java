package com.grelobites.dandanator.util.emulator.zxspectrum.spectrum;

import com.grelobites.dandanator.util.emulator.zxspectrum.*;

/**
 * $Id: Spectrum128K.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * ZX Spectrum 128K emulator
 * <p>
 * $Log: Spectrum128K.java,v $
 * Revision 1.3  2005/03/18 16:40:48  mviara
 * Added support for speaker.
 * <p>
 * Revision 1.2  2004/07/18 11:22:29  mviara
 * Better 128K emulator.
 */
public class Spectrum128K implements InPort, OutPort, MMU, Polling, Spectrum {

    // Connected CPU
    protected Z80VirtualMachine cpu;
    protected Screen screen;
    protected ZXSnapshot snapshot;
    private int lastMmu;
    private int mmu = 0;
    private int romBank = 0;
    // Spectrum rom
    private byte roms[][] = new byte[2][0x4000];

    // Spectrum banked memory 128k
    private byte memory[][] = new byte[8][0x4000];


    public Spectrum128K() {
        screen = new Screen();
        snapshot = new ZXSnapshot();

    }

    /**
     * J80.InPort
     */
    public int inb(int port, int hi) {
        return 0xff;
    }

    /**
     * j80.OutPort
     */
    public void outb(int port, int value, int tstates) {
        switch (port) {
            case MMU_PORT:
                if ((mmu & MMU_DISABLE) != 0)
                    break;

                mmu = value;


                if ((mmu & MMU_VIDEO) != (lastMmu & MMU_VIDEO)) {
                    screen.setScreenMemory(memory[videoBank()], 0);
                }
                lastMmu = mmu;
                break;
        }
        //System.out.println("spectrum outb "+port+" = "+bite);
    }


    public void resetCPU(Z80VirtualMachine cpu) throws Exception {
        romBank = 0;
        lastMmu = mmu = 0;
        screen.setScreenMemory(memory[videoBank()], 0);
        cpu.addPeripheral(screen);
        cpu.addPeripheral(snapshot);
    }

    public void disconnectCPU(Z80VirtualMachine cpu) {
    }

    public void connectCPU(Z80VirtualMachine cpu) throws Exception {
        this.cpu = cpu;

        cpu.addOutPort(MMU_PORT, this);
        cpu.addOutPort(0xfffd, this);
        cpu.addOutPort(0xbffd, this);
        cpu.addOutPort(0xfd, this);
        cpu.addInPort(0xfffd, this);
        cpu.addPolling(20, this);
    }

    private int videoBank() {
        return ((mmu & MMU_VIDEO) != 0) ? 7 : 5;
    }

    private void checkScreen(int bank, int add) {
        if (bank == videoBank()) {
            if (add < SCREEN_MEMORY_SIZE) {
                screen.repaintScreen(add);
            } else if (add < SCREEN_ATTRIBUTE_SIZE + SCREEN_MEMORY_SIZE)
                screen.repaintAttribute(add - SCREEN_MEMORY_SIZE);
        }
    }

    private void pokeBank3(int add, int value) {
        int bank = mmu & 7;
        memory[bank][add] = (byte) value;
        checkScreen(bank, add);
    }

    private void pokeBank2(int add, int value) {
        memory[2][add] = (byte) value;
    }

    private void pokeBank1(int add, int value) {
        memory[5][add] = (byte) value;

        checkScreen(5, add);
    }

    private void pokeBank0(int add, int value) {
        if (cpu.isRunning())
            return;

        roms[romBank][add] = (byte) value;

        if (add == 0x3FFF) {
            System.out.println("Rom " + romBank + " loaded");
            romBank++;
        }
    }

    /**
     * j80.MMU
     */
    public void pokeb(int add, int value) {
        int bank = add / 0x4000;
        add &= 0x3fff;
        value &= 0xff;

        switch (bank) {
            case 0:
                pokeBank0(add, value);
                break;
            case 1:
                pokeBank1(add, value);
                break;
            case 2:
                pokeBank2(add, value);
                break;
            case 3:
                pokeBank3(add, value);
                break;
        }

    }


    public int peekb(int add) {
        int bank = add / 0x4000;
        add &= 0x3fff;

        switch (bank) {
            default:
                int romBank = (mmu & MMU_ROM) != 0 ? 1 : 0;
                return roms[romBank][add] & 0xff;

            case 1:
                return memory[5][add] & 0xff;

            case 2:
                return memory[2][add] & 0xff;

            case 3:
                return memory[mmu & 7][add] & 0xff;

        }
    }


    /**
     * Polling called every 20 ms
     */
    public void polling(Z80VirtualMachine cpu) {
        cpu.irq();

    }


    public String toString() {
        return "Spectrum 128K $Revision: 330 $";
    }
}
