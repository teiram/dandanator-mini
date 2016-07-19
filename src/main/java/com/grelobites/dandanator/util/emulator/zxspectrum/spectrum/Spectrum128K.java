package com.grelobites.dandanator.util.emulator.zxspectrum.spectrum;

import com.grelobites.dandanator.util.emulator.zxspectrum.InputPort;
import com.grelobites.dandanator.util.emulator.zxspectrum.MMU;
import com.grelobites.dandanator.util.emulator.zxspectrum.OutputPort;
import com.grelobites.dandanator.util.emulator.zxspectrum.PollingTarget;
import com.grelobites.dandanator.util.emulator.zxspectrum.Z80VirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Spectrum128K implements InputPort, OutputPort, MMU, PollingTarget {
    private static final Logger LOGGER = LoggerFactory.getLogger(Spectrum128K.class);
    protected Z80VirtualMachine cpu;
    protected FxScreen screen;
    protected ZXSnapshotLoader snapshot;
    private int lastMmu;
    private int mmu = 0;
    private int romBank = 0;
    // Spectrum rom
    private byte roms[][] = new byte[2][0x4000];

    // Spectrum banked memory 128k
    private byte memory[][] = new byte[8][0x4000];


    public Spectrum128K() {
        screen = new FxScreen();
        snapshot = new ZXSnapshotLoader();
    }

    @Override
    public int inb(int port, int hi) {
        return 0xff;
    }

    @Override
    public void outb(int port, int value, int tstates) {
        switch (port) {
            case SpectrumConstants.MMU_PORT:
                if ((mmu & SpectrumConstants.MMU_DISABLE) != 0)
                    break;

                mmu = value;


                if ((mmu & SpectrumConstants.MMU_VIDEO) !=
                        (lastMmu & SpectrumConstants.MMU_VIDEO)) {
                    screen.setScreenMemory(memory[videoBank()], 0);
                }
                lastMmu = mmu;
                break;
        }
    }


    public void onCpuReset(Z80VirtualMachine cpu) throws Exception {
        romBank = 0;
        lastMmu = mmu = 0;
        screen.setScreenMemory(memory[videoBank()], 0);
        cpu.addPeripheral(screen);
        cpu.addPeripheral(snapshot);
    }

    public void unbind(Z80VirtualMachine cpu) {
    }

    public void bind(Z80VirtualMachine cpu) throws Exception {
        this.cpu = cpu;

        cpu.addOutPort(SpectrumConstants.MMU_PORT, this);
        cpu.addOutPort(0xfffd, this);
        cpu.addOutPort(0xbffd, this);
        cpu.addOutPort(0xfd, this);
        cpu.addInPort(0xfffd, this);
        cpu.addPollingTarget(20, this);
    }

    private int videoBank() {
        return ((mmu & SpectrumConstants.MMU_VIDEO) != 0) ? 7 : 5;
    }

    private void checkScreen(int bank, int address) {
        if (bank == videoBank()) {
            if (address < SpectrumConstants.SCREEN_MEMORY_SIZE) {
                screen.repaintScreen(address);
            } else if (address < SpectrumConstants.SCREEN_ATTRIBUTE_SIZE
                    + SpectrumConstants.SCREEN_MEMORY_SIZE)
                screen.repaintAttribute(address -
                        SpectrumConstants.SCREEN_MEMORY_SIZE);
        }
    }

    private void pokeBank3(int address, int value) {
        int bank = mmu & 7;
        memory[bank][address] = (byte) value;
        checkScreen(bank, address);
    }

    private void pokeBank2(int address, int value) {
        memory[2][address] = (byte) value;
    }

    private void pokeBank1(int address, int value) {
        memory[5][address] = (byte) value;

        checkScreen(5, address);
    }

    private void pokeBank0(int address, int value) {
        if (cpu.isRunning())
            return;

        roms[romBank][address] = (byte) value;

        if (address == 0x3FFF) {
            LOGGER.info("Rom " + romBank + " loaded");
            romBank++;
        }
    }

    @Override
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

    @Override
    public int peekb(int add) {
        int bank = add / 0x4000;
        add &= 0x3fff;

        switch (bank) {
            default:
                int romBank = (mmu & SpectrumConstants.MMU_ROM) != 0 ? 1 : 0;
                return roms[romBank][add] & 0xff;

            case 1:
                return memory[5][add] & 0xff;

            case 2:
                return memory[2][add] & 0xff;

            case 3:
                return memory[mmu & 7][add] & 0xff;

        }
    }

    @Override
    public void poll(Z80VirtualMachine cpu) {
        cpu.irq();

    }
}
