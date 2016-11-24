package com.grelobites.romgenerator.util.gameloader.loaders.tap;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TapeLoader implements Z80operations {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapeLoader.class);
    private static final int LD_BYTES_ADDR = 0x556;
    private static final int BANK_SIZE = 0x4000;
    private static final int FRAME_TSTATES = 69888;
    private static final int INTERRUPT_TSTATES = 32;
    private final Z80 z80;
    private final Clock clock;
    private final Memory z80Ram;
    private Tape tape;
    private LoaderDetector loaderDetector;
    private final byte z80Ports[] = new byte[0x10000];
    private int ulaPort;

    public TapeLoader() {
        z80Ram = new FlatMemory(0x10000);
        z80 = new Z80(this);
        tape = new Tape();
        loaderDetector = new LoaderDetector(tape);
        this.clock = Clock.getInstance();
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
            return z80Ports[port] & 0xff;
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
        try (InputStream romis = TapeLoader.class.getResourceAsStream("/loader/48.rom")) {
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
        try (InputStream loaderStream = TapeLoader.class.getResourceAsStream("/loader/loader.48.sna")) {
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
        long intTStates = clock.getTstates() + INTERRUPT_TSTATES;
        long frameTStates = clock.getTstates() + 1000;
        z80.setINTLine(true);
        z80.execute(intTStates);
        z80.setINTLine(false);
        z80.execute(frameTStates);
    }

    private List<byte[]> getRamBanks() {
        List<byte[]> banks = new ArrayList<byte[]>();
        for (int i = 1; i < 4; i++) {
            byte[] bank = Arrays.copyOfRange(z80Ram.asByteArray(), i * BANK_SIZE, (i + 1) * BANK_SIZE);
            banks.add(bank);
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

    public Game loadTape0(InputStream is) {
        initialize();
        z80.setBreakpoint(LD_BYTES_ADDR, true);
        tape.insert(is);
        loadSnaLoader();
        int frameLimit = 100;
        while (!tape.isEOT() && frameLimit-- >= 0) {
            executeFrame();
        }
        frameLimit = 100;
        while (frameLimit-- >= 0) {
            executeFrame();
        }

        LOGGER.debug("Exited loop with frameLimit " + frameLimit
                + ". Z80 State before save " + z80.getZ80State());
        tape.stop();
        RamGame ramGame = contextAsGame();
        GameUtil.pushPC(ramGame);
        return ramGame;
    }

    public Game loadTape(InputStream is) {
        initialize();
        z80.setBreakpoint(LD_BYTES_ADDR, true);
        tape.insert(is);
        loadSnaLoader();
        int lastTapePos = 0;
        long lastTstates = clock.getTstates();
        while (!tape.isEOT()) {
            runProcessor();
            if (tape.getTapePos() - lastTapePos == 0) {
                if (clock.getTstates() - lastTstates > 100000000) {
                    LOGGER.debug("Tape stays stopped. Position: " + tape.getTapePos()
                            + ", tstates: " + clock.getTstates());
                    break;
                }
            } else {
                lastTapePos = tape.getTapePos();
                lastTstates = clock.getTstates();
            }
        }

        LOGGER.debug("Exited loop with Z80 State " + z80.getZ80State());
        tape.stop();
        RamGame ramGame = contextAsGame();
        GameUtil.pushPC(ramGame);
        return ramGame;
    }


    @Override
    public void breakpoint() {
        if (z80.getRegPC() == LD_BYTES_ADDR) {
            LOGGER.debug("LD_BYTES_ADDR Breakpoint reached with tape in state " + tape.getState());
            if (tape.flashLoad(z80, z80Ram)) {
                z80.setRegPC(z80.pop());
            }
        }
    }

    @Override
    public void execDone() {
        LOGGER.debug("execDone!!");
    }

}
