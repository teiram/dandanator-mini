package com.grelobites.romgenerator.exomizer;

public class Match {
    private int offset;
    private int length;
    private Match next;

    public Match(Match next, int offset, int length) {
        this.length = length;
        this.offset = offset;
        this.next = next;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public Match getNext() {
        return next;
    }

    public void setNext(Match next) {
        this.next = next;
    }
}
