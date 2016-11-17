package com.grelobites.romgenerator.zxspectrum.tape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class BlockBitInputStream implements BitInputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockBitInputStream.class);

    private static final int ZERO_DURATION_STANDARD = 748;
    private static final int ONE_DURATION_STANDARD = 1496;

    private InputStream data;
    private Queue<Edge> nextValues;

    public BlockBitInputStream(InputStream data) throws IOException {
        LOGGER.debug("Creating new BlockBitInputStream of size " + data.available());
        this.data = data;
        this.nextValues = new LinkedList<>();
        calculateNextValues();
    }

    private static void fillSymbolValues(Queue<Edge> target, boolean symbol) {
        int length = symbol ? ONE_DURATION_STANDARD : ZERO_DURATION_STANDARD;
        target.add(new Edge(true, length));
        target.add(new Edge(false, length));
    }

    private void calculateNextValues() {
        try {
            int value = data.read();
            if (value > -1) {
                int mask = 0x80;
                while (mask != 0) {
                    fillSymbolValues(nextValues, (mask & value) != 0);
                    mask >>= 1;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("In calculateNextValues", e);
        }
    }

    @Override
    public int read() {
        try {
            if (nextValues.isEmpty()) {
                calculateNextValues();
            }
            if (!nextValues.isEmpty()) {
                Edge currentEdge = nextValues.peek();
                int value = currentEdge.getValue();
                if (currentEdge.getTstates() == 0) {
                    nextValues.remove();
                }
                return value;
            } else {
                return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int skip(int value) {
        int remaining = value;
        if (nextValues.isEmpty()) {
            calculateNextValues();
        }
        while (!nextValues.isEmpty() && remaining > 0) {
            Edge currentEdge = nextValues.peek();
            if (currentEdge.getTstates() <= remaining) {
                remaining -= currentEdge.getTstates();
                nextValues.remove();
            } else {
                currentEdge.setTstates(currentEdge.getTstates() - remaining);
                remaining = 0;
            }
            if (nextValues.isEmpty()) {
                calculateNextValues();
            }
        }
        return value - remaining;
    }
}
