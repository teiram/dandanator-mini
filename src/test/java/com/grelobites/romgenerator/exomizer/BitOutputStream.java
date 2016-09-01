package com.grelobites.romgenerator.exomizer;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BitOutputStream extends ByteArrayOutputStream {

    private int bitBuffer;
    private long position;

    public BitOutputStream() {
        position = 0;
        bitBuffer = 1;
    }

    @Override
    public void write(int b)  {
        super.write(b);
        position += 1;
    }

    public long position() {
        return position;
    }

    public void writeBits(int count, int val) {
    /* this makes the bits appear in reversed
     * big endian order in the output stream */
        while (count-- > 0) {
            bitBuffer <<= 1;
            bitBuffer |= (val & 0x1);
            val >>= 1;
            if ((bitBuffer & 0x100) > 0) {
            /* full byte, flush it */
                write(bitBuffer & 0xff);
                bitBuffer = 1;
            }
        }
    }

    public void writeGammaCode(int code) {
        writeBits(1, 1);
        while (code-- > 0) {
            writeBits(1, 0);
        }
    }

}
