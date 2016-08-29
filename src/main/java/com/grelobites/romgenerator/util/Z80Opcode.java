package com.grelobites.romgenerator.util;

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
    public static final byte DEC_HL = (byte) 0x2B;      // DEC HL
    public static final byte DEC_SP = (byte) 0x3B;

    public static final byte LD_IX_NN_0 = (byte) 0xDD;       // LD IX, (nn)
    public static final byte LD_IX_NN_1 = (byte) 0x2A;

    public static final byte LD_SP_NN_0 = (byte) 0xED;      // LD SP, (nn)
    public static final byte LD_SP_NN_1 = (byte) 0x7B;

    public static final byte LD_NN_A = (byte) 0x32; //LD (nn), A


    private static byte lowByte(int addr) {
        return (byte) (addr & 0xff);
    }

    private static byte highByte(int addr) {
        return (byte) ((addr >> 8) & 0xff);
    }

    public static byte[] LD_IX_NN(int addr) {
        byte[] code = new byte[4];
        code[0] = LD_IX_NN_0;
        code[1] = LD_IX_NN_1;
        code[2] = lowByte(addr);
        code[3] = highByte(addr);
        return code;
    }

    public static byte[] LD_SP_NN(int addr) {
        byte[] code = new byte[4];
        code[0] = LD_SP_NN_0;
        code[1] = LD_SP_NN_1;
        code[2] = lowByte(addr);
        code[3] = highByte(addr);
        return code;
    }

    public static byte[] LD_NN_A(int addr) {
        byte[] code = new byte[3];
        code[0] = LD_NN_A;
        code[1] = lowByte(addr);
        code[2] = highByte(addr);
        return code;
    }
}
