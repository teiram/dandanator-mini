package com.grelobites.romgenerator.util.gameloader.loaders.tap;

import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tape implements ClockTimeoutListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(Tape.class);
    private static final int EAR_OFF = 0xbf;
    private static final int EAR_ON = 0xff;
    private static final int EAR_MASK = 0x40;

    private static final int LEADER_LENGHT = 2168;
    private static final int SYNC1_LENGHT = 667;
    private static final int SYNC2_LENGHT = 735;
    private static final int ZERO_LENGHT = 855;
    private static final int ONE_LENGHT = 1710;
    private static final int HEADER_PULSES = 8063;
    private static final int DATA_PULSES = 3223;
    //private static final int END_BLOCK_PAUSE = 3500000;
    private static final int END_BLOCK_PAUSE = 4000000;


    public enum State {
        STOP, START, LEADER, LEADER_NOCHG, SYNC, NEWBYTE,
        NEWBYTE_NOCHG, NEWBIT, HALF2, LAST_PULSE, PAUSE, TZX_HEADER, PURE_TONE,
        PURE_TONE_NOCHG, PULSE_SEQUENCE, PULSE_SEQUENCE_NOCHG, NEWDR_BYTE,
        NEWDR_BIT, PAUSE_STOP, CSW_RLE, CSW_ZRLE
    }


    private State state;
    private boolean playing;
    private int earBit;
    private byte[] tapeBuffer;
    private int idxHeader;
    private int tapePos;
    private int blockLen;
    private int bitTime;
    private final Clock clock;
    private int leaderPulses;
    private int mask;
    private List<Integer> blockOffsets;
    private boolean eot = false;
    private int readBytes = 0;
    private boolean throwOnEot = false;

    public Tape(Clock clock) {
        this.clock = clock;
        state = State.STOP;
        tapePos = 0;
        earBit = EAR_ON;
        idxHeader = 0;
        playing = false;
        blockOffsets = new ArrayList<>();
    }

    public Tape(Clock clock, boolean throwOnEot) {
        this(clock);
        this.throwOnEot = throwOnEot;
    }

    private static int readInt(byte buffer[], int start, int len) {
        int res = 0;

        for (int idx = 0; idx < len; idx++) {
            res |= ((buffer[start + idx] << (idx * 8)) & (0xff << idx * 8));
        }
        return res;
    }

    public Clock getClock() {
        return clock;
    }

    public boolean isEOT() {
        return eot;
    }

    private static String readBlockName(byte[] buffer, int offset, int length) {
        return new String(Arrays.copyOfRange(buffer, offset, offset + length));
    }

    private boolean findTapBlockOffsets() {
        int offset = 0;

        int blocksToBlackList = 0;

        while (offset < tapeBuffer.length) {
            if ((tapeBuffer.length - offset) < 2) {
                return false;
            }
            int len = readInt(tapeBuffer, offset, 2);

            if (offset + len + 2 > tapeBuffer.length) {
                return false;
            }
            String name = readBlockName(tapeBuffer, offset + 4, 10);

            if (name.equals("128Museum ")) {
                blocksToBlackList = 2;
                LOGGER.debug("Skipping 128Museum blocks");
            } else if (--blocksToBlackList <= 0) {
                blockOffsets.add(offset);
                LOGGER.debug("Adding tape block with length " + len + " and name " + name + " at offset " + offset);
            }
            offset += len + 2;

        }
        LOGGER.debug("Number of blocks in tape " + blockOffsets.size());

        return true;
    }

    public boolean insert(File fileName) {
        try (FileInputStream is = new FileInputStream(fileName)) {
            return insert(is);
        } catch (IOException ioe) {
            LOGGER.error("Inserting tape", ioe);
        }
        return false;
    }

    public boolean insert(InputStream is) {
        try {
            tapeBuffer = Util.fromInputStream(is);
        } catch (IOException ioe) {
            LOGGER.error("Inserting tape", ioe);
            return false;
        }

        tapePos = idxHeader = readBytes = 0;
        blockOffsets.clear();
        eot = playing = false;

        state = State.STOP;
        if (!findTapBlockOffsets()) {
            return false;
        }
        return true;
    }

    public void rewind() {
        state = State.STOP;
        tapePos = idxHeader = readBytes = 0;
        eot = playing = false;
    }

    public void eject() {
        stop();
        tapePos = idxHeader = readBytes = 0;
        eot = false;
        state = State.STOP;
    }

    private boolean playTap() {
        switch (state) {
            case STOP:
                stop();
                break;
            case START:
                tapePos = blockOffsets.get(idxHeader);
                blockLen = readInt(tapeBuffer, tapePos, 2);
                LOGGER.debug("Starting tape block " + idxHeader + " of len " + blockLen);
                tapePos += 2;
                leaderPulses = tapeBuffer[tapePos] >= 0 ? HEADER_PULSES : DATA_PULSES;
                earBit = EAR_OFF;
                state = State.LEADER;
                clock.setTimeout(LEADER_LENGHT);
                break;
            case LEADER:
                earBit ^= EAR_MASK;
                if (leaderPulses-- > 0) {
                    clock.setTimeout(LEADER_LENGHT);
                    break;
                }
                state = State.SYNC;
                clock.setTimeout(SYNC1_LENGHT);
                break;
            case SYNC:
                earBit ^= EAR_MASK;
                state = State.NEWBYTE;
                clock.setTimeout(SYNC2_LENGHT);
                break;
            case NEWBYTE:
                mask = 0x80; //MSB to LSB direction
            case NEWBIT:
                earBit ^= EAR_MASK;
                if ((tapeBuffer[tapePos] & mask) == 0) {
                    bitTime = ZERO_LENGHT;
                } else {
                    bitTime = ONE_LENGHT;
                }
                state = State.HALF2;
                clock.setTimeout(bitTime);
                break;
            case HALF2:
                earBit ^= EAR_MASK;
                clock.setTimeout(bitTime);
                mask >>>= 1;
                if (mask == 0) {
                    tapePos++;
                    readBytes++;
                    if (--blockLen > 0) {
                        state = State.NEWBYTE;
                    } else {
                        state = State.PAUSE;

                    }
                } else {
                    state = State.NEWBIT;
                }
                break;
            case PAUSE:
                earBit ^= EAR_MASK;
                state = State.PAUSE_STOP;
                clock.setTimeout(END_BLOCK_PAUSE); // 1 sec. pause
                break;
            case PAUSE_STOP:
                idxHeader++;
                if (idxHeader == blockOffsets.size()) {
                    LOGGER.debug("Last tape byte detected");
                    stop();
                    onEot();
                } else {
                    state = State.START; // START
                    playTap();
                }
        }
        return true;
    }

    public boolean play() {
        if (!playing) {
            if (idxHeader >= blockOffsets.size()) {
                LOGGER.warn("Trying to play with blocks exhausted");
                return false;
            }
            state = State.START;
            tapePos = blockOffsets.get(idxHeader);
            clock.addClockTimeoutListener(this);
            clockTimeout();
            playing = true;
        }
        return true;
    }


    public void stop() {
        if (playing) {
            if (state == State.PAUSE_STOP) {
                idxHeader++;
                if (idxHeader >= blockOffsets.size()) {
                    eot = true;
                }
            }
            state = State.STOP;
            clock.removeClockTimeoutListener(this);
            playing = false;
        }
    }

    public int getTapePos() {
        return tapePos;
    }
    public boolean isPlaying() {
        return playing;
    }

    public State getState() {
        return state;
    }

    public int getEarBit() {
        return earBit;
    }

    public int getReadBytes() {
        return readBytes;
    }

    @Override
    public void clockTimeout() {
        playTap();
    }

    public boolean flashLoad(Z80 cpu, Memory memory) {
        LOGGER.debug("Tape.flashLoad with status " + this
            + ", CPU status " + cpu.getZ80State());
        clock.clearTimeout();
        stop();
        if (idxHeader >= blockOffsets.size()) {
            return false;
        }

        tapePos = blockOffsets.get(idxHeader);
        blockLen = readInt(tapeBuffer, tapePos, 2);
        tapePos += 2;
        //AF and AF have been already switched here
        if (cpu.getRegAx() != (tapeBuffer[tapePos] & 0xff)) {
            cpu.xor(tapeBuffer[tapePos]);
            cpu.setCarryFlag(false);
            idxHeader++;
            return true;
        }
        //Parity includes flag byte
        cpu.setRegA(tapeBuffer[tapePos]);

        int count = 0;
        int addr = cpu.getRegIX();    // Address start
        int nBytes = cpu.getRegDE();  // Length
        LOGGER.debug("Flash loading " + nBytes + " bytes on address " + Integer.toHexString(addr));
        LOGGER.debug("In header " + idxHeader + " from set of blocks of size " + blockOffsets.size());
        while (count < nBytes && count < blockLen - 1) {
            memory.poke8(addr, tapeBuffer[tapePos + count + 1]);
            cpu.xor(tapeBuffer[tapePos + count + 1]);
            addr = (addr + 1) & 0xffff;
            count++;
            readBytes++;
        }

        //Load DE byte count
        if (count == nBytes) {
            cpu.xor(tapeBuffer[tapePos + count + 1]); //Parity byte
            cpu.cp(0x01);
        }

        //Less bytes on tape than requested on DE
        //It should have failed with timeout in LD-SAMPLE (0x05ED)
        // signalled as CARRY==reset & ZERO==set
        if (count < nBytes) {
            cpu.setFlags(0x50); // when B==0xFF, then INC B, B=0x00, F=0x50
        }

        cpu.setRegIX(addr);
        cpu.setRegDE(nBytes - count);
        idxHeader++;
        if (idxHeader >= blockOffsets.size()) {
            onEot();
        }

        return true;
    }

    private void onEot() {
        eot = true;
        if (throwOnEot) {
            throw new TapeFinishedException("Tape completed");
        }
    }

    @Override
    public String toString() {
        return "Tape{" +
                "state=" + state +
                ", playing=" + playing +
                ", earBit=" + earBit +
                ", idxHeader=" + idxHeader +
                ", tapePos=" + tapePos +
                ", blockLen=" + blockLen +
                ", blockOffsets= " + blockOffsets +
                ", tapeBuffer.length=" + (tapeBuffer != null ? tapeBuffer.length : "nil") +
                ", eot=" + eot +
                '}';
    }
}
