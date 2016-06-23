package com.grelobites.dandanator.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

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

    private byte[] data = new byte[27];


    public static SNAHeader fromByteArray(ByteArrayInputStream is) throws IOException {
        SNAHeader snaHeader = new SNAHeader();
        is.read(snaHeader.data);
        return snaHeader;
    }

    public byte[] asByteArray() {
        return data;
    }

    public byte[] getWord(int offset) {
        return Arrays.copyOfRange(data, offset, 2);
    }

    public int getRegisterValue(int offset) {
        return data[offset] + (data[offset + 1] << 256);
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
}
