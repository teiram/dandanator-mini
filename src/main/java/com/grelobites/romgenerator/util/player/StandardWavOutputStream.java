package com.grelobites.romgenerator.util.player;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class StandardWavOutputStream extends FilterOutputStream {

    private static final int SPECTRUM_CLOCK = 3500000;
    private static final int WAV_HEADER_LENGTH = 44;
    private static final int HEADER_PULSE_LENGTH = 2168;
    private static final int SYNC_P0_LENGTH = 667;
    private static final int SYNC_P1_LENGTH = 735;

    private byte[] getWavHeader(int wavDataLength) {
        int byteRate = format.getSampleRate() * format.getChannelType().channels();
        int sampleRate = format.getSampleRate();
        short numChannels = (short) format.getChannelType().channels();

        ByteBuffer buffer = ByteBuffer.allocate(WAV_HEADER_LENGTH)
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

    private List<ByteArrayOutputStream> buffers;
    private ByteArrayOutputStream currentBuffer;
    private ByteArrayOutputStream wavStream;
    private StandardWavOutputFormat format;

    private int tStatesToSamples(int tstates) {
        int upper = tstates * format.getSampleRate();
        return upper / SPECTRUM_CLOCK + (upper % SPECTRUM_CLOCK == 0 ? 0 : 1);
    }

    private int getLowValue() {
        return format.isReversePhase() ? format.getHighValue() : format.getLowValue();
    }

    private int getHighValue() {
        return format.isReversePhase() ? format.getLowValue() : format.getHighValue();
    }

    private void writeSamples(int samples, boolean high) throws IOException {
        int highValue = getHighValue();
        int lowValue = getLowValue();
        for (int i = 0; i < samples; i++) {
            wavStream.write(high ? highValue : lowValue);
            if (format.getChannelType() == ChannelType.STEREO) {
                wavStream.write(high ? highValue: lowValue);
            } else if (format.getChannelType() == ChannelType.STEREOINV) {
                wavStream.write(high ? lowValue: highValue);
            }
        }
    }

    private void writeBit(boolean bit) throws IOException {
        int pulseSamples = tStatesToSamples(bit ? format.getOneDurationTStates() :
                format.getZeroDurationTStates());
        writeSamples(pulseSamples, true);
        writeSamples(pulseSamples, false);
    }

    private void writeByte(int value) throws IOException {
        value &= 0xff;
        int mask = 0x80;
        for (int i = 0; i < 8; i++) {
            writeBit((value & mask) != 0);
            mask >>= 1;
        }
    }

    public StandardWavOutputStream(OutputStream out, StandardWavOutputFormat format) {
        super(out);
        this.format = format;
        buffers = new ArrayList<>();
        currentBuffer = new ByteArrayOutputStream();
        buffers.add(currentBuffer);
    }

    private void writeHeader() throws IOException {
        int headerSamples = new Double(format.getPilotDurationMillis() * format.getSampleRate() / 1000).intValue();
        int pulseSamples = tStatesToSamples(HEADER_PULSE_LENGTH);
        int cycles = headerSamples / (pulseSamples * 2);
        for (int i = 0; i < cycles; i++) {
            writeSamples(pulseSamples, true);
            writeSamples(pulseSamples, false);
        }
    }

    private void writeLeadOut() throws IOException {
        int leadOutSamples = new Double(format.getLeadOutDurationMillis() * format.getSampleRate() / 1000).intValue();
        writeSamples(leadOutSamples, true);
    }

    private void writeSync() throws IOException {
        writeSamples(tStatesToSamples(SYNC_P0_LENGTH), true);
        writeSamples(tStatesToSamples(SYNC_P1_LENGTH), false);
    }

    @Override
    public void write(int b) throws IOException {
        currentBuffer.write(b);
    }

    public void nextBlock() {
        currentBuffer = new ByteArrayOutputStream();
        buffers.add(currentBuffer);
    }

    @Override
    public void flush() throws IOException {
        wavStream = new ByteArrayOutputStream();

        for (ByteArrayOutputStream buffer: buffers) {
            if (buffer.size() > 0) {
                writeHeader();
                writeSync();

                for (byte aData : buffer.toByteArray()) {
                    writeByte(Byte.toUnsignedInt(aData));
                }
            }
        }
        writeLeadOut();
        wavStream.flush();
        out.write(getWavHeader(wavStream.size()));
        out.write(wavStream.toByteArray());
        out.flush();
        wavStream.reset();
    }
}
