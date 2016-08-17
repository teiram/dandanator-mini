package com.grelobites.romgenerator.util;

import com.grelobites.romgenerator.Constants;
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


    public static final int REG_I = 0;
    public static final int REG_HL_alt = 1;
    public static final int REG_DE_alt = 3;
    public static final int REG_BC_alt = 5;
    public static final int REG_AF_alt = 7;
    public static final int REG_HL = 9;
    public static final int REG_DE = 11;
    public static final int REG_BC = 13;
    public static final int REG_IY = 15;
    public static final int REG_IX = 17;
    public static final int INTERRUPT_ENABLE = (byte) 19;
    public static final int REG_R = 20;
    public static final int REG_AF = 21;
    public static final int REG_SP = 23;
    public static final int INTERRUPT_MODE = (byte) 25;
    public static final int BORDER_COLOR = (byte) 26;
    public static final int REG_PC = (byte) 27;
    public static final int PORT_7FFD = (byte) 29;
    public static final int TRDOS_ROM_MAPPED = (byte) 30;

    private static final List<Integer> VALID_INTERRUPT_MODES = Arrays.asList(new Integer[] {0, 1, 2});
    private byte[] data;

    public SNAHeader(int size) {
        data = new byte[size];
    }

    public static SNAHeader from48kSNAGameByteArray(byte[] in) {
        SNAHeader header = new SNAHeader(Constants.SNA_HEADER_SIZE);
        System.arraycopy(in, 0, header.data, 0, Constants.SNA_HEADER_SIZE);
        return header;
    }

    public static SNAHeader from128kSNAGameByteArray(byte[] in) {
        SNAHeader header = new SNAHeader(Constants.SNA_EXTENDED_HEADER_SIZE);
        System.arraycopy(in, 0, header.data, 0, Constants.SNA_HEADER_SIZE);
        int extendedHeaderOffset = Constants.SNA_HEADER_SIZE + Constants.SLOT_SIZE * 3;
        header.setWord(REG_PC, in[extendedHeaderOffset], in[extendedHeaderOffset + 1]);
        header.setByte(PORT_7FFD, in[extendedHeaderOffset + 2]);
        header.setByte(TRDOS_ROM_MAPPED, in[extendedHeaderOffset + 3]);
        return header;
    }

    public static SNAHeader fromInputStream(InputStream is, int size) throws IOException {
        SNAHeader header = new SNAHeader(size);
        is.read(header.data);
        return header;
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
        return getRegisterValue(REG_SP) >= 16384 &&
                VALID_INTERRUPT_MODES.contains(getValue(INTERRUPT_MODE));
    }

    @Override
    public String toString() {
        return Util.dumpAsHexString(data);
    }
}
