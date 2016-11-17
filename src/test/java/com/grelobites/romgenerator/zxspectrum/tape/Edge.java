package com.grelobites.romgenerator.zxspectrum.tape;

public class Edge {
    private boolean value;
    private long tstates;

    public Edge(boolean value, long tstates) {
        this.value = value;
        this.tstates = tstates;
    }

    public boolean isValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public int getValue() {
        tstates--;
        return value ? 1 : 0;
    }

    public long getTstates() {
        return tstates;
    }

    public void setTstates(long tstates) {
        this.tstates = tstates;
    }
}
