package com.grelobites.romgenerator.util.player;


public enum ChannelType {
    MONO(1),
    STEREO(2),
    STEREOINV(6);

    private int bits;
    ChannelType(int bits) {
        this.bits = bits;
    }
    int bits() {
        return bits;
    }
    public int channels() { return bits & 3; }

}
