package com.grelobites.romgenerator.media;


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
    int channels() { return bits & 3; }

}
