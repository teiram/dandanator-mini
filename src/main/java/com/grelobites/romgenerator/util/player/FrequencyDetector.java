package com.grelobites.romgenerator.util.player;


import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
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
    private static final int FFT_SIZE = 1024;
    private static final float SAMPLE_RATE = 44100.0f;
    private static final int MINIMUM_LEVEL = 500;
    private static final int CONSECUTIVE_DETECTIONS = 20;

    final long timeoutMillis;
    final Consumer<Optional<Float>> frequencyConsumer;

    public FrequencyDetector(long timeoutMillis, Consumer<Optional<Float>> frequencyConsumer) {
        this.timeoutMillis = timeoutMillis;
        this.frequencyConsumer = frequencyConsumer;
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

    private static boolean checkThreshold(ByteBuffer buffer, int samples) {
        int maximum = 0;
        for (int i = 0; i < samples; i++) {
            short sample = buffer.getShort();
            if (Math.abs(sample) > maximum) {
                maximum = Math.abs(sample);
            }
        }
        return maximum > MINIMUM_LEVEL;
    }

    private void updateFrequency(Float frequency) {
        LOGGER.debug("Scheduled frequency update: " + frequency);
        Platform.runLater(() -> {
            LOGGER.debug("Detected frequency was " + frequency);
            frequencyConsumer.accept(Optional.ofNullable(frequency));
        });
    }

    public void runDetection()  {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        try (TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info)) {
            boolean noFrequencyDetected = true;
            long startTime = System.currentTimeMillis();
            line.open(format);
            line.start();
            byte[] buffer = new byte[FFT_SIZE * 2];  //16 bits per sample (2 bytes)
            int numRead;
            int checkCount = 0;
            int lastFrequencyIndex = -1;
            Float detectedFrequency = null;
            while ((numRead = line.read(buffer, 0, buffer.length)) > 0 &&
                    (System.currentTimeMillis() - startTime < timeoutMillis) &&
                    noFrequencyDetected) {
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN);
                if (checkThreshold(byteBuffer, numRead / 2)) {
                    int frequencyIndex = getDominantFrequency(buffer);
                    if (frequencyIndex == lastFrequencyIndex) {
                        checkCount++;
                        if (checkCount >= CONSECUTIVE_DETECTIONS) {
                            detectedFrequency = (SAMPLE_RATE * frequencyIndex) / FFT_SIZE;
                            noFrequencyDetected = false;
                        }
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
