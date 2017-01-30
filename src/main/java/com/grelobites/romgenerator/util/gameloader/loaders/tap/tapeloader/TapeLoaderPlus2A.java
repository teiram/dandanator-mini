package com.grelobites.romgenerator.util.gameloader.loaders.tap.tapeloader;

import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.loaders.Z80GameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.Z80State;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.memory.SpectrumPlus2Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TapeLoaderPlus2A extends TapeLoaderBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapeLoaderPlus2A.class);

    private final SpectrumPlus2Memory z80Ram;
    private final static int[] SPECTRUM_BANKS =  new int[] {5, 2, 0, 1, 3, 4, 6, 7};
    private int last7ffd;
    private int last1ffd;

    public TapeLoaderPlus2A() {
        super();
        z80Ram = new SpectrumPlus2Memory(last7ffd, last1ffd);
    }

    @Override
    public int fetchOpcode(int address) {
        return peek8(address);
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
        if ((port & 0x0001) == 0) {
            loaderDetector.onAudioInput(z80);
            return tape.getEarBit();
        } else {
            return 0xff;
        }
    }

    @Override
    public void outPort(int port, int value) {
        //LOGGER.debug(String.format("OUT 0x%04x -> 0x%04x", value, port));
        clock.addTstates(4); // 4 clocks for write byte to bus
        if ((port & 0x0001) == 0) {
            ulaPort = value;
        } else if ((port & 0xc002) == 0x4000) {
            //Port 7FFD decoding
            last7ffd = value;
            //LOGGER.debug("Setting 7FFD to 0x" + Integer.toHexString(last7ffd));
            z80Ram.setLast7ffd(last7ffd);
            if ((value & 0x20) != 0) {
                LOGGER.debug("DISABLING FURTHER MAPPING CHANGE!!!!");
            }
        } else if ((port & 0xf002) == 0x1000) {
            last1ffd = value;
            //LOGGER.debug("Setting 1FFD to 0x" + Integer.toHexString(last1ffd));
            z80Ram.setLast1ffd(last1ffd);
        }
        z80Ports[port] = (byte) value;
    }

    @Override
    public void contendedStates(int address, int tstates) {
        clock.addTstates(tstates);
    }

    private void loadSpectrumRoms() {
        loadSpectrumRom("/loader/plus23-0.rom", 0);
        loadSpectrumRom("/loader/plus23-1.rom", 1);
        loadSpectrumRom("/loader/plus23-2.rom", 2);
        loadSpectrumRom("/loader/plus23-3.rom", 3);
    }

    private void loadSpectrumRom(String resource, int index) {
        try (InputStream romis = TapeLoaderPlus2A.class.getResourceAsStream(resource)) {
            z80Ram.loadBank(Util.fromInputStream(romis), index);
        } catch (IOException ioe) {
            LOGGER.debug("Loading Spectrum ROM", ioe);
        }
    }

    @Override
    protected void loadTapeLoader() {
        try (InputStream loaderStream = TapeLoaderPlus2A.class
                .getResourceAsStream("/loader/loader.+2a.z80")) {
            RamGame game = (RamGame) new Z80GameImageLoader().load(loaderStream);
            GameUtil.popPC(game);
            GameHeader header = game.getGameHeader();
            Z80State z80state = getStateFromHeader(header);

            int slot = 0;
            for (int i : SPECTRUM_BANKS) {
                z80Ram.loadBank(game.getSlot(slot++), SpectrumPlus2Memory.RAM_1STBANK + i);
            }

            z80Ram.setLast1ffd(header.getPort1ffdValue(0));
            z80Ram.setLast7ffd(header.getPort7ffdValue(0));

            LOGGER.debug("Calculated Z80State as " + z80state);
            z80.setZ80State(z80state);

        } catch (IOException ioe) {
            LOGGER.debug("Loading Tape Loader", ioe);
        }
    }

    @Override
    protected void initialize() {
        super.initialize();
        clock.reset();
        loadSpectrumRoms();
    }

    @Override
    protected List<byte[]> getRamBanks() {
        List<byte[]> banks = new ArrayList<>();
        for (int i : SPECTRUM_BANKS) {
            banks.add(z80Ram.getBank(SpectrumPlus2Memory.RAM_1STBANK + i));
        }
        return banks;
    }

    @Override
    protected RamGame contextAsGame() {
        GameHeader header = fromZ80State(z80.getZ80State());
        header.setPort1ffdValue(last1ffd);
        header.setPort7ffdValue(last7ffd);

        RamGame game =  new RamGame(GameType.RAM128, getRamBanks());
        game.setGameHeader(header);
        game.setHoldScreen(true);
        game.setHardwareMode(HardwareMode.HW_PLUS2A);
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

}
