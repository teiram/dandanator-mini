package com.grelobites.romgenerator.zxspectrum.tape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;

public class HeaderBitInputStream implements BitInputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderBitInputStream.class);
    private static final int HEADER_PULSE_LENGTH = 2168;
    private static final int HEADER_LOOPS = 2048;
    private static final int SYNC_P0_TSTATES = 667;
    private static final int SYNC_P1_TSTATES = 735;

    private Queue<Edge> nextValues;

    public HeaderBitInputStream() {
        nextValues = new LinkedList<>();
        for (int i = 0; i < HEADER_LOOPS; i++) {
            nextValues.add(new Edge(true, HEADER_PULSE_LENGTH));
            nextValues.add(new Edge(false, HEADER_PULSE_LENGTH));
        }
        nextValues.add(new Edge(true, SYNC_P0_TSTATES));
        nextValues.add(new Edge(false, SYNC_P1_TSTATES));
    }

    @Override
    public int read() {
        if (!nextValues.isEmpty()) {
            int value = nextValues.peek().getValue();
            if (nextValues.peek().getTstates() == 0) {
                nextValues.remove();
            }
            return value;
        }
        return -1;
    }

    @Override
    public int skip(int value) {
        int remaining = value;
        while (!nextValues.isEmpty() && remaining > 0) {
            Edge currentEdge = nextValues.peek();
            if (currentEdge.getTstates() <= remaining) {
                remaining -= currentEdge.getTstates();
                nextValues.remove();
            } else {
                currentEdge.setTstates(currentEdge.getTstates() - remaining);
                remaining = 0;
            }
        }
        return value - remaining;
    }
}
