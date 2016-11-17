package com.grelobites.romgenerator.z80core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class TapeLoader implements Z80operations {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapeLoader.class);
    private static final int LAST_K = 23560;
    private static final int FLAGS = 23611;

    private final Z80 z80;
    private final Clock clock;
    private final Memory z80Ram;
    private Tape tape;
    private long tStatesLastIn;
    private final byte z80Ports[] = new byte[0x10000];
    private boolean detectScreenRamWrites = false;
    private boolean screenRamWritten = false;
    private int ulaPort;

    public TapeLoader() {
        z80Ram = new FlatMemory(0x10000);
        z80 = new Z80(this);
        tape = new Tape();
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
        if (detectScreenRamWrites) {
            if (address >= 0x4000 && address <= (0x4000 + 0x1b00)) {
                LOGGER.debug("Attempt to write on " + Integer.toHexString(address));
                screenRamWritten = true;
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
            tStatesLastIn = clock.getTstates();
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
        try (InputStream romis = (TapeLoader.class.getResourceAsStream("/48.rom"))) {
            z80Ram.load(romis, 0, 0x4000);
        } catch (IOException ioe) {
            LOGGER.debug("Loading Spectrum ROM", ioe);
        }
    }

    private static int tStatesInFrames(int frames) {
        return 69888 * frames; //16K/48K specific
    }

    private void runLoadCommand() {
        LOGGER.debug("runLoadCommand. PC = " + Integer.toHexString(z80.getRegPC()));
        executeFrames(100); //Let the Spectrum initialize
        LOGGER.debug("After initialization. PC = " + Integer.toHexString(z80.getRegPC()));
        z80Ram.poke8(LAST_K, (byte) 0xEF); // LOAD keyword
        z80Ram.poke8(FLAGS, (byte) (z80Ram.peek8(FLAGS) | 0x20));
        executeFrames(10);

        LOGGER.debug("After LOAD Command. PC = " + Integer.toHexString(z80.getRegPC()));

        z80Ram.poke8(LAST_K, (byte) 0x22); // " symbol
        z80Ram.poke8(FLAGS, (byte) (z80Ram.peek8(FLAGS) | 0x20));
        executeFrames(10);

        LOGGER.debug("After Quotation Mark. PC = " + Integer.toHexString(z80.getRegPC()));

        z80Ram.poke8(LAST_K, (byte) 0x22); // " symbol
        z80Ram.poke8(FLAGS, (byte) (z80Ram.peek8(FLAGS) | 0x20));
        executeFrames(10);
        LOGGER.debug("After Quotation Mark. PC = " + Integer.toHexString(z80.getRegPC()));

        z80Ram.poke8(LAST_K, (byte) 0x0D); // Enter key
        z80Ram.poke8(FLAGS, (byte) (z80Ram.peek8(FLAGS) | 0x20));
        executeFrames(10);
        LOGGER.debug("After Enter. PC = " + Integer.toHexString(z80.getRegPC()));
    }

    private void initialize() {
        z80.reset();
        clock.reset();
        loadSpectrumRom();
    }

    private void executeFrames(int frames) {
        int tstates = tStatesInFrames(frames);
        while (tstates-- > 0) {
            z80.execute();
        }
    }

    private void executeStates(long states) {
        while (states-- > 0) {
            z80.execute();
        }
    }

    public void loadTape(File tapeFile) {
        initialize();
        tape.insert(tapeFile);
        runLoadCommand();
        if (tape.play()) {
            do {
                z80.execute();
            } while (!tape.isFinishing());
            detectScreenRamWrites = true;
            for (int i = 0; i < 100000000; i++) {
                z80.execute();
                if (screenRamWritten) {
                    LOGGER.debug("Detected write attempt on video RAM");
                    break;
                }
            }
            saveSna();
        } else {
            throw new IllegalStateException("Unable to play tape");
        }
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
                            .put(Arrays.copyOfRange(z80Ram.asByteArray(), 0x4000, 0xffff))
                            .array());
        } catch (Exception e) {
            LOGGER.error("Writing SNA", e);
        }
    }

    @Override
    public void breakpoint() {
        LOGGER.debug("breakpoint!!");

    }

    @Override
    public void execDone() {
        LOGGER.debug("execDone!!");
    }

    public static void main(String[] args) {
        TapeLoader loader = new TapeLoader();
        loader.loadTape(new File("/Users/mteira/Desktop/SH.TAP"));
    }

}
