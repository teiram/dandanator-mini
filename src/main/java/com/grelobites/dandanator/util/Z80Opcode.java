package com.grelobites.dandanator.util;

public class Z80Opcode {

    public static final byte DI = (byte) 0xF3;          // DI
    public static final byte EI = (byte) 0xFB;          // EI
    public static final byte HALT = (byte) 0x76;        // HALT
    public static final byte IMH = (byte) 0xED;         // IM Header
    public static final byte IM0 = (byte) 70;           // IM0
    public static final byte IM1 = (byte) 86;           // IM1
    public static final byte IM2 = (byte) 94;           // IM2
    public static final byte NOP = (byte) 0;            // NOP
    public static final byte RET = (byte) 0xC9;         // RET
    public static final byte PUSH_HL = (byte) 0xE5;     // PUSH HL
    public static final byte POP_HL = (byte) 0xE1;      // POP HL
}
