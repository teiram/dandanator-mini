package com.grelobites.romgenerator.util.gameloader.loaders.tap.tapeloader;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.loaders.Z80GameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.BreakpointReachedException;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.ExecutionForbiddenException;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.TapeFinishedException;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.memory.FlatMemory;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.Memory;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.Tape;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.Z80State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TapeLoader48 extends TapeLoaderBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapeLoader48.class);

    private static final int SCREEN_START = BANK_SIZE;
    private static final int SCREEN_END = SCREEN_START + SCREEN_SIZE;

    private final Memory z80Ram;
    private boolean breakOnScreenRamWrites;
    private Integer breakpointPC;

    public TapeLoader48() {
        super();
        z80Ram = new FlatMemory(clock, 0x10000);
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
        if (breakOnScreenRamWrites && (address >= SCREEN_START && address < SCREEN_END)) {
            if (z80.getRegPC() >= SCREEN_START) {
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
        clock.addTstates(4); // 4 clocks for write byte to bus
        if ((port & 0x0001) == 0) {
            ulaPort = value;
        } else {
            z80Ports[port] = (byte) value;
        }
    }

    @Override
    public void contendedStates(int address, int tstates) {
        clock.addTstates(tstates);
    }

    private void loadSpectrumRom() {
        try (InputStream romis = TapeLoader48.class.getResourceAsStream("/loader/48.rom")) {
            z80Ram.load(Util.fromInputStream(romis), 0, 0, 0x4000);
        } catch (IOException ioe) {
            LOGGER.debug("Loading Spectrum ROM", ioe);
        }
    }

    @Override
    protected void initialize() {
        super.initialize();
        loadSpectrumRom();
    }

    @Override
    protected List<byte[]> getRamBanks() {
        List<byte[]> banks = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            byte[] bank = Arrays.copyOfRange(z80Ram.asByteArray(), i * BANK_SIZE, (i + 1) * BANK_SIZE);
            banks.add(bank);
        }
        return banks;
    }

    @Override
    void prepareForLoading() {
        try (InputStream loaderStream = TapeLoader48.class
                .getResourceAsStream("/loader/loader.48.z80")) {
            RamGame game = (RamGame) new Z80GameImageLoader().load(loaderStream);
            GameUtil.popPC(game); //Since games are loaded with the PC pushed

            Z80State z80state = getStateFromHeader(game.getGameHeader());

            int slot = 0;
            for (int i : new int[] {0x4000, 0x8000, 0xc000}) {
                z80Ram.load(game.getSlot(slot++), 0, i, BANK_SIZE);
            }
            LOGGER.debug("Calculated Z80State as " + z80state);
            z80.setZ80State(z80state);
        } catch (IOException ioe) {
            LOGGER.debug("Loading Tape Loader", ioe);
        }
    }

    @Override
    protected RamGame contextAsGame() {
        GameHeader header = fromZ80State(z80.getZ80State());
        RamGame game =  new RamGame(GameType.RAM48, getRamBanks());
        game.setGameHeader(header);
        game.setHoldScreen(true);
        game.setHardwareMode(HardwareMode.HW_48K);
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
        prepareForLoading();
        breakOnScreenRamWrites = false;
        int stoppedFrames = 0;
        try {
            while (!tape.isEOT()) {
                executeFrame();

                if (tape.getReadBytes() >= DETECTION_THRESHOLD) {
                    breakOnScreenRamWrites = true;
                }

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
        } catch (TapeFinishedException tfe) {
            LOGGER.debug("Tape finished with cpu status " + z80.getZ80State(), tfe);
        }
    }

    public Game loadTape(InputStream tapeFile) {
        loadTapeInternal(tapeFile);
        LOGGER.debug("Z80 State before save " + z80.getZ80State());

        tape.stop();
        RamGame ramGame = contextAsGame();
        GameUtil.pushPC(ramGame);
        return ramGame;
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
