package com.grelobites.romgenerator.util.player;


import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class FrequencyDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrequencyDetector.class);
    private static final int DEFAULT_FFT_SIZE = 1024;
    private static final float DEFAULT_SAMPLE_RATE = 44100.0f;
    private static final int DEFAULT_LEVEL_THRESHOLD = 500;
    private static final int DEFAULT_CONSECUTIVE_DETECTIONS = 10;
    private static final int DEFAULT_TIMEOUT_MILLIS = 2000;
    private static final float DEFAULT_FREQUENCY_TOLERANCE = 100;
    public static final float SUCCESS_FREQUENCY = 4000;
    public static final float ERROR_FREQUENCY = 5000;
    private static final List<Float> DEFAULT_EXPECTED_FREQUENCIES = Arrays.asList(SUCCESS_FREQUENCY, ERROR_FREQUENCY);

    final long timeoutMillis;
    final Consumer<Optional<Float>> frequencyConsumer;
    final float sampleRate;
    final int fftSize;
    final int levelThreshold;
    final int consecutiveDetections;
    final List<Float> expectedFrequencies;
    final float frequencyTolerance;

    public static class Builder {
        long timeoutMillis = DEFAULT_TIMEOUT_MILLIS;
        Consumer<Optional<Float>> frequencyConsumer;
        float sampleRate = DEFAULT_SAMPLE_RATE;
        int fftSize = DEFAULT_FFT_SIZE;
        int consecutiveDetections = DEFAULT_CONSECUTIVE_DETECTIONS;
        int levelThreshold = DEFAULT_LEVEL_THRESHOLD;
        List<Float> expectedFrequencies = DEFAULT_EXPECTED_FREQUENCIES;
        float frequencyTolerance = DEFAULT_FREQUENCY_TOLERANCE;

        public Builder withTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public Builder withFrequencyConsumer(Consumer<Optional<Float>> frequencyConsumer) {
            this.frequencyConsumer = frequencyConsumer;
            return this;
        }

        public Builder withExpectedFrequencies(List<Float> expectedFrequencies) {
            this.expectedFrequencies = expectedFrequencies;
            return this;
        }

        public Builder withFrequencyTolerance(float frequencyTolerance) {
            this.frequencyTolerance = frequencyTolerance;
            return this;
        }

        public Builder withSampleRate(float sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public Builder withFftSize(int fftSize) {
            this.fftSize = fftSize;
            return this;
        }

        public Builder withLevelThreshold(int levelThreshold) {
            this.levelThreshold = levelThreshold;
            return this;
        }

        public Builder withConsecutiveDetections(int consecutiveDetections) {
            this.consecutiveDetections = consecutiveDetections;
            return this;
        }

        public FrequencyDetector build() {
            return new FrequencyDetector(frequencyConsumer, timeoutMillis,
                    expectedFrequencies, frequencyTolerance,
                    sampleRate,
                    fftSize, levelThreshold, consecutiveDetections);
        }

    }


    public FrequencyDetector(Consumer<Optional<Float>> frequencyConsumer, long timeoutMillis,
                             List<Float> expectedFrequencies,
                             float frequencyTolerance,
                             float sampleRate,
                             int fftSize, int levelThreshold, int consecutiveDetections) {
        this.timeoutMillis = timeoutMillis;
        this.frequencyConsumer = frequencyConsumer;
        this.sampleRate = sampleRate;
        this.fftSize = fftSize;
        this.levelThreshold = levelThreshold;
        this.consecutiveDetections = consecutiveDetections;
        this.expectedFrequencies = expectedFrequencies;
        this.frequencyTolerance = frequencyTolerance;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void start() {
        new Thread(this::runDetection).start();
    }

    private int getDominantFrequency(byte[] data) {
        int samples = data.length / 2;
        Complex[] source = new Complex[samples];
        ByteBuffer buffer = ByteBuffer.wrap(data)
                .order(ByteOrder.BIG_ENDIAN);
        buffer.rewind();
        for (int i = 0; i < samples; i++) {
            source[i] = new Complex(buffer.getShort(), 0);
        }
        Complex[] result = FFT.fft(source);
        double maxValue = 0.0;
        double minValue = Double.MAX_VALUE;
        int freqIndex = 0;
        for (int j = 0; j <= result.length / 2; j++) {
            double value = result[j].abs();
            if (value > maxValue) {
                maxValue = value;
                freqIndex = j;
            }
            if (value < minValue) {
                minValue = value;
            }
        }

        return freqIndex;
    }

    private boolean isLevelThresholdReached(ByteBuffer buffer, int samples) {
        int maximum = 0;
        for (int i = 0; i < samples; i++) {
            short sample = buffer.getShort();
            if (Math.abs(sample) > maximum) {
                maximum = Math.abs(sample);
            }
        }
        return maximum > levelThreshold;
    }

    private void updateFrequency(Float frequency) {
        LOGGER.debug("Scheduled frequency update: " + frequency);
        Platform.runLater(() -> {
            LOGGER.debug("Detected frequency was " + frequency);
            frequencyConsumer.accept(Optional.ofNullable(frequency));
        });
    }

    private Optional<Float> getDetectedFrequency(int frequencyIndex) {
        float detectedFrequency = (sampleRate * frequencyIndex) / fftSize;
        for (float expectedFrequency : expectedFrequencies) {
            if (Math.abs(detectedFrequency - expectedFrequency) < frequencyTolerance) {
                return Optional.of(expectedFrequency);
            }
        }
        return Optional.empty();
    }

    public void runDetection()  {
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        try (TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info)) {
            boolean detectedFrequencyExtinguished = false;
            long startTime = System.currentTimeMillis();
            line.open(format);
            line.start();
            byte[] buffer = new byte[fftSize * 2];  //16 bits per sample (2 bytes)
            int numRead;
            int checkCount = 0;
            int lastFrequencyIndex = -1;
            Float detectedFrequency = null;
            while ((numRead = line.read(buffer, 0, buffer.length)) > 0 &&
                    (System.currentTimeMillis() - startTime < timeoutMillis) &&
                    !detectedFrequencyExtinguished) {
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN);
                if (isLevelThresholdReached(byteBuffer, numRead / 2)) {
                    int frequencyIndex = getDominantFrequency(buffer);
                    LOGGER.debug("Dominant frequency index is " + frequencyIndex);
                    if (frequencyIndex == lastFrequencyIndex) {
                        Optional<Float> detectedFrequencyOptional = getDetectedFrequency(frequencyIndex);
                        if (detectedFrequencyOptional.isPresent()) {
                            checkCount++;
                            if (checkCount >= consecutiveDetections) {
                                detectedFrequency = detectedFrequencyOptional.get();
                                LOGGER.debug("Detected frequency " + detectedFrequency);
                            }
                        }
                    } else if (checkCount >= consecutiveDetections) {
                        detectedFrequencyExtinguished = true;
                        LOGGER.debug("Detected Frequency was extinguished");
                    } else {
                        detectedFrequency = null;
                        checkCount = 0;
                    }
                    lastFrequencyIndex = frequencyIndex;
                }
            }
            updateFrequency(detectedFrequency);
        } catch (Exception e) {
            LOGGER.error("In runDetection", e);
        }
    }

}
