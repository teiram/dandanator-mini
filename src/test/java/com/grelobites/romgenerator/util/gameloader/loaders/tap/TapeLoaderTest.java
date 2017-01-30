package com.grelobites.romgenerator.util.gameloader.loaders.tap;

import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.Z80GameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.memory.FlatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TapeLoaderTest implements Z80operations {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapeLoaderTest.class);
    private static final int LD_BYTES_ADDR = 0x556;
    private static final int BANK_SIZE = 0x4000;
    private static final int SCREEN_SIZE = 0x1b00;
    private static final int SCREEN_START = BANK_SIZE;
    private static final int SCREEN_END = SCREEN_START + SCREEN_SIZE;
    private static final int FRAME_TSTATES = 69888;
    private static final int INTERRUPT_TSTATES = 32;
    private static final int DETECTION_THRESHOLD = 1024 * 12;
    private final Z80 z80;
    private final Clock clock;
    private final Memory z80Ram;
    private Tape tape;
    private LoaderDetector loaderDetector;
    private final byte z80Ports[] = new byte[0x10000];
    private boolean breakOnScreenRamWrites = false;
    private int ulaPort;
    private Integer breakpointPC;

    public TapeLoaderTest() {
        clock = new Clock();
        z80Ram = new FlatMemory(clock, 0x10000);
        z80 = new Z80(clock, this);
        tape = new Tape(clock);
        loaderDetector = new LoaderDetector(tape);
    }

    @Override
    public int fetchOpcode(int address) {
        return z80Ram.peek8(address);
    }

    @Override
    public int peek8(int address) {
        return z80Ram.peek8(address);
    }

    @Override
    public void poke8(int address, int value) {
        if (breakOnScreenRamWrites && (address >= SCREEN_START && address < SCREEN_END)) {
            if (z80.getRegPC() >= 0x4000) {
                throw new ExecutionForbiddenException("Attempt to write on screen");
            } else {
                LOGGER.debug("Ignoring write attempt from ROM 0x" + Integer.toHexString(z80.getRegPC()));
            }
        }
        z80Ram.poke8(address, value);
    }

    @Override
    public int peek16(int address) {
        return z80Ram.peek16(address);
    }

    @Override
    public void poke16(int address, int word) {
        z80Ram.poke16(address, word);
    }

    @Override
    public int inPort(int port) {
        clock.addTstates(4); // 4 clocks for read byte from bus
        if ((port & 0x0001) == 0) {
            loaderDetector.onAudioInput(z80);
            return tape.getEarBit();
        } else {
            LOGGER.debug("IN " + Integer.toHexString(port) + "@ 0x" + Integer.toHexString(z80.getRegPC()));
            //return z80Ports[port] & 0xff;
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
        try (InputStream romis = TapeLoaderTest.class.getResourceAsStream("/loader/48.rom")) {
            z80Ram.load(Util.fromInputStream(romis), 0, 0, 0x4000);
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

    private void loadSnaLoader() {
        try (InputStream loaderStream = TapeLoaderTest.class.getResourceAsStream("/loader/loader.48.sna")) {
            ByteBuffer loader = ByteBuffer.wrap(Util.fromInputStream(loaderStream)).order(ByteOrder.LITTLE_ENDIAN);
            Z80State z80state = new Z80State();
            z80state.setRegI(loader.get());
            z80state.setRegHLx(loader.getShort());
            z80state.setRegDEx(loader.getShort());
            z80state.setRegBCx(loader.getShort());
            z80state.setRegAFx(loader.getShort());
            z80state.setRegHL(loader.getShort());
            z80state.setRegDE(loader.getShort());
            z80state.setRegBC(loader.getShort());
            z80state.setRegIY(loader.getShort());
            z80state.setRegIX(loader.getShort());
            z80state.setIFF2(loader.get() != 0);
            z80state.setRegR(loader.get());
            z80state.setRegAF(loader.getShort());
            int sp = loader.getShort() & 0xffff;
            z80state.setIM(fromOrdinal(loader.get()));
            loader.get(); //Skip border color
            byte[] ramData = new byte[0xc000];
            loader.get(ramData);
            z80Ram.load(ramData, 0, 0x4000, 0xc000);

            z80state.setRegPC((z80Ram.peek8(sp) & 0xff) | (z80Ram.peek8(sp + 1) << 8));
            z80state.setRegSP((sp + 2) & 0xffff);

            LOGGER.debug("Calculated Z80State as " + z80state);
            z80.setZ80State(z80state);

        } catch (IOException ioe) {
            LOGGER.debug("Loading SNA Loader", ioe);
        }
    }

    private void initialize() {
        z80.reset();
        clock.reset();
        loadSpectrumRom();
        loaderDetector.reset();
    }

    private void executeFrame() {
        long intTStates = clock.getTstates() + INTERRUPT_TSTATES;
        long frameTStates = clock.getTstates() + FRAME_TSTATES;
        z80.setINTLine(true);
        z80.execute(intTStates);
        z80.setINTLine(false);
        z80.execute(frameTStates);
    }

    private void runProcessor() {
        long intTStates = clock.getTstates() + 10;
        long frameTStates = clock.getTstates() + 50;
        z80.setINTLine(true);
        z80.execute(intTStates);
        z80.setINTLine(false);
        z80.execute(frameTStates);
    }

    public void loadTapeInternal(File tapeFile) {
        initialize();
        z80.setBreakpoint(0x056b, true); //LD-BREAK
        if (breakpointPC != null) {
            z80.setBreakpoint(breakpointPC, true);
            tape.rewind();
        } else {
            tape.insert(tapeFile);
        }
        loadSnaLoader();
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
                LOGGER.debug("Detected screen write on 0x" + Integer.toHexString(z80.getLastPC())
                        + ", after reading " + tape.getReadBytes() + " bytes", efe);
                breakpointPC = z80.getLastPC();
                loadTapeInternal(null);
            } else {
                LOGGER.error("ExecutionForbidden with breakpointPC = " + breakpointPC);
            }
        } catch (BreakpointReachedException bre) {
            LOGGER.debug("Breakpoint reached. Finishing");
            z80.setRegPC(z80.getLastPC());
        }
    }

    private void loadTape(File tapeFile) {
        loadTapeInternal(tapeFile);
        LOGGER.debug("Z80 State before save " + z80.getZ80State());
        try (FileOutputStream fos = new FileOutputStream(new File(tapeFile.getPath() + ".z80"))) {
            GameImageLoader loader = new Z80GameImageLoader();
            RamGame ramGame = contextAsGame(z80.getZ80State());
            GameUtil.pushPC(ramGame);
            loader.save(ramGame, fos);
        } catch (IOException ioe) {
            LOGGER.error("Saving game ", ioe);
        }
    }

    private List<byte[]> getRamBanks() {
        List<byte[]> banks = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            byte[] bank = Arrays.copyOfRange(z80Ram.asByteArray(), i * BANK_SIZE, (i + 1) * BANK_SIZE);
            banks.add(bank);
        }
        return banks;
    }

    public RamGame contextAsGame(Z80State z80state) {
        GameHeader header = new GameHeader();
        header.setAFRegister(z80state.getRegAF());
        header.setBCRegister(z80state.getRegBC());
        header.setHLRegister(z80state.getRegHL());
        header.setPCRegister(z80state.getRegPC());
        header.setSPRegister(z80state.getRegSP());
        header.setIRegister(z80state.getRegI());
        LOGGER.debug(String.format("PC: %04x, SP: %04x", header.getPCRegister(), header.getSPRegister()));
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

        RamGame game =  new RamGame(GameType.RAM48, getRamBanks());
        game.setGameHeader(header);
        game.setHardwareMode(HardwareMode.HW_48K);
        return game;
    }

    public void saveSna() {
        try (FileOutputStream fos = new FileOutputStream("/Users/mteira/Desktop/output.sna")) {
            Z80State state = z80.getZ80State();
            int pc = state.getRegPC();
            int sp = state.getRegSP();
            LOGGER.debug("Saving SNA with PC: 0x" + Integer.toHexString(pc) +
                    ", SP: 0x" + Integer.toHexString(sp) +
                    ", ULA Port : 0x" + Integer.toHexString(ulaPort));

            z80Ram.poke8(--sp, (pc >> 8) & 0xff);
            z80Ram.poke8(--sp, pc & 0xff);

            fos.write(
                    ByteBuffer.allocate(49179)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .put(Integer.valueOf(state.getRegI()).byteValue())
                            .putShort(Integer.valueOf(state.getRegHLx()).shortValue())
                            .putShort(Integer.valueOf(state.getRegDEx()).shortValue())
                            .putShort(Integer.valueOf(state.getRegBCx()).shortValue())
                            .putShort(Integer.valueOf(state.getRegAFx()).shortValue())
                            .putShort(Integer.valueOf(state.getRegHL()).shortValue())
                            .putShort(Integer.valueOf(state.getRegDE()).shortValue())
                            .putShort(Integer.valueOf(state.getRegBC()).shortValue())
                            .putShort(Integer.valueOf(state.getRegIY()).shortValue())
                            .putShort(Integer.valueOf(state.getRegIX()).shortValue())
                            .put(Integer.valueOf(state.isIFF2() ? 0xFF : 0).byteValue())
                            .put(Integer.valueOf(state.getRegR()).byteValue())
                            .putShort(Integer.valueOf(state.getRegAF()).shortValue())
                            .putShort(Integer.valueOf(sp).shortValue())
                            .put(Integer.valueOf(state.getIM().ordinal()).byteValue())
                            .put(Integer.valueOf(ulaPort).byteValue())
                            .put(Arrays.copyOfRange(z80Ram.asByteArray(), 0x4000, 0x10000))
                            .array());
        } catch (Exception e) {
            LOGGER.error("Writing SNA", e);
        }
    }

    @Override
    public void breakpoint() {
        LOGGER.debug("In breakpoint!!!!");
        if (z80.getRegPC() == 0x056b) {
            LOGGER.debug("LD_BYTES_ADDR Breakpoint reached with tape in state " + tape.getState());
            LOGGER.debug("SP points to 0x" + Integer.toHexString(peek16(z80.getRegSP())));
            if (tape.flashLoad(z80, z80Ram)) {
                for (int i = 0; i < 10; i += 2) {
                    LOGGER.debug("SP Points to 0x" + Integer.toHexString(peek16(z80.getRegSP() - i)));
                }
                //z80.setRegPC(z80.pop());
                z80.setRegPC(0x05e2);
            } else {
                LOGGER.debug("flashload returned false!!");
            }
        } else if (z80.getRegPC() == breakpointPC) {
            LOGGER.debug("Reached breakpoint PC");
            throw new BreakpointReachedException("LastPC reached");
        }
    }

    @Override
    public void execDone() {
        LOGGER.debug("execDone!!");
    }

    public static void main(String[] args) {
        TapeLoaderTest loader = new TapeLoaderTest();
        loader.loadTape(new File("/Users/mteira/Desktop/tap/nonworking/dark sceptre.tap"));
    }

}
