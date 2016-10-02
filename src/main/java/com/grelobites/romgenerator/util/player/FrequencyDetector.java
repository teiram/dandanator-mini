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
import java.util.Optional;
import java.util.function.Consumer;

public class FrequencyDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrequencyDetector.class);
    private static final int DEFAULT_FFT_SIZE = 1024;
    private static final float DEFAULT_SAMPLE_RATE = 44100.0f;
    private static final int DEFAULT_LEVEL_THRESHOLD = 500;
    private static final int DEFAULT_CONSECUTIVE_DETECTIONS = 10;
    private static final int DEFAULT_TIMEOUT_MILLIS = 2000;


    final long timeoutMillis;
    final Consumer<Optional<Float>> frequencyConsumer;
    final float sampleRate;
    final int fftSize;
    final int levelThreshold;
    final int consecutiveDetections;

    public static class Builder {
        long timeoutMillis = DEFAULT_TIMEOUT_MILLIS;
        Consumer<Optional<Float>> frequencyConsumer;
        float sampleRate = DEFAULT_SAMPLE_RATE;
        int fftSize = DEFAULT_FFT_SIZE;
        int consecutiveDetections = DEFAULT_CONSECUTIVE_DETECTIONS;
        int levelThreshold = DEFAULT_LEVEL_THRESHOLD;

        public Builder withTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public Builder withFrequencyConsumer(Consumer<Optional<Float>> frequencyConsumer) {
            this.frequencyConsumer = frequencyConsumer;
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
            return new FrequencyDetector(frequencyConsumer, timeoutMillis, sampleRate,
                    fftSize, levelThreshold, consecutiveDetections);
        }

    }


    public FrequencyDetector(Consumer<Optional<Float>> frequencyConsumer, long timeoutMillis, float sampleRate,
                             int fftSize, int levelThreshold, int consecutiveDetections) {
        this.timeoutMillis = timeoutMillis;
        this.frequencyConsumer = frequencyConsumer;
        this.sampleRate = sampleRate;
        this.fftSize = fftSize;
        this.levelThreshold = levelThreshold;
        this.consecutiveDetections = consecutiveDetections;
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

    private boolean checkThreshold(ByteBuffer buffer, int samples) {
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
                if (checkThreshold(byteBuffer, numRead / 2)) {
                    int frequencyIndex = getDominantFrequency(buffer);
                    LOGGER.debug("Frequency is " + frequencyIndex);
                    if (frequencyIndex == lastFrequencyIndex) {
                        checkCount++;
                        if (checkCount >= consecutiveDetections) {
                            detectedFrequency = (sampleRate * frequencyIndex) / fftSize;
                            LOGGER.debug("Detected frequency " + detectedFrequency);
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
