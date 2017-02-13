package com.grelobites.romgenerator.util.gameloader.loaders.tap.tapeloader;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.Clock;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.LoaderDetector;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.Tape;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.TapeFinishedException;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.TapeLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.Z80;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.Z80State;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.Z80operations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

public abstract class TapeLoaderBase implements Z80operations, TapeLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapeLoaderBase.class);
    protected static final int LD_BYTES_RET_NZ_ADDR = 0x56b;
    protected static final int LD_BYTES_RET_POINT = 0x05e2;
    protected static final int BANK_SIZE = 0x4000;
    protected static final int FRAME_TSTATES = 69888;
    protected static final int INTERRUPT_TSTATES = 32;
    protected static final int DETECTION_THRESHOLD = 1024 * 12;
    protected static final int SCREEN_SIZE = 0x1b00;

    protected static final int MAXIMUM_LOAD_FRAMES = 10000;
    protected final Z80 z80;
    protected final Clock clock;
    protected Tape tape;
    protected LoaderDetector loaderDetector;
    protected final byte z80Ports[] = new byte[0x10000];
    protected int ulaPort;

    public TapeLoaderBase() {
        clock = new Clock();
        z80 = new Z80(clock, this);
        tape = new Tape(clock, true);
        loaderDetector = new LoaderDetector(tape);
    }

    protected static Z80.IntMode fromOrdinal(int mode) {
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

    public static Z80State getStateFromHeader(GameHeader header) {
        Z80State z80state = new Z80State();
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
        return z80state;
    }


    protected void initialize() {
        z80.reset();
        clock.reset();
    }

    protected void executeFrame() {
        long intTStates = clock.getTstates() + INTERRUPT_TSTATES;
        long frameTStates = clock.getTstates() + FRAME_TSTATES;
        z80.setINTLine(true);
        z80.execute(intTStates);
        z80.setINTLine(false);
        z80.execute(frameTStates);
    }

    abstract protected List<byte[]> getRamBanks();

    abstract void prepareForLoading();

    protected  GameHeader fromZ80State(Z80State z80state) {
        GameHeader header = new GameHeader();
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

        return header;
    }

    protected abstract RamGame contextAsGame();

    protected void loadTapeInternal(InputStream tapeFile) {
        initialize();
        z80.setBreakpoint(LD_BYTES_RET_NZ_ADDR, true);
        tape.insert(tapeFile);

        prepareForLoading();
        int executedFrames = 0;
        try {
            while (!tape.isEOT() && executedFrames++ < MAXIMUM_LOAD_FRAMES) {
                executeFrame();
            }
            LOGGER.debug("Tape didn't reach eof in " + executedFrames + " frames");
            throw new RuntimeException("Unable to convert TAP");
        } catch (TapeFinishedException tfe) {
            LOGGER.debug("Tape finished with cpu status " + z80.getZ80State(), tfe);
        }
        LOGGER.debug("Executing while PC in ROM");
        while (z80.getRegPC() < 0x4000) {
            z80.execute();
        }
        LOGGER.debug("PC left ROM with status " + z80.getZ80State());

    }

    @Override
    public Game loadTape(InputStream tapeFile) {
        loadTapeInternal(tapeFile);
        LOGGER.debug("Z80 State before save " + z80.getZ80State());

        tape.stop();
        RamGame ramGame = contextAsGame();
        GameUtil.pushPC(ramGame);
        return ramGame;
    }

    @Override
    public void execDone() {
        LOGGER.debug("execDone!!");
    }

}
