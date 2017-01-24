package com.grelobites.romgenerator.util.gameloader.loaders.tap.tapeloader;

import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.loaders.Z80GameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.BreakpointReachedException;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.ExecutionForbiddenException;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.memory.Spectrum128KMemory;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.Tape;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.Z80State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TapeLoader128 extends TapeLoaderBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapeLoader128.class);

    private final Spectrum128KMemory z80Ram;
    private int last7ffd;

    public TapeLoader128() {
        super();
        z80Ram = new Spectrum128KMemory(last7ffd);
    }

    private int getScreenStartAddress() {
        return z80Ram.getRamBankAddress((last7ffd & 0x08) != 0 ? 7 : 5);
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
        int screenStartAddress = getScreenStartAddress();
        if (breakOnScreenRamWrites && (address >= screenStartAddress && address < (screenStartAddress + SCREEN_SIZE))) {
            if (z80.getRegPC() >= BANK_SIZE) {
                throw new ExecutionForbiddenException("Attempt to write on screen");
            } else {
                LOGGER.debug("Ignoring write attempt from ROM 0x" + Integer.toHexString(z80.getRegPC()));
            }
        }
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
        } else if ((port & 0x8001) == 0) {
            //Port 7FFD decoding
            last7ffd = value;
            LOGGER.debug("Setting 7FFD to 0x" + Integer.toHexString(last7ffd));
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
        loadSpectrumRom("/loader/128-0.rom", 0);
        loadSpectrumRom("/loader/128-1.rom", 1);
    }

    private void loadSpectrumRom(String resource, int index) {
        try (InputStream romis = TapeLoader128.class.getResourceAsStream(resource)) {
            z80Ram.loadBank(Util.fromInputStream(romis), index);
        } catch (IOException ioe) {
            LOGGER.debug("Loading Spectrum ROM", ioe);
        }
    }

    @Override
    protected void loadTapeLoader() {
        try (InputStream loaderStream = TapeLoader128.class
                .getResourceAsStream("/loader/loader.128.z80")) {
            RamGame game = (RamGame) new Z80GameImageLoader().load(loaderStream);
            GameUtil.popPC(game);
            GameHeader header = game.getGameHeader();
            Z80State z80state = getStateFromHeader(header);

            int slot = 0;
            for (int i : new int[] {5, 2, 0, 1, 3, 4, 6, 7}) {
                z80Ram.loadBank(game.getSlot(slot++), Spectrum128KMemory.RAM_1STBANK + i);
            }

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
        for (int i : new int[]{5, 2, 0, 1, 3, 4, 6, 7}) {
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

    public void loadTapeInternal(InputStream tapeFile) {
        initialize();
        z80.setBreakpoint(LD_BYTES_RET_NZ_ADDR, true);
        if (breakpointPC != null) {
            z80.setBreakpoint(breakpointPC, true);
            tape.rewind();
        } else {
            tape.insert(tapeFile);
        }
        loadTapeLoader();
        breakOnScreenRamWrites = false;
        int stoppedFrames = 0;
        try {
            while (!tape.isEOT()) {
                executeFrame();
/*
                if (tape.getReadBytes() >= DETECTION_THRESHOLD) {
                    breakOnScreenRamWrites = true;
                }
*/
                if (tape.getState() == Tape.State.STOP || tape.getState() == Tape.State.PAUSE_STOP) {
                    if (++stoppedFrames > 1000) {
                        LOGGER.debug("Detected tape stopped with state " + z80.getZ80State());
                        break;
                    }
                } else {
                    stoppedFrames = 0;
                }
            }
        } catch (ExecutionForbiddenException efe) {
            if (breakpointPC == null && !tape.isEOT()) {
                LOGGER.debug("Detected screen write with cpu status " + z80.getZ80State()
                        + ", after reading " + tape.getReadBytes() + " bytes", efe);
                breakpointPC = z80.getLastPC();
                loadTapeInternal(null);
            }
        } catch (BreakpointReachedException bre) {
            z80.setRegPC(z80.getLastPC());
        }
    }

    @Override
    public void breakpoint() {
        if (z80.getRegPC() == LD_BYTES_RET_NZ_ADDR) {
            LOGGER.debug("LD_BYTES_ADDR Breakpoint reached with tape in state " + tape.getState());
            if (tape.flashLoad(z80, z80Ram)) {
                z80.setRegPC(LD_BYTES_RET_POINT);
            }
        } else if (z80.getRegPC() == breakpointPC) {
            //Stop execution and save state
            LOGGER.debug("Reached breakpoint PC");
            throw new BreakpointReachedException("Breakpoint PC");
        }
    }

}
