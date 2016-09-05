package com.grelobites.romgenerator.util;

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
    public static final int TR_DOS_ROM_MAPPED = (byte) 30;

}