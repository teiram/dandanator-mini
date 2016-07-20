package com.grelobites.dandanator.util;

import com.grelobites.dandanator.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 *
 0        1      byte   I
 1        8      word   HL',DE',BC',AF'
 9        10     word   HL,DE,BC,IY,IX
 19       1      byte   Interrupt (bit 2 contains IFF2, 1=EI/0=DI)
 20       1      byte   R
 21       4      words  AF,SP
 25       1      byte   IntMode (0=IM0/1=IM1/2=IM2)
 26       1      byte   BorderColor (0..7, not used by Spectrum 1.7)
 27       49152  bytes  RAM dump 16384..65535
 */
public class SNAHeader {
    private static final Logger LOGGER = LoggerFactory.getLogger(SNAHeader.class);


    public static final byte REG_I = 0;
    public static final byte REG_HL_alt = 1;
    public static final byte REG_DE_alt = 3;
    public static final byte REG_BC_alt = 5;
    public static final byte REG_AF_alt = 7;
    public static final byte REG_HL = 9;
    public static final byte REG_DE = 11;
    public static final byte REG_BC = 13;
    public static final byte REG_IY = 15;
    public static final byte REG_IX = 17;
    public static final byte INTERRUPT_ENABLE = (byte) 19;
    public static final byte REG_R = 20;
    public static final byte REG_AF = 21;
    public static final byte REG_SP = 23;
    public static final byte INTERRUPT_MODE = (byte) 25;
    public static final byte BORDER_COLOR = (byte) 26;
    public static final byte RAM_DUMP = (byte) 27;
    private static final List<Integer> VALID_INTERRUPT_MODES = Arrays.asList(new Integer[] {0, 1, 2});
    private byte[] data = new byte[Constants.SNA_HEADER_SIZE];


    public static SNAHeader fromStream(InputStream is) throws IOException {
        SNAHeader snaHeader = new SNAHeader();
        int read = is.read(snaHeader.data);
        if (read != Constants.SNA_HEADER_SIZE) {
            LOGGER.error("Unexpected size read from SNA Header. Was: " + read + ", expected: " + Constants.SNA_HEADER_SIZE);
            throw new IllegalArgumentException("Exhausted stream");
        }
        return snaHeader;
    }

    public static SNAHeader fromByteArray(byte[] in) {
        if (in.length >= Constants.SNA_HEADER_SIZE) {
            SNAHeader header = new SNAHeader();
            System.arraycopy(in, 0, header.data, 0, Constants.SNA_HEADER_SIZE);
            return header;
        } else {
            throw new IllegalArgumentException("Too short byte array provided");
        }
    }

    public byte[] asByteArray() {
        return data;
    }

    public byte[] getWord(int offset) {
        return Arrays.copyOfRange(data, offset, 2);
    }

    public int getRegisterValue(int offset) {
        return data[offset] & 0xFF |  ((data[offset + 1] << 8) & 0xFF00);
    }

    public void setWord(int offset, byte[] value) {
        data[offset] = value[0];
        data[offset + 1] = value[1];
    }

    public void setWordSwapped(int offset, byte high, byte low) {
        data[offset] = low;
        data[offset + 1] = high;
    }

    public void setWord(int offset, byte low, byte high) {
        data[offset] = low;
        data[offset + 1] = high;
    }

    public void setByte(int offset, byte value) {
        data[offset] = value;
    }

    public byte getByte(int offset) {
        return data[offset];
    }

    public int getValue(int offset) {
        return data[offset];
    }

    public boolean validate() {
        LOGGER.debug("Border Color: " + getByte(BORDER_COLOR)
                + ", SP: " + getRegisterValue(REG_SP)
                + ", IM: " + getByte(INTERRUPT_MODE));
        return getByte(BORDER_COLOR) < 8 &&
                getRegisterValue(REG_SP) >= 16384 &&
                VALID_INTERRUPT_MODES.contains(getValue(INTERRUPT_MODE));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SNAHeader [");
        for (byte value: data) {
            sb.append("0x").append(String.format("%02X", value)).append(" ");
        }
        sb.append(" ]");
        return sb.toString();
    }
}
