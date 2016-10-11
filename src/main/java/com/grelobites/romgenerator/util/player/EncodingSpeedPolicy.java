package com.grelobites.romgenerator.util.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncodingSpeedPolicy {
    private static final Logger LOGGER = LoggerFactory.getLogger(EncodingSpeedPolicy.class);

    private static final int MIN_SPEED = 7;
    private static final int MAX_SPEED = 0;
    private static final int STANDARD_SPEED = 8;

    private static final int DEFAULT_ACCUMULATIVE_SUCCESSES_THRESHOLD = 2;
    private static final int DEFAULT_ACCUMULATIVE_FAILURES_THRESHOLD = 2;

    private int maximumEncodingSpeed;
    private int encodingSpeed;
    private int consecutiveFailures;
    private int accumulativeFailuresThreshold = DEFAULT_ACCUMULATIVE_FAILURES_THRESHOLD;
    private int accumulativeSuccessesThreshold = DEFAULT_ACCUMULATIVE_SUCCESSES_THRESHOLD;
    private int consecutiveSuccesses;

    public int getAccumulativeFailuresThreshold() {
        return accumulativeFailuresThreshold;
    }

    public void setAccumulativeFailuresThreshold(int accumulativeFailuresThreshold) {
        this.accumulativeFailuresThreshold = accumulativeFailuresThreshold;
    }

    public int getAccumulativeSuccessesThreshold() {
        return accumulativeSuccessesThreshold;
    }

    public void setAccumulativeSuccessesThreshold(int accumulativeSuccessesThreshold) {
        this.accumulativeSuccessesThreshold = accumulativeSuccessesThreshold;
    }

    public EncodingSpeedPolicy(int maximumEncodingSpeed) {
        LOGGER.debug("Creating EncodingSpeedPolicy with maximum speed " + maximumEncodingSpeed);
        this.maximumEncodingSpeed = maximumEncodingSpeed;
        this.encodingSpeed = maximumEncodingSpeed;
    }

    public void reset(int maximumEncodingSpeed) {
        consecutiveFailures = consecutiveSuccesses = 0;
        this.maximumEncodingSpeed = maximumEncodingSpeed;
        encodingSpeed = maximumEncodingSpeed;
    }

    private void reduceSpeed() {
        if (encodingSpeed < MIN_SPEED) {
            encodingSpeed++;
            LOGGER.debug("Reduced speed to " + encodingSpeed);
        }
    }

    private void incrementSpeed() {
        if (encodingSpeed > maximumEncodingSpeed) {
            encodingSpeed--;
            LOGGER.debug("Increased speed to " + encodingSpeed);
        }
    }

    public int getEncodingSpeed() {
        if (consecutiveFailures >= accumulativeFailuresThreshold) {
            reduceSpeed();
            consecutiveFailures = 0;
        } else if (consecutiveSuccesses >= accumulativeSuccessesThreshold) {
            incrementSpeed();
            consecutiveSuccesses = 0;
        }
        return encodingSpeed;
    }

    public void onFailure() {
        consecutiveFailures++;
        consecutiveSuccesses = 0;
    }

    public void onSuccess() {
        consecutiveSuccesses++;
        consecutiveFailures = 0;
    }

    public boolean useStandardEncoding() {
        return encodingSpeed == STANDARD_SPEED;
    }
}
