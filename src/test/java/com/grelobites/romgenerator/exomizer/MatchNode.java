package com.grelobites.romgenerator.exomizer;

public class MatchNode {
    private final int index;
    private MatchNode next;

    public MatchNode(int index) {
        this.index = index;
        this.next = null;
    }

    public void setNext(MatchNode next) {
        this.next = next;
    }

    public MatchNode getNext() {
        return next;
    }

    public int getIndex() {
        return index;
    }
}
