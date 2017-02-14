package com.grelobites.romgenerator.util.gameloader.loaders.tap.tapeloader;

import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.Key;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.Keyboard;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.memory.Spectrum128KMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TapeLoader128 extends TapeLoaderBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapeLoader128.class);

    private static final int[] SPECTRUM_BANKS = new int[] {5, 2, 0, 1, 3, 4, 6, 7};
    private final Spectrum128KMemory z80Ram;
    private int last7ffd;
    private Keyboard keyboard;
    private String[] romResources = DEFAULT_ROM_RESOURCES;

    private static final int ULA_PORT_MASK = 0x01;

    private static final String[] DEFAULT_ROM_RESOURCES =
            new String[]{
                    "/loader/128-0.rom",
                    "/loader/128-1.rom"
            };

    private static final int ULA_AUDIO_MASK = 0x40;
    private static final int ULA_KEYS_MASK = 0x1F;


    public TapeLoader128() {
        super();
        z80Ram = new Spectrum128KMemory(last7ffd);
        keyboard = new Keyboard(clock);
    }

    @Override
    public int fetchOpcode(int address) {
        return z80Ram.peek8(address);
    }

    @Override
    public int peek8(int address) {
        clock.addTstates(3);
        return z80Ram.peek8(address);
    }

    @Override
    public void poke8(int address, int value) {
        clock.addTstates(3);
        z80Ram.poke8(address, value);
    }

    @Override
    public int peek16(int address) {
        clock.addTstates(3);
        return z80Ram.peek16(address);
    }

    @Override
    public void poke16(int address, int word) {
        clock.addTstates(3);
        z80Ram.poke16(address, word);
    }

    @Override
    public int inPort(int port) {
        clock.addTstates(4); // 4 clocks for read byte from bus
        if ((port & ULA_PORT_MASK) == 0) {
            loaderDetector.onAudioInput(z80);
            return (tape.getEarBit() & ULA_AUDIO_MASK) | (keyboard.getUlaBits(port) & ULA_KEYS_MASK);
        } else {
            return 0xff;
        }
    }

    @Override
    public void outPort(int port, int value) {
        //LOGGER.debug(String.format("OUT 0x%04x -> 0x%04x", value, port));
        clock.addTstates(4); // 4 clocks for write byte to bus
        if ((port & ULA_PORT_MASK) == 0) {
            ulaPort = value;
        } else if ((port & 0x8002) == 0) {
            //Port 7FFD decoding
            last7ffd = value;
            z80Ram.setLast7ffd(last7ffd);
            if ((value & 0x20) != 0) {
                LOGGER.debug("DISABLING FURTHER MAPPING CHANGE!!!!");
            }
        }
        z80Ports[port] = (byte) value;
    }

    @Override
    public void contendedStates(int address, int tstates) {
        clock.addTstates(tstates);
    }

    private void loadSpectrumRoms() {
        int index = 0;
        for (String rom : romResources) {
            loadSpectrumRom(rom, index++);
        }
    }

    private void loadSpectrumRom(String resource, int index) {
        LOGGER.debug("Loading rom " + resource + " in position " + index);
        try (InputStream romis = TapeLoader128.class.getResourceAsStream(resource)) {
            z80Ram.loadBank(Util.fromInputStream(romis), index);
        } catch (IOException ioe) {
            LOGGER.debug("Loading Spectrum ROM", ioe);
        }
    }

    @Override
    protected void prepareForLoading() {
        keyboard.pressKey(2000, Key.KEY_ENTER);
    }

    @Override
    protected void initialize() {
        super.initialize();
        clock.reset();
        last7ffd = 0;
        z80Ram.setLast7ffd(last7ffd);
        loadSpectrumRoms();
    }

    @Override
    protected List<byte[]> getRamBanks() {
        List<byte[]> banks = new ArrayList<>();
        for (int i : SPECTRUM_BANKS) {
            banks.add(z80Ram.getBank(Spectrum128KMemory.RAM_1STBANK + i));
        }
        return banks;
    }

    @Override
    protected RamGame contextAsGame() {
        GameHeader header = fromZ80State(z80.getZ80State());
        header.setPort7ffdValue(last7ffd);

        RamGame game =  new RamGame(GameType.RAM128, getRamBanks());
        game.setGameHeader(header);
        game.setHoldScreen(true);
        game.setHardwareMode(HardwareMode.HW_128K);
        return game;
    }

    @Override
    public void breakpoint() {
        if (z80.getRegPC() == LD_BYTES_RET_NZ_ADDR) {
            LOGGER.debug("LD_BYTES_ADDR Breakpoint reached with tape in state " + tape.getState());
            if (tape.flashLoad(z80, z80Ram)) {
                z80.setRegPC(LD_BYTES_RET_POINT);
            }
        }
    }

    public void setRomResources(String[] romResources) {
        if (romResources != null && romResources.length == DEFAULT_ROM_RESOURCES.length) {
            this.romResources = romResources;
        } else {
            throw new IllegalArgumentException("Invalid rom resources provided");
        }
    }
}
