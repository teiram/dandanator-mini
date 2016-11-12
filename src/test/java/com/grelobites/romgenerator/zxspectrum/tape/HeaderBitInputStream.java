package com.grelobites.romgenerator.zxspectrum.tape;

public class HeaderBitInputStream implements BitInputStream {
    private static final int HEADER_PULSE_LENGTH = 2168;
    private static final int SYNC_P0_TSTATES = 667;
    private static final int SYNC_P1_TSTATES = 735;


    private final int value;
    private int remaining;


    public HeaderBitInputStream(int value) {
        this.remaining = 0;
        this.value = value;
    }


    @Override
    public int read() {
        return remaining-- > 0 ? value : -1;
    }

    @Override
    public int skip(int value) {
        int returnValue = Math.min(value, remaining);
        remaining -= returnValue;
        return returnValue;
    }
}
