package com.grelobites.romgenerator.util.gameloader.loaders.tap;

import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
    private Clock clock;
    private int leaderPulses;
    private int mask;
    private List<Integer> blockOffsets;
    private boolean finishing = false;

    public Tape() {
        clock = Clock.getInstance();
        state = State.STOP;
        tapePos = 0;
        earBit = EAR_ON;
        idxHeader = 0;
        playing = false;
        blockOffsets = new ArrayList<>();
    }

    private static int readInt(byte buffer[], int start, int len) {
        int res = 0;

        for (int idx = 0; idx < len; idx++) {
            res |= ((buffer[start + idx] << (idx * 8)) & (0xff << idx * 8));
        }
        return res;
    }

    public boolean isFinishing() {
        return finishing;
    }

    private boolean findTapBlockOffsets() {
        int offset = 0;

        while (offset < tapeBuffer.length) {
            if ((tapeBuffer.length - offset) < 2) {
                return false;
            }
            int len = readInt(tapeBuffer, offset, 2);

            if (offset + len + 2 > tapeBuffer.length) {
                return false;
            }
            LOGGER.debug("Adding tape block with length " + len + " at offset " + offset);

            blockOffsets.add(offset);
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

        tapePos = idxHeader = 0;
        state = State.STOP;
        if (!findTapBlockOffsets()) {
            return false;
        }
        return true;
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
                if (tapePos == tapeBuffer.length) {
                    LOGGER.debug("Last byte detected");
                    finishing = true;
                    stop();
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
                    finishing = true;
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

    @Override
    public void clockTimeout() {
        playTap();
    }

    public boolean flashLoad(Z80 cpu, Memory memory) {
        LOGGER.debug("Tape.flashLoad with status " + this);
        clock.clearTimeout();
        stop();
        if (idxHeader >= blockOffsets.size()) {
            return false;
        }

        tapePos = blockOffsets.get(idxHeader);
        blockLen = readInt(tapeBuffer, tapePos, 2);
        tapePos += 2;
        if (cpu.getRegA() != (tapeBuffer[tapePos] & 0xff)) {
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

        while (count < nBytes && count < blockLen - 1) {
            memory.poke8(addr, tapeBuffer[tapePos + count + 1]);
            cpu.xor(tapeBuffer[tapePos + count + 1]);
            addr = (addr + 1) & 0xffff;
            count++;
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
            LOGGER.debug("Tape marked as finished");
            finishing = true;
        }

        return true;
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
                ", finishing=" + finishing +
                '}';
    }
}
