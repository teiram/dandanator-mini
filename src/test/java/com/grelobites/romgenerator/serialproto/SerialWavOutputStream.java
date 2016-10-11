package com.grelobites.romgenerator.serialproto;

import com.grelobites.romgenerator.util.player.ChannelType;
import com.grelobites.romgenerator.util.player.CompressedWavOutputFormat;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SerialWavOutputStream extends FilterOutputStream {

    private static final int HEADER_LENGTH = 44;
    private static final int LEAD_LENGTH = 1000;
    private static final int SAMPLE_LENGTH = 10;
    private static final int START_BIT = 0;
    private static final int STOP_BIT = 0;

    private static final int LOW_VALUE = 0x40;
    private static final int HIGH_VALUE = 0xc0;

    private byte[] getWavHeader(int wavDataLength) {
        int byteRate = format.getSampleRate() * format.getChannelType().channels();
        int sampleRate = format.getSampleRate();
        short numChannels = (short) format.getChannelType().channels();

        ByteBuffer buffer = ByteBuffer.allocate(HEADER_LENGTH)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put("RIFF".getBytes())                                 //ChunkID
                .putInt(wavDataLength + 36)                             //ChunkSize
                .put("WAVE".getBytes())                                 //Format
                .put("fmt ".getBytes())                                 //Subchunk1ID
                .putInt(0x00000010)                                     //Subchunk1Size (16 for PCM)
                .putShort((short) 1)                                    //AudioFormat 1=PCM
                .putShort(numChannels)                                  //NumChannels
                .putInt(sampleRate)                                     //SampleRate
                .putInt(byteRate)                                       //ByteRate
                .putShort(numChannels)                                  //Block align
                .putShort((short) 8)                                    //Bits per sample
                .put("data".getBytes())                                 //Subchunk2ID
                .putInt(wavDataLength);                                 //Subchunk2Size
        return buffer.array();
    }

    private boolean initialBit;
    private ByteArrayOutputStream buffer;
    private ByteArrayOutputStream wavStream;
    private CompressedWavOutputFormat format;

    private void writeBit(int bit) throws IOException {
        for (int i = 0; i < SAMPLE_LENGTH; i++) {
            wavStream.write(bit != 0 ? HIGH_VALUE : LOW_VALUE);
            if (format.getChannelType() == ChannelType.STEREO) {
                wavStream.write(bit != 0 ? HIGH_VALUE : LOW_VALUE);
            } else if (format.getChannelType() == ChannelType.STEREOINV) {
                wavStream.write(bit != 0 ? LOW_VALUE : HIGH_VALUE);
            }
        }
    }

    private void writeValue(int value) throws IOException {
        writeBit(START_BIT);
        int mask = 0x01;
        for (int i = 0; i < 8; i++) {
            writeBit(value & mask);
            mask <<= 1;
        }
        writeBit(STOP_BIT);
    }

    public SerialWavOutputStream(OutputStream out, CompressedWavOutputFormat format) {
        super(out);
        this.format = format;
        this.buffer = new ByteArrayOutputStream();
    }

    @Override
    public void write(int b) throws IOException {
        buffer.write(b);
    }

    @Override
    public void flush() throws IOException {
        byte[] data = buffer.toByteArray();
        wavStream = new ByteArrayOutputStream();
        int cheksum = 0;
        for (int i = 0; i < 100; i++) {
            writeBit(0);
        }
        for (byte aData : data) {
            int value = Byte.toUnsignedInt(aData);
            writeValue(value);
        }

        wavStream.flush();
        out.write(getWavHeader(wavStream.size()));
        out.write(wavStream.toByteArray());
        out.flush();
        buffer.reset();
        wavStream.reset();
    }
}
