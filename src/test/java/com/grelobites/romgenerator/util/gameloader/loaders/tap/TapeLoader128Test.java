package com.grelobites.romgenerator.util.gameloader.loaders.tap;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.loaders.Z80GameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.memory.SpectrumPlus2Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TapeLoader128Test implements Z80operations {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapeLoader128Test.class);
    private static final int LD_BYTES_RET_NZ_ADDR = 0x56b;
    private static final int LD_BYTES_RET_POINT = 0x05e2;
    private static final int BANK_SIZE = 0x4000;
    private static final int FRAME_TSTATES = 69888;
    private static final int INTERRUPT_TSTATES = 32;
    private static final int DETECTION_THRESHOLD = 1024 * 12;
    private static final int SCREEN_SIZE = 0x1b00;

    private final Z80 z80;
    private final Clock clock;
    private final SpectrumPlus2Memory z80Ram;
    private Tape tape;
    private LoaderDetector loaderDetector;
    private final byte z80Ports[] = new byte[0x10000];
    private int ulaPort;
    private int last7ffd;
    private int last1ffd;
    boolean breakOnScreenRamWrites;
    private Integer breakpointPC;
    private int lastInstruction = 0;
    private int lastAddress = 0;
    private Date lastTimestamp;

    public TapeLoader128Test() {
        clock = new Clock();
        z80Ram = new SpectrumPlus2Memory(last7ffd, last1ffd);
        z80 = new Z80(clock, this);
        tape = new Tape(clock, true);
        loaderDetector = new LoaderDetector(tape);
    }

    private int getScreenStartAddress() {
        return z80Ram.getRamBankAddress((last7ffd & 0x08) != 0 ? 7 : 5);
    }

    private int getScreenEndAddress() {
        return getScreenStartAddress() + SCREEN_SIZE;
    }

    @Override
    public int fetchOpcode(int address) {
        lastAddress = address;
        lastInstruction = peek8(address);
        lastTimestamp = new Date();
        return lastInstruction;
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
        try (InputStream romis = TapeLoader128Test.class.getResourceAsStream(resource)) {
            z80Ram.loadBank(Util.fromInputStream(romis), index);
        } catch (IOException ioe) {
            LOGGER.debug("Loading Spectrum ROM", ioe);
        }
    }

    private Z80.IntMode fromOrdinal(int mode) {
        switch (mode) {
            case 0:
                return Z80.IntMode.IM0;
            case 1:
                return Z80.IntMode.IM1;
            case 2:
                return Z80.IntMode.IM2;
        }
        throw new IllegalArgumentException("Invalid Interrupt mode: " + mode);
    }

    private void loadTapeLoader() {
        try (InputStream loaderStream = TapeLoader128Test.class
                .getResourceAsStream("/loader/loader.+2a.z80")) {
            RamGame game = (RamGame) new Z80GameImageLoader().load(loaderStream);
            GameUtil.popPC(game);

            Z80State z80state = new Z80State();
            GameHeader header = game.getGameHeader();
            z80state.setRegI(header.getIRegister());
            z80state.setRegHLx(header.getAlternateHLRegister());
            z80state.setRegDEx(header.getAlternateDERegister());
            z80state.setRegBCx(header.getAlternateBCRegister());
            z80state.setRegAFx(header.getAlternateAFRegister());
            z80state.setRegHL(header.getHLRegister());
            z80state.setRegDE(header.getDERegister());
            z80state.setRegBC(header.getBCRegister());
            z80state.setRegIY(header.getIYRegister());
            z80state.setRegIX(header.getIXRegister());
            z80state.setIFF2(header.getInterruptEnable() != 0);
            z80state.setRegR(header.getRRegister());
            z80state.setRegAF(header.getAFRegister());
            z80state.setIM(fromOrdinal(header.getInterruptMode()));
            z80state.setRegPC(header.getPCRegister());
            z80state.setRegSP(header.getSPRegister());

            int slot = 0;
            for (int i : new int[] {5, 2, 0, 1, 3, 4, 6, 7}) {
                z80Ram.loadBank(game.getSlot(slot++), 4 + i);
            }

            z80Ram.setLast1ffd(header.getPort1ffdValue(0));
            z80Ram.setLast7ffd(header.getPort7ffdValue(0));

            LOGGER.debug("Calculated Z80State as " + z80state);
            z80.setZ80State(z80state);

        } catch (IOException ioe) {
            LOGGER.debug("Loading Tape Loader", ioe);
        }
    }

    private void initialize() {
        z80.reset();
        clock.reset();
        loadSpectrumRoms();
    }

    private void executeFrame() {
        long intTStates = clock.getTstates() + INTERRUPT_TSTATES;
        long frameTStates = clock.getTstates() + FRAME_TSTATES;
        z80.setINTLine(true);
        z80.execute(intTStates);
        z80.setINTLine(false);
        z80.execute(frameTStates);
    }

    private List<byte[]> getRamBanks() {
        List<byte[]> banks = new ArrayList<>();
        for (int i : new int[]{5, 2, 0, 1, 3, 4, 6, 7}) {
            banks.add(z80Ram.getBank(4 + i));
        }
        return banks;
    }

    private RamGame contextAsGame() {
        GameHeader header = new GameHeader();
        Z80State z80state = z80.getZ80State();
        header.setAFRegister(z80state.getRegAF());
        header.setBCRegister(z80state.getRegBC());
        header.setHLRegister(z80state.getRegHL());
        header.setPCRegister(z80state.getRegPC());
        header.setSPRegister(z80state.getRegSP());
        header.setIRegister(z80state.getRegI());
        header.setRRegister(z80state.getRegR());
        header.setBorderColor(ulaPort & 0x7);
        header.setDERegister(z80state.getRegDE());
        header.setAlternateBCRegister(z80state.getRegBCx());
        header.setAlternateDERegister(z80state.getRegDEx());
        header.setAlternateHLRegister(z80state.getRegHLx());
        header.setAlternateAFRegister(z80state.getRegAFx());
        header.setIYRegister(z80state.getRegIY());
        header.setIXRegister(z80state.getRegIX());
        header.setInterruptEnable(z80state.isIFF1() ? 0xff : 0x00);
        header.setInterruptMode(z80state.getIM().ordinal());

        header.setPort1ffdValue(last1ffd);
        header.setPort7ffdValue(last7ffd);

        RamGame game =  new RamGame(GameType.RAM128, getRamBanks());
        game.setGameHeader(header);
        game.setHoldScreen(true);
        game.setHardwareMode(HardwareMode.HW_PLUS2A);
        return game;
    }

    private boolean frameOnRom(int stackDepth) {
        int address = z80.getRegSP();
        for (int i = 0; i < stackDepth; i++) {
            if (peek16(address) < 0x4000) {
                LOGGER.debug("Found return addres on ROM at address 0x" +
                    Integer.toHexString(address));
                return false;
            }
            address += 2;
        }
        return true;
    }

    private boolean exitConditionsMet() {
        return z80.getRegPC() >= 0x4000 &&
                z80.getRegHL() >= 0x4000 &&
                !frameOnRom(5);
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
        int executedFrames = 0;
        try {
            while (!tape.isEOT()) {

                executeFrame();
                executedFrames++;
/*
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
                */
            }
        } catch (ExecutionForbiddenException efe) {
            if (breakpointPC == null && !tape.isEOT()) {
                LOGGER.debug("Detected screen write with cpu status " + z80.getZ80State()
                        + ", after reading " + tape.getReadBytes() + " bytes", efe);
                breakpointPC = z80.getLastPC();
                loadTapeInternal(null);
            }
        } catch (TapeFinishedException tfe) {
            LOGGER.debug("Tape is finished after " + executedFrames + " executed frames");
            /*
            int maxWaitFrames = 100000;
            while (maxWaitFrames-- > 0 && !exitConditionsMet()) {
                z80.execute();
            }
            */
        } catch (BreakpointReachedException bre) {
            z80.setRegPC(z80.getLastPC());
        }
    }

    private class Z80Monitor implements Runnable {
        private Thread thread;
        private boolean running = false;
        public void start() {
            thread = new Thread(this, "Z80 Monitor");
            running = true;
            thread.start();
        }
        public void stop() {
            running = false;
            thread.interrupt();
        }
        public void run() {
            try {
                while (running) {
                    LOGGER.debug("Z80 Running with status " + z80.getZ80State());
                    LOGGER.debug("Memory " + z80Ram);
                    LOGGER.debug("Clock: " + clock);
                    LOGGER.debug(String.format("Last instruction decoded 0x%02x @ 0x%04x on %tc",
                            lastInstruction, lastAddress, lastTimestamp));
                    Thread.sleep(5000);
                }
            } catch (InterruptedException ie) {}
        }
    }

    public Game loadTape(InputStream tapeFile) {
        Z80Monitor monitor = new Z80Monitor();
        monitor.start();
        loadTapeInternal(tapeFile);
        monitor.stop();
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

    @Override
    public void execDone() {
        LOGGER.debug("execDone!!");
    }

    public static void main(String[] args) throws Exception {
        TapeLoader128Test loader = new TapeLoader128Test();
        Game game = loader.loadTape(new FileInputStream("/Users/mteira/Desktop/Dandanator/tap/128/wime.tap"));

        try (FileOutputStream fos = new FileOutputStream("/Users/mteira/Desktop/Dandanator/tap/128/wime.z80")) {
            new Z80GameImageLoader().save(game, fos);
        }
    }
}
