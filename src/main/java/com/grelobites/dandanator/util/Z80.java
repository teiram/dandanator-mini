package com.grelobites.dandanator.util;

/**
 * Created by mteira on 17/6/16.
 */
public class Z80 {

    public static final byte DI = (byte) 243;      //DI Instruction Z80
    public static final byte EI = (byte) 251;      // EI Instruction Z80
    public static final byte HALT = (byte) 118;    // HALT Instruction Z80
    public static final byte IMH = (byte) 237;     // IM Header Instruction Z80
    public static final byte IM0 = (byte) 70;      // IM0 Instruction Z80
    public static final byte IM1 = (byte) 86;      // IM1 Instruction Z80
    public static final byte IM2 = (byte) 94;      // IM2 Instruction Z80
    public static final byte NOP = (byte) 0;       // NOP Instruction Z80
    public static final byte RET = (byte) 201;     // RET Instruction Z80
}
