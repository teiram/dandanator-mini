package com.grelobites.romgenerator.fft;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SoundTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoundTests.class);

    @Test
    public void testSineWave() throws Exception {
        ToneInputStream is = new ToneInputStream(44100.0f, AudioSystem.NOT_SPECIFIED, 1.0, 9900.0);
        SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, is
                .getFormat(), ((int) is.getFrameLength()));
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(is.getFormat());
        line.start();

        int numRead = 0;
        byte[] buf = new byte[line.getBufferSize()];
        while ((numRead = is.read(buf, 0, buf.length)) >= 0) {
            int offset = 0;
            while (offset < numRead) {
                offset += line.write(buf, offset, numRead - offset);
            }
        }
        line.drain();
        line.stop();
    }

    private static int getDominantFrequency(byte[] data) {
        int samples = data.length / 2;
        Complex[] source = new Complex[samples];
        ByteBuffer buffer = ByteBuffer.wrap(data)
                .order(ByteOrder.BIG_ENDIAN);
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

        LOGGER.debug("DC value is " + result[0].abs() + ", max value ratio is " + maxValue / minValue
                + ", DC ratio is " + maxValue / result[0].abs());
        return freqIndex;
    }

    @Test
    public void recordTest() throws Exception {

        Thread thread = new Thread(() -> {
            try {
                testSineWave();
            } catch (Exception e) {
                LOGGER.debug("In testSineWave", e);
            }
        });
        thread.start();

        float sampleRate = 44100.0f;
        int fftSize = 1024; // 1024 temporal samples mean 1024 / 44100 seconds = 23.21 ms
        TargetDataLine line;
        AudioFormat format = //new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
               //44100.0f , 16, 1, 2, 44100.0f, false);
                new AudioFormat(sampleRate, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        byte[] buffer = new byte[fftSize * 2];  //16 bits per sample (2 bytes)
        int numRead;
        while ((numRead = line.read(buffer, 0, buffer.length)) > 0) {
            int frequencyIndex = getDominantFrequency(buffer);
            LOGGER.debug("Dominant frequency is " + ((sampleRate * frequencyIndex) / fftSize));
        }

    }

}
