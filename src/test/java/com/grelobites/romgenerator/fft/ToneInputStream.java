package com.grelobites.romgenerator.fft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ToneInputStream extends AudioInputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToneInputStream.class);
    private static final ByteArrayInputStream BOS = new ByteArrayInputStream(new byte[0]);
    private static final boolean BIG_ENDIAN = false;
    private long remainingFrames;
    private int bufferPosition;
    private byte[] soundData;

    private static AudioFormat getAudioFormat(float sampleRate, float frameRate) {
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, 16, 2, 4, frameRate, BIG_ENDIAN);
    }

    public ToneInputStream(float frameRate, long length, double amplitude, double frequency) {
        super(BOS, getAudioFormat(frameRate, frameRate), length);

        remainingFrames = length;
        amplitude = (amplitude * Math.pow(2, getFormat().getSampleSizeInBits() - 1));
        // length of one period in frames
        int periodLengthInFrames = (int) Math.round(getFormat().getFrameRate() / frequency);
        int bufferLength = periodLengthInFrames * getFormat().getFrameSize();
        ByteBuffer buffer = ByteBuffer.allocate(bufferLength)
                .order(format.isBigEndian() ?
                        ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        for (int frame = 0; frame < periodLengthInFrames; frame++) {
            float point = frame / (float) periodLengthInFrames;
            double value = Math.sin(point * 2.0 * Math.PI);
            Long nValue = Math.round(value * amplitude);
            buffer.putShort(nValue.shortValue());
            buffer.putShort(nValue.shortValue());
        }
        soundData = buffer.array();
        bufferPosition = 0;
    }

    @Override
    public int read() throws IOException {
        throw new IOException("Frame size is too big");
    }

    @Override
    public int available() {
        int	available = 0;
        if (remainingFrames == AudioSystem.NOT_SPECIFIED) {
            available = Integer.MAX_VALUE;
        } else {
            long availableBytes = remainingFrames * getFormat().getFrameSize();
            available = (int) Math.min(availableBytes, (long) Integer.MAX_VALUE);
        }
        return available;
    }

    @Override
    public int read(byte[] data, int nOffset, int nLength)
            throws IOException {
        if (nLength % getFormat().getFrameSize() != 0) {
            throw new IOException("length must be an integer multiple of frame size");
        }

        int	constrainedLength = Math.min(available(), nLength);
        int	remainingLength = constrainedLength;
        while (remainingLength > 0) {
            int	bytesToCopy = soundData.length - bufferPosition;
            bytesToCopy = Math.min(bytesToCopy, remainingLength);
            System.arraycopy(soundData, bufferPosition, data, nOffset, bytesToCopy);
            remainingLength -= bytesToCopy;
            nOffset += bytesToCopy;
            bufferPosition = (bufferPosition + bytesToCopy) % soundData.length;
        }
        int	framesRead = constrainedLength / getFormat().getFrameSize();
        if (remainingFrames != AudioSystem.NOT_SPECIFIED) {
            remainingFrames -= framesRead;
        }
        return remainingFrames == 0 ? -1 : constrainedLength;
    }

}
